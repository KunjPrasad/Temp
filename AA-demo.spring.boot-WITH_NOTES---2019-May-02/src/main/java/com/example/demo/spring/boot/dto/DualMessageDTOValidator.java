package com.example.demo.spring.boot.dto;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.example.demo.spring.boot.dto.DualMessageDTO;

/**
 * Custom Spring validator to validate DualMessageDTO object
 * 
 * @author KunjPrasad
 *
 */
@Component
public class DualMessageDTOValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DualMessageDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DualMessageDTO dto = (DualMessageDTO) target;
        //ValidationUtils.rejectIfEmptyOrWhitespace(errors, "msg1", "string.empty", new Object[] {}, "msg1 is empty");
        if (dto.getMsg1() != null && dto.getMsg1().length() < 2) {
            errors.rejectValue("msg1", "string.small", "msg1 length is less than 2");
        }
        errors.rejectValue("msg1", "string.small", "blatant rejection");
        /* Commenting for now to test mix of JSR-303 and Spring validations
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "msg2", "string.empty", "msg2 is empty");
        if (dto.getMsg2() != null && dto.getMsg2().length() < 2) {
            errors.rejectValue("msg2", "string.small", "msg2 length is less than 2");
        }
        */
    }
}
