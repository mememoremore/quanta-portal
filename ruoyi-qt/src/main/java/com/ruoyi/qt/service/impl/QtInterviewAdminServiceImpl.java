package com.ruoyi.qt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewEvaluation;
import com.ruoyi.qt.domain.QtInterviewOfferBody;
import com.ruoyi.qt.domain.QtInterviewProfile;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.domain.QtInterviewRound;
import com.ruoyi.qt.domain.QtMemberRecord;
import com.ruoyi.qt.domain.QtStatusBody;
import com.ruoyi.qt.mapper.QtCohortMapper;
import com.ruoyi.qt.mapper.QtInterviewMapper;
import com.ruoyi.qt.service.IQtInterviewAdminService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.qt.util.QtDictUtils;
import com.ruoyi.qt.util.QtInterviewNoticeTemplates;
import com.ruoyi.qt.util.QtInterviewNoticeTemplates.NoticeMail;
import com.ruoyi.qt.util.QtInterviewRounds;
import com.ruoyi.qt.util.QtInterviewStatuses;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class QtInterviewAdminServiceImpl implements IQtInterviewAdminService
{
    private static final DateTimeFormatter FIRST_ROUND_INTERVIEW_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> FIRST_ROUND_INTERVIEW_SLOTS = Set.of("2026-09-22 18:30:00",
            "2026-09-23 18:30:00");

    @Autowired
    private QtInterviewMapper qtInterviewMapper;

    @Autowired
    private QtInterviewNotifier interviewNotifier;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private QtCohortMapper qtCohortMapper;

    @Autowired
    private ISysRoleService roleService;

    @Override
    public List<QtInterviewApplication> selectAdminList(QtInterviewApplication query)
    {
        query.setScopedDepartment(QtAuthUtils.scopedDepartment());
        if (query.getRoundId() != null && query.getRoundId() == 2L)
        {
            query.setSortDepartment(QtAuthUtils.currentDepartment());
        }
        return qtInterviewMapper.selectAdminApplicationList(query);
    }

    @Override
    public List<QtInterviewApplication> selectMemberList(QtInterviewApplication query)
    {
        // 塔员可见全部部门，不做 scopedDepartment 过滤
        return qtInterviewMapper.selectAdminApplicationList(query);
    }

    @Override
    public QtInterviewApplication selectApplication(Long applicationId)
    {
        QtInterviewApplication application = requireApplication(applicationId);
        QtAuthUtils.assertApplicationScope(application.getFirstChoice(), application.getSecondChoice());
        return application;
    }

    @Override
    public QtInterviewProfile selectProfile(Long applicationId)
    {
        selectApplication(applicationId);
        return qtInterviewMapper.selectProfileByApplicationId(applicationId);
    }

    @Override
    public List<QtInterviewResult> selectResults(Long applicationId)
    {
        QtInterviewApplication application = selectApplication(applicationId);
        List<QtInterviewResult> list = qtInterviewMapper.selectResultsByApplicationId(applicationId);
        String scoped = QtAuthUtils.scopedDepartment();
        if (scoped == null)
        {
            return list;
        }
        list.removeIf(item -> item.getDepartment() != null && !scoped.equals(item.getDepartment())
                && !application.getFirstChoice().equals(scoped) && !application.getSecondChoice().equals(scoped));
        if (scoped != null)
        {
            list.removeIf(item -> !scoped.equals(item.getDepartment()));
        }
        return list;
    }

    @Override
    public List<QtInterviewEvaluation> selectEvaluations(QtInterviewEvaluation query)
    {
        if (query.getApplicationId() != null)
        {
            requireApplication(query.getApplicationId());
        }
        if (query.getRoundId() != null)
        {
            query.setRoundId(resolveEvalRoundId(query.getRoundId()));
        }
        if (!canViewAllDepartments())
        {
            String mine = requireOwnDepartment();
            query.setDepartment(mine);
        }
        return qtInterviewMapper.selectEvaluationList(query);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_RECRUIT_STATISTICS, allEntries = true)
    public QtInterviewEvaluation saveEvaluation(QtInterviewEvaluation evaluation, Long operatorUserId, String operator)
    {
        validateEvaluation(evaluation);
        QtInterviewApplication application = requireApplication(evaluation.getApplicationId());
        String department = resolveEvalDepartment(evaluation.getDepartment(), application);
        assertOwnDepartmentWrite(department);
        evaluation.setDepartment(department);
        evaluation.setRoundId(resolveEvalRoundId(evaluation.getRoundId()));
        evaluation.setEvaluatorUserId(operatorUserId);
        QtInterviewEvaluation old = qtInterviewMapper.selectEvaluationByUnique(evaluation.getApplicationId(),
                evaluation.getRoundId(), department, operatorUserId);
        evaluation.setUpdateBy(operator);
        if (old == null)
        {
            evaluation.setCreateBy(operator);
            qtInterviewMapper.insertEvaluation(evaluation);
            markProcessing(application, operator);
            return qtInterviewMapper.selectEvaluationById(evaluation.getEvaluationId());
        }
        old.setContent(evaluation.getContent());
        old.setUpdateBy(operator);
        qtInterviewMapper.updateEvaluation(old);
        return qtInterviewMapper.selectEvaluationById(old.getEvaluationId());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_RECRUIT_STATISTICS, allEntries = true)
    public QtInterviewEvaluation updateEvaluation(QtInterviewEvaluation evaluation, Long operatorUserId, String operator)
    {
        QtInterviewEvaluation old = qtInterviewMapper.selectEvaluationById(evaluation.getEvaluationId());
        if (old == null)
        {
            throw new ServiceException("面评不存在");
        }
        if (!operatorUserId.equals(old.getEvaluatorUserId()) && !QtAuthUtils.isCeo())
        {
            throw new ServiceException("无权修改他人面评");
        }
        if (StringUtils.isEmpty(evaluation.getContent()))
        {
            throw new ServiceException("面评内容不能为空");
        }
        old.setContent(evaluation.getContent());
        old.setUpdateBy(operator);
        qtInterviewMapper.updateEvaluation(old);
        return qtInterviewMapper.selectEvaluationById(old.getEvaluationId());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConstants.CACHE_QT_RECRUIT_STATISTICS, CacheConstants.CACHE_QT_MY_RESULTS,
            CacheConstants.CACHE_QT_MY_APPLICATION, CacheConstants.CACHE_QT_MEMBER_COHORTS }, allEntries = true)
    public Map<String, Object> saveDecision(Long applicationId, Long roundNo, String department, String decision,
            String operator)
    {
        String resultStatus = QtInterviewStatuses.normalizeDecision(decision);
        if (resultStatus == null)
        {
            throw new ServiceException("decision 只能是 PASS 或 OUT");
        }
        QtInterviewApplication application = selectApplication(applicationId);
        department = requireChoiceDepartment(department, application);
        if (QtInterviewStatuses.isPass(resultStatus))
        {
            QtDictUtils.requireActiveDepartment(department, "department");
        }
        if (!QtAuthUtils.isCeo())
        {
            QtAuthUtils.assertDepartmentScope(department);
        }
        QtInterviewRound round = QtInterviewRounds.require(qtInterviewMapper, roundNo);
        int roundNumber = round.getRoundNo() == null ? 0 : round.getRoundNo();
        QtInterviewResult current = qtInterviewMapper.selectResultByAppRoundDept(application.getApplicationId(),
                round.getRoundId(), department);
        if (roundNumber == 2)
        {
            assertAdvanced(application, department);
            if (current == null || current.getScore() == null)
            {
                throw new ServiceException("请先填写该部门的二面分数");
            }
        }
        Date now = new Date();
        if (current == null)
        {
            current = new QtInterviewResult();
            current.setApplicationId(application.getApplicationId());
            current.setUserId(application.getUserId());
            current.setRoundId(round.getRoundId());
            current.setDepartment(department);
            current.setResultStatus(resultStatus);
            current.setPublishedTime(now);
            current.setCreateBy(operator);
            current.setUpdateBy(operator);
            qtInterviewMapper.insertInterviewResult(current);
        }
        else
        {
            current.setResultStatus(resultStatus);
            current.setPublishedTime(now);
            current.setUpdateBy(operator);
            qtInterviewMapper.updateInterviewResult(current);
        }

        if (roundNumber == 1)
        {
            refreshRoundOneStatus(application, operator);
        }
        else if (roundNumber == 2)
        {
            refreshRoundTwoOffer(application, operator);
        }
        else
        {
            markProcessing(application, operator);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("applyStatus", application.getApplyStatus());
        data.put("offeredDepartment", application.getOfferedDepartment());
        data.put("noticeStatus", application.getNoticeStatus());
        data.put("resultStatus", resultStatus);
        data.put("department", department);
        data.put("roundNo", roundNumber);
        return data;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_MY_RESULTS, allEntries = true)
    public Map<String, Object> saveFirstRoundInterviewTime(Long applicationId, Long roundNo, String interviewTime,
            String operator)
    {
        if (roundNo == null || roundNo.longValue() != 1L)
        {
            throw new ServiceException("仅支持设置一面面试时间");
        }
        Date slot = requireFirstRoundInterviewSlot(interviewTime);
        QtInterviewApplication application = selectApplication(applicationId);
        QtInterviewRound round = QtInterviewRounds.require(qtInterviewMapper, 1L);
        upsertFirstRoundInterviewTime(application, round, application.getFirstChoice(), slot, operator);
        upsertFirstRoundInterviewTime(application, round, application.getSecondChoice(), slot, operator);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("applicationId", application.getApplicationId());
        data.put("roundNo", Integer.valueOf(1));
        data.put("interviewTime", interviewTime.trim());
        return data;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConstants.CACHE_QT_RECRUIT_STATISTICS, CacheConstants.CACHE_QT_MY_RESULTS,
            CacheConstants.CACHE_QT_MY_APPLICATION }, allEntries = true)
    public Map<String, Object> saveScore(Long applicationId, String department, Integer score, String operator)
    {
        if (score == null || score < 0 || score > 100)
        {
            throw new ServiceException("分数必须是 0 到 100 的整数");
        }
        QtInterviewApplication application = selectApplication(applicationId);
        department = requireChoiceDepartment(department, application);
        assertOwnDepartmentWrite(department);
        assertAdvanced(application, department);
        QtInterviewRound round = qtInterviewMapper.selectRoundByNo(2);
        if (round == null)
        {
            throw new ServiceException("面试轮次不存在");
        }
        QtInterviewResult current = qtInterviewMapper.selectResultByAppRoundDept(application.getApplicationId(),
                round.getRoundId(), department);
        if (QtInterviewStatuses.isDecided(current))
        {
            throw new ServiceException("该志愿已评定，分数已锁定");
        }
        Date now = new Date();
        if (current == null)
        {
            current = new QtInterviewResult();
            current.setApplicationId(application.getApplicationId());
            current.setUserId(application.getUserId());
            current.setRoundId(round.getRoundId());
            current.setDepartment(department);
            current.setResultStatus(QtInterviewStatuses.PENDING);
            current.setScore(BigDecimal.valueOf(score));
            current.setCreateBy(operator);
            current.setUpdateBy(operator);
            qtInterviewMapper.insertInterviewResult(current);
        }
        else
        {
            current.setScore(BigDecimal.valueOf(score));
            current.setUpdateBy(operator);
            current.setUpdateTime(now);
            qtInterviewMapper.updateInterviewResult(current);
        }
        markProcessing(application, operator);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("department", department);
        data.put("score", score);
        data.put("updatedBy", operator);
        return data;
    }

    @Override
    public Map<String, Object> previewNotice(Long applicationId)
    {
        QtInterviewApplication application = selectApplication(applicationId);
        return buildNoticePreview(application, requireNoticeReady(application), true);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConstants.CACHE_QT_RECRUIT_STATISTICS, CacheConstants.CACHE_QT_MY_RESULTS,
            CacheConstants.CACHE_QT_MY_APPLICATION, CacheConstants.CACHE_QT_MEMBER_COHORTS }, allEntries = true)
    public Map<String, Object> sendNotice(Long applicationId, MultipartFile qrCode, String operator)
    {
        QtInterviewApplication application = selectApplication(applicationId);
        if ("SENT".equalsIgnoreCase(application.getNoticeStatus()))
        {
            throw new ServiceException("结果邮件已发送，不能重复发送");
        }
        NoticeOutcome outcome = requireNoticeReady(application);
        if (!QtAuthUtils.isCeo() && outcome.pass)
        {
            QtAuthUtils.assertDepartmentScope(outcome.offeredDepartment);
        }
        byte[] attachment = null;
        String attachmentName = null;
        if (outcome.pass)
        {
            attachment = requireQrBytes(qrCode);
            attachmentName = qrFileName(qrCode);
            bindOfferAndConvert(application, outcome.offeredDepartment, operator);
        }
        Map<String, Object> preview = buildNoticePreview(application, outcome, false);
        boolean emailSent = interviewNotifier.notifyApplicant(application.getUserId(),
                String.valueOf(preview.get("subject")), String.valueOf(preview.get("content")),
                String.valueOf(preview.get("plainContent")), attachmentName, attachment,
                qrContentType(attachmentName));
        if (!emailSent)
        {
            throw new ServiceException("邮件发送失败，请稍后重试");
        }
        application.setNoticeStatus("SENT");
        application.setUpdateBy(operator);
        qtInterviewMapper.updateApplicationAdminFields(application);
        preview.put("noticeStatus", "SENT");
        preview.put("emailSent", Boolean.TRUE);
        return preview;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = { CacheConstants.CACHE_QT_RECRUIT_STATISTICS, CacheConstants.CACHE_QT_MY_RESULTS,
            CacheConstants.CACHE_QT_MY_APPLICATION, CacheConstants.CACHE_QT_MEMBER_COHORTS }, allEntries = true)
    public Map<String, Object> offer(QtInterviewOfferBody body, String operator)
    {
        if (body == null || body.getApplicationId() == null)
        {
            throw new ServiceException("applicationId 不能为空");
        }
        QtInterviewApplication application = selectApplication(body.getApplicationId());
        String department = resolveOfferDepartment(body, application);
        Long roundNo = body.getRoundId() == null ? Long.valueOf(2) : body.getRoundId();
        return saveDecision(body.getApplicationId(), roundNo, department, body.getDecision(), operator);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_RECRUIT_STATISTICS, allEntries = true)
    public Map<String, Object> updateJoinStatus(Long applicationId, QtStatusBody body, String operator)
    {
        QtInterviewApplication application = selectApplication(applicationId);
        if (body == null || StringUtils.isEmpty(body.getJoinStatus()))
        {
            throw new ServiceException("joinStatus 不能为空");
        }
        String status = body.getJoinStatus().toUpperCase();
        if (!"PENDING".equals(status) && !"ACCEPTED".equals(status) && !"DECLINED".equals(status))
        {
            throw new ServiceException("joinStatus 只能是 PENDING/ACCEPTED/DECLINED");
        }
        application.setJoinStatus(status);
        application.setUpdateBy(operator);
        if (StringUtils.isNotEmpty(body.getRemark()))
        {
            application.setRemark(body.getRemark());
        }
        qtInterviewMapper.updateApplicationAdminFields(application);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("joinStatus", status);
        return data;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_RECRUIT_STATISTICS, allEntries = true)
    public Map<String, Object> updateFinalStatus(Long applicationId, QtStatusBody body, String operator)
    {
        QtInterviewApplication application = selectApplication(applicationId);
        if (body == null || StringUtils.isEmpty(body.getFinalStatus()))
        {
            throw new ServiceException("finalStatus 不能为空");
        }
        QtDictUtils.requireValue(QtDictUtils.APPLY_STATUS, body.getFinalStatus(), "finalStatus");
        application.setFinalStatus(body.getFinalStatus());
        application.setApplyStatus(body.getFinalStatus());
        application.setUpdateBy(operator);
        qtInterviewMapper.updateApplicationAdminFields(application);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("finalStatus", application.getFinalStatus());
        data.put("applyStatus", application.getApplyStatus());
        return data;
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.CACHE_QT_RECRUIT_STATISTICS,
            key = "T(com.ruoyi.qt.cache.QtCacheKeys).scopedDept()")
    public Map<String, Object> statistics()
    {
        String scoped = QtAuthUtils.scopedDepartment();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("submittedCount", 0);
        data.put("processingCount", 0);
        data.put("offeredCount", 0);
        data.put("rejectedCount", 0);
        List<Map<String, Object>> rows = qtInterviewMapper.selectApplyStatusCounts(scoped);
        if (rows != null)
        {
            for (Map<String, Object> row : rows)
            {
                String status = String.valueOf(row.get("applyStatus"));
                int cnt = ((Number) row.get("cnt")).intValue();
                if ("SUBMITTED".equals(status))
                {
                    data.put("submittedCount", cnt);
                }
                else if ("PROCESSING".equals(status))
                {
                    data.put("processingCount", cnt);
                }
                else if ("OFFERED".equals(status))
                {
                    data.put("offeredCount", cnt);
                }
                else if ("REJECTED".equals(status))
                {
                    data.put("rejectedCount", cnt);
                }
            }
        }
        data.put("pendingEvalCount", qtInterviewMapper.countPendingEval(scoped));
        data.put("pendingOfferCount", qtInterviewMapper.countPendingOffer(scoped));
        data.put("todayResumeCount", qtInterviewMapper.countTodayApplications());
        data.put("submissions", qtInterviewMapper.countTotalApplications(scoped));
        data.put("firstPassed", qtInterviewMapper.countRoundPassed(scoped, 1));
        data.put("secondPassed", qtInterviewMapper.countRoundPassed(scoped, 2));
        data.put("joined", qtInterviewMapper.countJoined(scoped));
        return data;
    }

    private QtInterviewApplication requireApplication(Long applicationId)
    {
        QtInterviewApplication application = qtInterviewMapper.selectApplicationById(applicationId);
        if (application == null)
        {
            throw new ServiceException("申请不存在");
        }
        return application;
    }

    private void validateEvaluation(QtInterviewEvaluation evaluation)
    {
        if (evaluation.getApplicationId() == null)
        {
            throw new ServiceException("applicationId 不能为空");
        }
        if (evaluation.getRoundId() == null)
        {
            throw new ServiceException("roundId 不能为空");
        }
        if (StringUtils.isEmpty(evaluation.getContent()))
        {
            throw new ServiceException("面评内容不能为空");
        }
    }

    private String resolveEvalDepartment(String department, QtInterviewApplication application)
    {
        if (StringUtils.isNotEmpty(department))
        {
            if (!department.equals(application.getFirstChoice()) && !department.equals(application.getSecondChoice()))
            {
                throw new ServiceException("该部门不在候选人志愿中");
            }
            return department;
        }
        return application.getFirstChoice();
    }

    private Long resolveEvalRoundId(Long roundIdOrNo)
    {
        return QtInterviewRounds.require(qtInterviewMapper, roundIdOrNo).getRoundId();
    }

    private void markProcessing(QtInterviewApplication application, String operator)
    {
        if ("SUBMITTED".equals(application.getApplyStatus()))
        {
            application.setApplyStatus("PROCESSING");
            application.setUpdateBy(operator);
            qtInterviewMapper.updateApplicationAdminFields(application);
        }
    }

    private String resolveOfferDepartment(QtInterviewOfferBody body, QtInterviewApplication application)
    {
        if (body.getVolunteerNo() != null)
        {
            if (body.getVolunteerNo() == 1)
            {
                return application.getFirstChoice();
            }
            if (body.getVolunteerNo() == 2)
            {
                return application.getSecondChoice();
            }
            throw new ServiceException("volunteerNo 只能是 1 或 2");
        }
        if (StringUtils.isNotEmpty(body.getDepartment()))
        {
            return body.getDepartment();
        }
        String scoped = QtAuthUtils.scopedDepartment();
        return scoped != null ? scoped : application.getFirstChoice();
    }

    private boolean canViewAllDepartments()
    {
        return QtAuthUtils.isCeo();
    }

    private String requireOwnDepartment()
    {
        String mine = QtAuthUtils.currentDepartment();
        if (StringUtils.isEmpty(mine))
        {
            throw new ServiceException("当前账号未绑定成员部门，无法操作该数据");
        }
        return mine;
    }

    private void assertOwnDepartmentWrite(String department)
    {
        String mine = requireOwnDepartment();
        if (!mine.equals(department))
        {
            throw new ServiceException("只能修改自己部门的数据");
        }
    }

    private Date requireFirstRoundInterviewSlot(String interviewTime)
    {
        String value = interviewTime == null ? null : interviewTime.trim();
        if (StringUtils.isEmpty(value) || !FIRST_ROUND_INTERVIEW_SLOTS.contains(value))
        {
            throw new ServiceException("请选择有效的一面面试时间");
        }
        try
        {
            LocalDateTime local = LocalDateTime.parse(value, FIRST_ROUND_INTERVIEW_TIME_FORMAT);
            return Date.from(local.atZone(ZoneId.systemDefault()).toInstant());
        }
        catch (DateTimeParseException ex)
        {
            throw new ServiceException("请选择有效的一面面试时间");
        }
    }

    private void upsertFirstRoundInterviewTime(QtInterviewApplication application, QtInterviewRound round,
            String department, Date interviewTime, String operator)
    {
        if (StringUtils.isEmpty(department))
        {
            return;
        }
        QtInterviewResult current = qtInterviewMapper.selectResultByAppRoundDept(application.getApplicationId(),
                round.getRoundId(), department);
        if (current == null)
        {
            current = new QtInterviewResult();
            current.setApplicationId(application.getApplicationId());
            current.setUserId(application.getUserId());
            current.setRoundId(round.getRoundId());
            current.setDepartment(department);
            current.setResultStatus(QtInterviewStatuses.PENDING);
            current.setInterviewTime(interviewTime);
            current.setCreateBy(operator);
            current.setUpdateBy(operator);
            qtInterviewMapper.insertInterviewResult(current);
            return;
        }
        current.setInterviewTime(interviewTime);
        current.setUpdateBy(operator);
        qtInterviewMapper.updateInterviewResult(current);
    }

    private String requireChoiceDepartment(String department, QtInterviewApplication application)
    {
        if (StringUtils.isEmpty(department))
        {
            throw new ServiceException("department 不能为空");
        }
        if (!department.equals(application.getFirstChoice()) && !department.equals(application.getSecondChoice()))
        {
            throw new ServiceException("该部门不在候选人志愿中");
        }
        return department;
    }

    private void assertAdvanced(QtInterviewApplication application, String department)
    {
        QtInterviewRound roundOne = qtInterviewMapper.selectRoundByNo(1);
        if (roundOne == null)
        {
            throw new ServiceException("面试轮次不存在");
        }
        QtInterviewResult firstRound = qtInterviewMapper.selectResultByAppRoundDept(application.getApplicationId(),
                roundOne.getRoundId(), department);
        if (!QtInterviewStatuses.isPass(firstRound))
        {
            throw new ServiceException("该志愿未通过一面，不能进行二面操作");
        }
    }

    private void refreshRoundOneStatus(QtInterviewApplication application, String operator)
    {
        QtInterviewRound roundOne = qtInterviewMapper.selectRoundByNo(1);
        QtInterviewResult first = resultOf(application, roundOne, application.getFirstChoice());
        QtInterviewResult second = resultOf(application, roundOne, application.getSecondChoice());
        if (QtInterviewStatuses.isOut(first) && QtInterviewStatuses.isOut(second))
        {
            application.setApplyStatus("REJECTED");
        }
        else
        {
            markProcessing(application, operator);
            return;
        }
        application.setUpdateBy(operator);
        qtInterviewMapper.updateApplicationAdminFields(application);
    }

    private void refreshRoundTwoOffer(QtInterviewApplication application, String operator)
    {
        QtInterviewRound roundTwo = qtInterviewMapper.selectRoundByNo(2);
        boolean firstAdvanced = isAdvanced(application, application.getFirstChoice());
        boolean secondAdvanced = isAdvanced(application, application.getSecondChoice());
        QtInterviewResult first = resultOf(application, roundTwo, application.getFirstChoice());
        QtInterviewResult second = resultOf(application, roundTwo, application.getSecondChoice());
        boolean firstPass = firstAdvanced && QtInterviewStatuses.isPass(first);
        boolean secondPass = secondAdvanced && QtInterviewStatuses.isPass(second);
        boolean firstOut = !firstAdvanced || QtInterviewStatuses.isOut(first);
        boolean firstPending = firstAdvanced && !QtInterviewStatuses.isDecided(first);
        boolean secondOut = !secondAdvanced || QtInterviewStatuses.isOut(second);

        String offered = null;
        String applyStatus = "PROCESSING";
        if (firstPass)
        {
            offered = application.getFirstChoice();
            applyStatus = "OFFERED";
        }
        else if (secondPass && firstOut && !firstPending)
        {
            offered = application.getSecondChoice();
            applyStatus = "OFFERED";
        }
        else if (allAdvancedOut(firstAdvanced, secondAdvanced, firstOut, secondOut))
        {
            applyStatus = "REJECTED";
            offered = null;
        }

        application.setOfferedDepartment(offered);
        application.setApplyStatus(applyStatus);
        if ("OFFERED".equals(applyStatus) && StringUtils.isEmpty(application.getJoinStatus()))
        {
            application.setJoinStatus("PENDING");
        }
        application.setUpdateBy(operator);
        qtInterviewMapper.updateApplicationAdminFields(application);
        if ("OFFERED".equals(applyStatus))
        {
            convertToMember(application, operator);
        }
    }

    private void bindOfferAndConvert(QtInterviewApplication application, String offeredDepartment, String operator)
    {
        application.setOfferedDepartment(offeredDepartment);
        application.setApplyStatus("OFFERED");
        if (StringUtils.isEmpty(application.getJoinStatus()))
        {
            application.setJoinStatus("PENDING");
        }
        application.setUpdateBy(operator);
        convertToMember(application, operator);
    }

    private boolean allAdvancedOut(boolean firstAdvanced, boolean secondAdvanced, boolean firstOut, boolean secondOut)
    {
        if (!firstAdvanced && !secondAdvanced)
        {
            return false;
        }
        return (!firstAdvanced || firstOut) && (!secondAdvanced || secondOut);
    }

    private boolean isAdvanced(QtInterviewApplication application, String department)
    {
        if (StringUtils.isEmpty(department))
        {
            return false;
        }
        QtInterviewRound roundOne = qtInterviewMapper.selectRoundByNo(1);
        return QtInterviewStatuses.isPass(resultOf(application, roundOne, department));
    }

    private QtInterviewResult resultOf(QtInterviewApplication application, QtInterviewRound round, String department)
    {
        if (application == null || round == null || StringUtils.isEmpty(department))
        {
            return null;
        }
        return qtInterviewMapper.selectResultByAppRoundDept(application.getApplicationId(), round.getRoundId(),
                department);
    }

    private NoticeOutcome requireNoticeReady(QtInterviewApplication application)
    {
        boolean firstAdvanced = isAdvanced(application, application.getFirstChoice());
        boolean secondAdvanced = isAdvanced(application, application.getSecondChoice());
        if (!firstAdvanced && !secondAdvanced)
        {
            throw new ServiceException("没有进入二面的志愿，不能发送结果邮件");
        }
        QtInterviewRound roundTwo = qtInterviewMapper.selectRoundByNo(2);
        QtInterviewResult first = resultOf(application, roundTwo, application.getFirstChoice());
        QtInterviewResult second = resultOf(application, roundTwo, application.getSecondChoice());
        if (firstAdvanced && (first == null || first.getScore() == null || !QtInterviewStatuses.isDecided(first)))
        {
            throw new ServiceException("请先完成所有二面志愿的评分和录用决定");
        }
        if (secondAdvanced && (second == null || second.getScore() == null || !QtInterviewStatuses.isDecided(second)))
        {
            throw new ServiceException("请先完成所有二面志愿的评分和录用决定");
        }
        boolean firstPass = firstAdvanced && QtInterviewStatuses.isPass(first);
        boolean secondPass = secondAdvanced && QtInterviewStatuses.isPass(second);
        String offered = firstPass ? application.getFirstChoice()
                : (secondPass ? application.getSecondChoice() : null);
        return new NoticeOutcome(offered != null, offered);
    }

    private Map<String, Object> buildNoticePreview(QtInterviewApplication application, NoticeOutcome outcome,
            boolean preview)
    {
        String deptLabel = interviewNotifier.departmentLabel(outcome.offeredDepartment);
        NoticeMail mail = QtInterviewNoticeTemplates.render(application.getRealName(), outcome.offeredDepartment,
                outcome.pass, preview);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("recipientName", application.getRealName());
        data.put("email", application.getEmail());
        data.put("offeredDepartment", outcome.offeredDepartment);
        data.put("offeredDepartmentLabel", StringUtils.isEmpty(deptLabel) ? null : deptLabel);
        data.put("result", outcome.pass ? QtInterviewStatuses.PASS : QtInterviewStatuses.OUT);
        data.put("resultLabel", outcome.pass ? "Pass" : "Out");
        data.put("subject", mail.subject);
        data.put("content", mail.html);
        data.put("plainContent", mail.plain);
        return data;
    }

    private byte[] requireQrBytes(MultipartFile qrCode)
    {
        if (qrCode == null || qrCode.isEmpty())
        {
            throw new ServiceException("录用邮件必须添加群二维码");
        }
        try
        {
            FileValidator.validate(qrCode, new String[] { "jpg", "jpeg", "png" }, FileValidator.SIZE_IMAGE);
            return qrCode.getBytes();
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("二维码图片不合法");
        }
    }

    private String qrFileName(MultipartFile qrCode)
    {
        String name = qrCode == null ? null : qrCode.getOriginalFilename();
        if (StringUtils.isEmpty(name))
        {
            return "qrcode.png";
        }
        return name;
    }

    private String qrContentType(String filename)
    {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        return "image/png";
    }

    private static final class NoticeOutcome
    {
        private final boolean pass;
        private final String offeredDepartment;

        private NoticeOutcome(boolean pass, String offeredDepartment)
        {
            this.pass = pass;
            this.offeredDepartment = offeredDepartment;
        }
    }

    /**
     * 录用为普通塔员：将申请人转换为正式塔员。
     * <p>
     * 步骤：
     * 1. 设置 sys_user.is_quanta_member = '1'，并写入 member_department（录用部门）；
     * 2. 幂等绑定 qt_member 角色（roleId 通过 roleKey 查询）；
     * 3. 在 qt_member_record 中建立当前届次的成员档案（roleCategory = MEMBER，memberStatus = ACTIVE）。
     * <p>
     * 仅在 offer 决策为 PASS 且 applyStatus 变为 OFFERED 时调用。
     */
    private void convertToMember(QtInterviewApplication application, String operator)
    {
        Long userId = application.getUserId();
        if (userId == null)
        {
            return;
        }
        SysUser user = userService.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在，无法转为塔员：userId=" + userId);
        }
        String offeredDept = application.getOfferedDepartment();
        user.setIsQuantaMember("1");
        if (StringUtils.isNotEmpty(offeredDept))
        {
            user.setMemberDepartment(offeredDept);
        }
        if (StringUtils.isNotEmpty(operator))
        {
            user.setUpdateBy(operator);
        }
        userService.updateUserProfile(user);

        // 幂等绑定 qt_member 角色
        Long qtMemberRoleId = resolveRoleIdByRoleKey("qt_member");
        if (qtMemberRoleId != null)
        {
            qtInterviewMapper.insertUserRoleIfAbsent(userId, qtMemberRoleId);
        }

        // 建立当前届次成员档案
        QtCohort current = qtCohortMapper.selectCurrentCohort();
        if (current != null)
        {
            QtMemberRecord existing = qtCohortMapper.selectRecord(userId, current.getCohortId());
            if (existing == null)
            {
                QtMemberRecord record = new QtMemberRecord();
                record.setUserId(userId);
                record.setCohortId(current.getCohortId());
                record.setRoleCategory("MEMBER");
                record.setMemberStatus("ACTIVE");
                record.setJoinTime(new Date());
                record.setRetainFlag("0");
                record.setCreateBy(operator);
                qtCohortMapper.insertRecord(record);
            }
            else if (!"ACTIVE".equals(existing.getMemberStatus()) || !"MEMBER".equals(existing.getRoleCategory()))
            {
                existing.setRoleCategory("MEMBER");
                existing.setMemberStatus("ACTIVE");
                existing.setUpdateBy(operator);
                qtCohortMapper.updateRecord(existing);
            }
        }
    }

    private Long resolveRoleIdByRoleKey(String roleKey)
    {
        if (StringUtils.isEmpty(roleKey))
        {
            return null;
        }
        for (SysRole role : roleService.selectRoleAll())
        {
            if (roleKey.equals(role.getRoleKey()))
            {
                return role.getRoleId();
            }
        }
        return null;
    }
}
