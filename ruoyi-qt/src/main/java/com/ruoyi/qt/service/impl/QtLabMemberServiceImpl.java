package com.ruoyi.qt.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtLabMember;
import com.ruoyi.qt.domain.QtLabMemberImportRow;
import com.ruoyi.qt.domain.QtMemberRecord;
import com.ruoyi.qt.domain.QtMemberRetainBody;
import com.ruoyi.qt.mapper.QtCohortMapper;
import com.ruoyi.qt.mapper.QtLabMemberMapper;
import com.ruoyi.qt.service.IQtLabMemberService;
import com.ruoyi.qt.util.QtLabMemberImportParser;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class QtLabMemberServiceImpl implements IQtLabMemberService
{
    private static final Logger log = LoggerFactory.getLogger(QtLabMemberServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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
        return qtLabMemberMapper.selectAdminMemberList(query);
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
}
