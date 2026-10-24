-- 论坛互动：只新增关系表，不改写原帖/评论。发布新后端前执行，可重复执行。
-- 应用事务统一先锁 qt_forum_post 行，再写关系/删除帖子，避免并发孤儿记录。
create table if not exists qt_forum_post_like (
    id bigint not null auto_increment primary key,
    post_id bigint not null,
    user_id bigint not null,
    create_time datetime(6) not null default current_timestamp(6),
    unique key uk_forum_like_post_user (post_id, user_id),
    key idx_forum_like_user_time (user_id, create_time, id)
) engine=innodb comment='论坛帖子点赞';

create table if not exists qt_forum_post_favorite (
    id bigint not null auto_increment primary key,
    post_id bigint not null,
    user_id bigint not null,
    create_time datetime(6) not null default current_timestamp(6),
    unique key uk_forum_favorite_post_user (post_id, user_id),
    key idx_forum_favorite_user_time (user_id, create_time, id)
) engine=innodb comment='论坛帖子私密收藏';
