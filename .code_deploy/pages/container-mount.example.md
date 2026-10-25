# quanta-app 容器挂载 Pages 根目录（示例）

目标：让 `quanta-app` 容器内的应用进程只对 Pages 根目录可写，且不引入任何额外特权。
**不要**假设"重启容器就能新增挂载"——必须按容器当前的实际启动方式重建。

## 1. 先核实现状（只读检查）

```bash
# 当前容器的启动参数与挂载（记录下来，重建时原样保留）
docker inspect quanta-app --format '{{json .HostConfig.Binds}}'
docker inspect quanta-app --format '{{json .Config.Env}}'   # 确认 env 文件来源
docker inspect quanta-app --format '{{.HostConfig.User}}'   # 是否已指定非 root UID
docker inspect quanta-mysql --format '{{json .HostConfig.Binds}}'  # 数据卷，绝对不能丢
```

## 2. 重建容器（保留原有数据卷与 env）

在原 `docker run` 参数基础上**追加**一个绑定挂载，其余参数原样保留：

```bash
docker run -d --name quanta-app-new \
  # ...原有的 -p / --env / --network / 其他 -v 参数原样保留...
  -v /srv/quanta-pages:/srv/quanta-pages \
  -e QT_PAGES_ENABLED=true \
  -e QT_PAGES_ROOT_DIR=/srv/quanta-pages \
  -e QT_PAGES_GITHUB_TOKEN="$QT_PAGES_GITHUB_TOKEN" \
  --user 1001:1001 \
  --restart unless-stopped \
  <原镜像>
docker rm quanta-app && docker rename quanta-app-new quanta-app
```

要点：

- **同一绝对路径**：容器内 `/srv/quanta-pages` 与宿主机一致，`current` 相对符号链接才能被宿主机
  Nginx 正确解析（`current -> releases/{id}`）。
- **只挂 Pages 根目录**：不挂载 Docker socket、证书私钥、宿主机其他目录。
- **非 root 运行**：`--user 1001:1001` 与 `prepare-pages.sh` 的 UID/GID 一致；
  应用对该目录可写，Nginx 以宿主机用户只读遍历。
- 环境变量走 `.env`/秘钥管理，不写进脚本（对照 `.code_deploy/deploy.env.example`）。

## 3. 宿主机 Nginx

Nginx 直接读宿主机路径 `/srv/quanta-pages/sites/<slug>/current`，无需进入容器。
模板见 `nginx-pages.conf.example`；启用前执行 `nginx -t`，通过后再 reload 一次——
**此后成员部署不再需要 reload Nginx**。

## 4. 验证

```bash
docker inspect quanta-app --format '{{json .HostConfig.Binds}}'   # 确认新挂载
docker exec quanta-app touch /srv/quanta-pages/.rw-test && \
  docker exec quanta-app rm /srv/quanta-pages/.rw-test && echo "应用可写 OK"
sudo -u nginx test -r /srv/quanta-pages/sites && echo "Nginx 可读 OK"
```
