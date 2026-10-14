package com.ruoyi.qt.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileValidator;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.qt.domain.QtInterviewApplication;
import com.ruoyi.qt.domain.QtInterviewProfile;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.service.IQtInterviewAdminService;
import com.ruoyi.qt.service.IQtInterviewService;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.framework.security.ProfileAccessSigner;

@RestController
@RequestMapping("/qt/interview")
public class QtInterviewController extends BaseController
{
    @Autowired
    private IQtInterviewService qtInterviewService;

    @Autowired
    private IQtInterviewAdminService qtInterviewAdminService;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ProfileAccessSigner profileAccessSigner;

    @PostMapping("/apply")
    @RateLimiter(time = 60, count = 5, limitType = LimitType.USER, key = "rate_limit:apply:")
    @RepeatSubmit(message = "正在提交简历，请勿重复提交")
    public AjaxResult apply(QtInterviewApplication application, QtInterviewProfile profile,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile)
    {
        if (StringUtils.isEmpty(application.getFirstChoice()) || StringUtils.isEmpty(application.getSecondChoice()))
        {
            return AjaxResult.error("请完整选择两个志愿");
        }
        if (application.getFirstChoice().equals(application.getSecondChoice()))
        {
            return AjaxResult.error("两个志愿不能相同");
        }
        com.ruoyi.qt.util.QtDictUtils.requireActiveDepartment(application.getFirstChoice(), "firstChoice");
        com.ruoyi.qt.util.QtDictUtils.requireActiveDepartment(application.getSecondChoice(), "secondChoice");
        application.setUserId(getUserId());
        application.setApplyStatus("SUBMITTED");
        application.setUpdateBy(getUsername());
        if (StringUtils.isEmpty(application.getCreateBy()))
        {
            application.setCreateBy(getUsername());
        }

        try
        {
            if (photoFile != null && !photoFile.isEmpty())
            {
                application.setPhotoUrl(uploadPhoto(photoFile));
            }
            else if (StringUtils.isEmpty(application.getPhotoUrl()))
            {
                return AjaxResult.error("请上传证件照");
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error(photoUploadMessage(e));
        }

        try
        {
            if (resumeFile != null && !resumeFile.isEmpty())
            {
                application.setResumeUrl(uploadResume(resumeFile));
                application.setResumeFileName(originalFileName(resumeFile));
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error(resumeUploadMessage(e));
        }

        profile.setUserId(getUserId());
        profile.setUpdateBy(getUsername());
        if (StringUtils.isEmpty(profile.getCreateBy()))
        {
            profile.setCreateBy(getUsername());
        }
        if (StringUtils.isEmpty(profile.getCodingExperience()))
        {
            profile.setCodingExperience("0");
        }
        return toAjax(qtInterviewService.saveMyApplication(application, profile));
    }

    @GetMapping("/my")
    public AjaxResult myApplication()
    {
        QtInterviewApplication application = qtInterviewService.selectMyApplication(getUserId());
        QtInterviewProfile profile = qtInterviewService.selectMyProfile(getUserId());
        if (application != null)
        {
            com.ruoyi.qt.util.QtInterviewAccessUrls.fill(application, profileAccessSigner, serverConfig);
        }
        boolean newApplicationsOpen = qtInterviewService.isNewApplicationsOpen();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("application", application);
        data.put("profile", profile);
        data.put("newApplicationsOpen", newApplicationsOpen);
        data.put("canUpdate", newApplicationsOpen || application != null);
        return success(data);
    }

    @GetMapping("/myResults")
    public AjaxResult myResults()
    {
        List<QtInterviewResult> list = qtInterviewService.selectMyResultList(getUserId());
        return success(list);
    }

    /**
     * 塔员只读查看全部新生报名信息（仅基本信息，剥离电话/照片等敏感字段）
     */
    @GetMapping("/member/applications")
    public AjaxResult memberApplications(QtInterviewApplication query)
    {
        com.ruoyi.qt.util.QtAuthUtils.requireQuantaMember();
        List<QtInterviewApplication> list = qtInterviewAdminService.selectMemberList(query);
        for (QtInterviewApplication app : list)
        {
            com.ruoyi.qt.util.QtInterviewAccessUrls.clearSensitiveFiles(app);
        }
        return success(list);
    }

    /**
     * 录入/更新一面二面等轮次结果。管理端请优先使用 /qt/interview/admin/*。
     */
    @PreAuthorize("@ss.hasPermi('qt:interview:admin:evaluate')")
    @PostMapping("/result")
    public AjaxResult saveResult(@org.springframework.web.bind.annotation.RequestBody QtInterviewResult result)
    {
        com.ruoyi.qt.util.QtAuthUtils.requireManagement();
        if (result.getUserId() == null)
        {
            return AjaxResult.error("userId不能为空");
        }
        if (result.getRoundId() == null)
        {
            return AjaxResult.error("roundId不能为空");
        }
        if (StringUtils.isEmpty(result.getDepartment()))
        {
            return AjaxResult.error("department不能为空");
        }
        com.ruoyi.qt.util.QtAuthUtils.assertDepartmentScope(result.getDepartment());
        String status = com.ruoyi.qt.util.QtInterviewStatuses.normalizeDecision(result.getResultStatus());
        if (status == null)
        {
            return AjaxResult.error("resultStatus 只能是 PASS 或 OUT");
        }
        if (result.getApplicationId() == null)
        {
            QtInterviewApplication application = qtInterviewService.selectMyApplication(result.getUserId());
            if (application == null)
            {
                return AjaxResult.error("该用户尚未投递简历");
            }
            result.setApplicationId(application.getApplicationId());
        }
        return success(qtInterviewAdminService.saveDecision(result.getApplicationId(), result.getRoundId(),
                result.getDepartment(), status, getUsername()));
    }

    private String uploadPhoto(MultipartFile photoFile) throws Exception
    {
        String uploadPath = RuoYiConfig.getUploadPath() + "/qt/interview-photo";
        return FileUploadUtils.upload(uploadPath, photoFile, MimeTypeUtils.IMAGE_EXTENSION,
                FileValidator.SIZE_INTERVIEW_PHOTO);
    }

    private String uploadResume(MultipartFile resumeFile) throws Exception
    {
        String uploadPath = RuoYiConfig.getUploadPath() + "/qt/interview-resume";
        return FileUploadUtils.upload(uploadPath, resumeFile, MimeTypeUtils.PDF_EXTENSION, true,
                FileValidator.SIZE_RESUME);
    }

    /** 上传异常面向学生，去掉后端术语，直接说清楚该怎么改。 */
    private String photoUploadMessage(Exception e)
    {
        if (isSizeExceeded(e))
        {
            return "证件照不能超过 " + FileValidator.SIZE_INTERVIEW_PHOTO / 1024 / 1024 + " MB，请压缩后重试";
        }
        return fallback(e, "证件照上传失败，请重试");
    }

    private String resumeUploadMessage(Exception e)
    {
        if (e instanceof InvalidExtensionException)
        {
            return "简历必须是 PDF 文件";
        }
        if (isSizeExceeded(e))
        {
            return "PDF 简历不能超过 " + FileValidator.SIZE_RESUME / 1024 / 1024 + " MB，请压缩后重试";
        }
        return fallback(e, "简历上传失败，请重试");
    }

    private static boolean isSizeExceeded(Exception e)
    {
        return e.getMessage() != null && e.getMessage().contains("文件大小超出限制");
    }

    private static String fallback(Exception e, String message)
    {
        return StringUtils.isEmpty(e.getMessage()) ? message : e.getMessage();
    }

    private String originalFileName(MultipartFile file)
    {
        if (file == null)
        {
            return null;
        }
        String name = file.getOriginalFilename();
        if (StringUtils.isEmpty(name))
        {
            return "resume.pdf";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
