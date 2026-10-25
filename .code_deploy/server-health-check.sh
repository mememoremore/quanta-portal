#!/bin/bash
# 只读健康检查：容器状态 + 应用启动日志尾部
docker ps --filter name=quanta-app --format '{{.Names}} {{.Status}}'
echo "---- app logs tail ----"
docker logs quanta-app --tail 8 2>&1 | tail -8
