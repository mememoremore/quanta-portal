-- 招新录用邮件按部门记录发送状态
CREATE TABLE IF NOT EXISTS qt_interview_notice (
    notice_id       bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_id  bigint       NOT NULL COMMENT '申请 ID',
    department      varchar(32)  NOT NULL COMMENT '标准部门码',
    notice_status   varchar(16)  NOT NULL DEFAULT 'UNSENT' COMMENT 'UNSENT/SENT',
    create_by       varchar(64)  DEFAULT NULL,
    create_time     datetime     DEFAULT NULL,
    update_by       varchar(64)  DEFAULT NULL,
    update_time     datetime     DEFAULT NULL,
    PRIMARY KEY (notice_id),
    UNIQUE KEY uk_qt_interview_notice_app_dept (application_id, department),
    KEY idx_qt_interview_notice_app (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招新录用邮件按部门状态';
