package com.ead.course.configs.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled=true)
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    AuthenticationEntryPointImpl authenticationEntryPoint;

    @Autowired
    AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    public AuthenticationJwtFilter authenticationJwtFilter() {
        return new AuthenticationJwtFilter();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        String hierarchy = "ROLE_ADMIN > ROLE_INSTRUCTOR \n ROLE_INSTRUCTOR > ROLE_STUDENT \n ROLE_STUDENT > ROLE_USER";
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }


    @Bean
    public DefaultMethodSecurityExpressionHandler expressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable());

        http.addFilterBefore(authenticationJwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }




    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception{
        return authConfig.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
