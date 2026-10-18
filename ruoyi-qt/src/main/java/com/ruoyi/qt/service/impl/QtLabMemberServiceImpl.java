package com.ruoyi.qt.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtLabMemberImportRow;
import com.ruoyi.qt.domain.QtMemberManageBody;
import com.ruoyi.qt.domain.QtMemberRecord;
import com.ruoyi.qt.domain.QtMemberRetainBody;
import com.ruoyi.qt.mapper.QtCohortMapper;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.service.IQtLabMemberService;
import com.ruoyi.qt.util.QtAuthUtils;
import com.ruoyi.qt.util.QtLabMemberImportParser;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class QtLabMemberServiceImpl implements IQtLabMemberService
{
    private static final Logger log = LoggerFactory.getLogger(QtLabMemberServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern LOGIN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.@-]{2,20}$");

    private static final Set<String> MANAGED_ROLE_KEYS = Set.of("qt_member", "qt_mgmt", "qt_manager");

    private static final Map<String, List<String>> ROLE_TITLES = new LinkedHashMap<String, List<String>>();

    static
    {
        ROLE_TITLES.put("MGMT", List.of("VP", "负责人", "CTO"));
        ROLE_TITLES.put("MANAGER", List.of("经理"));
        ROLE_TITLES.put("INTERN", List.of("实习生"));
        ROLE_TITLES.put("MEMBER", List.of("成员"));
    }

    @Autowired
    private QtLabMemberMapper qtLabMemberMapper;

    @Autowired
    private QtCohortMapper qtCohortMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private TokenService tokenService;

    @Override
    public List<QtLabMember> selectLabMemberList(QtLabMember query)
    {
        List<QtLabMember> list = qtLabMemberMapper.selectLabMemberList(query);
        if (list != null)
        {
            for (QtLabMember member : list)
            {
                member.setPhonenumber(null);
            }
        }
        return list;
    }

    @Override
    public List<QtLabMember> selectAdminMemberList(QtLabMember query)
    {
        // startPage 的分页只作用于第一条 SQL。先取出并清掉，避免被届次查询吃掉；
        // 届次解析完再放回线程变量，LIMIT 只落在成员名单 SQL 上。
        Page<?> page = PageHelper.getLocalPage();
        PageHelper.clearPage();
        try
        {
            resolveCohortFilter(query);
        }
        finally
        {
            if (page != null)
            {
                PageHelper.setLocalPage(page);
            }
        }
        return qtLabMemberMapper.selectAdminMemberList(query);
    }

    /**
     * 届次页签传届次编号或 unassigned。真正的届在 qt_member_record.cohort_id，
     * 历史数据里 sys_user.member_cohort 可能是编号或届次名称，作为兜底一并匹配。
     */
    private void resolveCohortFilter(QtLabMember query)
    {
        String value = text(query == null ? null : query.getMemberCohort());
        if ("unassigned".equalsIgnoreCase(value))
        {
            query.setCohortId(null);
            query.setCohortName(null);
            query.setUnassignedCohort(Boolean.TRUE);
            query.setMemberCohort(null);
            return;
        }
        query.setUnassignedCohort(Boolean.FALSE);
        QtCohort cohort = null;
        if (StringUtils.isNotEmpty(value))
        {
            try
            {
                cohort = qtCohortMapper.selectCohortById(Long.valueOf(value));
            }
            catch (NumberFormatException ex)
            {
                cohort = null;
            }
            if (cohort == null)
            {
                cohort = qtCohortMapper.selectCohortByName(value);
            }
        }
        if (cohort != null)
        {
            query.setCohortId(cohort.getCohortId());
            query.setCohortName(cohort.getCohortName());
        }
        else
        {
            query.setCohortId(null);
            query.setCohortName(null);
        }
        query.setMemberCohort(null);
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.CACHE_QT_MEMBER_COHORTS, key = "'all'")
    public Map<String, Object> selectCohorts()
    {
        List<QtCohort> list = qtCohortMapper.selectCohortList(new QtCohort());
        QtCohort current = qtCohortMapper.selectCurrentCohort();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("currentCohort", current);
        List<Map<String, Object>> cohorts = new ArrayList<Map<String, Object>>();
        for (QtCohort cohort : list)
        {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", cohort.getCohortId());
            item.put("name", cohort.getCohortName());
            item.put("isCurrent", "1".equals(cohort.getIsCurrent()));
            cohorts.add(item);
        }
        data.put("cohorts", cohorts);
        return data;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_MEMBER_COHORTS, allEntries = true)
    public Map<String, Object> retain(Long userId, QtMemberRetainBody body, String operator)
    {
        if (body == null || !Boolean.TRUE.equals(body.getRetain()))
        {
            throw new ServiceException("retain 必须为 true");
        }
        QtCohort source = body.getSourceCohortId() == null ? qtCohortMapper.selectCurrentCohort()
                : qtCohortMapper.selectCohortById(body.getSourceCohortId());
        if (source == null)
        {
            throw new ServiceException("源届次不存在");
        }
        QtMemberRecord sourceRecord = qtCohortMapper.selectRecord(userId, source.getCohortId());
        if (sourceRecord == null)
        {
            throw new ServiceException("该用户在源届次中没有档案");
        }
        QtCohort target;
        boolean created = false;
        if (body.getTargetCohortId() != null)
        {
            target = qtCohortMapper.selectCohortById(body.getTargetCohortId());
            if (target == null)
            {
                throw new ServiceException("目标届次不存在");
            }
        }
        else
        {
            String nextName = nextCohortName(source);
            created = qtCohortMapper.selectCohortByName(nextName) == null;
            target = getOrCreateNextCohort(source, operator);
        }
        QtMemberRecord existing = qtCohortMapper.selectRecord(userId, target.getCohortId());
        if (existing == null)
        {
            QtMemberRecord next = new QtMemberRecord();
            next.setUserId(userId);
            next.setCohortId(target.getCohortId());
            next.setRoleCategory(sourceRecord.getRoleCategory());
            next.setMemberStatus("ACTIVE");
            next.setJoinTime(new Date());
            next.setRetainFlag("0");
            next.setCreateBy(operator);
            qtCohortMapper.insertRecord(next);
            existing = next;
        }
        sourceRecord.setMemberStatus("RETAINED");
        sourceRecord.setRetainFlag("1");
        sourceRecord.setUpdateBy(operator);
        qtCohortMapper.updateRecord(sourceRecord);

        SysUser user = userService.selectUserById(userId);
        if (user != null)
        {
            user.setMemberCohort(target.getCohortName());
            user.setIsQuantaMember("1");
            user.setUpdateBy(operator);
            userService.updateUser(user);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("sourceStatus", sourceRecord.getMemberStatus());
        data.put("targetRecord", existing);
        data.put("createdCohort", created);
        return data;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.CACHE_QT_MEMBER_COHORTS, allEntries = true)
    public void rollover()
    {
        QtCohort current = qtCohortMapper.selectCurrentCohort();
        if (current == null)
        {
            return;
        }
        QtCohort next = getOrCreateNextCohort(current, "system");
        List<QtMemberRecord> records = qtCohortMapper.selectRecordsByCohort(current.getCohortId());
        Date now = new Date();
        for (QtMemberRecord record : records)
        {
            if (!"ACTIVE".equals(record.getMemberStatus()))
            {
                continue;
            }
            String role = record.getRoleCategory();
            boolean retainConfirmed = "1".equals(record.getRetainFlag());
            if ("MGMT".equals(role) || (("MANAGER".equals(role) || "INTERN".equals(role)) && !retainConfirmed))
            {
                record.setMemberStatus("RESIGNED");
                record.setUpdateBy("system");
                qtCohortMapper.updateRecord(record);
                SysUser user = userService.selectUserById(record.getUserId());
                if (user != null)
                {
                    user.setIsQuantaMember("0");
                    user.setUpdateBy("system");
                    userService.updateUser(user);
                }
                continue;
            }
            if (retainConfirmed || "MEMBER".equals(role) || "CEO".equals(role))
            {
                record.setMemberStatus("RETAINED");
                record.setRetainFlag("1");
                record.setUpdateBy("system");
                qtCohortMapper.updateRecord(record);
                if (qtCohortMapper.selectRecord(record.getUserId(), next.getCohortId()) == null)
                {
                    QtMemberRecord nextRecord = new QtMemberRecord();
                    nextRecord.setUserId(record.getUserId());
                    nextRecord.setCohortId(next.getCohortId());
                    nextRecord.setRoleCategory(record.getRoleCategory());
                    nextRecord.setMemberStatus("ACTIVE");
                    nextRecord.setJoinTime(now);
                    nextRecord.setRetainFlag("0");
                    nextRecord.setCreateBy("system");
                    qtCohortMapper.insertRecord(nextRecord);
                }
                SysUser user = userService.selectUserById(record.getUserId());
                if (user != null)
                {
                    user.setMemberCohort(next.getCohortName());
                    user.setIsQuantaMember("1");
                    user.setUpdateBy("system");
                    userService.updateUser(user);
                }
            }
        }
        qtCohortMapper.clearCurrentFlag();
        next.setIsCurrent("1");
        next.setUpdateBy("system");
        qtCohortMapper.updateCohort(next);
        current.setIsCurrent("0");
        current.setStatus("ARCHIVED");
        current.setUpdateBy("system");
        qtCohortMapper.updateCohort(current);
    }

    @Override
    @CacheEvict(cacheNames = { CacheConstants.CACHE_QT_MEMBER_COHORTS, CacheConstants.CACHE_QT_DASHBOARD_STATS },
            allEntries = true)
    public String importLabMembers(InputStream inputStream, boolean updateSupport, String operator)
    {
        List<QtLabMemberImportRow> rows = QtLabMemberImportParser.parse(inputStream);
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (QtLabMemberImportRow row : rows)
        {
            try
            {
                provisionMember(row, updateSupport, operator);
                successNum++;
                successMsg.append("<br/>" + successNum + "\u3001\u8d26\u53f7 " + row.getUserName() + " \u5f00\u901a\u6210\u529f");
            }
            catch (Exception ex)
            {
                failureNum++;
                String name = StringUtils.isEmpty(row.getUserName()) ? ("\u7b2c" + row.getSourceRow() + "\u884c")
                        : row.getUserName();
                failureMsg.append("<br/>" + failureNum + "\u3001\u8d26\u53f7 " + name + " \u5bfc\u5165\u5931\u8d25\uff1a" + ex.getMessage());
                log.error("import lab member failed: {}", name, ex);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "\u5f88\u62b1\u6b49\uff0c\u5bfc\u5165\u5931\u8d25\uff01\u5171 " + failureNum
                    + " \u6761\u6570\u636e\u683c\u5f0f\u4e0d\u6b63\u786e\uff0c\u9519\u8bef\u5982\u4e0b\uff1a");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "\u606d\u559c\u60a8\uff0c\u6570\u636e\u5df2\u5168\u90e8\u5bfc\u5165\u6210\u529f\uff01\u5171 " + successNum
                + " \u6761\uff0c\u6570\u636e\u5982\u4e0b\uff1a");
        return successMsg.toString();
    }

    private void provisionMember(QtLabMemberImportRow row, boolean updateSupport, String operator)
    {
        validateRow(row);
        SysUser existing = userService.selectUserByUserName(row.getUserName());
        SysUser user = toUser(row, operator);
        user.setRoleIds(resolveRoleIds(row.getRoleCategory()));
        QtCohort cohort = resolveCohort(row.getMemberCohort());
        user.setMemberCohort(cohort.getCohortName());
        if (existing == null)
        {
            String password = configService.selectConfigByKey("sys.user.initPassword");
            if (StringUtils.isEmpty(password))
            {
                password = "123456";
            }
            user.setPassword(SecurityUtils.encryptPassword(password));
            user.setCreateBy(operator);
            userService.insertUser(user);
        }
        else if (updateSupport)
        {
            userService.checkUserAllowed(existing);
            user.setUserId(existing.getUserId());
            user.setPassword(null);
            user.setUpdateBy(operator);
            userService.updateUser(user);
        }
        else
        {
            throw new ServiceException("\u8d26\u53f7\u5df2\u5b58\u5728");
        }
        upsertMemberRecord(user.getUserId(), cohort.getCohortId(), row.getRoleCategory(), operator);
    }

    private void validateRow(QtLabMemberImportRow row)
    {
        if (StringUtils.isEmpty(row.getUserName()))
        {
            throw new ServiceException("\u767b\u5f55\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (row.getUserName().length() < UserConstants.USERNAME_MIN_LENGTH
                || row.getUserName().length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("\u8d26\u53f7\u957f\u5ea6\u5fc5\u987b\u5728 2 \u5230 20 \u4e2a\u5b57\u7b26\u4e4b\u95f4");
        }
        if (StringUtils.isEmpty(row.getNickName()))
        {
            row.setNickName(row.getUserName());
        }
        if (StringUtils.isEmpty(row.getEmail()) || !EMAIL_PATTERN.matcher(row.getEmail()).matches())
        {
            throw new ServiceException("\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        if (!"PRODUCT".equals(row.getMemberDepartment()) && !"DESIGN".equals(row.getMemberDepartment())
                && !"FRONTEND".equals(row.getMemberDepartment()) && !"BACKEND".equals(row.getMemberDepartment()))
        {
            throw new ServiceException("\u6210\u5458\u90e8\u95e8\u4ec5\u652f\u6301 PRODUCT/DESIGN/FRONTEND/BACKEND");
        }
        SysUser unique = new SysUser();
        unique.setUserName(row.getUserName());
        unique.setEmail(row.getEmail());
        unique.setPhonenumber(row.getPhonenumber());
        SysUser existing = userService.selectUserByUserName(row.getUserName());
        if (existing != null)
        {
            unique.setUserId(existing.getUserId());
        }
        if (!userService.checkUserNameUnique(unique) && existing == null)
        {
            throw new ServiceException("\u767b\u5f55\u8d26\u53f7\u5df2\u5b58\u5728");
        }
        if (!userService.checkEmailUnique(unique))
        {
            throw new ServiceException("\u90ae\u7bb1\u8d26\u53f7\u5df2\u5b58\u5728");
        }
        if (StringUtils.isNotEmpty(row.getPhonenumber()) && !userService.checkPhoneUnique(unique))
        {
            throw new ServiceException("\u624b\u673a\u53f7\u7801\u5df2\u5b58\u5728");
        }
        if (StringUtils.isNotEmpty(row.getStudentNo()) && !userService.checkStudentNoUnique(row.getStudentNo()))
        {
            SysUser byName = userService.selectUserByUserName(row.getUserName());
            if (byName == null || !row.getStudentNo().equals(byName.getStudentNo()))
            {
                throw new ServiceException("\u5b66\u53f7\u5df2\u5b58\u5728");
            }
        }
    }

    private SysUser toUser(QtLabMemberImportRow row, String operator)
    {
        SysUser user = new SysUser();
        user.setDeptId(row.getDeptId() == null ? QtLabMemberImportParser.DEFAULT_DEPT_ID : row.getDeptId());
        user.setUserName(row.getUserName());
        user.setNickName(row.getNickName());
        user.setEmail(row.getEmail());
        user.setPhonenumber(row.getPhonenumber());
        user.setSex(row.getSex());
        user.setStatus(row.getStatus());
        user.setMemberNo(row.getMemberNo());
        user.setMemberDepartment(row.getMemberDepartment());
        user.setMemberTitle(row.getMemberTitle());
        user.setStudentNo(row.getStudentNo());
        user.setClassName(row.getClassName());
        user.setMajor(row.getMajor());
        user.setIsQuantaMember("1");
        user.setRemark(operator);
        return user;
    }

    private QtCohort resolveCohort(String cohortName)
    {
        if (StringUtils.isNotEmpty(cohortName))
        {
            QtCohort named = qtCohortMapper.selectCohortByName(cohortName.trim());
            if (named != null)
            {
                return named;
            }
            throw new ServiceException("\u5c4a\u6b21\u4e0d\u5b58\u5728\uff1a" + cohortName);
        }
        QtCohort current = qtCohortMapper.selectCurrentCohort();
        if (current == null)
        {
            throw new ServiceException("\u5f53\u524d\u5c4a\u6b21\u4e0d\u5b58\u5728");
        }
        return current;
    }

    private Long[] resolveRoleIds(String roleCategory)
    {
        List<String> keys = new ArrayList<String>();
        keys.add("qt_member");
        if ("MGMT".equals(roleCategory))
        {
            keys.add("qt_mgmt");
        }
        else if ("MANAGER".equals(roleCategory))
        {
            keys.add("qt_manager");
        }
        List<Long> ids = new ArrayList<Long>();
        for (SysRole role : roleService.selectRoleAll())
        {
            if (keys.contains(role.getRoleKey()))
            {
                ids.add(role.getRoleId());
            }
        }
        if (ids.isEmpty())
        {
            throw new ServiceException("\u672a\u627e\u5230\u5854\u5458\u89d2\u8272\uff0c\u8bf7\u5148\u521d\u59cb\u5316 qt_member");
        }
        return ids.toArray(new Long[0]);
    }

    private void upsertMemberRecord(Long userId, Long cohortId, String roleCategory, String operator)
    {
        QtMemberRecord existing = qtCohortMapper.selectRecord(userId, cohortId);
        if (existing == null)
        {
            QtMemberRecord record = new QtMemberRecord();
            record.setUserId(userId);
            record.setCohortId(cohortId);
            record.setRoleCategory(roleCategory);
            record.setMemberStatus("ACTIVE");
            record.setJoinTime(new Date());
            record.setRetainFlag("0");
            record.setCreateBy(operator);
            qtCohortMapper.insertRecord(record);
            return;
        }
        existing.setRoleCategory(roleCategory);
        existing.setMemberStatus("ACTIVE");
        existing.setUpdateBy(operator);
        qtCohortMapper.updateRecord(existing);
    }

    private QtCohort getOrCreateNextCohort(QtCohort source, String operator)
    {
        String nextName = nextCohortName(source);
        QtCohort existing = qtCohortMapper.selectCohortByName(nextName);
        if (existing != null)
        {
            return existing;
        }
        QtCohort next = new QtCohort();
        next.setCohortName(nextName);
        next.setCohortYear(source.getCohortYear() == null ? null : source.getCohortYear() + 1);
        next.setIsCurrent("0");
        next.setStatus("ACTIVE");
        next.setCreateBy(operator);
        qtCohortMapper.insertCohort(next);
        return next;
    }

    private String nextCohortName(QtCohort source)
    {
        String name = source.getCohortName();
        if (StringUtils.isEmpty(name))
        {
            return String.valueOf(source.getCohortYear() == null ? 1 : source.getCohortYear() + 1);
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(name);
        String lastNum = null;
        int lastStart = -1;
        int lastEnd = -1;
        while (matcher.find())
        {
            lastNum = matcher.group(1);
            lastStart = matcher.start(1);
            lastEnd = matcher.end(1);
        }
        if (lastNum == null)
        {
            return name + "-next";
        }
        int next = Integer.parseInt(lastNum) + 1;
        return name.substring(0, lastStart) + next + name.substring(lastEnd);
    }

    @Override
    public Map<String, Object> managementOptions()
    {
        QtAuthUtils.requireMemberCeo();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("contractVersion", "member-admin-v1");
        data.put("canCreate", Boolean.TRUE);
        data.put("departments", departmentOptions());
        List<Map<String, Object>> cohorts = new ArrayList<Map<String, Object>>();
        for (QtCohort cohort : qtCohortMapper.selectCohortList(new QtCohort()))
        {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("value", String.valueOf(cohort.getCohortId()));
            item.put("label", StringUtils.isEmpty(cohort.getCohortName())
                    ? String.valueOf(cohort.getCohortId()) : cohort.getCohortName());
            cohorts.add(item);
        }
        data.put("cohorts", cohorts);
        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        profiles.add(roleProfile("MGMT", "管理层", "后台登录", "按已有管理层模板操作，招新仍按所属部门校验"));
        profiles.add(roleProfile("MANAGER", "经理层", "后台登录", "本部门招新", "本部门资料上传"));
        profiles.add(roleProfile("INTERN", "普通塔员（实习生）", "塔员门户，四方向资料均可查看"));
        profiles.add(roleProfile("MEMBER", "普通成员", "塔员门户，四方向资料均可查看"));
        data.put("roleProfiles", profiles);
        return data;
    }

    @Override
    public Map<String, Object> managedMember(Long userId, String memberCohort)
    {
        QtAuthUtils.requireMemberCeo();
        SysUser user = userService.selectUserById(userId);
        if (user == null || !"0".equals(user.getDelFlag()))
        {
            throw fail("成员不存在", HttpStatus.NOT_FOUND);
        }
        boolean locked = isProtectedMember(user);
        QtMemberRecord record = null;
        String cohortValue = text(memberCohort);
        if (StringUtils.isNotEmpty(cohortValue) && !"unassigned".equalsIgnoreCase(cohortValue))
        {
            QtCohort requested = requireCohort(cohortValue);
            record = qtCohortMapper.selectRecord(userId, requested.getCohortId());
        }
        if (record == null)
        {
            record = qtCohortMapper.selectLatestRecordByUser(userId);
        }
        String roleCategory = record != null ? text(record.getRoleCategory()) : deriveRoleCategory(user.getRoles());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("userId", user.getUserId());
        data.put("nickName", StringUtils.trim(user.getRealName()));
        data.put("userName", user.getUserName());
        data.put("memberDepartment", user.getMemberDepartment());
        data.put("memberTitle", StringUtils.isNotEmpty(user.getMemberTitle()) ? user.getMemberTitle() : defaultTitle(roleCategory));
        data.put("memberCohort", record != null ? String.valueOf(record.getCohortId()) : "");
        data.put("roleCategory", roleCategory);
        data.put("version", memberVersion(user));
        data.put("protected", locked);
        data.put("canEdit", !locked);
        data.put("canAssignRoles", !locked);
        List<String> roles = new ArrayList<String>();
        if (user.getRoles() != null)
        {
            for (SysRole role : user.getRoles())
            {
                if (role != null && StringUtils.isNotEmpty(role.getRoleKey()))
                {
                    roles.add(role.getRoleKey());
                }
            }
        }
        data.put("roles", roles);
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> createManagedMember(QtMemberManageBody body, String operator)
    {
        QtAuthUtils.requireMemberCeo();
        if (body == null || !"create".equals(body.getMode()))
        {
            throw fail("无效的成员操作", HttpStatus.BAD_REQUEST);
        }
        String roleCategory = text(body.getRoleCategory());
        if (!"INTERN".equals(roleCategory) && !"MEMBER".equals(roleCategory))
        {
            throw fail("新增成员默认普通塔员，请创建后再调整管理角色", HttpStatus.BAD_REQUEST);
        }
        String realName = requireRealName(body.getNickName());
        String department = requireDepartment(body.getMemberDepartment());
        requireTitle(roleCategory, body.getMemberTitle());
        QtCohort cohort = requireCohort(body.getMemberCohort());
        String userName = text(body.getUserName());
        if (!LOGIN_NAME_PATTERN.matcher(userName).matches())
        {
            throw fail("登录账号需为 2–20 位字母、数字或 . _ @ -", HttpStatus.BAD_REQUEST);
        }
        String email = text(body.getEmail());
        if (StringUtils.isNotEmpty(email) && (email.length() > 64 || !EMAIL_PATTERN.matcher(email).matches()))
        {
            throw fail("请输入有效的账号邮箱", HttpStatus.BAD_REQUEST);
        }
        SysUser unique = new SysUser();
        unique.setUserName(userName);
        if (!userService.checkUserNameUnique(unique))
        {
            throw fail("账号已存在", HttpStatus.CONFLICT);
        }
        if (StringUtils.isNotEmpty(email))
        {
            unique.setEmail(email);
            if (!userService.checkEmailUnique(unique))
            {
                throw fail("邮箱账号已存在", HttpStatus.CONFLICT);
            }
        }
        String password = configService.selectConfigByKey("sys.user.initPassword");
        if (StringUtils.isEmpty(password))
        {
            password = "123456";
        }
        SysUser user = new SysUser();
        user.setDeptId(QtLabMemberImportParser.DEFAULT_DEPT_ID);
        user.setUserName(userName);
        user.setNickName(userName);
        user.setEmail(StringUtils.isEmpty(email) ? null : email);
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setStatus(UserConstants.NORMAL);
        user.setMemberDepartment(department);
        user.setMemberTitle(text(body.getMemberTitle()));
        user.setMemberCohort(String.valueOf(cohort.getCohortId()));
        user.setIsQuantaMember("1");
        user.setRoleIds(resolveRoleIds(roleCategory));
        user.setCreateBy(operator);
        userService.insertUser(user);
        userService.updateRealName(user.getUserId(), realName);
        upsertMemberRecord(user.getUserId(), cohort.getCohortId(), roleCategory, operator);
        return savedMember(user.getUserId());
    }

    @Override
    @Transactional
    public Map<String, Object> updateManagedMember(Long userId, QtMemberManageBody body, String operator)
    {
        QtAuthUtils.requireMemberCeo();
        if (body == null || (!"identity".equals(body.getMode()) && !"access".equals(body.getMode())))
        {
            throw fail("无效的成员操作", HttpStatus.BAD_REQUEST);
        }
        SysUser user = userService.selectUserById(userId);
        if (user == null || !"0".equals(user.getDelFlag()))
        {
            throw fail("成员不存在", HttpStatus.NOT_FOUND);
        }
        if (isProtectedMember(user))
        {
            throw fail("该账号受保护", HttpStatus.FORBIDDEN);
        }
        String cohortValue = text(body.getMemberCohort());
        boolean identity = "identity".equals(body.getMode());
        QtCohort target = null;
        if (StringUtils.isNotEmpty(cohortValue) && !"unassigned".equalsIgnoreCase(cohortValue))
        {
            target = requireCohort(cohortValue);
        }
        if (identity && target == null)
        {
            throw fail("请选择届数", HttpStatus.BAD_REQUEST);
        }
        if (!memberVersion(user).equals(text(body.getVersion())))
        {
            throw fail("成员资料已被更新，请重新加载", HttpStatus.CONFLICT);
        }
        QtMemberRecord currentRecord = qtCohortMapper.selectLatestRecordByUser(userId);
        String department = requireDepartment(body.getMemberDepartment());
        String roleCategory = text(body.getRoleCategory());
        String title = text(body.getMemberTitle());
        if (identity)
        {
            if (currentRecord == null)
            {
                requireTitle(roleCategory, title);
            }
            else if (!roleCategory.equals(text(currentRecord.getRoleCategory()))
                    || !title.equals(text(user.getMemberTitle())))
            {
                throw fail("请通过分配权限修改身份层级", HttpStatus.BAD_REQUEST);
            }
            userService.updateRealName(user.getUserId(), requireRealName(body.getNickName()));
            updatePlacement(user.getUserId(), department, currentRecord == null ? title : null,
                    String.valueOf(target.getCohortId()), operator);
            syncCohortRecord(user.getUserId(), currentRecord, target, roleCategory, operator);
            return savedMember(user.getUserId());
        }
        if (!ROLE_TITLES.containsKey(roleCategory))
        {
            throw fail("请选择已有的成员角色", HttpStatus.BAD_REQUEST);
        }
        requireTitle(roleCategory, title);
        updatePlacement(user.getUserId(), department, title,
                StringUtils.isNotEmpty(cohortValue) ? String.valueOf(target.getCohortId()) : null, operator);
        boolean rolesChanged = syncMemberRoles(user.getUserId(), user.getRoles(), roleCategory);
        if (currentRecord != null)
        {
            currentRecord.setRoleCategory(roleCategory);
            currentRecord.setUpdateBy(operator);
            qtCohortMapper.updateRecord(currentRecord);
        }
        if (rolesChanged)
        {
            tokenService.revokeUserSessions(user.getUserId());
        }
        return savedMember(user.getUserId());
    }

    /**
     * 未分届成员首次建档；已有档案改届时移动当前档案，目标届已有另一条档案则拒绝。
     */
    private void syncCohortRecord(Long userId, QtMemberRecord currentRecord, QtCohort target,
            String roleCategory, String operator)
    {
        if (currentRecord == null)
        {
            QtMemberRecord record = new QtMemberRecord();
            record.setUserId(userId);
            record.setCohortId(target.getCohortId());
            record.setRoleCategory(roleCategory);
            record.setMemberStatus("ACTIVE");
            record.setJoinTime(new Date());
            record.setRetainFlag("0");
            record.setCreateBy(operator);
            qtCohortMapper.insertRecord(record);
            return;
        }
        if (target.getCohortId().equals(currentRecord.getCohortId()))
        {
            return;
        }
        QtMemberRecord duplicate = qtCohortMapper.selectRecord(userId, target.getCohortId());
        if (duplicate != null)
        {
            throw fail("该成员在目标届已有档案，请先处理", HttpStatus.CONFLICT);
        }
        currentRecord.setCohortId(target.getCohortId());
        currentRecord.setUpdateBy(operator);
        qtCohortMapper.updateRecord(currentRecord);
    }

    private String deriveRoleCategory(List<SysRole> roles)
    {
        if (roles != null)
        {
            for (SysRole role : roles)
            {
                if (role == null)
                {
                    continue;
                }
                if ("qt_mgmt".equals(role.getRoleKey()))
                {
                    return "MGMT";
                }
                if ("qt_manager".equals(role.getRoleKey()))
                {
                    return "MANAGER";
                }
            }
        }
        return "MEMBER";
    }

    private String defaultTitle(String roleCategory)
    {
        List<String> titles = ROLE_TITLES.get(roleCategory);
        return titles == null || titles.isEmpty() ? "" : titles.get(0);
    }

    private Map<String, Object> savedMember(Long userId)
    {
        SysUser saved = userService.selectUserById(userId);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("userId", userId);
        data.put("version", saved == null ? "0" : memberVersion(saved));
        return data;
    }

    private void updatePlacement(Long userId, String department, String title, String memberCohort, String operator)
    {
        SysUser patch = new SysUser();
        patch.setUserId(userId);
        patch.setMemberDepartment(department);
        patch.setMemberTitle(StringUtils.isEmpty(title) ? null : title);
        patch.setMemberCohort(StringUtils.isEmpty(memberCohort) ? null : memberCohort);
        patch.setUpdateBy(operator);
        userService.updateMemberPlacement(patch);
    }

    private boolean syncMemberRoles(Long userId, List<SysRole> currentRoles, String roleCategory)
    {
        Set<String> desired = new LinkedHashSet<String>();
        desired.add("qt_member");
        if ("MGMT".equals(roleCategory))
        {
            desired.add("qt_mgmt");
        }
        else if ("MANAGER".equals(roleCategory))
        {
            desired.add("qt_manager");
        }
        Set<String> current = new HashSet<String>();
        if (currentRoles != null)
        {
            for (SysRole role : currentRoles)
            {
                if (role == null || StringUtils.isEmpty(role.getRoleKey()))
                {
                    continue;
                }
                current.add(role.getRoleKey());
                if (MANAGED_ROLE_KEYS.contains(role.getRoleKey()) && !desired.contains(role.getRoleKey())
                        && role.getRoleId() != null)
                {
                    SysUserRole link = new SysUserRole();
                    link.setUserId(userId);
                    link.setRoleId(role.getRoleId());
                    roleService.deleteAuthUser(link);
                }
            }
        }
        Map<String, Long> catalog = new HashMap<String, Long>();
        for (SysRole role : roleService.selectRoleAll())
        {
            if (role != null && StringUtils.isNotEmpty(role.getRoleKey()) && !catalog.containsKey(role.getRoleKey()))
            {
                catalog.put(role.getRoleKey(), role.getRoleId());
            }
        }
        for (String key : desired)
        {
            if (current.contains(key))
            {
                continue;
            }
            Long roleId = catalog.get(key);
            if (roleId == null)
            {
                throw fail("未找到塔员角色，请先初始化 " + key, HttpStatus.BAD_REQUEST);
            }
            roleService.insertAuthUsers(roleId, new Long[] { userId });
        }
        Set<String> currentManaged = new HashSet<String>(current);
        currentManaged.retainAll(MANAGED_ROLE_KEYS);
        return !currentManaged.equals(desired);
    }

    private boolean isProtectedMember(SysUser user)
    {
        if (user.getUserId() != null && user.getUserId().equals(SecurityUtils.getUserId()))
        {
            return true;
        }
        String title = text(user.getMemberTitle()).toUpperCase();
        if ("CEO".equals(title) || "ADMIN".equals(title))
        {
            return true;
        }
        if (user.getRoles() != null)
        {
            for (SysRole role : user.getRoles())
            {
                if (role != null && ("ceo".equals(role.getRoleKey()) || "admin".equals(role.getRoleKey())))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private QtCohort requireCohort(String memberCohort)
    {
        Long cohortId;
        try
        {
            cohortId = Long.valueOf(text(memberCohort));
        }
        catch (NumberFormatException ex)
        {
            throw fail("请选择有效的届数", HttpStatus.BAD_REQUEST);
        }
        QtCohort cohort = qtCohortMapper.selectCohortById(cohortId);
        if (cohort == null)
        {
            throw fail("请选择有效的届数", HttpStatus.BAD_REQUEST);
        }
        return cohort;
    }

    private String requireDepartment(String department)
    {
        String value = text(department);
        if (!"FRONTEND".equals(value) && !"BACKEND".equals(value) && !"PRODUCT".equals(value) && !"DESIGN".equals(value))
        {
            throw fail("请选择有效的所属部门", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private void requireTitle(String roleCategory, String title)
    {
        List<String> titles = ROLE_TITLES.get(roleCategory);
        if (titles == null || !titles.contains(text(title)))
        {
            throw fail("请选择与身份层级对应的职位", HttpStatus.BAD_REQUEST);
        }
    }

    private String requireRealName(String nickName)
    {
        String realName = text(nickName);
        if (realName.isEmpty() || realName.length() > 30)
        {
            throw fail("真实姓名需为 1–30 字", HttpStatus.BAD_REQUEST);
        }
        return realName;
    }

    private String memberVersion(SysUser user)
    {
        if (user.getUpdateTime() == null)
        {
            return "0";
        }
        return String.valueOf(user.getUpdateTime().getTime() / 1000L);
    }

    private List<Map<String, Object>> departmentOptions()
    {
        List<Map<String, Object>> departments = new ArrayList<Map<String, Object>>();
        departments.add(option("FRONTEND", "前端"));
        departments.add(option("BACKEND", "后端"));
        departments.add(option("PRODUCT", "产品"));
        departments.add(option("DESIGN", "设计"));
        return departments;
    }

    private Map<String, Object> roleProfile(String value, String label, String... permissions)
    {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("value", value);
        item.put("label", label);
        item.put("titles", ROLE_TITLES.get(value));
        item.put("permissions", List.of(permissions));
        return item;
    }

    private Map<String, Object> option(String value, String label)
    {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }

    private static String text(String value)
    {
        return StringUtils.trim(value);
    }

    private static ServiceException fail(String message, int code)
    {
        return new ServiceException(message, code);
    }
}
