package org.icgc.dcc.storage.server.jwt;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Slf4j
public class JWTAuthorizationFilter extends GenericFilterBean {

    private final String REQUIRED_STATUS = "Approved";

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {

            val details = (OAuth2AuthenticationDetails) authentication.getDetails();
            val user = (JWTUser) details.getDecodedDetails();

            boolean hasCorrectStatus = user.getStatus().equalsIgnoreCase(REQUIRED_STATUS);

            if (!hasCorrectStatus) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

}

