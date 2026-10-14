package com.ruoyi.qt.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtAccessMetric;

public interface QtAccessMetricMapper
{
    int insertBatch(@Param("list") List<QtAccessMetric> list);

    List<QtAccessMetric> selectGlobalList(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    List<QtAccessMetric> selectUriAgg(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime);

    int deleteBefore(@Param("before") Date before);
}
