package com.example.demo.spring.boot.ctrl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.demo.spring.boot.dto.DualMessageDTO;
import com.example.demo.spring.boot.dto.TestDTO;
import com.example.demo.spring.boot.srv.TestService;
import com.example.demo.spring.boot.util.Log;
import com.example.demo.spring.boot.util.StringLowerPropertyEditor;
import com.example.demo.spring.boot.util.StringMultiplier;
import com.example.demo.spring.boot.util.StringUpperPropertyEditor;
import com.example.demo.spring.boot.util.TimeAttributeAdderController;
import com.example.demo.spring.boot.util.UserStory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

// @formatter: off
/*
 * **IMPORTANT CONCEPTS..**:
 *
 * **VERY VERY VERY IMPORTANT**: FEW TOPICS FROM "SECURITY" THAT ALSO APPLIES to REQUEST/RESPONSE: (All topics shown below are select important ones)
 * 
 * [1] (Also discussed in EtceteraController.. search for keyword "SecurityContextPersistenceFilter") See Pg50 on Table6.1 of Spring-security-doc-pdf; There 
 * .. is a "HeaderWriterFilter" filter whose job is to write responseHeader at end of a request. THIS SHOWS A WAY to write response headers common to all
 * .. response; Or maybe based on request-attributes, and do so AFTER the request has been processed by controller. ALSO, an important aspect shown by
 * ..  implementation is how code can be put after chain.doFilter() method, and that gets executed at end -- This is how Spring adds postProcessingFilter
 *
 * [2] Note: you can change Http-method types using filter, see https://stackoverflow.com/questions/15365660/spring-mvc-and-x-http-method-override-parameter
 * .. BUT, BEST NEVER INCORPORATE this filter (HiddenHttpMethodFilter) as this is a huge security risk!
 *
 * [3] See https://stackoverflow.com/questions/17205841/how-to-end-the-session-in-spring-3  -- the idea that when wanting to invalidate a session, then don't 
 * .. do it on a controller annotated with @SessionAttributes, and maybe, not even in a method where one of the param has @SessionAttribute annotation. 
 * --|---- RELATED#1: FIRST OF ALL.. NOTE THAT THIS IS A BAD DESIGN!! ideally, the job of controllers is to do the processing and return result. 
 * .. Validation or failing the sessions is job of Security-filters invoked BEFORE a request hits the controller. Let's say that there is some logic
 * .. that is checked in controller and for some cases, user should be logged out. In such cases, rather than invalidating session, there are 2 ways to
 * .. proceed: (i) Either forward request to logout-url. Since it is a "forward" request, so it will remain in server and not go out, (ii) throw an
 * .. exception and let the current processing fail. Maybe, also mark some session-flag that it is no longer valid for subsequent requests. And then
 * .. in later requests, fail the check -- so this will need custom authenticators!
 * --|---- RELATED#2: Best to invalidate session with same logic as in SecurityContextLogoutHandler.logout() method, Maybe also incorporating 
 * .. other LogoutHandler implementations. Look in SecurityController for more details.
 * --|---- RELATED#2: to know about the annotations, see https://www.boraji.com/spring-mvc-4-sessionattributes-example
 *
 * [4] See notes in javadoc of SessionFixationProtectionStrategy - It mentions that doing so can cause some issues with @SessionBean and that should be 
 * .. aptly handled based on the code. One such example of failure is seen in https://stackoverflow.com/questions/10106870/security-sessionfixationprotectionstrategy-interfering-with-session-scoped-beans
 * .. where user says that on invalidation of previous session, the @preDestroy of session-bean is called, and that closes a "DB connection" object, 
 * .. that the programming logic stores in session
 * --|---- FIRST, HOW TO SOLVE.. 
 * --|----|---- The user is not very clear in how he solved.. though it is clearly said to not close the DB connection in session bean's @preDestroy,
 * .. because that was causing the issues. 
 * --|----|----|---- RELATED: See https://stackoverflow.com/questions/22848563/predestroy-on-session-scoped-spring-mvc-controllers  -- (a) NOTE that
 * .. @PreDestroy is called for session-beans! (b) **IMPORTANT** NOTE that SessionScope allows provision to register extra destroy-handlers
 * --|----|---- One solution could be to set a new logoutHandler that comes before the "SecurityContextLogoutHandler" and closes the "DB connection" 
 * .. on logout. However, that won't work if there is a timeout on user-session
 * --|----|---- Other solution, see https://stackoverflow.com/questions/11843010/logout-session-timeout-catching-with-spring-security -- is to add a 
 * .. listener for SessionDestroyedEvent.. and this will work consistently both on logout and session-timeout.. BUT, the issue here is that the session
 * .. would have already been destroyed, so connection object cannot be retrieved for closure
 * --|----|---- CORRECT SOLUTION: Look at code of "SessionFixationProtectionStrategy". It allows overwriting extractAttributes() method, wherein, user
 * .. can add the logic to nullify the connection object in original session when copying, and also add logic in @PreDestroy to bypass closing the
 * .. connection if the field is null. This way, at time of new session creation for fixation protection.. the connection object will get transferred,
 * .. the old session will not have a session, so nothing to worry about there, and the new session will get connection
 * --|---- RELATED: Is this a bad-design? Not necessarily - without knowing more information. Generally connection objects are pooled and kept separately.
 * .. So, it might come to think that maybe user-code is bad design. But consider the scenario where the service is a "provider" that allows users to make
 * .. DB connection. This may seem as a situation where a connection per session may apply. It could still be bad situation because unless the total 
 * .. count of connections are kept within a limit.. this logic can blow up - each incoming user connection will make session.
 *
 * [5] Note that Spring boot auto-configures to expose static-files within certain folders. This is not good from security viewpoint and is best disabled. As said in
 * .. Spring-mvc-docs.pdf, when users want to put jsp files, it is best done within WEB-INF folder, and same should also apply for such static-files too (as best
 * .. practices in security). Still.. the fact remains that Spring boot auto-configures to show them - so what to do?
 * --|---- For information, see https://spring.io/blog/2013/12/19/serving-static-web-content-with-spring-boot  and   https://www.logicbig.com/tutorials/spring-framework/spring-boot/boot-serve-static.html
 * --|---- See Spring-Boot's "ResourceProperties " github code for list of locations that are exposed. (5.a) Realize that paths like "classpath:/static/" means maven 
 * .. folder structure like "/src/main/resources/static/...". Similarly, "classpath:/resources/" means maven folder structure like "/src/main/resources/resources/...". 
 * .. (5.b) In this project, 3 folders for resources, static and public have been made and files within it could be seen. (5.c) DO NOTE: both /static and /resources are 
 * .. given same file "test.html", but only the one from /resources shows up because it is queries first! (5.d) If you want to disable it.. check code of SpringBoot's
 * .. WebMvcAutoConfiguration .addResourceHandlers() -- essentially you'll need to add a resource-mapping for "/**" which will override default
 * --|---- REALIZE that if you want to expose certain files, this shows how to do so by modifying resourceHandlerRegistry. Same can be seen in
 * .. https://www.boraji.com/spring-mvc-5-static-resources-handling-example
 * 
 *
 *
 *
 *
 * CONTINUING REQUEST/RESPONSE NOTES BELOW:
 * 
 * #1: Use of @RequestMapping on class and method level
 * @RequestParam, @PathVariable shown
 * NOTE: there is a @RequestPart for use with multipart!!
 * QUICK: If it is instead needed to use SOAP.. then at least basics are covered in https://dzone.com/articles/simple-java-soap-web-service-using-jdk-tools
 * 
 * 
 *
 * #2: Use of validation
 * 0) **VERY VERY VERY IMPORTANT* BASIC DETAILS:
 * ---- Read the hibernate-validator-version6 pdf file for many useful extra comments
 * ----|---- See Pg133 -- in that SpEL can also be used for validation
 * ----|----- Just to mention at beginning.. that there are notes on how to use SpEL::  https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#expressions
 * .. To work with SpEL, one mainly needs a parser (spelParser to convert from spel expression to an evaluation-action); and an evaluation-context on which the 
 * .. parser acts to create the result (as desired by spel expression). The evaluation-context in itself contains a root-object, and can also register extra properties 
 * .. and functions that can be used. Also, for spel use, see https://www.baeldung.com/spring-expression-language
 * ----|---- Example of spel use in validation:: See https://javatar81.blogspot.com/2016/06/hibernate-validator-spring-expression.html
 * ---- REALIZE that bean-validation-2 api is now available (jsr-380) which provides lot more functionalities!
 * ---- Compare adding validator for web vs JMS. There aren't so many different options available - just a few places to register; Also, no 
 * .. @ControllerAdvice class
 *
 * i) See ValidationConfiguration.class to wire message source so that #2 works
 *
 * ii) Use of hibernate validation and returning message:
 * --|---- as hardwired by user 
 * --|---- from message-source; Also, the returned message has values from validator annotation! Link also given in messages.properties. 
 * .. See https://raymondhlee.wordpress.com/2014/07/26/including-field-value-in-validation-message-using-spring-validation-framework-for-jsr-303/)
 * --|---- **VERY IMPORTANT**: Enhanced messages based on user input http://blog.codeleak.pl/2014/06/better-error-messages-with-bean.html
 *
 * iii) Use of payload (Think of payload as error-handler); See https://www.logicbig.com/how-to/code-snippets/jcode-bean-validation-javax-validation-payload.html
 * ..and https://www.logicbig.com/tutorials/java-ee-tutorial/bean-validation/constraint-payload.html
 *
 * iv) **VERY VERY IMPORTANT**: On topic of validation, realize that there are 2 ways to proceed. Say, we need a string to only be alphanumeric.
 * .. One choice is that we make a new type for such fields and directly fail serialization if that field is not conforming to requirements. This
 * .. is a very strict case, since the request fails even before it could hit the handling method. A disadvantage of doing so is that user will not
 * .. receive all failures which they could have from bindingResult (which would have accumulated all failures before raising exception). The other 
 * .. choice is to receive value as string, and do validation on it later, but the drawback here is that one may forget to add validation, and cause
 * .. leak of bad data in database. It's like whether a number-field should be kept as string or long (for example..)
 * --|---- **IMPORTANT - ARCHITECTURE QUESTION**: ON TOPIC OF BINDINGRESULT -- Should we throw exception in codes of all controller method when there
 * .. is a bindingResult with error and we do so before hitting service layer? Or is there an alternative? One alternative is to add an aspect before
 * .. hitting the service layer, provided it has bindingResult as a method-param. Then we check it for errors before proceeding. This can ease the code
 * .. but it creates a scenario where the service-method is given an unnecessary parameter that won't be used (and Sonar may complain about it).
 * .. DO NOTE that this scenario is not a limitation of AspectJ but of Spring-AspectJ integration (See POint#5 on Aspect logging). AspectJ in itself
 * .. allows accessing calling-method but Spring's integration does not. So, this would be needed for Spring-implementation due to its limitation.
 *
 * v) REALIZE that although many documentations will say to validate everywhere, it is sufficient to do so at "points of coupling" to other system. 
 * --|---- Example#1: if receiving inputs from UI, there is no need to do a whole lot extra validation at service/db level. The object is likely to 
 * .. remain validated as long as it flows inside same system. 
 * --|---- Example#2: Consider case when server writes message to a MessageQueue and later reads from it. Is validation needed there..?! Not "likely", 
 * .. since the data written would have been valid, so no need to re-validate on reading it back. 
 * --|---- **IMPORTANT**: HOWEVER, one may do security-checks like to check that a different user be able to read only selected entries from DB-data 
 * .. stored by some other user
 * --|---- see https://www.baeldung.com/javax-validation-method-constraints
 *
 * vi) **VERY VERY IMPORTANT**: When making custom validator.. maybe extend the "SmartValidator" instead of validator! That way you can also use validation
 * .. groups. ALSO, do recall from hibernate-validator docs, that the default group associated to a constraint when none is specified by user is "Default".
 * .. This can be useful when you want to run a validation that has no group.. in conjugation with one that has a group!
 * 
 *
 *
 * #3: ***VERY VERY IMPORTANT*** MethodArgumentResolver and Validation!
 * i) Spring has 4 different concepts: ArgumentResolver, Converter (can also have converter factory), PropertyEditor and Formatter
 * .. (See Related-comments for details)
 *
 * ii) See DualMessageDTOMethodArgumentResolver.class for custom argument resolver implementation. 
 * ..Reference: https://www.petrikainulainen.net/programming/spring-framework/spring-from-the-trenches-creating-a-custom-handlermethodargumentresolver/ 
 *
 * iii) Example of custom spring validator: https://www.concretepage.com/spring/spring-mvc/spring-mvc-validator-with-initbinder-webdatabinder-registercustomeditor-example
 * .. and https://www.intertech.com/Blog/spring-frameworks-webdatabinder/  (**NOTE: Different binding date-formats for different arguments of method!)
 *
 * iv) From #iii and others, note: 
 * (iv.a) Custom Spring Validator can use Spring wiring support 
 * (iv.b) When wiring a custom validator to a controller, if you are registering its type as "Validator" as done here, then need to use @Resource and not @Autowired, so as to bind by name
 * (iv.c) The Validator/Message source binding done in #2 to enable custom message need not be removed to enable custom validator. They can co-exist
 * (iv.d) NOTE that: @InitBinder is defined one per model per controller. However, if you don't give any model name, then it works throughout the controller, and becomes one per controller!
 * (iv.e) **IMPORTANT** Note that within the @InitBinder, one needs to bind validator if you want it automatically called. Else will have to manually call validator in corresponding method
 * **IMPORTANT**: See Related#2 for much-important extra info on @InitBinder
 * 
 * #3.v) ***VERY VERY IMPORTANT*** See combination of:
 * (3.v.a) https://stackoverflow.com/questions/18091936/spring-mvc-valid-validation-with-custom-handlermethodargumentresolver
 * (3.v.b) https://stackoverflow.com/questions/3423262/what-is-modelattribute-in-spring-mvc   --and--   https://stackoverflow.com/questions/21824012/spring-modelattribute-vs-requestbody
 * (3.v.c) https://stackoverflow.com/questions/28938540/how-can-i-access-path-variables-in-my-custom-handlermethodargumentresolver
 * The ideas are as follows:
 * ---- From 3.v.a, notice that:
 * ----|---- On the surface the post seems to imply that "Spring has a defect" wherein, either let spring construct the object (say, using 
 * .. @ModelAttribute) and then it will invoke validation; OR, if you construct on own (using argumentResolver), then won't be able to 
 * .. auto-validate using @Valid. BUT, THAT IS SIMPLE.. IN DEEP, IT MIGHT BE MORE! First, note that "ModelAttributeMethodProcessor" that handles
 * .. logic for @ModelAttribute is itself an ArgumentResolver. Looking at its github code shows how it also creates a "BindingResult" object and
 * .. attaches it to the "ModelAndViewContainer" -- AND THAT IS MAIN REASON -- how we are able to write BindingResult argument in methods and have
 * .. it auto-populated by Spring-MVC (See https://github.com/spring-projects/spring-framework/blob/master/spring-web/src/main/java/org/springframework/web/method/annotation/ModelAttributeMethodProcessor.java)
 * .. [Digressing#1] We also see "isBindExceptionRequired" method which shows it being hardwired in logic to look for BindingResult argument in (i+1)th
 * .. location to identify if an error should be raised or just bindingResult sent. [Digressing#2] Also, "bindingResult.getModel()" goes back to 
 * .. "AbstractBindingResult" which shows that it returns a map with original object and errors object (stored as a special key) [End of digression].
 * .. Coming back, we now see how same code can be added to custom argument-resolver to enable a bindingResult. So.. this is not an "un-doable" task!
 * ----|---- This brings up related architecture concerns. Like, (i) When doing custom resolver, do not also have another @RequestBody.. else Spring
 * .. will make 2 bindingResult objects in ModelAndViewContainer, and will get confused on which one to add to method! A better option may be that when
 * .. making own handler (which comes after the "ModelAttributeMethodProcessor"), use existing bindingResult instead of making new one. Maybe, use "method"
 * .. argument to identify if that is possible. (ii) Other option is always to just throw validation exception inside the handler implementation!
 * ----|---- Just to mention explicitly, if you want to do JSR-303 validations, then autowire the validator in handler implementation and use it!
 * ----|---- RELATED and IMPORTANT: See discussion in "etceteraController" on RequestMappingHandlerAdapter. Specially how it can be configured to 
 * .. create global default for WebBinder that gets used for all calls.
 * ---- From 3.v.c, notice how pathParams can be obtained back in custom argumentResolver! Similarly, but much simpler, once the request object is obtained, one can get requestParams, headers, etc.
 * ---- To understand more on modelAttribute, see https://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/html/mvc.html#mvc-ann-modelattrib-methods
 * .. and https://www.baeldung.com/spring-mvc-and-the-modelattribute-annotation   Look at Spring rules for constructing it.. in short, when
 * .. @ModelAttribute is put on method-return-type, it means that Spring will run that method before running actual handler, and as per logic
 * .. of the method, Spring can add "attributes" in model. Thus when the model comes to handler, it'll always have those defaults/values set.
 * .. Furthermore, See Pg 46/47 of "Spring Mvc Docs" for better understanding of how @ModelAttribute, binding and validation works, 
 * .. which, by the way, are all these 3 are different topics!           
 * ---- IMPORTANT.. NOTE: Since deserialization is already done before @ModelAttribute is formed, so custom json serialization is NOT called!!
 * .. So, in this example, in "/get2/.." method, test1 and test2 won't change even if a @JsonDeserialize(using=...) is added. AND, this is reason 
 * .. for difference between JsonSerializer (that work on body) vs argumentResolvers, converters, etc that work on forms or QueryParam, pathParam, etc
 * 
 * #3.vi) @Valid vs @Validated
 * [[IMPORTANT: before proceeding.. remember to also read related comments#2, and specialy point#2.5]]
 * IMPORTANT webpage with all related details: https://reflectoring.io/bean-validation-with-spring-boot/   can also see https://stackoverflow.com/questions/18911154/how-to-specify-validation-group-for-valid
 * ---- When @Validated is on method-arg (and not on class), then it behaves like @Valid but allows passing validation group. The separate annotation is 
 * .. needed since @Valid in jsr-303 does not allow groups with @Valid.
 * ----|---- Note that when doing nested validation.. then each class will be defining their own validations with groups. So, the idea is to trigger nested
 * .. validation using @Valid, and then let the definition in validation get handled by defined groups. Point is.. there is no need to use @Validated
 * .. for nested validations. ALSO, if you look at definition of @Validated, it is not supported for "FIELD" target
 * ----|---- **IMPORTANT**: Example of using validation-groups, even on class-level custom validation annotation defined -- see https://blog.codeleak.pl/2014/08/validation-groups-in-spring-mvc.html)
 * ---- @Validated on class-level triggers MethodValidationPostProcessor (if registered; done by default in Spring-boot). This allows for checking for 
 * .. validation on method arguments (either objects with @Valid, or primitive type). HOWEVER, this does not puts result in bindingResult, but raises
 * .. exception - because validation is done as part of post-processor
 * ----|---- Since @ModelAttribute are made from request-params, so they are also validated if they are in method-argument. Once again, failure results
 * .. in raising an exception rather than binding it to BindingResult
 * ----|---- When @Validated is on class (to trigger method-param validation), it cannot also be used with method-param, so how to tell the validation
 * .. group to use for method-param validation? Use @Validated on top of method, and pass the group!
 * ---- **VERY VERY IMPORTANT** extra points to remember
 * ----|---- Since @Validated works using a postProcessor.. so do remember, that it is available everywhere in Spring.. even in different service class!
 * .. Or even if you are in DB layer class, listening from JMS, etc.. anything!
 * ----|---- If you need to modify above behavior.. maybe look at the github code for the postProcessor, and "MethodValidationInterceptor"
 * .. which is used by it [[Like, if you want post-processor to be able to put violations in BindingResult, rather than throw exception!]]
 * 
 * #3.vii) Note that even the beans created after loading properties can be validated: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-validation
 * .. To add, if you want to read some other properties file, just make a class with @Configuration, adding @PropertySource; 
 * .. and inside add @Autowired Environment..., as done in MavenConfiguration
 * 
 * #3.viii) **VERY VERY VERY IMPORTANT**: Say you want to add a ModelAttribute, or a PropertyEditor -- but globally. How to do? 
 * .. This is best done by coding it in a class annotated with @ControllerAdvice. See https://keenformatics.blogspot.com/2013/08/how-to-create-global-initbinder-in-spring.html
 * .. and last paragraph of #2.1 of https://www.baeldung.com/spring-mvc-and-the-modelattribute-annotation
 * .. OTHER OPTION could be to configure it in RequestMappingHandlerAdapter (See comments in EtceteraController class)
 *
 * #3.ix) If you have a list type input, then using @Valid does not work. SO what to do?
 * --|---- Option-1: Using wrappen list object - but not sure if it works properly with jackson, see https://stackoverflow.com/questions/28150405/validation-of-a-list-of-objects-in-spring
 * --|---- Option-2, but it requires Java-8 and Hibernate validator-6: see https://www.baeldung.com/bean-validation-container-elements
 * --|---- Option-3: Make your own validator, call it within a method from controller and do validation
 * --|----|---- If going by option-3 above, what kind of “fieldNames” should be given to error object.. also how to integrate hibernate-validator 
 * .. call? (1) Make a bean of “LocalValidatorFactoryBean” – in some config file; (2) Autowire the LocalValidatorFactoryBean bean.. say using
 * .. @Resource(name=”..”), and using its type as Spring-Validator (see api of LocalValidatorFactoryBean); (3) Call the bean when you need 
 * .. validation – this invoked equivalent of hibernate validator on the bean; (4) On how to format error message, see: https://stackoverflow.com/questions/12680730/validate-a-list-of-nested-objects-with-spring-validator
 * .. (5) Also, do realize to add necessary null-pointer checks in validation method; (6) NOTE: following link#4, while 
 * .. ValidadationUtils.invokeValidator(...) works fine in adding error message, but an exception will be raised if you try doing 
 * .. error.rejectValue("fieldName", "code", "message") after pushing in nestedPath with brackets - for array. This fails because
 * .. there is no actual field by that name.. and spring tries to do a get call and fails. Instead, try to use the code as done by
 * .. Spring itself, i.e. look at SpringValidatorAdapter.validate() method where it explicitly sets nestedField to use
 *
 * #3.x) Making custom validation "annotation" (..and not just a validator for an object): See https://www.baeldung.com/spring-mvc-custom-validator
 * .. NOTE that it also shows how to make class level validation-annotation that works on multiple fields --AND-- for that example, it shows how the
 * .. extended annotation can have extra custom values passed to it (i.e. outside of groups, payload, message and value)
 * ---- RELATED: Realize that when doing initialization in validation-implementor class, its a good practice to also read groups and payload
 * ---- NOTE: You can use @Autowired to connect to Spring bean. See https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#validation-beanvalidation-spring-constraints
 * ---- **VERY VERY IMPORTANT**: Since static utilities exist to access the incoming-request.. so this custom-annotation validator implementation 
 * .. can do checks dependent on the request paramters or headers. This is dynamic validation! Same also applies when doing dynamic validation 
 * .. inside custom validator
 * ---- **IMPORTANT**: Note that JSR-380, or Bean-Validation 2.0 API is now available through which a lot more annotations are available. Also, it is 
 * .. possible to put validation annotations: (a) on containers, and (b) on the generic variable inside the container 
 * .. (like, @NotEmpty List<@Valid User> userList)! See https://www.baeldung.com/javax-validation
 * 
 * 
 * 
 * #4: Exception and exception-handling
 * i) Notice that all exceptions are runtime. So no worry of checked exception. 
 *
 * ii) Also, they specialize from one exception; So only one handler needed
 *
 * iii) Also note that there are 2 special handlers: for constraintViolationExcp; And for all "Unexpected" exceptions
 * .. **VERY VERY IMPORTANT**: Note that for single exception handler like construct as done here.. where a single handler is for various
 * .. exceptions corresponding to different HttpResponseStatus. in such case don't add @ResponseBody annotation to exception handling method.
 * .. Here, you DO want to return ResponseEntity with controlled response status!! If you just use @ResponseBody.. then all of the responses
 * .. to exception will actually go with 200 status!!
 *
 * iv) All exception have arg-constructor and not no-arg constructor. This ensures that whenever they are called, the arguments are given
 *
 * v) **IMPORTANT** Controlling order of @ControllerAdvice: https://stackoverflow.com/questions/19498378/setting-precedence-of-multiple-controlleradvice-exceptionhandlers
 *
 * vi) NOTE: when throwing exception because of improper configuration (example: unable to configure datasource in DatasourceConfiguration.java),
 * .. then do NOT throw ApplicationException! Just throw a runtimeExcepion. ApplicationException is when there is an explicit application related issue!!
 *
 * vii) Control ordering of advice using @Order: https://stackoverflow.com/questions/19498378/setting-precedence-of-multiple-controlleradvice-exceptionhandlers
 *
 * viii) **VERY VERY IMPORTANT**: See https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-error-handling
 * .. The main idea is that @ControllerAdvice is available to dispatcherServlet. So what do we do when an exception is raised by filter - which happens
 * .. before DispatcherServlet, and so it won't have access to @ControllerAdvice. This is where this section is used! NOTE:
 * --|---- By extending errorAttributes, one can control what response is returned. 
 * --|----|---- **IMPORTANT** Note that the errorAttributes implementation used by Spring is DefaultErrorAttributes -- which, by Javadoc -- can also 
 * .. return bindingResult exceptions (i.e. BindException()). THIS MAKES "DefaultErrorAttributes" usable as a good template for making own error-DTO, 
 * .. because that should be able to show list of associated failures, and not just one exception. 
 * --|---- Building up on "ErrorController" for handling errors.. and on your own design on having all exceptions extend just one exception-type for 
 * .. which a handler is made.. maybe, for sprng boot, you can just make custom "ErrorController" INSTEAD OF THE EXCEPTION-HANDLER CONTROLLER-ADVICE!
 * --|----|---- HOW IS SPRING BOOT EVEN ABLE TO DO THAT? Searrch for "registerErrorPageFilter " in EtceteraController2 -- which comes down to 
 * .. initialization of ErrorPageFilter(), which by Spring boot docs is down very high up in filter chain (See https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-embedded-container-servlets-filters-listeners-beans)
 *
 *
 *
 * #5: Aspect-logging
 * i) Around logging delegated to aspect. NOTE: Error logging is not done in aspect -- that is delegated to handler!! If being done by aspect
 * .. then it should be removed from handler! Done here using new @Log marker annotation; And LogAspect in the aspect-handler using the annotation
 * --|---- Just for knowledge.. since aspect-logs can display class names: See https://stackoverflow.com/questions/15202997/what-is-the-difference-between-canonical-name-simple-name-and-class-name-in-jav
 * --|---- **VERY IMPORTANT**: See comments in point#10 on how process parameters can be stored so that they can be logged properly in one place
 *
 * ii) **IMPORTANT**: Note that @Order can added to aspect to control the ordering of multiple aspects. See https://stackoverflow.com/questions/9051728/ordering-aspects-with-spring-aop-mvc
 *
 * iii) **VERY VERY IMPORTANT**: @Transactional aspect is by default configured to have lowest order. See https://stackoverflow.com/questions/49678581/spring-retry-with-transactional   
 * .. This also means that any log, or even Spring Security's pre/post-handle are done before Transactional
 *
 * iv) **VERY IMPORTANT**: Look at "MethodInvocationProceedingJoinPoint" -- that is the one being sent to Spring Aspect. In its Github code, 
 * .. notice that it does not give fileName, lineNumber. BUT EVEN MORE IMPORTANT is that unlike AspectJ, it does not allow identifying the 
 * .. caller-class from where the call is made to the method, it only identifies the called-method. For this reason, defining Pointcuts based on 
 * .. "call(..)" is disallowed in Spring; It only allows making it based on "execution(..)". Had there been an access to calling method, one could
 * .. have also looked at arguments for the caller. A repercussion of this, See #2 above - on validation.
 * FOR MORE INFORMATION ON ASPECTJ:
 * --|---- AspectJ can also provide information about caller method. See https://www.eclipse.org/aspectj/doc/released/progguide/language-thisJoinPoint.html
 * --|---- A good difference to know for AspectJ.. between call(..) and execution(..) pointcut. See https://stackoverflow.com/questions/18132822/execution-vs-call-join-point
 * --|---- See here for use of if() clause to dynamically check if an aspect is to be called. https://www.eclipse.org/aspectj/doc/released/adk15notebook/ataspectj-pcadvice.html
 * 
 *
 * 
 * #6: Swagger:
 * i) See https://www.baeldung.com/swagger-2-documentation-for-spring-rest-api  ..and for meaning of configurations 
 * .. and also security of swagger (maybe will secure later)
 * .. Done here on SwaggerConfig. Note that the urls that get available are: /your-app-root/v2/api-docs  and  /your-app-root/swagger-ui.html
 * ii) For nice doc on use of Swagger annotation: See https://jakubstas.com/spring-jersey-swagger-create-documentation/
 * iii) **IMPORTANT**: 
 * ---- For @Api annotation, the "tags" is more important, give proper name, and keep it single valued
 * ---- For @ApiResponses, each code needs to be unique.. so if there are multiple validations that can cause failure, list all as single string
 * .. ALSO add name of user story or defects!
 * ---- For @ApiParam, don't change "name" field else it'll create vagueness in pathparam/requestparam mapping. Change "value" as it is for description
 * ---- For @ApiModel annotation, do NOT add "value" field as it replaces class name
 * ---- For @ApiModelProperty, value is more important. The name, notes property are not used
 * iv) Note that even after making Docket bean as done.. additional customizations can be done, by calling methods after .build(). See ApiInfo in
 * .. section on "Customizing Swagger" in https://dzone.com/articles/spring-boot-restful-api-documentation-with-swagger
 * v) **VERY IMPORTANT**: See mavenController for important details on changing swagger to files during test execution!! "convertSwagger2markup"
 * 
 * 
 * 
 * #7: Filter (used for Rate-limiting) : So, 2 features
 * i) Spring has concept of filter and filterRegistration.. Note that if no special filter-registration is done, then filter mapped to /* url; and also enabled
 * .. make filterregistrationBean if you want changes. See https://gist.github.com/rgiaviti/80d50041541475d5ad7a752b53aa4eed  and   https://stackoverflow.com/questions/19825946/how-to-add-a-filter-class-in-spring-boot
 * ii) For RateLimiter, see https://www.baeldung.com/guava-rate-limiter -- also done in code as filter. **NOTE**: the rateLimiting filter is 
 * .. disabled in test environment. Also notice how test is set up to read corresponding property!! The otherwise suggested .isEnabled(..) does
 * .. not seem to work!!
 * iii) **VERY VERY IMPORTANT** : In the Servlet 2.4 specification, response.sendError() and response.setStatus() are treated differently. 
 * .. The former redirects you to the configured error page, but the latter still assumes that you’re going to be providing the response yourself
 * .. so, when throwing error from Filter, use "sendError", not, setStatus. See https://stackoverflow.com/questions/44205440/whats-the-difference-between-setstatus500-and-senderror500
 * iv) **VERY VERY IMPORTANT**: It is observed, in rateLimitingFilter's registration bean.. that simply setting enabled(false) for test 
 * .. environment somehow still does not work!! So the other alternative is to simply map the filter to a non-existing url!
 * v) **VERY VERY IMPORTANT**: Note that when running Spring in embedded mode, there is inherently no profile associated with it! Thus it can 
 * .. become hard to control bean creating specific to this mode (like, for example, to say that a h2-datasource should be made ONLY when 
 * .. running in this profile, and maybe use JNDI when in some other profile. The way to address is by adding "spring.profile.active=embedded" 
 * .. property in application.properties. This sets profile for embedded mode! **ALSO NOTE** if you pass {spring.profile.active,embedded} as
 * .. system param when running Application.main in eclipse, then also this is set. And by Spring's order of loading properties, it will first
 * .. check for application.properties file, then override it with application-embeded.properties. At this momemnt if none of application.props
 * .. or application-embedded.props is found, then Spring just sets the default values!!
 * **ALL IN ALL**-- maybe best is to always use a profile, and not make application.properties file explicitly!!
 * vi) See "CompositeFilter" in one of the answer in https://stackoverflow.com/questions/22869901/how-to-get-dispatcherserveletinitializer-functionality-in-spring-boot
 * .. ESSENTIALLY NOTE.. a filter can be defines which is rather a "filter-chain"!
 *          @Bean
            public Filter compositeFilter() {
                CompositeFilter compositeFilter = new CompositeFilter();
                compositeFilter.setFilters(ImmutableList.of(new CorsFilter(), shiroFilter));
                return compositeFilter
            }
 * 
 * 
 * 
 * #8: HATEOAS (on own.. not of Spring)
 * i) To implement a dynamic-programmatic way of HATEOAS.. would require use of "ResponseFilter". Spring does NOT have it; 
 * .. And so one needs to use "ResponseBodyAdvice". See https://mtyurt.net/post/spring-modify-response-headers-after-processing.html  or   https://sdqali.in/blog/2016/06/08/filtering-responses-in-spring-mvc/
 * .. ALSO NOTE (from website): Note that the postHandle method of HandlerInterceptor is not always ideally suited for use with 
 * .. @ResponseBody and ResponseEntity methods. In such cases an HttpMessageConverter writes to and commits the response before 
 * .. postHandle is called which makes it impossible to change the response, for example to add a header. Instead an application can 
 * .. implement ResponseBodyAdvice and either declare it as an @ControllerAdvice bean or configure it directly on RequestMappingHandlerAdapter.
 *
 * ii) See use of BaseDTO, BasePerfTimeHandler, BaseUserStoryHandler for related implementation. **VERY IMPORTANT**: Remember to make BaseHateosHandler 
 * .. as lowest priority so it is executed last!! ALSO, notice the way response class is checked in supports() method
 * .. ALSO used are PreFilterTimeRegistrationFilter (and its registration), @TimeattributeAdder, TimeAttributeAdderAspect, etc.
 * --|---- NOTE: These HATEOAS fields should be made such that they are serialized, but not deserialized!! -- Though doing so may affect testing
 *
 * iii) Notice the way the same implementation is used to log time - which can be used in performance analysis. This also includes 
 * .. changing priority in filterRegistration bean to give it the highest priority filter!!
 *
 * iv) **VERY VERY IMPORTANT**: When json is serialized, it uses the "keys" as set by paramter name or @JsonProperty. HOW DO YOU INTERNATIONALIZE HERE? 
 * .. There is also extra related issues here: if you say that we should use different response classes, then what return value do we put on method? Do we
 * .. revert to returning a map value and lose strong object model? And even if we do that, how do we ensre custom deserialization when user sends json?
 * .. And do all this while being open to accommodate more language extensions?
 * --|---- The best answer I can think of.. use ResponseBodyAdvice; along with HATEOAS concept. So, have a field called "_keyDescription" which is of 
 * .. map-type. Within the _keyDescription map, add "key-names" used in json data as key, and the meaning of those keys as values. It is here, in 
 * .. setting the value.. that internationalization can be brought in. So, user will see json-data keys in which-ever language, but the description
 * .. will tell it in their language what it means. This solution solves all problems above. FEW MORE THINGS TO NOTE:
 * --|----|---- Based on application, we need to decide in the advice whether description for all fields should be given, or only for the ones that 
 * .. are non-null. DO NOTE THOUGH.. if you are having "Consumer Driven Contracts" (See https://martinfowler.com/articles/consumerDrivenContracts.html) 
 * .. where same entity can be changed to dto with different fields being hidden (by making them null), depending on customer requirement, then the 
 * .. responseBodyAdvice doing this should come after the one that makes the "_keyDescription" HATEOAS map, and simultaneously remove those entries
 * .. to keep consistency with customer contracts. 
 * --|----|----|---- **REALIZE** that this is a security issue, i.e. you don't want to give details about nulled fields to users who are non authorized to know of it 
 * --|----|---- It can be integrated by swagger, by requiring that description-values in a particular language be considered as default if it is not found 
 * .. for a specific language as chosen by user. Also, best would be if Swagger also could integrate with it.. but not sure how to do that.. so there may be 
 * .. some repitition here!
 * --|----|---- This approach could be particularly useful for OPTIONS call. One of the issue with secured-REST is that it is a client contract. Unless
 * .. someone actually knows what json to send, they can't send it! So, maybe an OPTIONs endpoint can be made that returns a sample data back to customer
 * .. with information on schema. Essentially, this becomes a way to transfer schema to user! - but via json, and dynamically!
 * --|----|----|---- Useful in UI label: Combining with above OPTIONS call.. UI can include logic wherein instead of writing text by their own, they pick it
 * .. up from services. This reduces duplication and "naturally" integrates internationalization! So, UI simply becomes responsible for arranging data,
 * .. but provisioning of the data is from server! This also fits with the **SECURITY** aspect where OPTION based UI design won't show certain fields to 
 * .. users if they are no authorized to see it, and so, no field-information is being provided. This also makes OPTION different from response of, say, GET
 * .. or PUT, etc call, where a field to which the user is authorized can still have a null value, but it doesn't mean that we don't show it to them.
 * --|----|----|---- **ALSO** See notes under OtherDetailsController related to using Swagger, Hateoas, and data-governance
 * --|----|---- See Martin Fowler Microservice (https://martinfowler.com/articles/microservices.html ; also have the pdf file for it) section on 
 * .. "Decentralized Data Management" where he says that different team may end up assigning different labels for same business-object. Firstly,
 * .. (it's a digression, but needed..) note that in a good microservice arcgitecture, one DB should not be controlled by 2 services that are 
 * .. considered independent. That's a bad-design; and means something else needs addressing. (Still digressing..) Now, there may just be foreign-keys 
 * .. across different DB, to help reference one another, and keeping consistency of names there requires enterprise level EDAD! (End of digression, 
 * .. relating to point here..) HOWEVER, if we are in just one microservice and one DB, in that case, one can add tasks for periodically checking if 
 * .. the dto-json-field"s" referring to same internationalized-description-holder have the same json-name. This check ensures that customer will always
 * .. get same json field name, no matter whichever api they choose to get that json! ALSO.. ideally, it'd be good if there is just one DTO with a 
 * .. json field referring to the internationalized-description-holder -- because that implies a good denormalized design; and so, ability to use 
 * .. consistent transformation rules from entity to dto
 * --|----|---- AGAIN: See Martin Fowler Microservice (https://martinfowler.com/articles/microservices.html ; also have the pdf file for it) section on 
 * .. "Decentralized Data Management".. One more advantage of this.. combined with the fact that there is an OPTIONS call, or even to get the 
 * .. _keyDescription with GET calls (configured by passing a header), is that when a microservice communicates with other service in back to get the 
 * .. data which it then forwards to user, then the calling service can pick up field-descripton-values from called-service, and pass the same to user.
 * .. This further ensures that same "field-description" is used across the enterprise
 * --|----|---- ALSO NOTICE: Making information available to people-with-disability, or to make code accessible (508 testing) is something that lies on
 * .. UI side. So, this is not something service can help with.. but say, if it is needed to convert to Braille - then that is something where a library 
 * .. can be inserted which makes this change before sending result out (OR.. maybe it could even be done on client side) -- I'm assuming that Braille 
 * .. conversion is a "script-change", but it has same vocabulary as English. So, it uses same words as English, but in different scipt. If that is not
 * .. the case, then Braille-specific vocabulary can be inserted this way!
 *
 * 
 * 
 * #9: Custom response (Multipart section is VERY VERY IMPORTANT)
 * i) XML
 * i.1) To get xml response, add spring's "jackson-dataformat-xml" in pom. See https://stackoverflow.com/questions/32654291/spring-boot-rest-with-xml-support
 * i.2) Note that xml needs an outermost tag that is not needed for json. This makes it necessary to have a class level tag, as added in TestDTO
 * .. This is "@JacksonXmlRootElement", and not "@XmlRootElement" for jaxb. See https://stackoverflow.com/questions/45230599/spring-boot-xml-change-root-element-name?rq=1
 * i.3) NOTE, the @JsonProperty also changes tag for xml response!!
 * 
 * ii) See related notes #4 for info on CSV. Note that a custom csv mime type is made. Protocol-buffer is not done; Nor excel, pdf, etc. 
 * .. As a reference, see https://aboullaite.me/spring-boot-excel-csv-and-pdf-view-example/
 * .. **VERY VERY IMPORTANT**: Note that doing zip of response json, or xml, or html - in Spring Boot now.. is VERY VERY EASY!!! There are
 * .. properties that do it automatically! See https://stackoverflow.com/questions/21410317/using-gzip-compression-with-spring-boot-mvc-javaconfig-with-restful
 * .. and http://bisaga.com/blog/programming/web-compression-on-spring-boot-application/
 * .. ALSO NOTE, in Postman, when receiving request, you will still get JSON! But if you check response header, that will have extra header 
 * .. "content-encoding=gzip"!! See server.compression.enabled property and related! (ALSO, maybe don't have it in test setting)
 * 
 * iii) Realize that in addition to custom message converter.. one can always.. just put the writing logic inside the controller/service itself. Just that becomes restrictive
 * .. when compared to having custom message converter. See https://stackoverflow.com/questions/22947751/how-to-return-csv-data-in-browser-from-spring-controller
 * 
 * iv) If the service is to downloading file, remember: 
 * iv.1) Add the content-disposition, content-length and content-type (application/octet-stream)
 * iv.2) If using @RestController on class, either: (a) use void return type and copy file to response stream, or, (2) make and return 
 * .. FileSystemResource. **VERY IMPORTANT**: Note for (iv.2.a), when return type is "void", spring requires user to set header and body!
 * .. ALSO NOTE: There is something called "ClassPathRespource" - to return file from classpath!!
 * iv.3) reference: http://www.javainuse.com/spring/boot-file-download
 * iv.4) **VERY VERY IMPORTANT**: Note that Spring has a streaming response option; and also a corresponding handler, StreamingResponseBody and 
 * .. StreamingResponseBodyReturnValueHandler. See https://www.logicbig.com/how-to/code-snippets/jcode-spring-mvc-streamingresponsebody.html
 * 
 * v) MULTIPART/FORM-DATA:
 * v.0) On a deeper level, concepts related to multipart implementation include MultipartResolver , MultipartConfigElement and MultipartFilter.
 * .. They are discussed in EtceteraController. Also see https://www.baeldung.com/spring-file-upload
 * v.1) At least for Spring-boot, everything is covered. Just start using: @RequestParam("paramFile") MultipartFile file - to access file inside 
 * .. a multipartfile-form data linked to field name "paramFile"
 * --|---- **IMPORTANT**: Maybe, as a good design, always add file-hash with all files being uploaded. This could be a useful check - probably redundant and 
 * .. unnecessary though
 * --|---- **VERY VERY VERY IMPORTANT**: Look at javadoc for MultipartFilter and MultipartResolver. Realize the following:
 * --|----|---- If you want to "enforce" criteria that file-hash is always given for any file, the code can be put in custom multipart-resolver, but best to
 * .. add it in a filter that runs after MultipartFilter, but before controller-method is invoked. The reason to add this code in filter and not in validator
 * .. is because this'll be a "cross-parameter validation" since files and hashes will be in different multipart-parameter, so their common parent object is
 * .. the request object, and so this check should be in filter
 * --|----|---- This is mentioned in SpringSecurity docs regarding MultipartFilter :: "Specifying the MultipartFilter before the Spring Security filter means that 
 * .. there is no authorization for invoking the MultipartFilter which means anyone can place temporary files on your server" This means that MultipartFilter
 * .. job is to read multipart body and make temp files - if provided in multipart
 * --|----|---- IN ADDITION TO ABOVE: Maybe, you want to allow user to send file-chunks and the service does the work of joining them. Not sure how it'll
 * .. speed things up, since same request channel is being used. In this case, you'll need to modify the MultipartResolver so that it reads the chunks,
 * .. maybe also validates with the chunk-hash, and then combine it form big file which is then attached to multipart's multi-value-map. At this point,
 * .. maybe, you also want to check the file hash of combined file, or maybe do virus scan on it, or maybe remove part-chunks from multi-value-map
 * v.2) With the same "paramFile" key in multipart/form-data, you can send 2 files, or use same key twice. And then in Sprig request, use:
 * @RequestParam("paramFile") Multipart[] fileArr -- Thus, being able to send multiple files
 * v.3) Can also use a @ModelAttribute to change multipart/form-data to a class object!
 * v.4) **IMPORTANT**: It is possible to configure max multipart size file uploaded and max request size! See http://blog.netgloo.com/2015/02/08/spring-boot-file-upload-with-ajax/
 * v.5) **VERY VERY IMPORTANT**: using @RequestPart vs @RequestParam when dealing with multipart data: See section 16.10.5 of https://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/htmlsingle/spring-framework-reference.html#mvc-multipart-forms-non-browsers
 * .. The point is that using "multipart/mixed" (not "multipart/form-data"), one can send files and json. In this case we want the json to become
 * .. an object when it comes inside Spring. This is done via @RequestPart, but not by @RequestParam. Also, using @RequestPart instead of @RequestParam
 * .. in a multipart request body does no harm!!
 * v.6) ** VERY VERY IMPORTANT**: "RETURNING" a multipart/form-data: Return MultiValueMap<String, Object> object (an implementation is LinkedMultiValueMap<String, Object>)
 * .. If one of the values is made a FileSystemResource(..), then that file is sent; And if one is made an object, then a corresponding json is sent
 * .. In spring, this serialization to multipart is done via "FormHttpMessageConverter" (See example in its javadoc)
 * .. NOTE: If you need the object to returned, say as xml, and not json.. then will need to, maybe write a new FormHttpMessageConverter such that
 * .. when it invokes converter for objects, it uses xml and not json! (say, using AllEncompassingFormHttpMessageConverter()). Recall that json
 * .. conversion itself is based on MappingJackson2HttpMessageConverter -- changing it will globally change json parsing!!
 * .. NOTE: Above behavior of returning mixed json and form and file should ideally be "multipart/mixed"; Not "multipart/form-data". BUT, spring
 * .. is unable to work if user gives accepts of "Multipart/mixed". It is likely that "FormHttpMessageConverter" works only with multipart/form-data
 * .. and not with multipart/mixed. So keep that in mind!! ..and maybe that can also affect when reading data. BUT: multipart/form-data should work!!
 * .. IMPORTANT: Note that for multipart, the full set of response that can be made is MultiValueMap<String, HttpEntity<?>> (See https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#rest-template-multipart)
 * .. However, if one just creates a multi-value-map of objects, then Spring adds proper headers accordingly.. but it can be changed!
 * v.7) -- Modifying Multipart config in spring boot : See https://spring.io/guides/gs/uploading-files/  
 * .. Essentially Spring boot provides help with Multipart.. but if you want to configure it such that new setting is available to every 
 * .. servlet made, then the best option is to define own @Bean of return type "MultipartConfigElement". It is also possible to change 
 * .. Spring boot properties to control behavior of multi-part. Can also define a bean of type "MultipartResolver" to control the resolver. 
 * .. See https://stackoverflow.com/questions/25699727/multipart-file-upload-spring-boot
 * v.8) **IMPORTANT**: See javadoc for Spring's "MultipartFilter". Briefly: This is a filter that resolves multipart requests via a MultipartResolver. 
 * .. First, it looks up the MultipartResolver in Spring's root web application context. If no MultipartResolver bean is found, this filter falls 
 * .. back to a default MultipartResolver, i.e. StandardServletMultipartResolver for Servlet 3.0, based on a multipart-config section in web.xml.
 * .. So, in Spring boot it is not needed to have it explicitly. ****HOWEVER**** its use come when using Spring security's csrf-prevention.
 * .. Essentially, we need to ensure that multipartFilter is placed before Spring security filter, else will need to pass csrf in url (which is 
 * .. unsafe). See https://docs.spring.io/spring-security/site/docs/4.2.x/reference/html/csrf.html#csrf-multipart  -- this is when using form-upload
 * .. so cannot use jQuery to add csrf-header. OTHER OPTION.. Use x-auth-token, but on UI side save it in localStorage!
 * v.9) See https://spring.io/blog/2013/05/11/content-negotiation-using-spring-mvc -- Essentially configureContentNegotiation() method in WebMvcConfigurer
 * .. can be used to configure default response mediaType, or even make mapping with custom mediaType names! Also, it should be configured for security!
 * v.10) See OtherDetailsController on how Multipart-type request/response (i.e. using @RequestPart for as method param, and MultiValueMap<> for 
 * .. response) naturally fits into one of the requirement, wherein, there can be indepedent modes to upload data (like sending json vs sending file),
 * .. and user is free to select any mode, but the corresponding service parses data and fills value in all mode.
 *
 * **REALIZE DIFFERENCE between MULTIPART and FORM-URL_ENCODED" Data**: Multipart gives a request-body in "multiple-parts", separated by a separator.
 * .. On other hand, Form-data that is url-encoded, looks like: "param1=value1&param2=value2". There is no multiple-data-parts. This is just like set of queryParams
 * .. that might have otherwise come in url. It is sent in requestBody to help with security (like POST request, say, for login process)
 *
 * vi) **VERY VERY VERY IMPORTANT**: [RELATED TO MULTIPART] How to have @Transactional between file and DB operation
 * vi.1) REALIZE that in an ideal scenario, all the different operations are either done in same DB, or are JTA compatible. However, sometimes that is not possible,
 * .. or not chosen because it affects efficiency. For an alternate design that could be used when JTA cannot be used, see "Why the need for JTA and XA-transactions?"
 * .. in JTA-controller, and the Transactional-Inbox/Outbox pattern (also mentioned in notes there)
 * vi.2) In this case, however, a more-streamed design along similar design-lines of Transactional Inbox/Outbox could be used. First, start with all file operations, 
 * .. saving them in directory, etc. Only when all are successfully done, do you start a Transactional DB operation where the saved file-names are put in DB record.
 * .. Now, whenever a file is needed, you first query DB tables to see if a file-information is present, and then use the corresponding path stored alongside the file
 * .. to retrieve it. DO NOT try to directly access file. 
 * --|---- When file deletion is needed, start by deleting file metadata and putting an entry in "file-delete" table to invoke file deletion async-ly. Since the metadata is
 * .. gone, so the file cannot be accessed by application, since metadata always "gates" and controls such operation
 * --|---- When you need to update a file, you store the user provided file **with a different name**, and then update the metadata in DB to point to new location 
 * .. for same file. 
 * --|---- A few more **IMPORTANT** design points to note that are happening: 
 * --|----|---- REALIZE THAT for this to work, it is best if all incomin files are stored with some uuid name. And the link between the file and its correct name is 
 * .. made in DB-table. This way, one need not worry about name collision, etc. This is what got the CMS design down
 * vi.3) **IMPORTANT**: So what patterns are used?
 * --|---- TRANSACTIONAL OUTBOX: When trying to delete file, instead of doing it immediately and getting susceptible to not be able to undo the operation if a failure
 * .. is encountered, the delete operation is instead queued!
 * --|---- TRANSACTIONAL INBOX: This is not quite directly used - because if file copy on server fails, then user is immediately told to attempt again. Thus, the burden
 * .. of retrial goes on user, rather than making an inbox which is read by an asynchronous processor that needs to retry. **HOWEVER**, one can think of a better
 * .. execution wherein a table is made to add entry of file that is about to be added. Now, if file addition is successful, then during the metadata-creation stage, it is
 * .. also required to delete entries from this "file-added" table. This way, we can ensure that there will never be a case of orphaned files using disk space
 * --|---- MICROSERVICE SINGLE-DB PATTERN: Realize that a good micro-service structure is only possible when it accesses only one DB. When wanting to access
 * .. multiple storage locations, it is best to treat them as 2 separate micro-service. Now, one makes calls to their own DB, and the DAO-method-logic should be 
 * .. such that it accesses the 2nd DB and returns the data. This mixes in CQRS pattern. (See #10 below for another way in which CQRS is done. CQRS is also
 * .. mentioned in JpaController)
 * --|----|---- **IMPORTANT**: Realize that by sticking to just ONE-DB-ACCESS design in microservice, you can now: (i) Centralize file storage throughout the 
 * .. enterprise, (ii) Since file access (by micro-service, from centralized-storage) is hidden by second API (made available to micro-service by team that manages
 * .. storage), the API could be enhanced independently to, say, allow multiple type of storage options, like, from disk, from remote, etc. To the end user, this is 
 * .. not something they need to worry about, and is easily extensible. REALIZE that this was another down point in CMS design.
 * --|----|---- REALIZE that single-DB pattern, by name, means that it does not allow for having JTA. In this regard, also see notes in JtaController
 *
 * vii) Note that there is a @JsonAnySetter annotation that can be used to collect any unlisted property (See https://www.baeldung.com/jackson-annotations)
 * .. Maybe a map-valued variable can be added in base class with this annotation, and then a warning-log can be made whenever this field is non-empty. This
 * .. can help identify if attacks are being made, or, maybe if a customer is just confused on usage and giving unnecessray inputs. 
 * 
 * 
 * 
 * #10) Asynchronous processing:
 * i) See https://spring.io/guides/gs/async-method/  -- good example. Essentially, configuration and use is simple. The "/getAsync6" below does so
 *
 * ii) Spring provides: "ThreadPoolTaskExecutor" object -- which does not throw exception if queue is full.. instead it blocks. 
 * .. To return result, use Java's completableFuture (which also have .complete(..)) method to convert Sync-to-Async results like 
 * .. javascript's Promise.resolve(..). Spring also provides a AsyncResult<>(..) object to return asynchronous result
 *
 * iii) **VERY VERY VERY IMPORTANT**: A GOOD DESIGN PRACTICE: Start first by noticing how Spring-security provides static SecurityContextHolder 
 * .. class that works on a thread-local "Authentication" object. 
 * --|---- Taking this idea further, it seems a good general practice to store any other variable(s) which are coming from external in a thread-local 
 * .. object that is bound to request (..AND, VERY IMPORTANT.. to clear the thread-local after request is done). This can be implemented in an elegant
 * .. manner, wherein methods just call a static utility and expect to be provided the value. HOWEVER, in the backdrop, the static utility is making
 * .. external call and then storing the result in thread-local so that it can be reused later without making another call. 
 * --|----|---- A way to implement above, specially in web setting, is to simply store the values in the Request-attribute. An advantage of this way
 * .. is that there won't be a need to clear threadLocal when done
 * --|----|---- An even better way could be to have a map-valued attribute in the request, and then store these values there. 
 * --|----|---- Further improvement can be done by starting some map-keys with, say, an underscore. This would signal in the log that these should not 
 * .. be dumped -- because they are really internal. Or, if they need to be dumped, then they should be encrypted first
 * --|----|---- Also lastModUser and lastModTime could be stored in the map.. this way, same value of lastModTime will apply to all audit entries for DB update
 * --|----|---- ADVANTAGES OF USING THIS PATTERN: 
 * --|----|----|---- (1) The map can then be dumped during when making log of a failure. This way, all the critical values that were made in processing
 * .. chain and which would be good to log for debug purpose, could be accessed at single point. This also decouples the need to keep on passing parameter 
 * .. in functions just for logging purpose, or to make bulky log statements just to capture process parameters. And this also fits in the framework of 
 * .. aspect-logging or handler-logging, as mentioned in Point#5
 * --|----|----|---- (2) Having a singular map with all necessary processing parameters is useful if we want to do async-processing. While spring-security 
 * .. provides a wrapper class that transfers the security context, this is a more general way to proceed. A related design could be to make a "HandOff"
 * .. object used in async processing where these values are stored separately than data object as handoffs are done for async processing. NOTICE how this
 * .. model comes very close to what happens in http/jms processing where the headers are separate than request-body, but they are still an integral part
 * .. of overall responce
 * --|---- where to use it? -- One example could be in case of domain-aggregates spread across different places due to legacy design. For example: in 
 * .. PatentCenter, the registrationNumber and customerNumber were separate. Application of this design means that within authenticated token, a "field"
 * .. is added to store registration number.. such that when its getter-method is called for first time, then it queries other repositories to get the 
 * .. value and then stores/caches it there. Thus, even though the backend is separate due to legacy design, the outside world will always see it as a 
 * .. coherent single unit! 
 * --|----|---- The same can also be done when querying data from DB by modifying behavior of Spring-repo methods [[This relates to comment made 
 * .. in JpaController]]. Either that logic can be Entity-class dependent/independent. Or, it can involve looking at annotations/aspects! The point
 * .. is that if a call can be done async in beginning - then do it! Even better, if it is made to look like a sync call from outside, but does 
 * .. async call and caching on back side!!
 * --|---- REMEMBER to not get carried away too much and start launching async calls to get data pre-emptively. Remember the YAGNI antipattern - You
 * .. Aint Gonna Need it!
 * --|---- **VERY VERY IMPORTANT**: Just for completeness of discussion, it is necessary to mention that: You need to add a filter/advice that empties 
 * .. out the threadLocal at start and at end of request. If not, then it can cause security leakage.. allowing next run of thread to latch on to the old
 * .. value (See SpringSecurity pdf doc, Pg.105, section 15.3, on SecurityContextPersistenceFilter: It says "Clearing the ThreadLocal in which the context
 * .. is stored is essential, as it might otherwise be possible for a thread to be replaced into the servlet container’s thread pool, with the security
 * .. context for a particular user still attached. This thread might then be used at a later stage, performing operations with the wrong credentials."). In terms
 * .. of implementation, this also means that another best-possible alternative, if not using filter, is to introduce threadLocal via an "around"-aspect ONLY. This
 * .. way it can be better and consistently controlled
 * --|----|---- On same topic, See Point#5 in https://www.baeldung.com/java-threadlocal -- A threadLocal cannot guarantee in ExecutorService that same  
 * .. thread will execute operations on an object (containing static threadLocal)
 * --|---- Regarding storage in threadlocal.. ALSO REALIZE that..
 * --|----|---- This works with basic coding only if the value to be set does not change very often
 * --|----|---- This works when we are looking at sequential code-execution in thread without huge parallelism (..because queries data is ThreadLocal!)
 * --|----|---- This may not work properly if we are talking of DB-like-scenario where we need transaction and/or can have lazy loading which can 
 * .. cause failures due to missing session
 *
 * iv)  **VERY VERY IMPORTANT**: As shown above.. when going in microservice domain, it may be possible that data in a domain-aggregate is coming 
 * .. from different sources - this is an example of CQRS pattern. Here, a complex object is made by collecting data from various source. HOWEVER,
 * .. it is really really important to be clear on how to compose the full from its pieces! --BECAUSE-- it gets even more messy when making updates.
 * .. Such a pattern would require that service takes care of updating each component, and then update its own view -- but maybe all components don't
 * .. update at same time, in which case: (a) it would be calling service's responsibility to retry; (b) overall logic in all services should be to 
 * .. have some optimistic-lock to prevent double changing of data; (c) it will be service's responsibility to update its aggregate data; (d) MOST
 * .. IMPORTANT: Within the query side itself, there should be logic to account for security.. for example, search for "DB-related data security" in
 * .. JpaController, and check the example there. Some fields are null because it is disallowed by security. In such cases, the aggregating service
 * .. should itself prevent the user from being able to change it.. and thus, pass burden of updates on the service -- which of-course won't be able
 * .. to do so because of security concern! In this case, the orchestrating servie, shouldm't allow user to set new value! [[NOTE: For notes on CQRS,
 * .. best, see https://microservices.io/patterns/data/cqrs.html  For another article, see https://martinfowler.com/bliki/CQRS.html]]
 * 
 * 
 * 
 * 
 * 
 * 
 * Related #1:
 * ***VERY VERY IMPORTANT***: See ExceptionLogLevel.java for example of enum describing a custom-attribute and custom-method-implementation with all enumerations!! 
 * This can be taken as proxy for delegate-pattern
 * See https://stackoverflow.com/questions/7413872/can-an-enum-have-abstract-methods
 * 
 *
 *
 * Related #2: PropertyEditor, Converter (can also have converter factory), MessageConverter, HandlerMethodArgumentResolver and Formatter. 
 *
 * 2.0) HandlerMethodArgumentResolver : There are quick and easy documentations already available. Just remember to use it for complex logic.. and maybe 
 * .. do your own validation as part of it. However, that would complicate the ability to also use method-validation on it! As mentioned earlier, for
 * .. guidance, look at code of "ModelAttributeMethodProcessor" and main-comments in #(3.v) above
 *
 * 2.1) Property-Editor:
 * ---- Reference-doc: https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-beans
 * ---- It is subtle and mentioned in few words at few places on how PropertyEditors are different. It is used when you want to (a) read form-data 
 * .. and put it in bean, (b) combine request-params into an object. It does not come up when it is needed to read a "json-message-body". Why do they come 
 * .. in picture? Because when spring receives, say, a form-data, then it needs to make a javaBean. Spring starts by creating blank object using default
 * .. constructor, then wraps it with BeanWrapper, which then sets the values. At that point, the values can be changed using propertyEditor, before they 
 * .. are fed to bean.
 * ----|---- "To customize REQUEST PARAMETER DATA BINDING, we can use @InitBinder annotated methods within our controller." -- See https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/spring-custom-property-editor.html
 * ----|---- "In order to have Spring collect and BIND THE DATA FROM AN HTML FORM PAGE (OR QUERY STRING PARAMETER DATA)..." -- See https://www.intertech.com/Blog/spring-frameworks-webdatabinder/
 * ---- Realize that the word "property" referred to here are those from JavaBeans; And so, the "PropertyEditor" is also from JDK.
 * ----|---- **VERY VERY IMPORTANT**: For this reason, propertyEditors are thread unsafe!!
 * .. See https://github.com/spring-projects/spring-framework/issues/17150 and https://github.com/openjdk-mirror/jdk7u-jdk/blob/master/src/share/classes/java/beans/PropertyEditorSupport.java)
 * .. NOTE from java-code that it has "value" as one of its member
 * .. AND FOR THAT REASON, when configuring propertyEditor to Web-binder (during @InitBinder or otherwise), ALWAYS associate a new object 
 * .. and do not reuse same editor (See http://forum.spring.io/forum/spring-projects/web/22959-databinder-propertyeditor-and-thread-safety)
 * ---- Compared to Converters, there is just one class to implement for PropertyEditors to both read and write object
 * ---- **IMPORTANT** The better advantage of editors is that it is possible to associate different editors corresponding to same target-class-type 
 * .. but to different fields (see at end of https://www.intertech.com/Blog/spring-frameworks-webdatabinder/)
 * ----|---- HOWEVER, NOTE that propertyEditors cannot be bound via any method in WebMvcConfig -- which should be.. because they are not thread-safe,
 * .. and so, doing a one-time binding would be a thread-unsafe practice.
 * 
 * 2.2) Converter
 * ---- Reference doc: https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#core-convert
 * ----|---- Realize that Spring has "TypeConverter" which is different from "Converter" (they are in different package, plus former is closer 
 * .. to PropertyEditor).. so it may get confusing!
 * ----|---- **VERY VERY IMPORTANT**: Looking in referene doc, there are different interfaces mentioned, like, Converter, ConverterFactory, 
 * .. ConvesionService and ConditionalConverter. While all seem to be very similar, since the DataBinder can only take "Converter" implementations,
 * .. so this greatly restricts ability to use "annotation based conditionality/control". [[DIGRESSION: related also is the concept of 
 ( .. "FormatterRegistry" which is discussed later]]
 * ---- Since there is no Java interface to back to, the converters are stateless as long as they are made so. This is in contrast to propertyEditors 
 * .. which are definitely not thread-safe!
 * ---- PropertyEditors came in overall-picture due to bean-wrapping of request/form-params which happens at time of communicating with UI. However,
 * .. converters don't have any such restrictions and can be used anywhere within the code, and/or in aspects, etc. For this reason:
 * ----|---- Converters can be defined between any pair of Java-object type, and not-necessarily from/to String conversion. However, this does increase
 * .. class count by two-fold, compared to propertyEditors, if the intent is indeed to change from/to-String
 * ----|---- Web-binders DO NOT ALLOW having different converters for different field of same type. But this is allowed for propertyEditors.
 * ----|---- ALL-IN-ALL, if you can work with property-editors, do so. Working with converters may be extra work!
 * ---- ONE PLACE WHERE CONVERTERS SHINE.. they "converters" can be used to change the string-value from properties file into custom object type. 
 * .. See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-conversion
 * ---- Example of spring converter: (this also has converterFactory) https://www.baeldung.com/spring-type-conversions and https://www.petrikainulainen.net/programming/spring-framework/spring-from-the-trenches-using-type-converters-with-spring-mvc/
 *
 * 2.3) **IMPORTANT** Formatters
 * ---- Their goal is the "format" data to suit a given context. Thus, the most important aspect about formatters is that they allow locale dependent 
 * .. conversion, which is not directly allowed by converter or by property-editors. [[It is still possible to do so indirectly by using static 
 * .. utilities to get handle of request, and of local from there]]
 * ---- Like converters, since they are not constrained by some back java-interface, so, they can be made and used as thread-safe
 * ---- [[IMPORTANT DIGRESSION]] It may seem natural that a related concept to Formatters, is of "FormatterRegistry", specially given the naming of 
 * .. latter. However, this is one more place where it becomes confusing (like, previously, TypeConverters are not Converters; And genericConverter 
 * .. is not Converter!). 
 * ----|---- Looking at API, FormatterRegistry extends ConversionService! That is why, in the FormatterRegistry in WebMvcConfig, you can register 
 * .. both formatters and converters!
 * ----|---- **VERY VERY IMPORTANT**: One of the best part about it is that they allow registration of "AnnotationFormatterFactory". By name, this seems
 * .. like a formatter, but, unfortunately does not extend the Formatter interface. This allows for annotation-based conversion. [[Example usage is 
 * .. described below in Section 2.5]]
 * 
 * 2.4) MessageBodyConverter: 
 * ---- Can make custom Json Ser/Deser-ializers; and activate using @JsonSerialize / @JsonDeserialize(using=...); as done for TestDTO. 
 * ----|---- **IMPORTANT**: See javadoc for @JsonSerialize : there are various other serialization/deserialization customization options, including for collections
 * .. map-key/map-value
 * ----|---- See #9.vii - use of @JsonAnySetter and @JsonAnyGetter to collect json nodes in a map. 
 * ---- **VERY VERY IMPORTANT**: Repeating because of importance.. As said, Custom deserialization doesn't work on @ModelAttribute; 
 * .. But it would work with @RequestBody!! AND, this is reason for difference between JsonSerializer (that work on request-body) vs argumentResolvers, 
 * .. converters, formatters, etc. that work on QueryParam, pathParam, etc!
 * ---- This is different from "PropertyEditor" because now there is converter for JSON body message, which is different from property-editing 
 * .. based on BeanWrapper, which is used for request/form-params
 * ---- NOTE.. VERY VERY IMPORTANT: In case a serializer/deserialize is expected for all instances of an object, then:
 * ----|---- best use @JsonComponent, while also leveraging Spring-wiring (see in ReqRespController, and also in 
 * .. https://www.baeldung.com/spring-boot-jsoncomponent   https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-json-components
 * ---|---- Or, traditionally, register a "Module" Bean. See code for "StringUpperSerializationModule" AND also spring-boot docs in above link
 * ---- **VERY VERY IMPORTANT**: Note that it is possible to use a no-arg constructor for json-deserialization! By using @JsonCreator and @JsonProperty,
 * .. See https://stackoverflow.com/questions/21920367/why-when-a-constructor-is-annotated-with-jsoncreator-its-arguments-must-be-ann
 * ----|---- Cons (of using no-arg constructor with annotations):
 * ----|----|---- It will look clumsy for objects with multiple fields
 * ----|----|---- Anytime you want to make object in codebase, you'll have to go via constructor
 * ----|----|---- The logic for object creation and object population is coupled
 * ----|---- Pros (of using no-arg constructor with annotations):
 * ----|----|---- You can add static utilities. Maybe have one of the arguments as "version" and set fields to different values based on it (Bloch's java book)
 * ----|----|---- Things can get complex, since the logic of "object creation" gets interwined with values that are put in it
 * ----|----|---- There cannot be a case of non-valid object created (Bloch's java book -- an object will only be created after all checks are done). 
 * ----|----|----|---- HOWEVER, a counter to that is.. how often do we need such strong logic in a web-enviornment. Plus, it makes object closed for 
 * .. future modifications.
 * ---- NICE PROBLEM: What if we have a class being used for message body, but we want to automatically-control the serialization of a String member 
 * .. of that class based on what annotation was added to the member.
 * ----|---- SOLUTION: One can make custom serializer/deserializer using @JsonComponent. Start by getting default serialization result, then read the
 * .. annotation on class-member, and uppercase/trim as needed!
 * ---- **IMPORTANT.. NOTE..**: A good condition to add in responseBodyAdvice could be to nullify list/array type fields with empty list! So, when they
 * .. are de-serialized, the default is set to emptyList. But when they are serialized, it is turned back to null. The advantage of nullifying is that
 * .. objectMapper could be configured to not show them at all if they are null. That won't be the case if they were empty! This also plays nicely
 * .. with idea of internationalization as discussed in Hateoas section where description of json keys arealso sent
 *
 * 2.5) **VERY VERY VERY IMPORTANT**: 2 nice advanced-level problems
 * ---- PROBLEM DISCUSSION#1: Say we have one endpoint handled by method accepting String type argument. What if want the user provided 
 * .. input to be trimmed, uppercased before it is fed to this method.. but this should be done selectively, i.e. not all String inputs everywhere
 * .. in code be uppercased; but only where the annotation is
 * ----|---- SOLUTION [BEST]: Via WebMvcConfig, it is possible to register a "AnnotationFormatterFactory" that can change from String to String, but can
 * .. also trim/uppercase String before passing it over. Now, when you pass a request/path param, it will get changed based on whether the annotation is 
 * .. present or not. DO NOTE: 
 * ----|----|---- (a) This behavior is overriden if @InitBinder is defined (a.i) with custom propertyEditor registered for that data-type 
 * .. and not for a particular field-name, or, (a.ii) with custom propertyEditor registered for that data-type and that field-name [Meaning, that 
 * .. propertyEditors take higher preference than formatters; And fieldname-specific propertyEditors take higher preference that editors based only on
 * .. field type] 
 * ----|----|---- (b) This conversion also applied for fields inside the ModelAttribute object. So, when making annotation, allow it to have both fields
 * .. and parameters as target
 * ----|---- OTHER SOLUTION: Either make a custom class, like, AnnotatedString.class, and then make a custom HandlerMethodArgumentResolver which requires
 * .. that the corresponding "MethodArgument" have a @RequestParam annotation, and then it also reads other annotation and decides how to resolve it. 
 * .. Can also try doing same but making a custom resolver for String type. Either way, it is not as good solution because (a) using 
 * .. HandlerMethodArgumentResolver is generally for complex situations, (b) the logic to invoke validator will need to be coded by user, which will 
 * .. further get complicated since user can have it as method-argument or model-attribute-object-field, and both go through different validation.. 
 * .. All in all, much more messy and restrictive!
 * ---- PROBLEM DISCUSSION#2: In #2.4 above, it is discussed on how custom @JsonComponent could be used to change field value based on annotation made
 * .. on field in the class definition. WHAT IF we need to change the field or class value based on annotation made at method-argument level, i.e. where
 * .. the @RequestBody annotation is made in controller-class
 * ----|---- SOLUTION: Unlike above, we cannot make a new type. INSTEAD, IN THIS CASE, the easier thing to do would be to make a bean that extends
 * ..  "RequestBodyAdviceAdapter". HOWEVER, using RequestBodyAdvice is tricky in that registering it is more involved. Instead of  adding it in 
 * .. WebMvcConfigurer, we need to extend "WebMvcConfigurationSupport", wherein, override the requestMappingHandlerAdapter, then, first get the one made 
 * .. by super(), then register your newly made requestBodyAdvice! See https://deventh.blogspot.com/2017/07/spring-mvc-requestbodyadvice-or-how-to.html
 * ----|----|---- **VERY VERY VERY IMPORTANT** : NOTE that with RequestBodyAdvice -- we can implement a scenario where same class gets used with partial
 * .. inputs by different methods, maybe, identified by value of a RequestParam. For example, in a PATCH call where different update-type operations 
 * .. require different portions of class to be non-empty, while all others should be null. The json-deserializer won't be able to handle such dynamic
 * .. requirements. ALSO.. one can use the request-body-advice to fill certain fields of the object (read from request-body), with values from 
 * .. request-params or headers. This can further be useful when doing custom validation!! 
 * ----|----|---- **QUESTION**: Do validations get invoked before/after the request-body-advice is applied?! All above would fail if the advice gets applied
 * .. after the validations! 
 * ----|----|---- IMPORTANT and NECESSARY uses of RequestBodyAdvice.. see in OtherDetailsController
 * 
 * 
 *
 * Related #3 (**VERY IMPORTANT** many points):
 * 3.1) **IMPORTANT**: Notice that BaseDTO annotations of @JsonProperty show how to have JSON-serialization only and not deserialization! 
 * .. Reference, see https://stackoverflow.com/questions/12505141/only-using-jsonignore-during-serialization-but-not-deserialization
 * .. ALSO SEE: https://stackoverflow.com/questions/37697359/jsonpropertyaccess-jsonproperty-access-write-only-not-working
 *
 * 3.2) **VERY VERY IMPORTANT** See the use of "RequestContextHolder" in "TimeAttributeAdderAspect" -- this is a static utility 
 * .. through which request can be accessed anywhere! See https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/context/request/RequestContextHolder.html
 * .. or, https://stackoverflow.com/questions/6300812/get-the-servlet-request-object-in-a-pojo-class
 *
 * 3.3) **IMPORTANT**: Also note in "TimeAttributeAdderAspect" -- a way to get method-annotations from joinpoint. Also see: https://stackoverflow.com/questions/6604428/get-annotated-parameters-inside-a-pointcut
 * 
 * 3.4) **IMPORTANT**: Notice controlling serialization to not include null values. See https://stackoverflow.com/questions/30042507/for-spring-boot-1-2-3-how-to-set-ignore-null-value-in-json-serialization
 * --|---- **VERY VERY VERY IMPORTANT**: A good strategy would be to add a requestBodyAdvice that assigns empty-list to deserialized collection objects.
 * .. The easiest way to ensure this would be to add empty list/set in default constructor of DTO. HOWEVER, it is also more important to also add a 
 * .. responseBodyAdvice that converts all empty collection to null, and also converts all objects that have all their non-transient fields as null,
 * .. back to null (maybe require all DTO to define isEmpty() method), and do this recursively. This, along with non-null inclusion, ensures that the client
 * .. always gets the minimal representation of fields back from service call. THIS IS USEFUL, say, when we add a new field which should always remain empty
 * .. or null for some client. If we now returned this field back to client, it may break their services!!
 * 
 * 3.5) **VERY IMPORTANT**: Notice the 2 annotations TimeAttributeAdder and TimeAttributeAdderController. The perf-time logic should ideally 
 * .. use aspect on @TimeAttributeAdder annotation. However, this requires giving start and end name always.. which we know are fixed for 
 * .. controller. So, ideally we want something like annotation-inheritance -- but that is not allowed in Java! 
 * .. **IMPORTANT** As workaround to annotation inheritance, (1) @TimeAttributeAdderController is made - that takes @TimeAttributeAdder 
 * .. with fixed values so that it need not be repeated. (2) In trying to get annotation value in "TimeAttributeAdderAspect", 
 * .. use Spring's AnnotationUtils that can check annotation on annotations! (This is how @Transactional can be specialized!); And, (3) in making
 * .. poincut, include both @TimeAttributeAdder and @TimeAttributeAdderController - because pointcut identification logic checks annotation
 * .. and not annotation on annotation! Reference: See https://stackoverflow.com/questions/7761513/is-there-something-like-annotation-inheritance-in-java/18585833#18585833
 * .. ALSO note, way to "or" two pointcut criteria in one!
 * 
 * 3.6) **VERY IMPORTANT**: Note that the code logic can be seen in 2 ways.. (i) have filter and aspect always add the time-values; And then have
 * .. the perf-time-handler identify whether it should be put in response object. The other way to think is to check some criteria to identify
 * .. if the timing should even be stored in request on not. BUT.. the latter proposal becomes very muddied, particularly when thinking about 
 * .. how to control time logging done in first filter! So, from point of view of separation of code-logic, the code architecture used here
 * .. IS A BETTER CHOICE!!
 * .. ON SIMILAR LINES, note how corresponding attribute-names are defined inside the BaseDTO and its static-inner classes
 * .. so that the ownership of the constants are at correct place!
 * 
 * 3.7) **IMPORTANT**: Note that BaseTimeHandler should be last.. this way it can also add a time there! So BaseUserStoryHandler should have
 * .. priority 1 less!
 * 
 * 3.8) Just for information: #1)@EnableAspectJAutoProxy on a @Configuration class enabled aspectj-autoproxy setting in Spring. It has annotation
 * .. properties so that proxy-mode is always class and not inheritance (See Pg27 pg Spring-Mvc-pdf-doc.. and that if a controller inherits an
 * .. interface, proxying automatically becomes Java based and can stop working; Either use @Scope() on controller making proxy as class-based,
 * .. or configure @EnableAspectJAutoProxy to globally use class only; #2) Why the need to have both @Aspect and @Component on a spring aspect?
 * .. "With the aspectj support enabled, any bean defined in your application context with a class that is an AspectJ aspect (i.e., has the @Aspect 
 * .. annotation) will be automatically detected by Spring and used to configure Spring AOP!!" -- This is why @Aspect is needed. The @Component is
 * .. needed to make the class as a bean. See https://docs.spring.io/spring/docs/4.3.15.RELEASE/spring-framework-reference/html/aop.html
 * 
 * 
 * 
 * Related #4 (**VERY IMPORTANT** many points):
 * 4.1) **VERY VERY IMPORTANT**: Design consideration: Referencing to (#9.i.2).. That is also reason to NOT return a list object directly
 * .. from a controller.. because when returning as xml, the outermost tag name will be uncontrollable!!
 * .. EVEN MORE, **that will break Base Hateoas and perf-time aspect!! -- even if returned format is JSON; **And it becomes difficult to 
 * .. put a custom httpMessageConverter on it!
 * .. EVEN MORE, when creating swagger documentation, it may cause uncontrollable listing of internal properties; Like, when showing 
 * .. FileSystemresource in swagger
 * 4.2) **VERY VERY IMPORTANT**: 
 * ---- i) It seems that Spring MVC identifies the message body reader/writer in 2 steps: First it looks at produces 
 * .. in @RequestMapping, and then it looks in its default setting for xml, json. When I made a custom csv writer, but did not include "text/csv"
 * .. in produces, then it errored. When I included it, it worked. But then, other accept type stopped working!
 * ---- ii) If nothing is included in process, application/json and text/xml still worked. 
 * ---- iii) ***FINAL POINT being.. if you are adding custom message converter, do include it in @Produces if it applies
 * ---- iv) In case of consumes, it seems the Spring strictly requires all consumption types to be listed if at least one type is explicitly put
 * .. Thus, if writing "text/csv", then need to mention json, xml also explicitly. If none are mentioned, then Spring automatically parses json, xml
 * .. **UPDATE**: Note that when registering converters.. Spring docs suggest to call separate method to ensure that default converter
 * .. registration is not undone. I did not do so.. maybe that also affects above observation. Alternately, at end, call: super.addDefaultHttpMessageConverters();
 * 
 *
 *
 * Related #5: Spring-Internationalization!
 * (1) When dealing with dispatcher servlet and processes thereafter: See https://www.baeldung.com/spring-boot-internationalization    
 * .. It involves: (a) a bean of type LocalResolver, whose job is to determine locale from incoming request based on a particular strategy. 
 * .. Now, this may set Locale for the very first request, and so it is needed to add an interceptor so that if there is a locale change in 
 * .. between, then that is also picked up
 * (2) **VERY IMPORTANT**: ABOVE does NOT work when trying to internationalize messages in filters (..and Spring Security) because these come 
 * .. before dispatcher-servlet is hit, and so LocaleContext object might not yet be set-up (that is otherwise done by LocaleResolver). 
 * .. See all answers of https://stackoverflow.com/questions/8026320/spring-security-localeresolver   As said in lower answers, one can use 
 * .. RequestContextFilter, but in reality, even that is useless because it does not have a complex logic set up for Locale. SO.. best solution 
 * .. is to write a filter by self, where, you can have a chained/delegated logic for Locale identification, like, start with session, 
 * .. then url-param, then accept-language, then cookie.. and lastly, by looking at ip-address. Furthermore, such filters can be placed 
 * .. at the very beginning of filter-chain so that the locale is set early on
 * --|----BUT WHY TO SET LOCALE EARLY ON??! See below: This helps in ensuring that all messages in application are internationalized; also, 
 * .. now each component (..even a filter) can have a Locale dependent logic, since it will always be set with "fresh" value corresponding 
 * .. to "that" request at the very beginning itself
 * (3) A very good code-design/practice is to externalize even the errror message. This makes it easy to internationalize! See 
 * .. https://blog.codecentric.de/en/2017/08/localization-spring-security-error-messages-spring-boot/  (for old version spring), and example of
 * .. Spring-security internalized messages on https://github.com/spring-projects/spring-security/tree/master/core/src/main/resources/org/springframework/security  
 * .. Also look at github code for Spring security classes to see how the messages are internationalized (Example, see ProviderManager, 
 * .. in how the messageSource is used to get message which is then thrown in exceptions). 
 * --|---- Along these lines, an even better practice would be to always add message in any runtime error thrown in code to always be made 
 * .. using messageSource. This externalizes the error message, allowing internationalization - and even a refactoring free testing. 
 * .. Another good practice would be to always construct the runtime error using an "Error" object. This can (a) **most important**: allow 
 * .. sending a list of error-messages for single processing, (b) allow coder to give hint during error construction - identifying explicitly 
 * .. that which object/ field errored. A dummy "request" object can be made onto which the error is referenced - when the error is actually 
 * .. with the request (like, filter throwing exception). NOTE: ONE DRAWBACK is that inbuilt "error" object does not allow passing different 
 * .. messages to use for error-logging in server, vs sending response to user ..that is something to also keep in mind when designing the system!
 * (4) Can also see: https://blog.codecentric.de/en/2017/08/localization-spring-security-error-messages-spring-boot/       https://auth0.com/blog/exception-handling-and-i18n-on-spring-boots-apis/
 * (5) See main-point#8 above on HATEOAS - it can also be used for internaltionalization of json structure itself
 * 
 *
 *
 * Related #6: Spring RestTemplate and AsyncRestTemplate
 * (1) Notice that when server returns 4xx or 5xx status, the restTemplate throws exception - regardless of whether the returned json message
 * .. could be formatted in the desired class-form or not. SO.. if your code is such that it returns a smaller json (say just message and time
 * .. field), which could still be interpreted as part of bigger json -- then it comes down to your code to catch the exceptions, get json body
 * .. out from it, parse it and return it.
 * --|---- **IMPORTANT**: Keeping this in mind, a good design for json should be to have all DTO(s) also extend a common error-json class. This
 * .. way, at client side, they can use the same class-definition for both success and failure and can have stronger constraint that the return
 * .. type from a call (whether success/failure) should always be the same!! [[Example: as done in PatentCenter with OPSG]]
 * --|----|---- **IMPORTANT**: With above in mind, also change the errorResponse-filter that Spring adds to handle errors from filters. It should 
 * .. return the error-message-DTO as made by you, and not the error-DTO that is configured by default
 *
 * (2) With the microservice architecture, the repository to a service may itself be another service. This is nicely abstracted out by "FeignClient"
 * .. in Spring cloud. Maybe, use this.. along with the practices of being able to modify Spring-Data default repository behavior.. or maybe
 * .. by complementing it - in order to get a collective-backend view (as needed in CQRS) -- See in #10 of the note for more detail
 *
 */
//@formatter: on

/**
 * This class exposes certain services
 * 
 * @author KunjPrasad
 *
 */
// the "tags" is more important, give proper name, and keep it single valued
@Api(value = "Test API", tags = { "testController" })
@RestController
// @Validated
@RequestMapping(value = "/test")
public class ReqRespController {

    static final String DEFAULT_EXTRA_MESSAGE = "DefaultExtraMessage";
    static final int MIN_MSG_SIZE = 2;

    @Autowired
    private TestService testService;

    @Resource(name = "dualMessageDTOValidator")
    private Validator dualMessageDTOValidator;

    @InitBinder("abcd")
    public void dataBinding(WebDataBinder binder) {
        binder.addValidators(dualMessageDTOValidator);
        // VERY VERY IMPORTANT.. DO NOT USE AUTOWIRED property editors! Make new ones - since property editors are
        // thread unsafe.
        // binder.registerCustomEditor(String.class, new StringLowerPropertyEditor());
        binder.registerCustomEditor(String.class, "msg1", new StringUpperPropertyEditor());
    }

    /**
     * Message#1 webservice
     * 
     * @param message
     * @param extraMessage
     * @return
     */
    @ApiOperation(value = "Get message#1", notes = "Use path param and request param to get response")
    @ApiResponses(
            value = { @ApiResponse(code = 200, message = "Successfully retrieved message", response = TestDTO.class),
                    @ApiResponse(code = 400, message = "Path param message size less than " + MIN_MSG_SIZE
                            + " (USXXXXX); Request param message size less than " + MIN_MSG_SIZE + " (USYYYYY); "
                            + "Path param message size greater than 6 (DEQQQQQ)") })
    @Log
    @RequestMapping(value = "/get1/{message}", method = RequestMethod.GET)
    public TestDTO getTestMessage(
            @ApiParam(value = "message prefix taken from path", required = true) @PathVariable("message") @Size(
                    min = MIN_MSG_SIZE,
                    message = "message size is less than " + MIN_MSG_SIZE) @StringMultiplier("3") String message,
            @ApiParam(value = "message suffix taken from query", required = false,
                    defaultValue = DEFAULT_EXTRA_MESSAGE) @RequestParam(value = "extraMsg", required = false,
                            defaultValue = DEFAULT_EXTRA_MESSAGE) @Size(min = MIN_MSG_SIZE,
                                    message = "{test.extraMsg}") String extraMessage) {
        System.out.println("~~~~~" + message);
        // processing
        TestDTO testDTO = testService.getTestMessage(message, extraMessage);
        // return
        return testDTO;
    }

    /**
     * Message#2 webservice
     * 
     * @param dualMsg
     * @param result
     * @return
     */
    @TimeAttributeAdderController
    @UserStory(stories = { "US10000", "US12367", "DE4567" })
    @Log
    @RequestMapping(value = "/get2/{msg1}/{msg2}",
            method = RequestMethod.GET,
            produces = { "text/csv" })
    public TestDTO getTwoTestMessage(@ModelAttribute("abcd") @Valid DualMessageDTO dualMsg, BindingResult result) {
        System.out.println(result);
        // processing
        TestDTO testDTO = testService.getTestMessage(dualMsg.getMsg1(), dualMsg.getMsg2());
        // return
        return testDTO;
    }

    @RequestMapping(value = "/getabcd", method = RequestMethod.POST)
    public TestDTO getAbcdTestMessage(@RequestBody @Valid DualMessageDTO dualMsg, BindingResult result) {
        System.out.println(result);
        System.out.println(dualMsg);
        // processing
        TestDTO testDTO = testService.getTestMessage(dualMsg.getMsg1(), dualMsg.getMsg2());
        // return
        return testDTO;
    }

    /**
     * File#3 webservice
     * 
     * @return
     */
    @RequestMapping(value = "/get3", method = RequestMethod.GET)
    public FileSystemResource getThreeFile(HttpServletResponse resp) {
        File fileOnFs = new File("C:/Users/Kunj/Desktop/Test/Test.txt");
        String fileNameInresponse = "TestResp.txt";

        // VERY VERY IMPORTANT: A better option could be to look at file-extension and then use corresponding type.; And
        // use octet-stream only as a default!!
        resp.setContentType("application/octet-stream");

        resp.setContentLengthLong(fileOnFs.length());
        // resp.setHeader("Content-Disposition", String.format("inline; filename=\"" + fileNameInresponse + "\""));
        // Here we have mentioned it to show as attachment
        resp.setHeader("Content-Disposition", String.format("attachment; filename=\"" + fileNameInresponse + "\""));

        // NOTE: There is something called "ClassPathRespource" - to return file from classpath!!
        return new FileSystemResource(fileOnFs);
    }

    /**
     * File#32 webservice
     * 
     * @return
     */
    @RequestMapping(value = "/get32", method = RequestMethod.GET)
    public StreamingResponseBody getThreeStreamingFile(HttpServletResponse resp) {
        File fileOnFs = new File("C:/Users/Kunj/Desktop/Test/Test.txt");
        String fileNameInresponse = "TestResp.txt";

        // VERY VERY IMPORTANT: A better option could be to look at file-extension and then use corresponding type.; And
        // use octet-stream only as a default!!
        resp.setContentType("application/octet-stream");

        resp.setContentLengthLong(fileOnFs.length());
        // resp.setHeader("Content-Disposition", String.format("inline; filename=\"" + fileNameInresponse + "\""));
        // Here we have mentioned it to show as attachment
        resp.setHeader("Content-Disposition", String.format("attachment; filename=\"" + fileNameInresponse + "\""));

        return new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                FileUtils.copyFile(fileOnFs, outputStream);
            }
        };
    }

    /**
     * Multipart/form-data#4 webservice
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/get4", method = RequestMethod.POST)
    public void getFourMultipartForm(@RequestPart("name") String name, @RequestPart("file") MultipartFile[] file)
            throws IOException {
        File saveFile1 = new File("C:/Users/Kunj/Desktop/" + name);
        FileUtils.copyInputStreamToFile(file[0].getInputStream(), saveFile1);
        File saveFile2 = new File("C:/Users/Kunj/Desktop/bbb" + name);
        FileUtils.copyInputStreamToFile(file[1].getInputStream(), saveFile2);
    }

    /**
     * Multipart/form-data return#5 webservice. Not returning multipart/mixed response
     * 
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/get5", method = RequestMethod.GET)
    public MultiValueMap<String, Object> getFiveMultipartFormResp()
            throws IOException {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<String, Object>();
        form.add("name", "aaa.txt");
        form.add("file", new FileSystemResource(new File("C:/Users/Kunj/Desktop/Test/Test.txt")));
        TestDTO testDTO = new TestDTO();
        testDTO.setMessage("tttttttt");
        form.add("json", testDTO);
        return form;
    }

    @RequestMapping(value = "/getAsync6", method = RequestMethod.GET)
    public TestDTO getAsyncMessage() throws InterruptedException, ExecutionException {
        System.out.println("~~~~ starting in controller");
        CompletableFuture<TestDTO> testDTOAsync = testService.getAsyncTestMessage();
        System.out.println("~~~~ async request sent.. back in controller");
        return testDTOAsync.get();
    }
}
