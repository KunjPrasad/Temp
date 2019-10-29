package com.example.demo.spring.boot.config.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.spring.boot.dto.TestDtoCsvMessageConverter;
import com.example.demo.spring.boot.util.StringMultiplierAnnotationFormatterFactory;

/*
 * NOTE: In Spring 5, WebMvcConfigurerAdapter is deprecated, and instead WebMvcConfigurer to be used
 * See https://stackoverflow.com/questions/47552835/the-type-webmvcconfigureradapter-is-deprecated
 */
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private StringMultiplierAnnotationFormatterFactory stringMultiplierAnnotationFormatterFactory;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // See comment#3 in TestController; it is not needed to have custom resolver if wanting to invoke validation
        // argumentResolvers.add(dualMessageDTOMethodArgumentResolver);
    }

    // ..if needed for custom formatter, see comments in TestController.java
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // add custom converter or formatter here..
        registry.addFormatterForFieldAnnotation(stringMultiplierAnnotationFormatterFactory);
    }

    // to add new converters
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // converts testDTO to csv
        converters.add(new TestDtoCsvMessageConverter());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.ignoreAcceptHeader(false);
        // for extra security.. but also need to change: useSuffixPatternMatching(false) in PathMatchCon􀂁gurer
        configurer.favorPathExtension(false);
        // this forces requests to send "Accept" header and not do so with query-param in request
        configurer.favorParameter(false);

        // to ensure json is default type - one can set defaultContentType. HOWEVER.. it does not seem like a nice idea
        // and is best left to service!
        // configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
