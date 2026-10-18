-- 学习资料批量删除后的文件清理任务。与资料删除同一事务写入，提交后再删物理文件。
-- 本脚本只提供结构，不在此自动执行，也不删除任何现有资料。

CREATE TABLE IF NOT EXISTS qt_material_file_cleanup (
    cleanup_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '清理任务 ID',
    material_id   BIGINT       NOT NULL COMMENT '已删除的资料 ID',
    file_path     VARCHAR(500) NOT NULL COMMENT '资料表中的存储路径，不是客户端传入路径',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED',
    attempt_count INT          NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    last_error    VARCHAR(500) DEFAULT NULL COMMENT '最近一次失败原因，不含文件正文',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '操作人',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (cleanup_id),
    KEY idx_qt_material_cleanup_status (status, cleanup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料文件清理任务';
