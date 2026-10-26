package com.ruoyi.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;
import com.ruoyi.framework.security.TrustedPortalOriginPolicy;

/**
 * 通用配置
 *
 * @author ruoyi
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Autowired
    private TrustedPortalOriginPolicy trustedPortalOriginPolicy;

    @Value("${ruoyi.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter()
    {
        // Pages 子域与门户同主域；启用 Pages 前必须把 CORS 来源收紧为精确列表
        trustedPortalOriginPolicy.assertStrictOriginsForPages();
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : allowedOrigins.split(","))
        {
            String value = origin.trim();
            if (value.isEmpty())
            {
                continue;
            }
            config.addAllowedOriginPattern(value);
        }
        if (config.getAllowedOriginPatterns() == null || config.getAllowedOriginPatterns().isEmpty())
        {
            config.addAllowedOriginPattern("*");
        }
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // blob 下载时前端要读取附件文件名（管理端 requestBlob 解析 Content-Disposition）
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("X-Trace-Id");
        // Refresh Cookie 需要携带凭据。allowedOriginPattern 会回显具体 Origin，不能改成字面量 *
        config.setAllowCredentials(true);
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 公开数据 API：仅允许 Pages 子域匿名读取，不带任何凭据（Cookie），与门户 CORS 完全隔离
        CorsConfiguration openPages = new CorsConfiguration();
        for (String pattern : trustedPortalOriginPolicy.pagesOriginPatterns())
        {
            openPages.addAllowedOriginPattern(pattern);
        }
        openPages.addAllowedHeader("*");
        openPages.addAllowedMethod("GET");
        openPages.addAllowedMethod("HEAD");
        openPages.addExposedHeader("X-Trace-Id");
        openPages.setAllowCredentials(false);
        openPages.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求（UrlBasedCorsConfigurationSource 首个匹配生效，具体路径必须先注册）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/open/pages/**", openPages);
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}