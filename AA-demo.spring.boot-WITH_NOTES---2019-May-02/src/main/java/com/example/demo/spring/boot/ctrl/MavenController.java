package com.example.demo.spring.boot.ctrl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.spring.boot.config.util.MavenConfiguration;

/**
 * This is not really a controller, but a placeholder class containing comments on various Maven plugin changes done.
 * One controller method added.. to serve static swagger
 * 
 * @author KunjPrasad
 *
 */
@RestController
public class MavenController {

// @formatter:off
/*
 * TODO:
 * -- There is a google checkstyle sheet -- if you want to use : https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml
 * -- The "module" you see in checkstyle are packages. Particular TreeWalker is a class that can then go over different packages/directories and 
 * .. add results from different other checkers (TreeWalker extends AbstractFileSetCheck - see its code for more info)
 * -- See how to add custom style -- is good : https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/custom-developed-checkstyle.html
 *
 * 
 *
 *
 *
 */
 
    // MAVEN CHEKSTYLE PLUGIN
    // Associated page:
    // https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/custom-developed-checkstyle.html
    // Associated page: http://checkstyle.sourceforge.net/config.html#Checker
    // It could be a good endeavor but requires effort, understanding of abstract syntax tree, and some of the efforts
    // could actually be delegated to Sonar!
    // Also note that this is otherwise in "reporting" section of maven.. which Maven does not do as part of build
    // process.. so need to learn about reporting phase separately
    // ALL IN ALL.. maybe avoid: (1) just say you rather relied on configured Sonar; (ii) Internal reviews
    // **VERY VERY IMPORTANT**: It seems that when adding dependency for development of own checkstyle checks.. it asked
    // to add rt.jar from libs.. and that stopped Spring unit test! So maybe avoid this!!

    // ============================

    // MAVEN COMPILER FOR SPRING BOOT
    // See https://stackoverflow.com/questions/42802712/java-compiler-version-for-maven-build-of-a-spring-boot-product
    // It says that some maven properties can be changed to configure source property for build
    // Also see https://docs.spring.io/spring-boot/docs/2.0.0.RELEASE/maven-plugin/

    // MAVEN SUREFIRE CONFIGURATION
    // Notice that by configuring <configuration> element of maven-surefire-plugin, system variables can be passed that
    // will only be available in test context. See StaticSwaggerGenerationTest. The property is otained by annotation
    // @Value("${swagger.output.single.adoc}"). Without it, it needed to be obtained using
    // System.getProperty("swagger.output.single.adoc")

    // NOTE: In maven, there is:
    // ${project.basedir} : for the base directory of maven project
    // saying path as "src/main/java", without "/" at beginning.. is always relative to ${project.basedir}
    // ${project.build.directory} : for the "target" folder!
    // ${project.build.outputDirectory}: for "target/classes" folder! (--notice the difference)

    // MAVEN-EXEC-PLUGIN
    // Realize that there are ways to invoke a code as part of build process using Maven exec plugin. This is done in:
    // https://blog.philipphauer.de/rest-api-documentation-swagger-asciidoc/ --- To generate Swagger!

    // **VERY VERY IMPORTANT**: MAVEN WAR PLUGIN (..and in addition to Swagger efforts)
    // ***Keep above in mind when doing similar things in future***
    // 1) Note that by using Maven war plugin, one can add extra final during the final package step. THUS, newly made
    // swagger files after asciidoc step can be then added to directory in war! It is suggested to add them in
    // WEB-INF/classes/documentation folder ..that way, they get treated as others in src/main/resource folder!!!
    // 2) **IMPORTANT** even after doing above, if you use eclipse plugin to deploy to server, then the documentation is
    // still not read! This seems to be an eclipse-plugin issue because if you yourself deploy in JBoss, everything
    // works fine! It is also noticed that when using Eclipse to deploy to Jboss.. it deploys, then undeploys, then
    // redeploys!! When deploying by self, you can still see documentation folder and the swagger html and pdf file in
    // if you unzip the war made (confirming work done by war plugin) and in
    // ..\standalone\tmp\vfs\temp\temp..\content-..\WEB-INF\classes\documentation folder - confirming it got
    // successfully deployed.. But when deploying via eclipse, the documentation folder exists but no file inside it!!
    // So, seems like an eclipse issue
    // 3) Look at PathMatchingResourcePatternResolver and its getResources(path) method. using path of "classpath*:/*"
    // you can list all classes and resources. Even this gives different result wen deploying via Eclipse vs when self
    // deploying to JBoss!! So in future, if you can't find a resource.. use it!!
    // 4) **IMPORTANT**: Use of ClassPathResource(..) of Spring to return a file from ClassPath!!

    // **IMPORTANT**: MAVEN-PROPERTIES-PLUGIN (and related, Maven-resource-plugin)
    // Maven properties plugin allows getting Maven properties in a file. This is run even before classes are made, so
    // the properties file made can be added to src/resources folder, and it will also go in war file. However, the
    // drawback is that "ALL" of maven properties and its dependency properties (including multiple versions get out)..
    // which may be too much information
    // Maven resource plugin allows to add files to resources. If configuring "filtering" as true, then the expression
    // in a file is evaluated using maven properties before adding it. The advantage is that this provides control over
    // the properties that are made available to application.
    // **VERY VERY IMPORTANT**: Note that by this method one can pass Maven build time, Maven project name, etc to
    // application!! .including any other system properties (like svn build #)

    // MAVEN-PROFILE
    // See https://stackoverflow.com/questions/166895/different-dependencies-for-different-build-profiles-in-maven 
    // .. and https://maven.apache.org/pom.html#Profiles
    // For example, consider when in embedded and test environment you want ActiveMQ to make JMS connectionFactory and topics 
    // .. and queues, but in Jboss environment, you want all these provided by JNDI. So, it is compile scope in embedded, test 
    // .. and provided scope in JBoss. The change in build for different profile can be done this way. 
    // --|---- NOTE: thinking about it, same issue should have also come with Datasource, but none was observed because in
    //         .. embedded and test, we used H2 database; But used Mysql in JBoss! So it was already separate to begin with
    
    // ============================

    // **VERY VERY IMPORTANT**: CONVERT SWAGGER TO MARKUP
    // 1) The complete flow is that swagger makes json but that is only available when the container is up. So, when
    // running unit tests, a container is made, then the swagger url is called that gives swagger.json. The second step
    // is to convert json to a format that can then be used by asciidoctor to change to pdf/html. This is done using
    // convertSwagger api (or convertSwgger2Markup maven plugin.. choose one!). And so, to generate static swagger
    // docs.. it is necessary that "test" step runs completely. This code is provided in StaticSwaggerGeneratorTest.java
    // (so that it can be run during unit test)
    // 2) Use io.springfox:springfox-staticdocs maven artifact to access api through which: (a) You give a url and it
    // collects swagger json and changes it to .adoc file that can then be used by asciidoctor, (b) access API through
    // which you can ask to join all adoc files to single one, and thus, when asciidoctor runs, it creates one html/pdf
    // file. Note that some places you'll see use of "Swagger2Markup" Api - but it is included in springfox-staticdocs.
    // Since this is done only during "test" phase, so the scope of artifact in pom.xml is also "test"
    // 3) After these, configure the asciidoctor plugin in maven's <build> and bind it to corresponding phase. This
    // triggers the build up of files after test is run
    // 4) Example of swaggerMarkup api documentation: https://swagger2markup.github.io/swagger2markup/1.3.1/
    // Note that by using the swaggerMarkup API, it
    // 5) Somehow, in asciidoctor, if you give outputFileName, then it does not copy the file! So, maybe just try to
    // control the filename of .adoc file created by convertSwagger2markup ..and asciidoctor will create pdf and html
    // files of same name!

    // ***VERY VERY IMPORTANT**: Notice how with combination of different maven plugins you can now Serve the html/pdf
    // of swagger obtained after asciidoc!

    // ============================
    
    // ***VERY VERY IMPORTANT**: How to make an EAR file:
    // Best, see https://javahonk.com/create-maven-ear-project-eclipse/   followed by   https://stackoverflow.com/questions/34014929/how-to-create-an-ear-file-for-an-existing-maven-project   
    // and   maybe, https://stackoverflow.com/questions/1134894/maven2-best-practice-for-enterprise-project-ear-file
    // The main idea is that a EAR file consists of jar and war file(s) as dependencies/modules in it. So, those files need to be existing in 
    // maven repository before it can be pulled and made into an EAR
    // ON RELATED NOTE.. see https://developer.jboss.org/thread/55590 which introduces the concept of "virtual host" configuration in Jboss 
    // through which different web-applications can be deployed to different context path on the same end-url!

    // ============================
    
    // ***VERY VERY IMPORTANT**: How to have 2 different test source for maven surefire
    // This can be useful in a Spring Boot setting where one can use src/test/java for normal unit tests; and say, src/spring-mock-integration-test/java 
    // folder for Spring-Boot's integration test using mockMvc. 
    // See https://stackoverflow.com/questions/10138559/howto-add-another-test-source-folder-to-maven-and-compile-it-to-a-separate-folde
    // -- note that the answer text says that it is referring from https://github.com/alimate/maven-source-sets/blob/master/pom.xml
    // -- but this (latter) pom.xml contains reference to maven-failsafe-plugin. When trying to just add a new folder for test, there is no 
    // need for maven-failsafe-plugin. HOWEVER.. do note that if the test-classes in newly added folder do not end with *Test.java naming 
    // convention, then it may not be considered by Surefire plugin. So, either keep the test-class name proper; or add the new naming pattern 
    // to include with surefire plugin.
    // A RELATED THING TO NOTE.. Notice that the folder structure in maven goes as "src/main/java", or "src/test/java" -- The question is what 
    // is that extra "java" subfolder!! That is because maven can be configured to use different compiler, like, scala or groovy.. and then you 
    // can make "src/main/scala".. and add scala code there! For example, see here when new unit test for groovy is added alongside java 
    // (https://stackoverflow.com/questions/19205767/include-new-test-directory-maven-surefire-plugin) -- realize that this is just to highlight 
    // that such combinations are possible.. if you really need it, maybe dive deeper in it!

// @formatter:on

    private String docResourceFolder;

    private String pdfDocFileName;

    private String htmlDocFileName;

    @Autowired
    public MavenController(MavenConfiguration mavenConfiguration) {
        this.docResourceFolder = mavenConfiguration.getMavenProperty("war.documentation.resource.folder");
        this.pdfDocFileName = mavenConfiguration.getMavenProperty("asciidoc.output.file.pdf");
        this.htmlDocFileName = mavenConfiguration.getMavenProperty("asciidoc.output.file.html");
    }

    @RequestMapping(value = "/getApiInfo", method = RequestMethod.GET)
    // NOTE: a default value is added for "returnType" requestparam so that it is never null, else a NPE can get thrown
    // at switch-case
    public Resource getThreeFile(@RequestParam(value = "type", defaultValue = "pdf") String returnType,
            HttpServletResponse resp) throws IOException {
        System.out.println(docResourceFolder);
        System.out.println(System.getProperty("war.documentation.resource.folder"));

        Resource returnResource;
        switch (returnType) {
        case "html": {
            returnResource = new ClassPathResource(docResourceFolder + "/" + htmlDocFileName);
            resp.setContentType(MediaType.TEXT_HTML_VALUE);
            resp.setHeader("Content-Disposition", String.format("inline; filename=\"" + returnResource.getFilename()
                    + "\""));
            break;
        }
        default: {
            returnResource = new ClassPathResource(docResourceFolder + "/" + pdfDocFileName);
            resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
            resp.setHeader("Content-Disposition", String.format("attachment; filename=\"" + returnResource.getFilename()
                    + "\""));
        }
        }
        resp.setContentLengthLong(returnResource.contentLength());
        return returnResource;
    }
}
