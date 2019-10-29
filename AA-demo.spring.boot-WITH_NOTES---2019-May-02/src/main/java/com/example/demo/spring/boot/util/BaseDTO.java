package com.example.demo.spring.boot.util;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Base class for all DTO response objects so that it can include enhancements
 * 
 * @author KunjPrasad
 *
 */
@ApiModel(description = "base class containing all HATEOAS and timing enhancements")
@Getter
@Setter
// NOTE using notation of "_" prefix to signify fields as internal
public class BaseDTO {

    @ApiModelProperty(value = "The time enhancement object in response")
    @JsonProperty(access = Access.READ_ONLY)
    private BaseTime _time;

    // NOTE.. same can also be done for giving documentation links
    @ApiModelProperty(value = "The user story enhancement object in response")
    @JsonProperty(access = Access.READ_ONLY)
    private BaseUserStory _userStory;

    // By default, make all members as null value so that they are not serialized/deserialized
    public BaseDTO() {

    }

    /**
     * Utility class to record various time instances
     * 
     * @author KunjPrasad
     *
     */
    @ApiModel(description = "base class containing all timing enhancements")
    @Getter
    @Setter
    @ToString
    public static class BaseTime {
        public static final String START_TIME_ATTR_NM = "startTime";
        public static final String SERVICE_START_TIME_ATTR_NM = "serviceStartTime";
        public static final String SERVICE_END_TIME_ATTR_NM = "serviceEndTime";

        @ApiModelProperty(value = "The start time of request before first web-filter")
        private long _startTime;

        @ApiModelProperty(value = "The end time of request just before last response body advice")
        private long _endTime;

        @ApiModelProperty(value = "The start time of request entering the controller")
        private long _serviceStartTime;

        @ApiModelProperty(value = "The end time of request exiting the controller")
        private long _serviceEndTime;
    }

    /**
     * Utility class to provide list of associated user stories
     * 
     * @author KunjPrasad
     *
     */
    @ApiModel(description = "base class containing all user story HATEOAS enhancements")
    @Getter
    @Setter
    public static class BaseUserStory {
        public static final String USER_STORY_ATTR_NM = "UserStory";

        @ApiModelProperty(value = "The list of user stories and defects associated with controller method")
        private List<String> _userStoryBag;
    }
}
