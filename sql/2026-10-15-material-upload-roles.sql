-- 给可登录后台的角色补齐学习资料列表/上传权限（按 role_key，已有关联则跳过）
-- 不授予 qt_member。账号需重新登录后 getInfo 才会带上新权限。

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key IN ('admin', 'ceo', 'qt_mgmt', 'qt_manager')
  AND r.status = '0'
  AND m.perms IN ('qt:material:list', 'qt:material:add')
  AND m.status = '0'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
