-- 先执行本增量脚本，再部署论坛可见范围后端；旧版本忽略新字段，原帖默认全部塔员。
-- 不删除旧数据，不把公开图片自动迁移为私有。可重复执行。
set @visibility_ddl = if(
  exists(select 1 from information_schema.columns where table_schema = database()
    and table_name = 'qt_forum_post' and column_name = 'visibility'),
  'select 1',
  'alter table qt_forum_post add column visibility varchar(16) not null default ''MEMBERS'' comment ''MEMBERS/MANAGERS'' after category');
prepare visibility_stmt from @visibility_ddl;
execute visibility_stmt;
deallocate prepare visibility_stmt;

create table if not exists qt_forum_image (
  image_id varchar(32) not null,
  owner_id bigint not null,
  post_id bigint null comment '未发布草稿仅上传者可读；发布后只认帖子权限',
  storage_path varchar(255) not null,
  media_type varchar(32) not null,
  create_time datetime not null default current_timestamp,
  primary key (image_id),
  key idx_forum_image_post (post_id),
  key idx_forum_image_owner (owner_id, create_time)
) engine=InnoDB default charset=utf8mb4 comment='论坛私有图片元数据，文件不走匿名下载';
