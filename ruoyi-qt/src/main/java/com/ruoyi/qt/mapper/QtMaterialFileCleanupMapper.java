package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtMaterialFileCleanup;

public interface QtMaterialFileCleanupMapper
{
    int insertCleanup(QtMaterialFileCleanup cleanup);

    List<QtMaterialFileCleanup> selectPending();

    int markResult(@Param("cleanupId") Long cleanupId, @Param("status") String status,
            @Param("lastError") String lastError);
}
