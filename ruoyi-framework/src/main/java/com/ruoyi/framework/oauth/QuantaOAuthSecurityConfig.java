package com.ruoyi.framework.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * OAuth 使用独立过滤链。主站全局关闭了 CSRF，这里的交互请求在业务流程里自行校验。
 */
@Configuration
public class QuantaOAuthSecurityConfig
{
    @Bean
    @Order(1)
    SecurityFilterChain quantaOAuthFilterChain(HttpSecurity http, CorsFilter corsFilter) throws Exception
    {
        return http.securityMatcher("/oauth2/**", "/.well-known/openid-configuration")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .addFilterBefore(corsFilter, LogoutFilter.class)
                .build();
    }
}
