package com.example.filter;

import com.example.tenancy.TenancyContext;
import com.example.tenancy.TenancyContextHolder;
import com.example.tenancy.Tenant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Component
@Order(1)
public class TenancyContextFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    TenancyContextHolder.setContext(buildTenancyContext(req));
    chain.doFilter(req, response);
  }

  private TenancyContext buildTenancyContext(HttpServletRequest request) {
    String tenantId = extractTenantId(request);
    if (tenantId != null) {
      return TenancyContext.newContext(new Tenant(tenantId, tenantId, true));
    }
    return TenancyContext.emptyContext();
  }

  private String extractTenantId(HttpServletRequest request) {
    String url = getRequestPath(request);
    AntPathMatcher matcher = new AntPathMatcher();
    matcher.setCaseSensitive(false);
    if (matcher.match("/{tenantId}/api/**", url)) {
      Map<String, String> templateVariables = matcher.extractUriTemplateVariables("/{tenantId}/api/**", url);
      return templateVariables.get("tenantId");
    }
    return null;
  }

  private String getRequestPath(HttpServletRequest request) {
    String url = request.getServletPath();
    if (request.getPathInfo() != null) {
      url += request.getPathInfo();
    }
    return url;
  }

}
