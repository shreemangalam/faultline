package dev.shreemangalam.faultline.restore.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class ApiSecurityConfiguration {
    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
            @Value("${faultline.security.enabled:true}") boolean enabled,
            @Value("${faultline.security.api-key:}") String apiKey) {
        return new ApiKeyAuthenticationFilter(enabled, apiKey);
    }

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            ApiKeyAuthenticationFilter apiKeyFilter,
            @Value("${faultline.security.enabled:true}") boolean enabled) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> {
                    if (!enabled) {
                        authorize.anyRequest().permitAll();
                    } else {
                        authorize.requestMatchers(HttpMethod.GET, "/api/v1/**", "/actuator/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/api/v1/**").authenticated()
                                .requestMatchers("/error").permitAll()
                                .anyRequest().denyAll();
                    }
                })
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("WWW-Authenticate", "ApiKey realm=\"faultline\"");
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"A valid X-Faultline-Api-Key header is required.\"}");
                }))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()")))
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
