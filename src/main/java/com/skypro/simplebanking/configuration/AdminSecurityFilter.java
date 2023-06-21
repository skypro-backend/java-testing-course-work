package com.skypro.simplebanking.configuration;

import com.skypro.simplebanking.dto.BankingUserDetails;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminSecurityFilter extends OncePerRequestFilter {
  private final String adminToken;

  public AdminSecurityFilter(@Value("${app.security.admin-token}") String adminToken) {
    this.adminToken = adminToken;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Optional<UsernamePasswordAuthenticationToken> authenticatedUserDetails =
        authenticateByKeyHeader(request);
    authenticatedUserDetails.ifPresent(
        details -> {
          SecurityContext context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(details);
          SecurityContextHolder.setContext(context);
        });
    filterChain.doFilter(request, response);
  }

  private Optional<UsernamePasswordAuthenticationToken> authenticateByKeyHeader(
      HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader("X-SECURITY-ADMIN-KEY"))
        .filter(StringUtils::hasText)
        .filter(token -> token.contentEquals(adminToken))
        .map(
            stringKey -> {
              BankingUserDetails userDetails = new BankingUserDetails(-1, "admin", "****", true);
              return UsernamePasswordAuthenticationToken.authenticated(
                  userDetails, "admin", userDetails.getAuthorities());
            });
  }
}
