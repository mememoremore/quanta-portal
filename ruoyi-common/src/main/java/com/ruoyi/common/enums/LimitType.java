package com.ruoyi.common.enums;

/**
 * 限流类型
 *
 * @author ruoyi
 */

public enum LimitType
{
    /**
     * 默认策略全局限流
     */
    DEFAULT,

    /**
     * 根据请求者IP进行限流
     */
    IP,

    /**
     * 根据登录用户限流；未登录时自动退回按 IP。
     * <p>
     * 校园网/实验室常共用一个公网出口，纯 IP 维度会把整栋楼一起限掉，
     * 因此登录后的接口一律用本类型。
     */
    USER
}
