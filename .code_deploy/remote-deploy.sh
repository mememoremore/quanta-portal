#!/usr/bin/env bash
# 在 ECS 上：/opt/ruoyi/remote-deploy.sh <发布目录名>
# 发布目录：/opt/ruoyi/releases/<名>/ 内已有解压后的 jar、admin-dist.tgz、sql/
set -eu
REL="${1:?release dir name required}"
BASE="/opt/ruoyi/releases/${REL}"

cp -a /opt/ruoyi/ruoyi-admin.jar "${BASE}/ruoyi-admin.jar.prev" 2>/dev/null || true
install -m 644 "${BASE}/ruoyi-admin.jar" /opt/ruoyi/ruoyi-admin.jar
chown ruoyi:ruoyi /opt/ruoyi/ruoyi-admin.jar

rm -rf "${BASE}/admin"
mkdir -p "${BASE}/admin"
tar -xzf "${BASE}/admin-dist.tgz" -C "${BASE}/admin"
rsync -a --delete "${BASE}/admin/" /opt/ruoyi/admin/

MYSQL_ROOT_PASSWORD="$(grep '^MYSQL_ROOT_PASSWORD=' /opt/ruoyi/.env | cut -d= -f2- | tr -d '\r')"
if [ -z "${MYSQL_ROOT_PASSWORD}" ]; then
  echo "!! 无法从 /opt/ruoyi/.env 读取 MYSQL_ROOT_PASSWORD" >&2
  exit 1
fi

if [ -d "${BASE}/sql" ]; then
  for f in $(ls -1 "${BASE}/sql/"*.sql 2>/dev/null | sort); do
    echo ">> SQL $(basename "$f")"
    docker exec -i quanta-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" ry-vue < "$f" || true
  done
fi

docker restart quanta-app
nginx -t
systemctl reload nginx

sha256sum /opt/ruoyi/ruoyi-admin.jar
grep -o 'assets/index-[^" ]*\.js' /opt/ruoyi/admin/index.html || true
docker ps --filter name=quanta-app --format '{{.Status}}'
