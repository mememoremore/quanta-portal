package com.ruoyi.framework.config;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.filter.RefererFilter;
import com.ruoyi.common.filter.RepeatableFilter;
import com.ruoyi.common.filter.TraceIdFilter;
import com.ruoyi.common.filter.XssFilter;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.metrics.AccessMetricsCollector;
import com.ruoyi.framework.metrics.AccessMetricsFilter;

/**
 * Filter配置
 *
 * @author ruoyi
 */
@Configuration
public class FilterConfig
{
    @Value("${xss.excludes}")
    private String excludes;

    @Value("${xss.urlPatterns}")
    private String urlPatterns;

    @Value("${referer.allowed-domains}")
    private String allowedDomains;

    @Value("${qt.metrics.enabled:true}")
    private boolean metricsEnabled;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    public FilterRegistrationBean xssFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns(StringUtils.split(urlPatterns, ","));
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("excludes", excludes);
        registration.setInitParameters(initParameters);
        return registration;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "referer.enabled", havingValue = "true")
    public FilterRegistrationBean refererFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new RefererFilter());
        registration.addUrlPatterns(Constants.RESOURCE_PREFIX + "/*");
        registration.setName("refererFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("allowedDomains", allowedDomains);
        registration.setInitParameters(initParameters);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration()
    {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<TraceIdFilter>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FilterRegistrationBean someFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AccessMetricsFilter> accessMetricsFilterRegistration(AccessMetricsCollector collector)
    {
        AccessMetricsFilter filter = new AccessMetricsFilter(collector, metricsEnabled);
        FilterRegistrationBean<AccessMetricsFilter> registration = new FilterRegistrationBean<AccessMetricsFilter>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setName("accessMetricsFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }

}
