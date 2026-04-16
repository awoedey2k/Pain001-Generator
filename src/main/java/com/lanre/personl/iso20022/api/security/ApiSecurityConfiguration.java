package com.lanre.personl.iso20022.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@Configuration
public class ApiSecurityConfiguration {

    @Bean
    RequestSizeLimitFilter requestSizeLimitFilter(ApiHardeningProperties properties) {
        return new RequestSizeLimitFilter(properties);
    }

    @Bean
    RateLimitingFilter rateLimitingFilter(
            ApiHardeningProperties properties,
            InMemoryRateLimiter rateLimiter
    ) {
        return new RateLimitingFilter(properties, rateLimiter);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiHardeningProperties properties,
            RequestSizeLimitFilter requestSizeLimitFilter,
            RateLimitingFilter rateLimitingFilter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!properties.isEnabled()) {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }

        http.httpBasic(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(basicAuthenticationEntryPoint(properties)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lifecycle/**").hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers("/api/v1/**").hasAnyRole("WRITER", "ADMIN")
                        .anyRequest().permitAll())
                .addFilterBefore(requestSizeLimitFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService(ApiHardeningProperties properties, PasswordEncoder passwordEncoder) {
        ApiHardeningProperties.User writer = properties.getAuth().getWriter();
        ApiHardeningProperties.User auditor = properties.getAuth().getAuditor();
        ApiHardeningProperties.User admin = properties.getAuth().getAdmin();

        UserDetails writerUser = buildUser(writer, passwordEncoder);
        UserDetails auditorUser = buildUser(auditor, passwordEncoder);
        UserDetails adminUser = buildUser(admin, passwordEncoder);
        return new InMemoryUserDetailsManager(writerUser, auditorUser, adminUser);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    BasicAuthenticationEntryPoint basicAuthenticationEntryPoint(ApiHardeningProperties properties) {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName(properties.getAuth().getRealm());
        return entryPoint;
    }

    private UserDetails buildUser(ApiHardeningProperties.User user, PasswordEncoder passwordEncoder) {
        return User.withUsername(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword()))
                .roles(user.getRole())
                .build();
    }
}
