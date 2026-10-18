package com.ruoyi.qt.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 把资料表里的存储路径解析到配置的资料根目录内。拒绝穿越和根目录以外的位置。
 */
public final class QtMaterialStoragePaths
{
    private QtMaterialStoragePaths()
    {
    }

    public static Path materialsRoot()
    {
        String profile = RuoYiConfig.getProfile();
        if (StringUtils.isEmpty(profile))
        {
            throw new ServiceException("资料存储未配置，未执行删除", HttpStatus.CONFLICT);
        }
        return Paths.get(profile, "upload", "qt", "materials").toAbsolutePath().normalize();
    }

    public static Path resolveStoredFile(String storedPath)
    {
        if (StringUtils.isEmpty(storedPath) || storedPath.indexOf('\\') >= 0 || storedPath.indexOf('\0') >= 0
                || storedPath.contains(".."))
        {
            throw untrusted();
        }
        String prefix = Constants.RESOURCE_PREFIX + "/";
        if (!storedPath.startsWith(prefix))
        {
            throw untrusted();
        }
        String relative = storedPath.substring(Constants.RESOURCE_PREFIX.length());
        while (relative.startsWith("/"))
        {
            relative = relative.substring(1);
        }
        if (StringUtils.isEmpty(relative) || relative.contains(".."))
        {
            throw untrusted();
        }
        Path root = materialsRoot();
        Path profile = root.getParent().getParent().getParent();
        Path target = profile.resolve(relative).normalize();
        if (!target.startsWith(root))
        {
            throw untrusted();
        }
        return target;
    }

    private static ServiceException untrusted()
    {
        return new ServiceException("资料文件位置不受信任，未执行删除", HttpStatus.CONFLICT);
    }
}
