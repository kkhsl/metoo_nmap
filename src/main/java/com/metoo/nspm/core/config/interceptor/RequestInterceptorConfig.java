package com.metoo.nspm.core.config.interceptor;

// 所以我们需要在 borrowservice 模块添加一个拦截器，配置令牌中继
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RequestInterceptorConfig implements RequestInterceptor {
//
//    @Override
//    public void apply(RequestTemplate requestTemplate) {
//        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails)
//                SecurityContextHolder.getContext().getAuthentication().getDetails();
//        requestTemplate.header("Authorization","Bearer" + details.getTokenValue());
//    }
//}
