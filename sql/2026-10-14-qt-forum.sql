-- 社区论坛：帖子与评论（2026-10-14 前端业务变更后端对接）
-- 作者姓名/部门实时关联 sys_user；评论的 reply_to_user_name 为写入时的姓名快照。

create table qt_forum_post (
    post_id       bigint auto_increment comment '帖子ID' primary key,
    title         varchar(100)          not null comment '标题（1-80字，服务端校验）',
    category      varchar(20)           not null comment '分类：PROJECT/TECH/LIFE',
    content       mediumtext            not null comment '正文（Markdown 原文，1-20000字）',
    author_id     bigint                not null comment '作者用户ID',
    comment_count int default 0         not null comment '评论数（增删时由服务端重算）',
    create_time   datetime              null,
    update_time   datetime              null,
    key idx_forum_post_author (author_id),
    key idx_forum_post_create (create_time)
) engine=innodb auto_increment=1 comment='社区论坛帖子';

create table qt_forum_comment (
    comment_id        bigint auto_increment comment '评论ID' primary key,
    post_id           bigint         not null comment '所属帖子ID',
    parent_id         bigint         null comment '父评论ID，顶层为 null',
    content           varchar(2000)  not null comment '评论内容（1-1000字）',
    author_id         bigint         not null comment '评论人用户ID',
    reply_to_user_name varchar(64)   null comment '被回复人姓名快照',
    create_time       datetime       null,
    key idx_forum_comment_post (post_id),
    key idx_forum_comment_parent (parent_id)
) engine=innodb auto_increment=1 comment='社区论坛评论';
