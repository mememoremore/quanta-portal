-- 实名与社区昵称。可重复执行：已有列跳过，已有实名不覆盖。
-- 不修改 user_name、nick_name 和报名表。

SET @db := DATABASE();

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN real_name varchar(64) NULL COMMENT ''实名'' COLLATE utf8mb4_general_ci AFTER nick_name',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'real_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_user u
INNER JOIN qt_interview_application a ON a.user_id = u.user_id
SET u.real_name = CONVERT(a.real_name USING utf8mb4) COLLATE utf8mb4_general_ci
WHERE (u.real_name IS NULL OR CHAR_LENGTH(TRIM(u.real_name)) = 0)
  AND CHAR_LENGTH(TRIM(a.real_name)) > 0;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE qt_member_profile ADD COLUMN community_nickname varchar(20) NOT NULL DEFAULT '''' COMMENT ''社区昵称，仅论坛展示'' AFTER user_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'qt_member_profile' AND COLUMN_NAME = 'community_nickname'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE qt_forum_comment ADD COLUMN reply_to_user_id bigint NULL COMMENT ''被回复用户，昵称变更后仍指向本人'' AFTER author_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'qt_forum_comment' AND COLUMN_NAME = 'reply_to_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE qt_forum_comment c
INNER JOIN qt_forum_comment p ON c.parent_id = p.comment_id
SET c.reply_to_user_id = p.author_id
WHERE c.reply_to_user_id IS NULL;
