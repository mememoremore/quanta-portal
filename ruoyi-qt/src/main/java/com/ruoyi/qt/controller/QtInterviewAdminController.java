package com.ruoyi.qt.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewDecisionBody;
import com.ruoyi.qt.domain.QtInterviewEvaluation;
import com.ruoyi.qt.domain.QtInterviewOfferBody;
import com.ruoyi.qt.domain.QtInterviewProfile;
import com.ruoyi.qt.domain.QtInterviewScoreBody;
import com.ruoyi.qt.domain.QtInterviewTimeBody;
import com.ruoyi.qt.domain.QtStatusBody;
import com.ruoyi.qt.service.IQtInterviewAdminService;
import com.ruoyi.qt.util.QtAuthUtils;

@RestController
@RequestMapping("/qt/interview/admin")
public class QtInterviewAdminController extends BaseController
{
    @Autowired
    private IQtInterviewAdminService qtInterviewAdminService;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ProfileAccessSigner profileAccessSigner;

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @GetMapping("/applications")
    public TableDataInfo applications(QtInterviewApplication query,
            @RequestParam(value = "round", required = false) Long round,
            @RequestParam(value = "departmentId", required = false) String departmentId,
            @RequestParam(value = "status", required = false) String status)
    {
        QtAuthUtils.requireQuantaMember();
        applyListAliases(query, round, departmentId, status);
        startPage();
        List<QtInterviewApplication> list = qtInterviewAdminService.selectAdminList(query);
        fillAccessUrls(list);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @GetMapping("/applications/{id:\\d+}")
    public AjaxResult application(@PathVariable("id") Long id)
    {
        QtAuthUtils.requireQuantaMember();
        QtInterviewApplication application = qtInterviewAdminService.selectApplication(id);
        fillAccessUrls(java.util.Collections.singletonList(application));
        QtInterviewProfile profile = qtInterviewAdminService.selectProfile(id);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("application", application);
        data.put("profile", profile);
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @GetMapping("/applications/{id:\\d+}/results")
    public AjaxResult results(@PathVariable("id") Long id)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtInterviewAdminService.selectResults(id));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @GetMapping("/evaluations")
    public AjaxResult evaluations(QtInterviewEvaluation query)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtInterviewAdminService.selectEvaluations(query));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:evaluate')")
    @Log(title = "招新面评", businessType = BusinessType.INSERT)
    @PostMapping("/evaluations")
    public AjaxResult addEvaluation(@RequestBody QtInterviewEvaluation evaluation)
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtInterviewAdminService.saveEvaluation(evaluation, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:evaluate')")
    @Log(title = "招新面评", businessType = BusinessType.UPDATE)
    @PutMapping("/evaluations/{evaluationId}")
    public AjaxResult editEvaluation(@PathVariable Long evaluationId, @RequestBody QtInterviewEvaluation evaluation)
    {
        QtAuthUtils.requireQuantaMember();
        evaluation.setEvaluationId(evaluationId);
        return success(qtInterviewAdminService.updateEvaluation(evaluation, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @Log(title = "招新录用", businessType = BusinessType.UPDATE)
    @PostMapping("/offers")
    public AjaxResult offers(@RequestBody QtInterviewOfferBody body)
    {
        return success(qtInterviewAdminService.offer(body, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @Log(title = "招新评定", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/{applicationId}/rounds/{roundNo}/departments/{department}/decision")
    public AjaxResult decision(@PathVariable Long applicationId, @PathVariable Long roundNo,
            @PathVariable String department, @RequestBody QtInterviewDecisionBody body)
    {
        String decision = body == null ? null : body.getDecision();
        return success(qtInterviewAdminService.saveDecision(applicationId, roundNo, department, decision, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @Log(title = "一面面试时间", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/{applicationId}/rounds/{roundNo}/interview-time")
    public AjaxResult interviewTime(@PathVariable Long applicationId, @PathVariable Long roundNo,
            @RequestBody QtInterviewTimeBody body)
    {
        QtAuthUtils.requireQuantaMember();
        String interviewTime = body == null ? null : body.getInterviewTime();
        return success(qtInterviewAdminService.saveFirstRoundInterviewTime(applicationId, roundNo, interviewTime,
                getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:evaluate')")
    @Log(title = "二面评分", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/{applicationId}/round2/departments/{department}/score")
    public AjaxResult score(@PathVariable Long applicationId, @PathVariable String department,
            @RequestBody QtInterviewScoreBody body)
    {
        Integer score = body == null ? null : body.getScore();
        return success(qtInterviewAdminService.saveScore(applicationId, department, score, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @GetMapping("/applications/{applicationId}/notice-preview")
    public AjaxResult noticePreview(@PathVariable Long applicationId)
    {
        return success(qtInterviewAdminService.previewNotice(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @Log(title = "招新结果邮件", businessType = BusinessType.UPDATE)
    @PostMapping("/applications/{applicationId}/notice")
    public AjaxResult notice(@PathVariable Long applicationId,
            @RequestParam(value = "qrCode", required = false) MultipartFile qrCode)
    {
        return success(qtInterviewAdminService.sendNotice(applicationId, qrCode, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:list')")
    @GetMapping("/statistics")
    public AjaxResult statistics()
    {
        QtAuthUtils.requireQuantaMember();
        return success(qtInterviewAdminService.statistics());
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:export')")
    @Log(title = "招新导出", businessType = BusinessType.EXPORT)
    @GetMapping("/applications/export")
    public void exportGet(HttpServletResponse response, QtInterviewApplication query,
            @RequestParam(value = "round", required = false) Long round,
            @RequestParam(value = "departmentId", required = false) String departmentId,
            @RequestParam(value = "status", required = false) String status)
    {
        export(response, query, round, departmentId, status);
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:export')")
    @Log(title = "招新导出", businessType = BusinessType.EXPORT)
    @PostMapping("/applications/export")
    public void exportPost(HttpServletResponse response, QtInterviewApplication query)
    {
        export(response, query, query.getRoundId(), query.getDepartment(), query.getApplyStatus());
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @Log(title = "入职状态", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/{id:\\d+}/join-status")
    public AjaxResult joinStatus(@PathVariable("id") Long id, @RequestBody QtStatusBody body)
    {
        return success(qtInterviewAdminService.updateJoinStatus(id, body, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('qt:interview:admin:offer')")
    @Log(title = "最终状态", businessType = BusinessType.UPDATE)
    @PutMapping("/applications/{id:\\d+}/final-status")
    public AjaxResult finalStatus(@PathVariable("id") Long id, @RequestBody QtStatusBody body)
    {
        return success(qtInterviewAdminService.updateFinalStatus(id, body, getUsername()));
    }

    private void export(HttpServletResponse response, QtInterviewApplication query, Long round, String departmentId,
            String status)
    {
        applyListAliases(query, round, departmentId, status);
        List<QtInterviewApplication> list = qtInterviewAdminService.selectAdminList(query);
        ExcelUtil<QtInterviewApplication> util = new ExcelUtil<QtInterviewApplication>(QtInterviewApplication.class);
        util.exportExcel(response, list, "招新申请");
    }

    private void applyListAliases(QtInterviewApplication query, Long round, String departmentId, String status)
    {
        if (query.getRoundId() == null && round != null)
        {
            query.setRoundId(round);
        }
        if (StringUtils.isEmpty(query.getDepartment()) && StringUtils.isNotEmpty(departmentId))
        {
            query.setDepartment(departmentId);
        }
        if (StringUtils.isNotEmpty(status))
        {
            if (query.getRoundId() != null)
            {
                query.setResultStatus(status);
            }
            else if (StringUtils.isEmpty(query.getApplyStatus()))
            {
                query.setApplyStatus(status);
            }
        }
    }

    private void fillAccessUrls(List<QtInterviewApplication> list)
    {
        if (list == null)
        {
            return;
        }
        for (QtInterviewApplication application : list)
        {
            com.ruoyi.qt.util.QtInterviewAccessUrls.fill(application, profileAccessSigner, serverConfig);
        }
    }
}
