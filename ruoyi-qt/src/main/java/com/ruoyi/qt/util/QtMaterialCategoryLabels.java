package com.ruoyi.qt.util;

import java.util.Map;
import com.ruoyi.qt.domain.QtMaterial;

/**
 * 学习资料四分类展示名
 */
public final class QtMaterialCategoryLabels
{
    private static final Map<String, String> LABELS = Map.of(
            "FRONTEND", "前端",
            "BACKEND", "后端",
            "PRODUCT", "产品",
            "DESIGN", "设计");

    private QtMaterialCategoryLabels()
    {
    }

    public static String label(String category)
    {
        if (category == null)
        {
            return "";
        }
        return LABELS.getOrDefault(category, category);
    }

    public static boolean isStandard(String category)
    {
        return QtMaterial.isStandardCategory(category);
    }
}
