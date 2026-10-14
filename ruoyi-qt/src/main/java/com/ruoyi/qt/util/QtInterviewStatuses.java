package com.ruoyi.qt.util;

import com.ruoyi.qt.domain.QtInterviewResult;

/**
 * Normalize interview result status. History FAIL is treated as OUT.
 */
public final class QtInterviewStatuses
{
    public static final String PENDING = "PENDING";
    public static final String PASS = "PASS";
    public static final String OUT = "OUT";

    private QtInterviewStatuses()
    {
    }

    public static String normalize(String status)
    {
        if (status == null || status.isEmpty())
        {
            return PENDING;
        }
        String value = status.trim().toUpperCase();
        if ("FAIL".equals(value))
        {
            return OUT;
        }
        return value;
    }

    public static String normalizeDecision(String decision)
    {
        String value = normalize(decision);
        if (!PASS.equals(value) && !OUT.equals(value))
        {
            return null;
        }
        return value;
    }

    public static boolean isPass(String status)
    {
        return PASS.equals(normalize(status));
    }

    public static boolean isOut(String status)
    {
        return OUT.equals(normalize(status));
    }

    public static boolean isDecided(String status)
    {
        String value = normalize(status);
        return PASS.equals(value) || OUT.equals(value);
    }

    public static boolean isPass(QtInterviewResult result)
    {
        return result != null && isPass(result.getResultStatus());
    }

    public static boolean isOut(QtInterviewResult result)
    {
        return result != null && isOut(result.getResultStatus());
    }

    public static boolean isDecided(QtInterviewResult result)
    {
        return result != null && isDecided(result.getResultStatus());
    }
}
