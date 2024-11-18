package zw.co.dcl.jchatbot.configs;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.co.dcl.jawce.engine.filter.MdcSessionIdFilter;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<MdcSessionIdFilter> mdcHeaderFilter() {
        FilterRegistrationBean<MdcSessionIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MdcSessionIdFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
