package com.example.springdemo.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("info.kube")
@Validated
public class InfoProps {
    private String prop;
    private String prop2;
    
    public String getProp() {
        return prop;
    }
    public void setProp(String prop) {
        this.prop = prop;
    }
    public String getProp2() {
        return prop2;
    }
    public void setProp2(String prop2) {
        this.prop2 = prop2;
    }

    public static class InfoPropsValidator implements Validator {

        public boolean supports(@NotNull Class<?> clazz) {
            return InfoProps.class.isAssignableFrom(clazz);
        }

        public void validate(@NotNull Object target, @NotNull Errors errors) {

            ValidationUtils.rejectIfEmptyOrWhitespace(errors,
                    "prop", "field.required");

            InfoProps infoProps = (InfoProps) target;
            if (!infoProps.getProp2().endsWith("2")) {
                errors.rejectValue("prop2", "field.extension.required",
                        new Object[]{"2"},
                        "prop2 must end with [2].");
            }

        }
    }
}
