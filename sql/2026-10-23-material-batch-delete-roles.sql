-- 给 CEO、管理层（qt_mgmt）和经理（qt_manager）补齐学习资料删除权限。
-- 不授予 qt_member，也不通过 admin 角色键开放批量删除。
-- 账号需重新登录后 getInfo 才会带上新权限。

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key IN ('ceo', 'qt_mgmt', 'qt_manager')
  AND r.status = '0'
  AND m.perms = 'qt:material:remove'
  AND m.status = '0'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
