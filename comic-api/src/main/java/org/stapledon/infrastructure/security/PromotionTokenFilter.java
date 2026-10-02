package org.stapledon.infrastructure.security;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stapledon.common.util.LogContext;
import org.stapledon.engine.promotion.DevPromotionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates another instance's promotion requests ({@code /api/v1/promotion/**}) by the shared secret in the {@code X-Promotion-Token}
 * header, granting the {@code PROMOTION} authority that {@link SecurityConfig} requires there. Only where {@code comics.promotion.serve=true}
 * and a token is set (dev); everywhere else no request gets the authority, so the endpoints answer 401.
 */
@Slf4j
@Component
public class PromotionTokenFilter extends OncePerRequestFilter {

    public static final String AUTHORITY = "PROMOTION";
    static final String PATH_PREFIX = "/api/v1/promotion/";

    private final boolean serve;
    private final byte[] token;

    public PromotionTokenFilter(@Value("${comics.promotion.serve:false}") boolean serve, @Value("${comics.promotion.token:}") String token) {
        this.serve = serve;
        this.token = token == null ? new byte[0] : token.strip().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String presented = request.getHeader(DevPromotionService.TOKEN_HEADER);
        if (!serve || token.length == 0) {
            log.debug("Promotion isn't served here; {} {} not authenticated", request.getMethod(), request.getRequestURI());
        } else if (presented == null) {
            log.debug("No promotion token on {} {}", request.getMethod(), request.getRequestURI());
        } else if (MessageDigest.isEqual(token, presented.strip().getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("promotion", null, List.of(new SimpleGrantedAuthority(AUTHORITY))));
            MDC.put(LogContext.USER, "promotion");
        } else {
            log.warn("Promotion token rejected on {} {}", request.getMethod(), request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}
