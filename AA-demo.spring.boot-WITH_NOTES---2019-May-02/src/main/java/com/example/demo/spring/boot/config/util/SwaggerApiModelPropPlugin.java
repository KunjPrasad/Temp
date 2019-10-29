package com.example.demo.spring.boot.config.util;

import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.swagger.annotations.ApiModelProperty;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import springfox.documentation.service.AllowableRangeValues;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;

import java.util.Arrays;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 500)
public class SwaggerApiModelPropPlugin implements ModelPropertyBuilderPlugin {

    @Override
    public boolean supports(DocumentationType arg0) {
        return true;
    }

    @Override
    public void apply(ModelPropertyContext context) {
        if (!context.getBeanPropertyDefinition().isPresent()) {
            return;
        }
        BeanPropertyDefinition def = context.getBeanPropertyDefinition().get();
        if (def.getField() == null) {
            return;
        }
        ApiModelProperty apiModelPropAnn = AnnotationUtils.findAnnotation(def.getField().getAnnotated(),
                ApiModelProperty.class);
        if (apiModelPropAnn == null) {
            return;
        }
        System.out.println(def.getField().getAnnotated().getName() + "," + apiModelPropAnn.value());
        context.getBuilder()
                .description("[TEST] " + apiModelPropAnn.value())
                .allowableValues(new AllowableRangeValues("5", "" + (10 + apiModelPropAnn.value().length())))
                .extensions(Arrays.asList(new StringVendorExtension("test-key", "test-val")));
    }
}
