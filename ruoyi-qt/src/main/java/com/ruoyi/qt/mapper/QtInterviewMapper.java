package com.ruoyi.qt.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewEvaluation;
import com.ruoyi.qt.domain.QtInterviewProfile;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.domain.QtInterviewRound;

public interface QtInterviewMapper
{
    QtInterviewApplication selectApplicationByUserId(Long userId);

    QtInterviewApplication selectApplicationById(Long applicationId);

    int insertInterviewApplication(QtInterviewApplication application);

    int updateInterviewApplication(QtInterviewApplication application);

    int updateApplicationAdminFields(QtInterviewApplication application);

    QtInterviewProfile selectProfileByUserId(Long userId);

    QtInterviewProfile selectProfileByApplicationId(Long applicationId);

    int insertInterviewProfile(QtInterviewProfile profile);

    int updateInterviewProfile(QtInterviewProfile profile);

    List<QtInterviewResult> selectResultsByUserId(Long userId);

    List<QtInterviewResult> selectResultsByApplicationId(Long applicationId);

    QtInterviewResult selectResultByUserIdAndRoundId(@Param("userId") Long userId, @Param("roundId") Long roundId);

    QtInterviewResult selectResultByAppRoundDept(@Param("applicationId") Long applicationId,
            @Param("roundId") Long roundId, @Param("department") String department);

    int insertInterviewResult(QtInterviewResult result);

    int updateInterviewResult(QtInterviewResult result);

    List<QtInterviewApplication> selectAdminApplicationList(QtInterviewApplication query);

    QtInterviewRound selectRoundById(Long roundId);

    QtInterviewRound selectRoundByNo(Integer roundNo);

    List<QtInterviewEvaluation> selectEvaluationList(QtInterviewEvaluation query);

    QtInterviewEvaluation selectEvaluationById(Long evaluationId);

    QtInterviewEvaluation selectEvaluationByUnique(@Param("applicationId") Long applicationId,
            @Param("roundId") Long roundId, @Param("department") String department,
            @Param("evaluatorUserId") Long evaluatorUserId);

    int insertEvaluation(QtInterviewEvaluation evaluation);

    int updateEvaluation(QtInterviewEvaluation evaluation);

    List<Map<String, Object>> selectApplyStatusCounts(@Param("scopedDepartment") String scopedDepartment);

    int countPendingEval(@Param("scopedDepartment") String scopedDepartment);

    int countPendingOffer(@Param("scopedDepartment") String scopedDepartment);

    int countTodayApplications();

    int countTotalApplications(@Param("scopedDepartment") String scopedDepartment);

    int countRoundPassed(@Param("scopedDepartment") String scopedDepartment, @Param("roundNo") Integer roundNo);

    int countJoined(@Param("scopedDepartment") String scopedDepartment);

    /**
     * 幂等绑定角色：若 sys_user_role 中不存在该 (userId, roleId) 则插入。
     */
    int insertUserRoleIfAbsent(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
