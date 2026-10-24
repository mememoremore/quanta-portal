package com.ruoyi.qt.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtPageSite;

/**
 * 成员静态站点 Mapper。意图分配、发布与清理都必须先经 selectForUpdate 锁定站点行。
 */
public interface QtPageSiteMapper
{
    int insertSite(QtPageSite site);

    QtPageSite selectById(Long siteId);

    QtPageSite selectByUserId(Long userId);

    QtPageSite selectBySlug(String slug);

    /** 带 FOR UPDATE 的行锁读取，必须在事务内调用 */
    QtPageSite selectForUpdate(Long siteId);

    int updateSite(QtPageSite site);

    int updateStatus(@Param("siteId") Long siteId, @Param("status") String status);
}
