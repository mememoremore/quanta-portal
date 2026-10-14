package com.ruoyi.qt.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewProfile;
import com.ruoyi.qt.domain.QtInterviewResult;

public interface IQtInterviewService
{
    int saveMyApplication(QtInterviewApplication application, QtInterviewProfile profile);

    /**
     * 是否仍接受从未投递过的新生报名。已有记录的人不受此开关限制。
     */
    boolean isNewApplicationsOpen();

    QtInterviewApplication selectMyApplication(Long userId);

    QtInterviewProfile selectMyProfile(Long userId);

    List<QtInterviewResult> selectMyResultList(Long userId);

    /**
     * 录入/更新面试轮次结果，并尝试邮件通知新生。
     */
    Map<String, Object> saveInterviewResult(QtInterviewResult result);
}
