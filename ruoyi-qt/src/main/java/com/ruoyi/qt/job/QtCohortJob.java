package com.ruoyi.qt.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.qt.service.IQtLabMemberService;

@Component("qtCohortJob")
public class QtCohortJob
{
    private static final Logger log = LoggerFactory.getLogger(QtCohortJob.class);

    @Autowired
    private IQtLabMemberService qtLabMemberService;

    public void rollover()
    {
        log.info("start quanta cohort rollover");
        qtLabMemberService.rollover();
        log.info("quanta cohort rollover finished");
    }
}
