package com.ruoyi.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 新生注册邮箱验证码 redis key
     */
    public static final String REGISTER_EMAIL_CODE_KEY = "register_email_codes:";

    /**
     * 找回密码邮箱验证码 redis key
     */
    public static final String RESET_EMAIL_CODE_KEY = "reset_email_codes:";

    /**
     * 新生修改邮箱验证码 redis key
     */
    public static final String PROFILE_EMAIL_CODE_KEY = "profile_email_codes:";

    /**
     * 业务缓存统一键前缀（与若依自身手写缓存的裸键区隔开）
     */
    public static final String CACHE_KEY_PREFIX = "cache:";

    /**
     * 未在 TTL 表中登记的缓存名的兜底过期时间（分钟）
     */
    public static final int CACHE_DEFAULT_TTL_MINUTES = 10;

    /**
     * 参数、字典等基础数据缓存的兜底过期时间（小时）。
     * 这类键原本永不过期，会在 maxmemory 下挤掉带 TTL 的登录令牌，故统一补 TTL。
     */
    public static final int BASE_CACHE_EXPIRATION_HOURS = 24;

    /** 控制台统计 */
    public static final String CACHE_QT_DASHBOARD_STATS = "qt:dashboard:stats";

    /** 招新看板统计 */
    public static final String CACHE_QT_RECRUIT_STATISTICS = "qt:recruit:statistics";

    /** C 端活动列表（不分页全量） */
    public static final String CACHE_QT_ACTIVITY_LIST = "qt:activity:list";

    /** C 端服装款式列表（不分页全量） */
    public static final String CACHE_QT_ITEM_LIST = "qt:item:list";

    /** C 端工位列表（不分页全量） */
    public static final String CACHE_QT_WORKSTATION_LIST = "qt:workstation:list";

    /** C 端图书列表（不分页全量） */
    public static final String CACHE_QT_BOOK_LIST = "qt:book:list";

    /**
     * 届次列表
     * <p>
     * 注意：学习资料（qt/materials）的列表与详情<b>不做缓存</b> —— 其可见性按当前用户的部门/角色过滤，
     * 全局 key 会造成跨用户串数据。
     */
    public static final String CACHE_QT_MEMBER_COHORTS = "qt:member:cohorts";

    /** 收款码配置 */
    public static final String CACHE_QT_PAYMENT_CONFIG = "qt:payment-config";

    /** 活动详情 */
    public static final String CACHE_QT_ACTIVITY_DETAIL = "qt:activity:detail";

    /** 图书详情 */
    public static final String CACHE_QT_BOOK_DETAIL = "qt:book:detail";

    /** 我的投递（key 含 userId） */
    public static final String CACHE_QT_MY_APPLICATION = "qt:my:application";

    /** 我的投递补充信息（key 含 userId） */
    public static final String CACHE_QT_MY_PROFILE = "qt:my:profile";

    /** 我的面试结果（key 含 userId） */
    public static final String CACHE_QT_MY_RESULTS = "qt:my:results";

    /* ==================== 上述缓存的 TTL（秒），RedisConfig 与手动缓存共用同一份 ==================== */

    public static final int TTL_QT_DASHBOARD_STATS = 60;

    public static final int TTL_QT_RECRUIT_STATISTICS = 60;

    public static final int TTL_QT_ACTIVITY_LIST = 120;

    public static final int TTL_QT_ITEM_LIST = 120;

    public static final int TTL_QT_WORKSTATION_LIST = 60;

    public static final int TTL_QT_BOOK_LIST = 60;

    public static final int TTL_QT_MEMBER_COHORTS = 600;

    public static final int TTL_QT_PAYMENT_CONFIG = 600;

    public static final int TTL_QT_ACTIVITY_DETAIL = 300;

    public static final int TTL_QT_BOOK_DETAIL = 300;

    /** 学生自查投递/进度的新鲜度窗口 */
    public static final int TTL_QT_MY_VIEW = 30;
}
