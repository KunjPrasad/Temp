package com.example.demo.spring.boot;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.robwin.swagger2markup.GroupBy;
import io.github.robwin.swagger2markup.Swagger2MarkupConverter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;

import springfox.documentation.staticdocs.Swagger2MarkupResultHandler;

/**
 * This is not really a test.. see in connection to "CONVERT SWAGGER TO MARKUP" in MavenController to understand its use
 * 
 * @author KunjPrasad
 *
 */
public class StaticSwaggerGenerationTest extends ApplicationTest {

    private static final String SWAGGER_API_URI = "/v2/api-docs";

    @Value("${swagger.output.single.adoc}")
    private String swaggerSingleAdocFileName;

    // To save the full swagger.json file, but not save it as adoc
    @Test
    public void saveSwaggerJsonFile() throws Exception {
        File jsonFile = new File(System.getProperty("swagger.output.file.json"));
        jsonFile.getParentFile().mkdirs();

        MvcResult mvcResult = this.mockMvc.perform(get(SWAGGER_API_URI)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        FileUtils.writeStringToFile(jsonFile, response.getContentAsString(), "UTF-8");
    }

    // To get the swagger files, but save individual overview, path, definition, security adoc files
    // @Test
    public void saveSwaggerPartFile() throws Exception {
        File jsonDir = new File(System.getProperty("swagger.output.dir"));
        jsonDir.mkdirs();

        Swagger2MarkupResultHandler.Builder builder = Swagger2MarkupResultHandler
                .outputDirectory(jsonDir.getAbsolutePath());
        mockMvc.perform(get(SWAGGER_API_URI).accept(MediaType.APPLICATION_JSON))
                .andDo(builder.build())
                .andExpect(status().isOk());
    }

    // To retrieve swagger.json, but change it to "single" adoc file that can be used by asciidoc
    @Test
    public void saveSwaggerJsonRunningThroughAsciidoc() throws Exception {
        File singleAdocFile = new File(swaggerSingleAdocFileName);
        singleAdocFile.getParentFile().mkdirs();

        MvcResult mvcResult = this.mockMvc.perform(get(SWAGGER_API_URI)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        String singleAdocStr = Swagger2MarkupConverter
                .fromString(response.getContentAsString())
                .withMarkupLanguage(MarkupLanguage.ASCIIDOC)
                .withPathsGroupedBy(GroupBy.TAGS)
                .build()
                .asString();
        FileUtils.writeStringToFile(singleAdocFile, singleAdocStr, "UTF-8");
    }
}
