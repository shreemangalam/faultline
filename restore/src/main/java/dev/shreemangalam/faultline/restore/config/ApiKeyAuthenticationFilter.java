package dev.shreemangalam.faultline.restore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

final class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Faultline-Api-Key";
    private final boolean enabled;
    private final byte[] expectedKey;

    ApiKeyAuthenticationFilter(boolean enabled, String apiKey) {
        this.enabled = enabled;
        if (enabled && (apiKey == null || apiKey.length() < 16)) {
            throw new IllegalStateException("FAULTLINE_API_KEY must contain at least 16 characters when API security is enabled.");
        }
        this.expectedKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/v1/") || isSafe(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        if (supplied != null && MessageDigest.isEqual(expectedKey, supplied.getBytes(StandardCharsets.UTF_8))) {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    "faultline-operator", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    private static boolean isSafe(String method) {
        return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);
    }
}
