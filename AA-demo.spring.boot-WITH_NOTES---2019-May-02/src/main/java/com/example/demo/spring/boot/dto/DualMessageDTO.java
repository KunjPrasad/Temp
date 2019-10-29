package com.example.demo.spring.boot.dto;

import javax.validation.constraints.Size;

import com.example.demo.spring.boot.util.StringMultiplier;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DualMessageDTO {
    @StringMultiplier("2")
    private String msg1;

    @StringMultiplier("3")
    @Size(min = 2)
    private String msg2;
}
