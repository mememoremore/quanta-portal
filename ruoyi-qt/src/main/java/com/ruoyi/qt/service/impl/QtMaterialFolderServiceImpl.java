package com.ruoyi.qt.service.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtMaterial;
import com.ruoyi.qt.domain.QtMaterialFolder;
import com.ruoyi.qt.domain.vo.QtMaterialFolderBreadcrumbItem;
import com.ruoyi.qt.domain.vo.QtMaterialFolderDetailVo;
import com.ruoyi.qt.mapper.QtMaterialFolderMapper;
import com.ruoyi.qt.service.IQtMaterialFolderService;
import com.ruoyi.qt.service.IQtMaterialService;
import com.ruoyi.qt.util.QtMaterialCategoryLabels;

@Service
public class QtMaterialFolderServiceImpl implements IQtMaterialFolderService
{
    @Autowired
    private QtMaterialFolderMapper qtMaterialFolderMapper;

    @Lazy
    @Autowired
    private IQtMaterialService qtMaterialService;

    @Override
    public List<QtMaterialFolder> listFolders(String category, Long parentId)
    {
        requireStandardCategory(category);
        long parent = parentId == null ? 0L : parentId;
        if (parent > 0)
        {
            QtMaterialFolder parentFolder = requireFolder(parent);
            if (!category.equals(parentFolder.getCategory()))
            {
                throw new ServiceException("目录与分类不一致", HttpStatus.BAD_REQUEST);
            }
        }
        return qtMaterialFolderMapper.selectFolderList(category, parent);
    }

    @Override
    public QtMaterialFolderDetailVo getFolderDetail(Long folderId)
    {
        QtMaterialFolder folder = requireFolder(folderId);
        QtMaterialFolderDetailVo vo = new QtMaterialFolderDetailVo();
        vo.setFolder(folder);
        vo.setBreadcrumb(buildBreadcrumb(folderId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QtMaterialFolder createFolder(QtMaterialFolder folder)
    {
        String category = folder.getCategory();
        requireStandardCategory(category);
        assertCategoryWritable(category);
        String folderName = normalizeFolderName(folder.getFolderName());
        long parentId = folder.getParentId() == null ? 0L : folder.getParentId();
        int depth = resolveChildDepth(parentId, category);
        assertSiblingNameAvailable(parentId, category, folderName, null, HttpStatus.CONFLICT);
        QtMaterialFolder row = new QtMaterialFolder();
        row.setParentId(parentId);
        row.setCategory(category);
        row.setFolderName(folderName);
        row.setDepth(depth);
        row.setCreateBy(folder.getCreateBy());
        qtMaterialFolderMapper.insertFolder(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QtMaterialFolder updateFolder(Long folderId, QtMaterialFolder patch)
    {
        QtMaterialFolder existing = requireFolder(folderId);
        assertCategoryWritable(existing.getCategory());
        boolean rename = StringUtils.isNotEmpty(patch.getFolderName());
        boolean move = patch.getParentId() != null && !patch.getParentId().equals(existing.getParentId());
        if (!rename && !move)
        {
            return existing;
        }
        long targetParentId = move ? patch.getParentId() : existing.getParentId();
        String targetName = rename ? normalizeFolderName(patch.getFolderName()) : existing.getFolderName();
        if (move)
        {
            assertNotMovingIntoDescendant(folderId, targetParentId);
            int newDepth = resolveChildDepth(targetParentId, existing.getCategory());
            int delta = newDepth - existing.getDepth();
            if (delta != 0)
            {
                int maxDepthAfter = maxDepthInSubtree(existing) + delta;
                if (maxDepthAfter > QtMaterialFolder.MAX_DEPTH)
                {
                    throw new ServiceException("移动后目录层级将超过 " + QtMaterialFolder.MAX_DEPTH + " 层",
                            HttpStatus.BAD_REQUEST);
                }
                applyDepthDelta(existing, delta);
            }
            existing.setParentId(targetParentId);
            existing.setDepth(newDepth);
        }
        if (rename || move)
        {
            assertSiblingNameAvailable(targetParentId, existing.getCategory(), targetName, folderId, HttpStatus.BAD_REQUEST);
            existing.setFolderName(targetName);
        }
        existing.setUpdateBy(patch.getUpdateBy());
        qtMaterialFolderMapper.updateFolder(existing);
        return existing;
    }

    @Override
    public void deleteFolder(Long folderId)
    {
        QtMaterialFolder folder = requireFolder(folderId);
        assertCategoryWritable(folder.getCategory());
        if (qtMaterialFolderMapper.countChildFolders(folderId) > 0)
        {
            throw new ServiceException("请先清空子目录", HttpStatus.BAD_REQUEST);
        }
        if (qtMaterialFolderMapper.countMaterialsInFolder(folderId) > 0)
        {
            throw new ServiceException("目录内仍有资料，无法删除", HttpStatus.BAD_REQUEST);
        }
        qtMaterialFolderMapper.deleteFolderById(folderId);
    }

    @Override
    public void validateMaterialFolderTarget(QtMaterial material)
    {
        Long folderId = material.getFolderId();
        if (folderId == null || folderId == 0L)
        {
            material.setFolderId(0L);
            return;
        }
        QtMaterialFolder folder = requireFolder(folderId);
        if (!Objects.equals(folder.getCategory(), material.getCategory()))
        {
            throw new ServiceException("资料分类与目标目录不一致", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void applyFolderFilter(QtMaterial query, Long folderIdParam)
    {
        if (folderIdParam == null)
        {
            return;
        }
        if (StringUtils.isNotEmpty(query.getFileName()))
        {
            return;
        }
        long folderId = folderIdParam;
        query.setFolderId(folderId);
        query.getParams().put("filterByFolder", "1");
    }

    @Override
    public void enrichFolderPaths(List<QtMaterial> materials)
    {
        if (materials == null || materials.isEmpty())
        {
            return;
        }
        Map<String, Map<Long, QtMaterialFolder>> cacheByCategory = new HashMap<>();
        for (QtMaterial material : materials)
        {
            Long folderId = material.getFolderId();
            if (folderId == null || folderId == 0L)
            {
                material.setFolderName("");
                material.setFolderPath(QtMaterialCategoryLabels.label(material.getCategory()));
                continue;
            }
            Map<Long, QtMaterialFolder> index = cacheByCategory.computeIfAbsent(material.getCategory(),
                    this::loadFolderIndex);
            QtMaterialFolder folder = index.get(folderId);
            if (folder == null)
            {
                continue;
            }
            material.setFolderName(folder.getFolderName());
            material.setFolderPath(formatFolderPath(material.getCategory(), folderId, index));
        }
    }

    @Override
    public List<QtMaterialFolderBreadcrumbItem> buildBreadcrumb(Long folderId)
    {
        if (folderId == null || folderId == 0L)
        {
            return Collections.emptyList();
        }
        Deque<QtMaterialFolderBreadcrumbItem> stack = new ArrayDeque<>();
        Long current = folderId;
        while (current != null && current > 0)
        {
            QtMaterialFolder folder = qtMaterialFolderMapper.selectFolderById(current);
            if (folder == null)
            {
                break;
            }
            stack.addFirst(new QtMaterialFolderBreadcrumbItem(folder.getFolderId(), folder.getFolderName(),
                    folder.getDepth()));
            current = folder.getParentId();
        }
        return new ArrayList<>(stack);
    }

    @Override
    public String formatFolderPath(String category, Long folderId)
    {
        if (folderId == null || folderId == 0L)
        {
            return QtMaterialCategoryLabels.label(category);
        }
        Map<Long, QtMaterialFolder> index = loadFolderIndex(category);
        return formatFolderPath(category, folderId, index);
    }

    private String formatFolderPath(String category, Long folderId, Map<Long, QtMaterialFolder> index)
    {
        List<String> parts = new ArrayList<>();
        parts.add(QtMaterialCategoryLabels.label(category));
        List<String> names = new ArrayList<>();
        Long current = folderId;
        while (current != null && current > 0)
        {
            QtMaterialFolder folder = index.get(current);
            if (folder == null)
            {
                folder = qtMaterialFolderMapper.selectFolderById(current);
            }
            if (folder == null)
            {
                break;
            }
            names.add(folder.getFolderName());
            current = folder.getParentId();
        }
        Collections.reverse(names);
        parts.addAll(names);
        return String.join(" / ", parts);
    }

    private Map<Long, QtMaterialFolder> loadFolderIndex(String category)
    {
        Map<Long, QtMaterialFolder> index = new HashMap<>();
        for (QtMaterialFolder folder : qtMaterialFolderMapper.selectFoldersByCategory(category))
        {
            index.put(folder.getFolderId(), folder);
        }
        return index;
    }

    private void assertCategoryWritable(String category)
    {
        QtMaterial probe = new QtMaterial();
        probe.setCategory(category);
        probe.setVisibility("MEMBER");
        qtMaterialService.validateUploadPolicy(probe);
    }

    private void requireStandardCategory(String category)
    {
        if (!QtMaterial.isStandardCategory(category))
        {
            throw new ServiceException("资料分类仅支持 FRONTEND/BACKEND/PRODUCT/DESIGN", HttpStatus.BAD_REQUEST);
        }
    }

    private QtMaterialFolder requireFolder(Long folderId)
    {
        if (folderId == null || folderId <= 0)
        {
            throw new ServiceException("目录不存在", HttpStatus.BAD_REQUEST);
        }
        QtMaterialFolder folder = qtMaterialFolderMapper.selectFolderById(folderId);
        if (folder == null)
        {
            throw new ServiceException("目录不存在", HttpStatus.BAD_REQUEST);
        }
        return folder;
    }

    private String normalizeFolderName(String folderName)
    {
        String name = StringUtils.trim(folderName);
        if (StringUtils.isEmpty(name))
        {
            throw new ServiceException("目录名称不能为空", HttpStatus.BAD_REQUEST);
        }
        if (name.length() > 50)
        {
            throw new ServiceException("目录名称不能超过 50 个字符", HttpStatus.BAD_REQUEST);
        }
        if (name.contains("/") || name.contains("\\"))
        {
            throw new ServiceException("目录名称不能包含 / 或 \\", HttpStatus.BAD_REQUEST);
        }
        return name;
    }

    private int resolveChildDepth(long parentId, String category)
    {
        if (parentId == 0L)
        {
            return 1;
        }
        QtMaterialFolder parent = requireFolder(parentId);
        if (!category.equals(parent.getCategory()))
        {
            throw new ServiceException("父目录与分类不一致", HttpStatus.BAD_REQUEST);
        }
        int depth = parent.getDepth() + 1;
        if (depth > QtMaterialFolder.MAX_DEPTH)
        {
            throw new ServiceException("目录层级不能超过 " + QtMaterialFolder.MAX_DEPTH + " 层", HttpStatus.BAD_REQUEST);
        }
        return depth;
    }

    private void assertSiblingNameAvailable(long parentId, String category, String folderName, Long excludeFolderId,
            int status)
    {
        QtMaterialFolder existing = qtMaterialFolderMapper.selectFolderByParentAndName(parentId, category, folderName);
        if (existing != null && (excludeFolderId == null || !existing.getFolderId().equals(excludeFolderId)))
        {
            throw new ServiceException("同级目录下已存在同名文件夹", status);
        }
    }

    private void assertNotMovingIntoDescendant(Long folderId, long targetParentId)
    {
        if (targetParentId == folderId)
        {
            throw new ServiceException("不能将目录移动到自身下面", HttpStatus.BAD_REQUEST);
        }
        if (targetParentId == 0L)
        {
            return;
        }
        Long current = targetParentId;
        while (current != null && current > 0)
        {
            if (current.equals(folderId))
            {
                throw new ServiceException("不能将目录移动到其子目录下", HttpStatus.BAD_REQUEST);
            }
            QtMaterialFolder parent = qtMaterialFolderMapper.selectFolderById(current);
            if (parent == null)
            {
                break;
            }
            current = parent.getParentId();
        }
    }

    private int maxDepthInSubtree(QtMaterialFolder root)
    {
        int max = root.getDepth();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(root.getFolderId());
        while (!queue.isEmpty())
        {
            Long parentId = queue.removeFirst();
            for (Long childId : qtMaterialFolderMapper.selectChildFolderIds(parentId))
            {
                QtMaterialFolder child = qtMaterialFolderMapper.selectFolderById(childId);
                if (child != null)
                {
                    max = Math.max(max, child.getDepth());
                    queue.addLast(childId);
                }
            }
        }
        return max;
    }

    private void applyDepthDelta(QtMaterialFolder root, int delta)
    {
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(root.getFolderId());
        while (!queue.isEmpty())
        {
            Long id = queue.removeFirst();
            QtMaterialFolder folder = qtMaterialFolderMapper.selectFolderById(id);
            if (folder == null)
            {
                continue;
            }
            qtMaterialFolderMapper.updateFolderDepth(id, folder.getDepth() + delta);
            for (Long childId : qtMaterialFolderMapper.selectChildFolderIds(id))
            {
                queue.addLast(childId);
            }
        }
    }
}
