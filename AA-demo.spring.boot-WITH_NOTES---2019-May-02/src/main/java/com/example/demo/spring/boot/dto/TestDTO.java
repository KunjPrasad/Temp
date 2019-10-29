package com.example.demo.spring.boot.dto;

import com.example.demo.spring.boot.util.BaseDTO;
import com.example.demo.spring.boot.util.StringUpperSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

//don't add "value" in @ApiModel.. that replaces class name
@ApiModel(description = "response class for message")
@Getter
@Setter
@JacksonXmlRootElement(localName = "test")
public class TestDTO extends BaseDTO {

    // either activate annotation for special case (currently done);
    // ..Or expose a "module" bean for general case (currently disabled)
    @JsonSerialize(using = StringUpperSerializer.class)
    @ApiModelProperty(value = "The message object")
    public String message;
}
