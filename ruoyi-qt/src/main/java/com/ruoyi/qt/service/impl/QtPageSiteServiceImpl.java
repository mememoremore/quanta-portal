package com.ruoyi.qt.service.impl;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtPageDeployment;
import com.ruoyi.qt.domain.QtPageSite;
import com.ruoyi.qt.domain.QtPageSiteCreateBody;
import com.ruoyi.qt.domain.QtPageSiteSaveResult;
import com.ruoyi.qt.domain.QtPageSiteUpdateBody;
import com.ruoyi.qt.mapper.QtPageSiteMapper;
import com.ruoyi.qt.pages.QtPagesAccess;
import com.ruoyi.qt.pages.QtPagesRuntime;
import com.ruoyi.qt.service.IQtPageDeployService;
import com.ruoyi.qt.service.IQtPageSiteService;

/**
 * 成员静态站点服务实现。
 *
 * 校验规则与设计一致：slug 3-63 位小写字母/数字/连字符且非保留字；仓库只接受规范化
 * 的 https://github.com/{owner}/{repo}；分支默认 pages；发布目录默认 /。
 */
@Service
public class QtPageSiteServiceImpl implements IQtPageSiteService
{
    private static final Set<String> RESERVED_SLUGS = Set.of("www", "admin", "api", "mail", "ftp", "root",
            "static", "assets", "current", "releases", "nginx", "page", "pages", "challenge");

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    private static final Pattern OWNER_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]{0,99}$");

    private static final Pattern REPO_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");

    private static final Pattern BRANCH_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{0,127}$");

    private static final String DEFAULT_BRANCH = "pages";

    private static final String DEFAULT_PUBLISH_DIR = "/";

    private static final int SLUG_RETRY_LIMIT = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private QtPageSiteMapper siteMapper;

    @Autowired
    private QtPagesAccess access;

    @Autowired
    private IQtPageDeployService deployService;

    @Autowired
    private QtPagesRuntime runtime;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QtPageSiteSaveResult create(QtPageSiteCreateBody body)
    {
        runtime.requireReady();
        long userId = access.requireCurrentMemberId();
        if (body == null || StringUtils.isEmpty(body.getRepoUrl()))
        {
            throw new ServiceException("仓库地址不能为空", HttpStatus.BAD_REQUEST);
        }
        if (siteMapper.selectByUserId(userId) != null)
        {
            throw new ServiceException("每位塔员只能创建一个站点", HttpStatus.CONFLICT);
        }
        Repository repository = normalizeRepoUrl(body.getRepoUrl());
        String branch = normalizeBranch(body.getBranch());
        String publishDir = normalizePublishDir(body.getPublishDir());
        String fixedSlug = StringUtils.isEmpty(body.getSlug()) ? null : normalizeSlug(body.getSlug());

        QtPageSite site = new QtPageSite();
        site.setUserId(userId);
        site.setRepoUrl(repository.url());
        site.setRepoOwner(repository.owner());
        site.setRepoName(repository.name());
        site.setBranch(branch);
        site.setPublishDir(publishDir);
        site.setConfigVersion(1L);
        site.setRequestVersion(0L);
        site.setStatus(QtPageSite.STATUS_ENABLED);
        site.setNextPollTime(new Date());

        for (int attempt = 0; true; attempt++)
        {
            site.setSlug(fixedSlug != null ? fixedSlug : randomSlug());
            try
            {
                siteMapper.insertSite(site);
                break;
            }
            catch (DuplicateKeyException ex)
            {
                if (fixedSlug != null)
                {
                    throw new ServiceException("该站点标识已被使用", HttpStatus.CONFLICT);
                }
                if (attempt >= SLUG_RETRY_LIMIT - 1)
                {
                    throw new ServiceException("站点创建冲突，请稍后重试", HttpStatus.CONFLICT);
                }
            }
        }

        QtPageDeployment deployment = deployService.enqueueLocked(site, QtPageDeployment.TRIGGER_CREATE);
        return new QtPageSiteSaveResult(site, deployment);
    }

    @Override
    public QtPageSite getOwnSite()
    {
        long userId = access.requireCurrentMemberId();
        return siteMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QtPageSiteSaveResult update(Long siteId, QtPageSiteUpdateBody body)
    {
        runtime.requireReady();
        long userId = access.requireCurrentMemberId();
        QtPageSite site = siteMapper.selectForUpdate(siteId);
        if (site == null || site.getUserId() == null || !site.getUserId().equals(userId))
        {
            throw new ServiceException("站点不存在", HttpStatus.NOT_FOUND);
        }
        if (body == null || (StringUtils.isEmpty(body.getRepoUrl()) && StringUtils.isEmpty(body.getBranch())
                && StringUtils.isEmpty(body.getPublishDir())))
        {
            throw new ServiceException("没有需要更新的配置", HttpStatus.BAD_REQUEST);
        }
        Repository repository = StringUtils.isEmpty(body.getRepoUrl()) ? null : normalizeRepoUrl(body.getRepoUrl());
        String branch = StringUtils.isEmpty(body.getBranch()) ? null : normalizeBranch(body.getBranch());
        String publishDir = StringUtils.isEmpty(body.getPublishDir()) ? null : normalizePublishDir(body.getPublishDir());
        if (repository != null)
        {
            site.setRepoUrl(repository.url());
            site.setRepoOwner(repository.owner());
            site.setRepoName(repository.name());
        }
        if (branch != null)
        {
            site.setBranch(branch);
        }
        if (publishDir != null)
        {
            site.setPublishDir(publishDir);
        }
        site.setConfigVersion(site.getConfigVersion() + 1);
        site.setNextPollTime(new Date());
        siteMapper.updateSite(site);

        // 配置更新 = 新部署意图：request_version 由 enqueueLocked 推进，旧的非终态目标一并失效
        QtPageDeployment deployment = deployService.enqueueLocked(site, QtPageDeployment.TRIGGER_CONFIG);
        return new QtPageSiteSaveResult(site, deployment);
    }

    record Repository(String url, String owner, String name)
    {
    }

    Repository normalizeRepoUrl(String repoUrl)
    {
        String value = repoUrl == null ? "" : repoUrl.trim();
        if (value.isEmpty())
        {
            throw new ServiceException("仓库地址不能为空", HttpStatus.BAD_REQUEST);
        }
        if (!value.startsWith("https://github.com/"))
        {
            throw new ServiceException("仅支持 https://github.com/ 下的公开仓库", HttpStatus.BAD_REQUEST);
        }
        URI uri;
        try
        {
            uri = URI.create(value);
        }
        catch (IllegalArgumentException ex)
        {
            throw new ServiceException("仓库地址不合法", HttpStatus.BAD_REQUEST);
        }
        if (uri.getUserInfo() != null || uri.getPort() != -1 || uri.getQuery() != null || uri.getFragment() != null)
        {
            throw new ServiceException("仓库地址不能包含凭据、端口或查询参数", HttpStatus.BAD_REQUEST);
        }
        String path = uri.getPath();
        while (path != null && path.endsWith("/"))
        {
            path = path.substring(0, path.length() - 1);
        }
        if (path == null || !path.startsWith("/"))
        {
            throw new ServiceException("仓库地址不合法", HttpStatus.BAD_REQUEST);
        }
        String[] segments = path.substring(1).split("/");
        if (segments.length != 2)
        {
            throw new ServiceException("仓库地址格式应为 https://github.com/{owner}/{repo}", HttpStatus.BAD_REQUEST);
        }
        String owner = segments[0];
        String repo = segments[1];
        if (repo.endsWith(".git"))
        {
            repo = repo.substring(0, repo.length() - 4);
        }
        if (!OWNER_PATTERN.matcher(owner).matches() || !REPO_PATTERN.matcher(repo).matches())
        {
            throw new ServiceException("仓库属主或名称不合法", HttpStatus.BAD_REQUEST);
        }
        return new Repository("https://github.com/" + owner + "/" + repo, owner, repo);
    }

    String normalizeSlug(String slug)
    {
        String value = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("xn--") || !SLUG_PATTERN.matcher(value).matches())
        {
            throw new ServiceException("站点标识需为 3-63 位小写字母、数字或连字符，且以字母/数字开头结尾", HttpStatus.BAD_REQUEST);
        }
        if (RESERVED_SLUGS.contains(value))
        {
            throw new ServiceException("该站点标识为保留字", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    String normalizeBranch(String branch)
    {
        String value = (branch == null || branch.trim().isEmpty()) ? DEFAULT_BRANCH : branch.trim();
        if (value.length() > 128 || value.contains("..") || value.contains("//") || value.contains("\\")
                || value.contains(" ") || value.startsWith("/") || value.startsWith("-")
                || value.endsWith("/") || value.endsWith(".") || !BRANCH_PATTERN.matcher(value).matches())
        {
            throw new ServiceException("分支名称不合法", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    String normalizePublishDir(String publishDir)
    {
        String value = (publishDir == null || publishDir.trim().isEmpty()) ? DEFAULT_PUBLISH_DIR : publishDir.trim();
        if (value.length() > 255 || value.contains("..") || value.contains("\\") || !value.startsWith("/"))
        {
            throw new ServiceException("发布目录不合法", HttpStatus.BAD_REQUEST);
        }
        while (value.length() > 1 && value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.equals("/"))
        {
            for (String segment : value.substring(1).split("/"))
            {
                if (segment.isEmpty() || segment.startsWith("."))
                {
                    throw new ServiceException("发布目录不能包含空段或隐藏目录", HttpStatus.BAD_REQUEST);
                }
            }
            if (!value.matches("^/[A-Za-z0-9_./-]+$"))
            {
                throw new ServiceException("发布目录不合法", HttpStatus.BAD_REQUEST);
            }
        }
        return value;
    }

    private static String randomSlug()
    {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++)
        {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
