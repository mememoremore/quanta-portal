package com.ruoyi.qt.util;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 招新/成员枚举走 sys_dict，避免前端写死。
 */
public final class QtDictUtils
{
    public static final String DEPT = "qt_department";
    public static final String APPLY_STATUS = "qt_apply_status";
    public static final String RESULT_STATUS = "qt_result_status";
    public static final String MEMBER_ROLE = "qt_member_role";
    public static final String MEMBER_STATUS = "qt_member_status";
    public static final String ACTIVITY_TYPE = "qt_activity_type";
    public static final String MATERIAL_VISIBILITY = "qt_material_visibility";

    private QtDictUtils()
    {
    }

    public static void requireValue(String dictType, String value, String fieldName)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        List<SysDictData> list = DictUtils.getDictCache(dictType);
        if (list == null || list.isEmpty())
        {
            return;
        }
        for (SysDictData item : list)
        {
            if (value.equals(item.getDictValue()))
            {
                return;
            }
        }
        throw new ServiceException(fieldName + " 不在字典 " + dictType + " 允许范围内");
    }

    public static void requireActiveDepartment(String value, String fieldName)
    {
        if ("ANDROID".equalsIgnoreCase(value))
        {
            throw new ServiceException("安卓部门已取消，请选择产品、设计、前端或后端");
        }
        requireValue(DEPT, value, fieldName);
    }
}
