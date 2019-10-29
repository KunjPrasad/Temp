package com.example.demo.spring.boot.util;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Utility class to define "NOT" profile conditionals
 * 
 * @author KunjPrasad
 *
 */
public class ProfileCondition {

    public static class NotEmbedded extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext conditionContext,
                AnnotatedTypeMetadata annotatedTypeMetadata) {
            if (!conditionContext.getEnvironment().acceptsProfiles("embedded")) {
                return ConditionOutcome.match("No Embedded profile has been found.");
            }
            return ConditionOutcome.noMatch("Embedded profile found.");
        }
    }

    public static class NotTest extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext conditionContext,
                AnnotatedTypeMetadata annotatedTypeMetadata) {
            if (!conditionContext.getEnvironment().acceptsProfiles("test")) {
                return ConditionOutcome.match("No Test profile has been found.");
            }
            return ConditionOutcome.noMatch("Test profile found.");
        }
    }
}
