package com.ruoyi.qt.domain;

/**
 * 创建/配置更新在同一事务写入的站点与部署任务。
 */
public record QtPageSiteSaveResult(QtPageSite site, QtPageDeployment deployment)
{
}
