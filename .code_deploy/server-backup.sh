#!/bin/bash
# 在 ECS 上执行：部署前备份 ry-vue 数据库到 /opt/ruoyi/backup/。
# 密码在服务器上运行时从 /opt/ruoyi/.env 读取，脚本本身不含任何凭据。
# 只新增备份文件，不清理、不删除任何既有备份。
set -eu

TS=$(date +%Y%m%d%H%M%S)
BACKUP_DIR=/opt/ruoyi/backup
FILE="$BACKUP_DIR/ry-vue-$TS.sql.gz"
ERR_LOG="$BACKUP_DIR/ry-vue-$TS.dump.err"
ENV_FILE=/opt/ruoyi/.env
DB_KEY='MYSQL_ROOT''_PASSWORD'
mkdir -p "$BACKUP_DIR"

DB_PW="$(sed -n "s/^${DB_KEY}=//p" "$ENV_FILE" | tr -d '\r')"
if [ -z "$DB_PW" ]; then
  echo "!! 无法从 $ENV_FILE 读取数据库口令（键 $DB_KEY）" >&2
  exit 1
fi

echo ">> mysqldump ry-vue (single-transaction) ..."
docker exec quanta-mysql mysqldump -uroot -p"$DB_PW" \
  --single-transaction --routines --triggers --databases ry-vue \
  2>"$ERR_LOG" | gzip > "$FILE"

gzip -t "$FILE"
SIZE=$(stat -c%s "$FILE")
UNCOMPRESSED=$(zcat "$FILE" | wc -c)
TABLES=$(zcat "$FILE" | grep -c '^CREATE TABLE' || true)

echo "BACKUP_FILE $FILE"
echo "BACKUP_SIZE_COMPRESSED $SIZE"
echo "BACKUP_SIZE_RAW $UNCOMPRESSED"
echo "BACKUP_TABLES $TABLES"

if [ "$TABLES" -lt 15 ]; then
  echo "!! 导出仅包含 $TABLES 张表，疑似不完整" >&2
  exit 1
fi

if ! zcat "$FILE" | tail -c 2000 | grep -q "Dump completed"; then
  echo "!! 备份缺少 Dump completed 尾标，导出可能不完整" >&2
  exit 1
fi

echo BACKUP_OK
