-- 学习资料目录：部门分类下最多 5 层文件夹；已有资料落在分类根（folder_id = 0）

CREATE TABLE IF NOT EXISTS qt_material_folder (
    folder_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '目录 ID',
    parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父目录，0 表示分类根下第一层',
    category      VARCHAR(32)  NOT NULL COMMENT 'FRONTEND/BACKEND/PRODUCT/DESIGN',
    folder_name   VARCHAR(50)  NOT NULL COMMENT '目录名称',
    depth         TINYINT      NOT NULL COMMENT '深度 1-5',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (folder_id),
    KEY idx_qt_material_folder_parent (parent_id, category),
    UNIQUE KEY uk_qt_material_folder_parent_name (parent_id, category, folder_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料目录';

ALTER TABLE qt_material
    ADD COLUMN folder_id BIGINT NOT NULL DEFAULT 0 COMMENT '所属目录，0 为分类根' AFTER category;

ALTER TABLE qt_material
    ADD KEY idx_qt_material_folder (folder_id, category);
