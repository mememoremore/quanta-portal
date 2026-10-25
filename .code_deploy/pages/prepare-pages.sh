#!/usr/bin/env bash
# 在目标 Linux 服务器上准备 Quanta Pages 根目录与权限。
#
# 用法（root 或 sudo 执行）：
#   sudo ./prepare-pages.sh /srv/quanta-pages 1001 1001
#   参数 1：Pages 根目录（必须以 / 开头，与应用内 qt.pages.root-dir 一致）
#   参数 2：运行 quanta-app 的 UID（无特权用户）
#   参数 3：运行 quanta-app 的 GID
#
# 约束：
# - 只创建/调整显式指定的 Pages 根目录，不修改任何现有门户目录
# - 不读取、不打印任何生产密钥
set -euo pipefail

PAGES_ROOT="${1:-}"
PAGES_UID="${2:-}"
PAGES_GID="${3:-}"

if [[ -z "$PAGES_ROOT" || -z "$PAGES_UID" || -z "$PAGES_GID" ]]; then
  echo "用法: $0 <pages-root> <app-uid> <app-gid>" >&2
  exit 1
fi
case "$PAGES_ROOT" in
  /*) ;;
  *) echo "pages-root 必须是绝对路径" >&2; exit 1 ;;
esac
if [[ ! "$PAGES_UID" =~ ^[0-9]+$ || ! "$PAGES_GID" =~ ^[0-9]+$ ]]; then
  echo "uid/gid 必须是数字" >&2
  exit 1
fi

echo "== Quanta Pages 目录准备 =="
echo "根目录: $PAGES_ROOT  应用 UID:GID = $PAGES_UID:$PAGES_GID"

mkdir -p "$PAGES_ROOT/sites"
chown "$PAGES_UID:$PAGES_GID" "$PAGES_ROOT" "$PAGES_ROOT/sites"
chmod 750 "$PAGES_ROOT" "$PAGES_ROOT/sites"

# Nginx 只需遍历读取：给予 other 组 x（进入）权限但不给读列目录内容以外的东西。
# 说明：站点目录由应用按需创建（owner=应用 UID），Nginx 以 other 权限遍历，
# 依赖 sites/ 与各站点目录的 other-x。若策略要求更严格，可改为把 Nginx 用户加入应用组。
chmod o+x "$PAGES_ROOT" "$PAGES_ROOT/sites"

echo "== 完成 =="
echo "后续：确认宿主机 Nginx 用户（如 nginx/www-data）可读取 $PAGES_ROOT 下的站点文件："
echo "  sudo -u \$(ps -o user= -p \$(pgrep -o nginx | head -1)) test -r $PAGES_ROOT/sites && echo OK"
echo "容器挂载与重建步骤见 container-mount.example.md；上线核对清单见 ROLLOUT-CHECKLIST.md"
