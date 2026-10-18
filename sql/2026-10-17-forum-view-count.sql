-- 社区论坛帖子浏览量（详情成功访问时服务端原子递增）
alter table qt_forum_post
    add column view_count int not null default 0 comment '浏览量（仅详情接口递增）' after comment_count;
