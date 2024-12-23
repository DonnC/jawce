package zw.co.dcl.jawce.engine.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import zw.co.dcl.jawce.engine.constants.EngineConstants;

import java.io.IOException;

/**
 * Helper filter to include waId(sessionId, msisdn, mobile, phone) in logs
 * <p>
 * Usage: logback encoder
 * <p>
 * {@code  <pattern>%d{yyyy-MM-dd HH:mm:ss} - [waId: %X{waId}] - %msg%n</pattern>}
 *
 *
 * <p>
 * Usage: Java
 * <p>
 * <pre>{@code
 * import org.springframework.boot.web.servlet.FilterRegistrationBean;
 * import org.springframework.context.annotation.Bean;
 * import org.springframework.context.annotation.Configuration;
 *
 * @Configuration public class FilterConfig {
 *  @Bean public FilterRegistrationBean<MdcSessionIdFilter> mdcHeaderFilter() {
 *      FilterRegistrationBean<MdcSessionIdFilter> registrationBean = new FilterRegistrationBean<>();
 *      registrationBean.setFilter(new MdcSessionIdFilter());
 *
 *      // Adjust this URL pattern to match your service's API
 *      registrationBean.addUrlPatterns("/*");
 *
 *      return registrationBean;
 * }
 * }
 * }</pre>
 */
public class MdcSessionIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String waId = request.getHeader(EngineConstants.JAWCE_RHOOK_SESSION_HEADER_KEY);
        if(waId != null) {
            MDC.put(EngineConstants.MDC_ID_KEY, waId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(EngineConstants.MDC_ID_KEY);
        }
    }
}
