#!/usr/bin/env bash
# 对已配置的测试站点域名做 Nginx 侧验收（在能解析该域名的机器上执行）。
#
# 用法：
#   ./verify-pages.sh https://alice-lab.page.quantacenter.com
#
# 校验点（对应设计第 7 节验收 4/8 的 Nginx 部分）：
#   1) 首页 200 且是 HTML
#   2) SPA 深链（无扩展名 + Accept HTML）200
#   3) 缺失静态资源 404（绝不回落门户首页）
#   4) 隐藏路径 404
#   5) POST 被拒绝（405/403）
#   6) 响应头包含 nosniff
# 需要登录态的操作由真实门户完成；本脚本不携带任何 Token。
set -euo pipefail

BASE_URL="${1:-}"
if [[ -z "$BASE_URL" ]]; then
  echo "用法: $0 <https://slug.page.quantacenter.com>" >&2
  exit 1
fi
[[ "$BASE_URL" == */ ]] && BASE_URL="${BASE_URL%/}"

fail=0
check() {
  local desc="$1" expected="$2" url="$3"; shift 3
  local status
  status=$(curl -sS -o /dev/null -w '%{http_code}' "$@" "$url") || { echo "FAIL  $desc（请求异常）"; fail=1; return; }
  if [[ ",$expected," == *",$status,"* ]]; then
    echo "OK    $desc -> $status"
  else
    echo "FAIL  $desc -> $status（期望 $expected）"
    fail=1
  fi
}

echo "== Quanta Pages Nginx 验收：$BASE_URL =="
check "首页可访问"            "200" "$BASE_URL/"
check "SPA 深链（接受 HTML）"  "200" "$BASE_URL/some/spa/route" -H 'Accept: text/html'
check "缺失 JS 返回 404"      "404" "$BASE_URL/assets/missing.js"
check "缺失图片返回 404"      "404" "$BASE_URL/images/missing.png"
check "隐藏路径 404"          "404" "$BASE_URL/.env"
check "隐藏目录 404"          "404" "$BASE_URL/.git/config"
check "POST 被拒绝"           "405,403" "$BASE_URL/" -X POST
check "未知路径带扩展名 404"   "404" "$BASE_URL/nope.html" -H 'Accept: text/html'

headers=$(curl -sS -I "$BASE_URL/")
echo "$headers" | grep -qi "x-content-type-options: nosniff" && echo "OK    nosniff 响应头" || { echo "FAIL  缺少 nosniff 响应头"; fail=1; }
echo "$headers" | grep -qi "referrer-policy" && echo "OK    Referrer-Policy 响应头" || { echo "FAIL  缺少 Referrer-Policy 响应头"; fail=1; }

echo "== 结果：$([[ $fail -eq 0 ]] && echo 全部通过 || echo 存在失败) =="
exit $fail
