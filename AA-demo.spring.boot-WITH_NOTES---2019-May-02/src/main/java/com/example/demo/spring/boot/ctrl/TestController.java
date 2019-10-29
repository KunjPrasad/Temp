package com.example.demo.spring.boot.ctrl;

/**
 * This is not really a controller, but a placeholder class containing comments on various spring-test aspects
 * 
 * @author KunjPrasad
 *
 */
public class TestController {

    // @formatter:off
    
    // A simple working example of test: https://spring.io/guides/gs/testing-web/
    // For docs, see: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html
    
    // To have different properties for src and test. See https://stackoverflow.com/questions/29669393/override-default-spring-boot-application-properties-settings-in-junit-test
    // Essentially: (1) either add application.properties in src/test/resources, (2) or, add application-test.properties in src/main/resources
    // ..and add @ActiveProfile("test") to context-loading class, (3) or, See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html
    // ..in that @TestPropertySource annotation holds higher precedence, so use it to give the correct property location
    // ----|---- In relation to above, note from logs when tests are run by Maven.. it prints that active profile is "test".. so maybe using 
    // ......... @ActiveProfile explicitly is not needed
    
    // **IMPORTANT**: See https://stackoverflow.com/questions/44180815/can-spring-boot-test-classes-reuse-application-context-for-faster-test-run
    // ..in that once the application-context is made, Spring-test default behavior is to reuse it. As an implementation.. make a common
    // ..base class where context is initialized using @RunWith(SpringRunner.class),@SpringBootTest ..and then continue extending it to make
    // ..other test classes that use the context
    
    // **IMPORTANT**: See the test-docs.. it is possible to only load the web-context and not application context if the desire is to simply 
    // ..test annotations in controller. But then why not full testing?!
    
	
	
	/*
    * VERY VERY VERY IMPORTANT: See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html 
	* .. and note: 
	* 1) That @SpringBootTest runs by default with webEnvironment="MOCK", meaning that it creates a mock-server and not an actual
	* .. one. This is why mockMvc cannot be used for Websocket testing.. that actually needs a full-server to which websocket
    * .. client can connect to	
	* 2) Contrary to #1 above, if using webEnvironment="RANDOM_PORT", then an actual server is deployed at random-available-port.
	* .. The advantage of using random-port is that it will always be known to run, since if fixed port is chosen, it may sometime
	* .. fail if that port is not available. ALSO.. when using this seeting, (2.1) Note that there is "@LocalServerPort" which can 
	* .. be used to get the port where the server is run, (2.2) also use an @Autowired "WebTestClient" for testing using the server
	* 3) NOTE that one of the dependencies loaded by Spring-Boot-testing is JsonPath - which is xPath for Json.. see examples
	* .. its use by searching for "JsonPathExpectationsHelper", and see the code in question of https://stackoverflow.com/questions/20972223/testing-messagemapping-websocket-methods-of-spring-mvc-controllers
	*
	*
	* VER IMPORTANT: Difference between SpringRunner and MockitoJUnitRunner -- https://stackoverflow.com/questions/49635396/runwithspringrunner-class-vs-runwithmockitojunitrunner-class
	*/
	
	
    // @formatter:on
}
