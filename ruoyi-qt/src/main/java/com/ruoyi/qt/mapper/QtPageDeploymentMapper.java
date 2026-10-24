package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtPageDeployment;

/**
 * 成员静态站点部署记录 Mapper。(site_id, request_version) 唯一，防止同一意图重复入队。
 */
public interface QtPageDeploymentMapper
{
    int insertDeployment(QtPageDeployment deployment);

    QtPageDeployment selectById(Long deploymentId);

    QtPageDeployment selectBySiteAndRequestVersion(@Param("siteId") Long siteId, @Param("requestVersion") Long requestVersion);

    /** 带行锁读取，用于 Worker 领取任务后的状态推进 */
    QtPageDeployment selectForUpdate(Long deploymentId);

    int updateDeployment(QtPageDeployment deployment);

    /** 手动部署去重：同站点、配置与站点当前一致且仍在排队的最新 MANUAL 任务 */
    QtPageDeployment selectReusableManual(@Param("siteId") Long siteId);

    /** 把早于 requestVersion 的非终态任务标记为 SUPERSEDED，返回受影响行数 */
    int markSupersededBefore(@Param("siteId") Long siteId, @Param("requestVersion") Long requestVersion);

    /** 历史列表（Task 6 分页使用） */
    List<QtPageDeployment> selectListBySite(@Param("siteId") Long siteId);
}
