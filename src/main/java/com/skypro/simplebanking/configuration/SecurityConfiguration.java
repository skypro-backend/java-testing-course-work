package com.skypro.simplebanking.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain httpSecurity(
            HttpSecurity httpSecurity, AdminSecurityFilter adminSecurityFilter) throws Exception {
        return httpSecurity
                .csrf()
                .disable()
                .logout()
                .disable()
                .formLogin()
                .disable()
                .requestCache()
                .disable()
                .cors()
                .and()
                .httpBasic()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                .antMatchers(HttpMethod.POST, "/user/")
                .hasRole("ADMIN")
                .antMatchers("/user/*")
                .hasRole("USER")
                .antMatchers("/account/**")
                .hasRole("USER")
                .antMatchers("/transfer/**")
                .hasRole("USER")
                .anyRequest()
                .authenticated()
                .and()
                .addFilterBefore(adminSecurityFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
