package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.system.domain.vo.RouterVo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工位预约退场：getRouters 构建结果不再包含工位菜单，其余菜单保留。
 */
class SysMenuServiceImplBuildMenusTest
{
    private final SysMenuServiceImpl service = new SysMenuServiceImpl();

    @Test
    void workstationMenusAreFilteredOut()
    {
        SysMenu child = menu(101L, 100L, "workstations", "workstations/index");
        SysMenu dir = dir(100L, "实验室管理", "member", child);
        List<RouterVo> routers = service.buildMenus(List.of(dir));
        assertTrue(routers.isEmpty());
    }

    @Test
    void reservationRecordMenuIsFilteredToo()
    {
        SysMenu child = menu(101L, 100L, "workstation-reservation", "workstation-reservations/index");
        SysMenu dir = dir(100L, "实验室管理", "member", child);
        assertTrue(service.buildMenus(List.of(dir)).isEmpty());
    }

    @Test
    void siblingMenusAreKept()
    {
        SysMenu workstation = menu(101L, 100L, "workstations", "workstations/index");
        SysMenu members = menu(102L, 100L, "members", "members/index");
        SysMenu dir = dir(100L, "实验室管理", "member", workstation, members);

        List<RouterVo> routers = service.buildMenus(List.of(dir));
        assertEquals(1, routers.size());
        assertEquals(1, routers.get(0).getChildren().size());
        assertEquals("members/index", routers.get(0).getChildren().get(0).getComponent());
    }

    private SysMenu dir(Long menuId, String name, String path, SysMenu... children)
    {
        SysMenu dir = menu(menuId, 0L, path, null);
        dir.setMenuName(name);
        dir.setMenuType(UserConstants.TYPE_DIR);
        dir.setChildren(new ArrayList<>(List.of(children)));
        return dir;
    }

    private SysMenu menu(Long menuId, Long parentId, String path, String component)
    {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setMenuName(path);
        menu.setMenuType(UserConstants.TYPE_MENU);
        menu.setIsFrame(UserConstants.NO_FRAME);
        menu.setVisible("0");
        menu.setStatus("0");
        return menu;
    }
}
