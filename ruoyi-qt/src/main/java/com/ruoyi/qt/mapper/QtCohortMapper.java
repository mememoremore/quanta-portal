package com.ruoyi.qt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.qt.domain.QtCohort;
import com.ruoyi.qt.domain.QtMemberRecord;

public interface QtCohortMapper
{
    List<QtCohort> selectCohortList(QtCohort query);

    QtCohort selectCohortById(Long cohortId);

    QtCohort selectCurrentCohort();

    QtCohort selectCohortByName(String cohortName);

    int insertCohort(QtCohort cohort);

    int updateCohort(QtCohort cohort);

    int clearCurrentFlag();

    QtMemberRecord selectRecord(@Param("userId") Long userId, @Param("cohortId") Long cohortId);

    List<QtMemberRecord> selectRecordsByCohort(Long cohortId);

    int insertRecord(QtMemberRecord record);

    int updateRecord(QtMemberRecord record);
}
