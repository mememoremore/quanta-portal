#!/usr/bin/env python3
"""通过 ECS API 调用云助手在指定实例上执行脚本（无 aliyun CLI 环境的替代）。

用法:
  python run-cloud-assistant.py probe                              # 验证凭证与 API 权限
  python run-cloud-assistant.py invoke <script.sh> [name]          # 发送命令，打印 InvokeId
        [--param KEY=VALUE ...]                                    # 自定义参数，脚本内用 {{KEY}} 占位
  python run-cloud-assistant.py wait <InvokeId> [timeout_s]        # 轮询至结束并打印结果

凭证读取 %USERPROFILE%\\.aliyun\\credentials 的 access_key_id/access_key_secret，
实例与地域读取同目录 deploy.env.local（ECS_INSTANCE_ID / ECS_REGION）。
密钥只读入内存，不打印。

签名: ACS3-HMAC-SHA256（阿里云当前推荐签名协议）。
网络安全: 仅 https；域名白名单 ecs.<region>.aliyuncs.com；解析后 IP 拒绝
私网/环回/链路本地/保留地址；禁止重定向；连接钉住已校验 IP 以防 DNS rebinding。
"""
import base64
import hashlib
import hmac
import http.client
import ipaddress
import json
import os
import re
import socket
import ssl
import sys
import time
import urllib.parse
import uuid
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
HOST_RE = re.compile(r"^ecs\.[a-z0-9-]+\.aliyuncs\.com$")


def parse_env(path, keys):
    values = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.replace("​", "").strip()
        if not line or line.startswith("#") or line.startswith("[") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        if k.strip() in keys:
            values[k.strip()] = v.strip()
    return values


def load_config():
    cred_path = Path.home() / ".aliyun" / "credentials"
    env = parse_env(HERE / "deploy.env.local", {"ECS_INSTANCE_ID", "ECS_REGION", "ALIYUN_CREDENTIALS_FILE"})
    if env.get("ALIYUN_CREDENTIALS_FILE"):
        cred_path = Path(os.path.expandvars(env["ALIYUN_CREDENTIALS_FILE"]))
    cred = parse_env(cred_path, {"access_key_id", "access_key_secret"})
    missing = [k for k in ("access_key_id", "access_key_secret") if not cred.get(k)]
    if missing:
        sys.exit(f"!! 凭证缺失: {missing}")
    instance = env.get("ECS_INSTANCE_ID")
    if not instance:
        sys.exit("!! deploy.env.local 缺少 ECS_INSTANCE_ID")
    region = env.get("ECS_REGION") or "cn-shanghai"
    return cred["access_key_id"], cred["access_key_secret"], instance, region


class PinnedHTTPSConnection(http.client.HTTPSConnection):
    """连接到调用前已通过安全校验的 IP，SNI/证书校验仍用原域名。"""

    def __init__(self, host, ip, **kw):
        super().__init__(host, **kw)
        self._pinned_ip = ip

    def connect(self):
        sock = socket.create_connection((self._pinned_ip, self.port), self.timeout)
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock = self._context.wrap_socket(sock, server_hostname=self.host)


def validate_host(host):
    if not HOST_RE.match(host):
        sys.exit(f"!! 域名不在白名单内: {host}")
    infos = socket.getaddrinfo(host, 443, type=socket.SOCK_STREAM)
    ips = sorted({ai[4][0] for ai in infos})
    if not ips:
        sys.exit(f"!! 域名解析失败: {host}")
    for ip_text in ips:
        ip = ipaddress.ip_address(ip_text)
        if (ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved
                or ip.is_multicast or ip.is_unspecified):
            sys.exit(f"!! 解析到非公网地址，拒绝请求: {host} -> {ip}")
    return host, ips[0]


def call_api(ak, sk, region, params):
    host, ip = validate_host(f"ecs.{region}.aliyuncs.com")
    payload_hash = hashlib.sha256(b"").hexdigest()
    headers = {
        "host": host,
        "x-acs-action": params["Action"],
        "x-acs-content-sha256": payload_hash,
        "x-acs-date": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "x-acs-signature-nonce": str(uuid.uuid4()),
        "x-acs-version": "2014-05-26",
    }
    signed_names = sorted(headers)
    query = "&".join(
        f"{urllib.parse.quote(str(k), safe='-_.~')}={urllib.parse.quote(str(v), safe='-_.~')}"
        for k, v in sorted(params.items())
    )
    canonical_headers = "".join(f"{name}:{headers[name]}\n" for name in signed_names)
    canonical_request = "\n".join(["GET", "/", query, canonical_headers, ";".join(signed_names), payload_hash])
    string_to_sign = "ACS3-HMAC-SHA256\n" + hashlib.sha256(canonical_request.encode("utf-8")).hexdigest()
    signature = hmac.new(sk.encode("utf-8"), string_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()
    headers["Authorization"] = (
        f"ACS3-HMAC-SHA256 Credential={ak}, SignedHeaders={';'.join(signed_names)}, Signature={signature}"
    )
    headers["Accept"] = "application/json"

    conn = PinnedHTTPSConnection(host, ip, context=ssl.create_default_context(), timeout=30)
    try:
        conn.request("GET", f"/?{query}", headers=headers)
        resp = conn.getresponse()
        body = resp.read().decode("utf-8", "replace")
        if resp.status != 200:
            try:
                err = json.loads(body)
                sys.exit(f"!! API 错误 {resp.status}: {err.get('Code')} — {err.get('Message')}")
            except json.JSONDecodeError:
                sys.exit(f"!! API 错误 {resp.status}: {body[:500]}")
        return json.loads(body)
    finally:
        conn.close()


TERMINAL = {"Finished", "Failed", "Timeout", "Cancelled", "PartialFailed", "Stopped"}


def cmd_probe(ak, sk, region):
    body = call_api(ak, sk, region, {"Action": "DescribeInvocations", "RegionId": region})
    print(f"PROBE_OK TotalCount={body.get('TotalCount')}")


def cmd_invoke(ak, sk, instance, region, script_path, name, run_params=None):
    content = Path(script_path).read_text(encoding="utf-8-sig").lstrip("﻿")
    api_params = {
        "Action": "RunCommand",
        "RegionId": region,
        "InstanceId.1": instance,
        "Type": "RunShellScript",
        "CommandContent": content,
        "Name": name,
        "Timeout": "1800",
    }
    if run_params:
        # RunCommand 自定义参数：脚本内 {{KEY}} 占位符，凭据不落任何脚本文件
        api_params["EnableParameter"] = "true"
        api_params["Parameters"] = json.dumps(run_params)
    body = call_api(ak, sk, region, api_params)
    print(f"INVOKE_ID {body.get('InvokeId')}")
    print(f"INSTANCE {instance}")
    print(f"REGION {region}")


def fetch_results(ak, sk, region, invoke_id):
    body = call_api(ak, sk, region, {
        "Action": "DescribeInvocationResults",
        "RegionId": region,
        "InvokeId": invoke_id,
        "ContentEncoding": "Base64",
    })
    return body.get("Invocation", {}).get("InvocationResults", {}).get("InvocationResult", []) or []


def fetch_status(ak, sk, region, invoke_id):
    for param_name in ("InvokeId.1", "InvokeId"):
        body = call_api(ak, sk, region, {
            "Action": "DescribeInvocations",
            "RegionId": region,
            param_name: invoke_id,
        })
        for item in body.get("Invocations", {}).get("Invocation", []) or []:
            if item.get("InvokeId") == invoke_id:
                return item.get("InvocationStatus")
    return None


def cmd_wait(ak, sk, instance, region, invoke_id, timeout_s):
    deadline = time.time() + timeout_s
    last_status = None
    while time.time() < deadline:
        mine = [r for r in fetch_results(ak, sk, region, invoke_id) if r.get("InstanceId") == instance]
        done = [r for r in mine if r.get("ExitCode") is not None and r.get("Output") is not None
                and str(r.get("ExitCode")) not in ("-2",)]
        status = None
        if not done:
            status = fetch_status(ak, sk, region, invoke_id)
            if status != last_status:
                print(f"STATUS {status}", flush=True)
                last_status = status
        if done:
            for r in done:
                out = base64.b64decode(r.get("Output") or "").decode("utf-8", "replace")
                print(f"EXIT_CODE {r.get('ExitCode')}")
                print("---- OUTPUT BEGIN ----")
                print(out.strip() or "(no output)")
                print("---- OUTPUT END ----")
            ok = status == "Finished" if status else int(done[0]["ExitCode"]) == 0
            sys.exit(0 if ok and int(done[0]["ExitCode"]) == 0 else 2)
        if status in TERMINAL and not done:
            sys.exit(f"!! 云助手命令状态 {status} 但无结果输出，InvokeId={invoke_id}")
        time.sleep(10)
    sys.exit(f"!! 等待超时（{timeout_s}s），InvokeId={invoke_id}，最后状态: {last_status}")


def main():
    args = sys.argv[1:]
    if not args:
        sys.exit(__doc__)
    ak, sk, instance, region = load_config()
    action = args[0]
    if action == "probe":
        cmd_probe(ak, sk, region)
    elif action == "invoke" and len(args) >= 2:
        run_params = {}
        positional = []
        i = 2
        while i < len(args):
            if args[i] == "--param" and i + 1 < len(args):
                key, sep, value = args[i + 1].partition("=")
                if not sep or not key.replace("-", "").replace("_", "").isalnum():
                    sys.exit(f"!! --param 需要 KEY=VALUE 形式: {args[i + 1]!r}")
                run_params[key] = value
                i += 2
                continue
            positional.append(args[i])
            i += 1
        name = positional[0] if positional else f"quanta-manual-{int(time.time())}"
        cmd_invoke(ak, sk, instance, region, args[1], name, run_params or None)
    elif action == "wait" and len(args) >= 2:
        timeout_s = int(args[2]) if len(args) > 2 else 1800
        cmd_wait(ak, sk, instance, region, args[1], timeout_s)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
