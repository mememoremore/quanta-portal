package com.ruoyi.qt.util;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.domain.QtInterviewRound;
import com.ruoyi.qt.mapper.QtInterviewMapper;

/**
 * Frontend sends round_no (1/2); table stores round_id.
 */
public final class QtInterviewRounds
{
    private QtInterviewRounds()
    {
    }

    public static QtInterviewRound require(QtInterviewMapper mapper, Long roundIdOrNo)
    {
        if (roundIdOrNo == null)
        {
            throw new ServiceException("\u9762\u8bd5\u8f6e\u6b21\u4e0d\u5b58\u5728");
        }
        if (roundIdOrNo > 0 && roundIdOrNo <= Integer.MAX_VALUE)
        {
            QtInterviewRound byNo = mapper.selectRoundByNo(roundIdOrNo.intValue());
            if (byNo != null)
            {
                return byNo;
            }
        }
        QtInterviewRound byId = mapper.selectRoundById(roundIdOrNo);
        if (byId == null)
        {
            throw new ServiceException("\u9762\u8bd5\u8f6e\u6b21\u4e0d\u5b58\u5728");
        }
        return byId;
    }
}
