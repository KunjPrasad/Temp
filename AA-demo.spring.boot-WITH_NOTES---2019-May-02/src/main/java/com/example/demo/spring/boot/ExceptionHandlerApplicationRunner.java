package com.example.demo.spring.boot;

import java.util.Map;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@Component
public class ExceptionHandlerApplicationRunner implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("~~~~~~~~~");

        Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
                .beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
        if (!matchingBeans.isEmpty()) {
            System.out.println("FOUND-1");
            matchingBeans.entrySet().stream()
                    .forEach(entry -> System.out.println(entry.getKey() + ";" + entry.getValue().getClass()));
        } else {
            System.out.println("NOT-FOUND-1");
        }

        HandlerExceptionResolver her = context.getBean("handlerExceptionResolver", HandlerExceptionResolver.class);
        if (her != null) {
            System.out.println("FOUND-2");
            System.out.println(her.getClass());
            HandlerExceptionResolverComposite herc = (HandlerExceptionResolverComposite) her;
            for (HandlerExceptionResolver her2 : herc.getExceptionResolvers()) {
                if (!ExceptionHandlerExceptionResolver.class.equals(her2.getClass())) {
                    continue;
                }
                ExceptionHandlerExceptionResolver eher = (ExceptionHandlerExceptionResolver) her2;
                eher.getMessageConverters().stream().forEach(conv -> System.out.println(conv.getClass()));
            }
        } else {
            System.out.println("NOT-FOUND-2");
        }
    }

}
