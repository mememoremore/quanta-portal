-- 塔员云端个人档案（可编辑字段；身份字段仍来自 sys_user）

create table qt_member_profile (
    user_id             bigint       not null comment '用户ID' primary key,
    community_nickname  varchar(20)  not null default '' comment '社区昵称，仅论坛展示',
    headline            varchar(80)  not null default '' comment '一句话介绍',
    bio                 varchar(300) not null default '' comment '个人简介',
    skills              varchar(500) not null default '[]' comment '技能标签 JSON 数组',
    github_url          varchar(300) not null default '' comment 'GitHub 链接',
    website_url         varchar(300) not null default '' comment '个人网站',
    contact_email       varchar(160) not null default '' comment '成员联系邮箱',
    show_contact_email  char(1)      not null default '0' comment '是否公开联系邮箱：0否 1是',
    update_time         datetime     null,
    key idx_member_profile_update (update_time)
) engine=innodb comment='塔员云端个人档案';
