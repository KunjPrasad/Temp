package com.example.demo.spring.boot.ctrl;

// @formatter:off
/*
* IMPORTANT.. NOTE:
* ---- This file has comments pertaining to general Spring. Spring-Boot specific details should go in EtceteraController2
* ---- The Spring-mvc-doc pdf-file has many comments and notes in it.. Keep it and do not delete it!!
*
*
*
* **IMPORTANT**.. Nice: A concise round-up of entire Spring flow: See https://stackify.com/spring-mvc/
*
*
*
* === CONFIG RELATED COMMENTS ===
* 
* **VERY VERY VERY VERY IMPORTANT**: MANY POSTS/BLOGS/GUIDES do this wrong! So read..
* -- See https://stackoverflow.com/questions/3652090/difference-between-applicationcontext-xml-and-spring-servlet-xml-in-spring-frame
* -- This posts and its ALL answers explain difference between application-context configuration vs spring-servlet configuration.
* -- While this may seem tedious.. it is very very important to know this! (Probably also in context of multi-module maven). 
*
*
* === START ===  
* 
* --|---- To begin:
* --|----|---- Realize that servlets are java classes that accept input and give output (for httpServlet, it is via http). 
* --|----|---- These http-servlet classes can be mapped to different url, meaning, these classes/method are invoked when a url is hit on the server
* --|----|---- Additionally, servlet Their configuration involve aspects like View-mapping, JsonBinding, RequestAdvice, etc. Think of all configurations 
*
* --|---- Now, for a moment, don't think of web-application, but a standalone Java. At this point, Spring can still do lot of dependency 
* injections, etc. Such configurations relate to Spring's application-context (See later of how this mixes with servlet-config)
* 
* --|---- In a full blown web-application, we need servlets (i.e. java classes - based on above simplification) to perform some business logic. 
* .. One way to do so would be to gain access to a common glob of sping-managed-beans that execute a non-trivial business-process. The servlet 
* .. are simply responsible for creating a "bridge" between it and the user. With this understanding of implementation, NOTE:
* --|----|---- It seems fair that since all servlets share this common-beans, that there be only one such collection-of-beans, referred by all servlets.
* .. Since all servlets are made by servletContext, so this common-glob is also associated to servlet-context, and not to individual servlets.
* .. ALSO note, since this common-glob is made before any other servlets, so the creation of this glob should be such that it is independent of
* .. any servlet-dependent-setting. Hence, servlets can access this glob, but not vice versa.
* --|----|---- Since the servlet-context is made before any other servlet, so it is natural that this common-glob should also get made first. 
* .. This is done in 2 step: (1) giving "servlet-context" an init-param with location of the xml to use for initializing the app-context, via 
* .. web.xml (this does not automatically load anything, it is just a param), and, (2) Adding a context-loader-listener that gets triggered when 
* .. the servletContext is being made, which reads the value given in #1 (or assumes a default), and tries to use that configuration file to load
* .. Spring-managed common-glob-of-beans [DIGRESSION: few things to note: (i) i've given an example of what happens with xml-config. The exact steps
* .. may be slightly different for when it is not an xml config -- but overall the flow is same; (ii) Spring boot does programmatic configuration 
* .. and there is no web.xml, so things are slightly more different for it; (iii) The servlet-context is made before listener is triggered, but the 
* .. servlets are not yet made. Look at github code for ContextLoaderListener and ContextLoader (the super class). The listener acts on ServletContextEvent
* .. which carries servletContext in it, so listener definitely cannot be triggered before servletContext object is made. Also, the context loader has
* .. logic showing how servletContext is bound in applicationContext]
* --|----|---- Now the servlet-context initialization is done, and if servlets need, they can access the application-context through servlet-context,
* .. via WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE attribute (see code for ContextLoader). Also, vice versa, the application-context 
* .. holds reference to servlet-context, via WebApplicationContext.getServletContext()
* --|----|---- **IMPORTANT** (including sub-points): With servletContext and root-app-context made, the next step is making any required servlets. 
* .. There are 3 overall options, in descending order of idealness:
* --|----|----|---- The most ideal design would be to make a new set of beans required to add the "bridging" web-functionalities that expose the 
* .. service logic (..within common-bean-glob) to external user. Hence, a new Spring applicationContext should be made, but which covers only the 
* .. new beans, and uses the common-bean-glob as parent. A suitable implementation would be if beans for the "bridging" web-functionalities, like,
* .. @Controller annotated beans, and validators, and jsonConverters are kept in a separate package, totally different from package containing 
* .. classes that make common-bean-glob. 
* --|----|----|----|---- **PARTICULARLY**, see 3rd answer in the above-post where the author explictly makes a break in defining controllers in
* .. spring-servlet-config, and the remaining component scan in application-config. 
* --|----|----|----|---- ** REALIZE** that since servlet-config is done after application-config, it is necessary to ensure that you don't mix any 
* .. part of application-config inside servlet-config, else a bean will get double initialized, and start throwing errors. SO, best design is to 
* .. also keep classes separate in different package
* --|----|----|----|---- Also see https://stackoverflow.com/questions/16458754/spring-web-mvc-dispatcher-servlet-xml-vs-applicationcontext-xml-plus-shared
* --|----|----|---- Another possibility could be that if all spring-beans for common-glob and for servlets are made within root-app-context, made 
* .. at time of servletContext creation. And then servlet registration is done later.
* --|----|----|---- A third possibility could be that if there needs to be just one servlet registered (like, only the dispatcher servlet), then 
* .. all spring-beans for common-glob and for servlets are made within servlet's app-context ..NOT made at time of servletContext creation, but 
* .. at time of servlet creation. This is least ideal way to make the context. 
* --|----|----|----|---- DO NOTE that in such cases, since the full context is made by servlet, so there won't be a need for contextLoaderListener. 
* .. See https://stackoverflow.com/questions/11815339/role-purpose-of-contextloaderlistener-in-spring
* 
*
*
* OTHER NOTES: 
* --|---- **VERY VERY IMPORTANT**: With introduction of Servlet-3, it is possible to programmatically configure --INSTEAD OF-- using web.xml
* .. See EtceteraController2, comments at beginning, and particularly the AbstractAnnotationConfigDispatcherServletInitializer class for doing so.
*
* --|---- Particular to case of container controlled creation of dispatcher-servlet by reading web.xml: We set the servlet-class value
* .. in web.xml and that's how container knows to make the Spring's dispatcher-servlet. Only for this (..and not for any other general servlet), it 
* .. has logic in it to search for root-app-context in servletContext and use that as parent for its web-app-context (i.e. dispatcherServlet's 
* .. app-context). Again, only for this class, it has logic to read certain initialization values from web.xml and identify the spring config 
* .. location based on those properties set by user in web.xml
*
* --|---- ContextLoaderListeners
* --|----|---- (See https://stackoverflow.com/questions/11815339/role-purpose-of-contextloaderlistener-in-spring) The contextLoaderListener is 
* .. invoked by J2EE container (..if it is registered in web.xml, or programmatically registered to servletContext) when making the servletContext 
* .. at time of deployment. The listener makes the applicationContext and binds it to servletContext. 
* --|----|---- When registering it via web.xml, it uses servlet-context-params, like, location of xml file, and many-other releveant parameters  
* .. can be passed (see https://stackoverflow.com/questions/16458754/spring-web-mvc-dispatcher-servlet-xml-vs-applicationcontext-xml-plus-shared   
* .. and also the github code of contextLoader class where the param names are listed)
* --|----|----|---- Note that 2 such parameters are: contextInitializerClasses and globalInitializerClasses. What's the difference? It is best 
* ..answered in JIRA https://github.com/spring-projects/spring-framework/issues/15938  : "globalInitializerClasses" init-param : "basically like 
* .. the existing "contextInitializerClasses" but applying to FrameworkServlets as well. They are applied right at the end of context initialization 
* .. but still before locally specified "contextInitializerClasses", if any, to preserve a remaining chance for overriding." So, root-app-context
* .. use globalInitializerClasses (from servletContext) and then contextInitializerClasses (still from servletContext) -- See github code for 
* .. determineContextInitializerClasses() method in ContextLoader. HOWEVER.. dispatcher-servlets use globalInitializerClasses (from servletContext)
* .. and then contextInitializerClasses (BUT FROM servlet's configuration and NOT from servletContext) -- See github code for applyInitializers()
* .. method in FrameworkServlet.
*
* === END ===
* 
* 
* 
* **VERY VERY VERY VERY IMPORTANT#2**: ON DISPATCHER-SERVLETS
*
* ---- DispatcherServlet extends FrameworkServlet. In Spring, if you want each servlet to have similar behavior (like, a parent relation with  
* .. root context), then, best is to extend FrameworkServlet when doing so.
* 
* ---- Initializing a dispatcher servlet: Call to usual constructor will work as good. Do note that since it extends FrameworkServlet, so it'll try to look
* .. for root-context in ServletContext and associate with it. You can do it more manually as shown in javadoc examples of WebApplicationInitializer, or 
* .. can borrow from code of AbstractAnnotationConfigDispatcherServletInitializer. 
* ----|---- ONE THING TO NOTE: while it does not matter what name you give the servlet, when you are in Spring boot, then a good option is to give it a 
* .. name as defined in DispatcherServletAutoConfiguration, and use other constant to name the servletRegistration. See https://stackoverflow.com/questions/28006501/how-to-specify-prefix-for-all-controllers-in-spring-boot   
* --|---- DO NOTE: Choosing above name for servlet and registration is not mandatory. In fact, it is configurable via properties. See https://www.baeldung.com/register-servlet
* --|---- FOR SPRING BOOT.. It auto-disables the auto-config of dispatcherServlet is such a bean is found. You can also add @SpringBootApplication(exclude=DispatcherServletAutoConfiguration.class) 
*
* ---- How do we say that DispatcherServlet has parent-child relation with root-app-context: The way it works is that it makes its own 
* .. app-context based on the values in web.xml; And finally it binds that context with a parent app-context if available. These details are not 
* .. in javadoc, but can be inferred from github code ..and looking at initialization of FrameworkServlet (super class of DispatcherServlet), 
* .. particularly by noting initWebApplicationContext() method in it. For example of config settings for dispatcherServlet, 
* .. see: https://stackoverflow.com/questions/11815339/role-purpose-of-contextloaderlistener-in-spring , can also see the api-doc for FrameworkServlet
* 
* ---- How is dispatcherServlet able to know the main application-context? Because root-web-context is registered in servlet context under the
* .. attribute name "org.springframework.web.context.WebApplicationContext.ROOT"
* 
* ---- **VERY IMPORTANT** How is dispatcherServlet able to know the its own application-context? App-contexts for different framework-servlets 
* .. are registered in servlet-context under the name "org.springframework.web.servlet.FrameworkServlet.CONTEXT.<servlet-name>", so much so that 
* .. if you attach a new context under same name, it will override original value. See https://stackoverflow.com/questions/30639842/how-exactly-are-the-root-context-and-the-dispatcher-servlet-context-into-a-sprin
*
* ---- If everything is so controlled, is it possible to have a FrameworkServlet with an app-context that is child of app-context of another 
* .. FrameworkServlet? YES! See https://stackoverflow.com/questions/8362301/spring-dispatcherservlet-context-inheritance     Main thing to note is 
* .. that by invoking FrameworkServlet's createWebApplicationContext(parent), it is possible to create a new app-context with a parent binding. A 
* .. call to the newly made app-context's register() method can then be made to do extra bindings (use AbstractAnnotationConfigDispatcherServletInitializer 
* .. for refrence to copy codes. The class is discussed in EtceteraController2 if you need, but not regarding dispatcherServlet alone!)
*
* ---- **VERY VERY IMPORTANT**: Spring in a EAR file with multiple war
* ----|---- At least in older posts, it seems that there was "parentContextKey" config value which could be used for it. See https://spring.io/blog/2007/06/11/using-a-shared-parent-application-context-in-a-multi-war-spring-application/  
* .. Similar process was also suggested in https://stackoverflow.com/questions/16162877/spring-mvc-sharing-context-within-ear
* ----|----|---- Reading from spring.io blog, the way it works is.. by doing so, you instruct the ContextLoader to use another class called
* .. ContextSingletonBeanFactoryLocator to search for a bean named by the value of parentContextKey, defined in a configuration file whose name 
* .. matches a certain pattern. By default, this pattern is ‘classpath*:beanRefContext.xml’, meaning all files called beanRefContext on the 
* .. classpath. (For a plain SingletonBeanFactoryLocator it’s ‘classpath*:beanRefFactory.xml’). This bean must be an ApplicationContext itself, and 
* .. this context will become the parent context for the WebApplicationContext created by the ContextLoader. However, if this context already exists 
* .. then that one will be used and no new context will be created (hence the name SingletonBeanFactoryLocator).
* ----|---- **HOWEVER** most recent answer for the question suggests that the mechanism is no longer available in Spring-5! See https://github.com/spring-projects/spring-framework/issues/20805
* .. and https://stackoverflow.com/questions/46902115/spring-framework-5-0-0-final-parent-context-not-getting-loaded
* ----|---- SO.. ANY SOLUTION? ONE thinking is based on the concept of JNDI related discussion (see start comment in EntityManagerConfigUserOne)
* .. The idea is that we can use JNDI when more than 1 independent applications will use a common object --AND-- initialization of that
* .. common object does not require knowledge of other applications (..and so there is a definite parent-child relationship). This is also the case here!
* .. So, the idea is to create the rootAppContext and bind it in JNDI.. and then have other war-files read from that JNDI. 
* ----|----|---- THAT BEING SAID.. I haven't done any such coding to provably show this.. but it definitely seems feasible
* ----|----|---- [DIGRESSION] See here for JNDI use via JndiTemplate: https://www.baeldung.com/spring-persistence-jpa-jndi-datasource
* 
*
*
* [[UNDERSTANDING WORKING OF DISPATCHER-SERVLET]]
* Reference: -- https://www.baeldung.com/spring-dispatcherservlet
*            -- https://dzone.com/articles/how-spring-mvc-really-works
*            -- https://www.programering.com/a/MTN1EjNwATE.html  <--- This one covers lot of internal details, the comments below are in addition
*               .. to it; Or, just describe it better!
*            -- It is instructive to look at Github codes whenever possible
*
* === START ===
* -- DispatcherServlet's doDispatch() is invoked. Note that at this point, only request and response is provided to dispatcherServlet, nothing else
* -- In doDispatch() : It finds the handler corresponding to request, possibly from handlerMapping (like, BeanNameUrlHandlerMapping, RequestMappingHandlerMapping). 
* .. [Ref. https://www.baeldung.com/spring-handler-mappings]. NOTE that springs concept is of handler is actually a "HandleExecutionChain", 
* .. comprising of actual handler method, and additional interceptors, lined together to make the execution-chain
* -- In doDispatch(): For the handler, get the handlerAdapter, which "bridges" the logic for handlerExecution by dispatcherServlet
* -- **IMPORTANT**: The github code shows that the next step is to check for "Not-Modified" if so asked in request.. BUT note that for
* .. requestMappingHandlerAdapter, this is disabled by default. Then the interceptor.preHandler() is run.. and then request is handled. NOW,
* .. this creates an issue in that if there is a mix of HandlerAdapter such that for some of them we define lastModified explicitly, then that
* .. will run before interceptor -- and so, will behave differently that RequestMappingHandlerAdapter. SO, best to not mix! Also, in current
* .. landscape, there is also no need to mix handlers! Just FYI, even in future, don't do it.
* -- **VERY VERY VERY IMPORTANT**: Note that when dispatcherServlet.doDispatch() invokes handlerAdapter.. the adapter ALWAYS return a 
* .. ModelAndView object. The question: How does this match with REST style programming where JSON is returned? --answered later. Also note that
* .. the handlerExecution chain provides reference to actual handling method which is given to handlerAdapter
* -- **VERY VERY VERY IMPORTANT**: Note that after handlerAdapter has gone through request, then the interceptor.postHandle() is called. 
* .. This is immediately after handling and before any further processing. AND, the IMPORTANT part, is that the "ModelAndView" response from
* .. handlerAdapter is passed to interceptor. So, if done properly, interceptor can actually change the response here!!
* -- Considering proper processing.. next step is calling of dispatcherServlet's processDispatchResult(). Now, if everything goes fine, dispatcherServlet
* .. doesn't do anything else special.. so we'll consider this as last step of dispatcherServlet (for almost all cases) -- and analyze it!!
* -- In processDispatchResult(): Note that most of code is related to error-processing except: (a) call to render() if a "ModelAndView" is
* .. provided, which is for most cases; (2) and, calling the final-execution in interceptor ..BUT note that at this point, the render() is 
* .. already done. So, best not modify anything in response here. THUS, combining with previous statement, it means that for most case without
* .. error, and assuming no final interceptor method defined.. then render() is the last-call-logic before ending doDispatch()!!
* -- In render(): Essentially, the "View" object is identified from "ModelAndView" object - preferably by using viewName and then using 
* .. view-resolver to identify the view; Else, directly getting View object out. Finally, the render() method of the view (not of dispatcherServlet)
* .. is called. **VERY VERY VERY IMPORTANT**: The question remains on how this "View"-rendering related to returning JSON response, tied to
* .. question raised earlier on how the processing is done by handlerAdapter when JSON is to be returned, and why it returns "ModelAndView" object?
* -- Since we are mainly using @RequestMapping, so to answer the question requires looking RequestMappingHandlerAdapter. Before going deep in it,
* .. note that this extends AbstractHandlerMethodAdapter, whose handle() method - the one that dispatcherServlet invokes in doDispatch(), it just
* .. delegates logic to handleInternal(). The point being, we need to analyze handleInternal() method in RequestMappingHandlerAdapter - and there 
* .. is nothing else of importance in AbstractHandlerMethodAdapter
* -- In handleInternal(), following main things are seen: (1) If synchronizeOnSession is true, and there is a session, then a mutex is used to
* .. synchronize request; (2) at end of method, there is call to prepareResponse(), which is defined in WebContentGenerator - which is grandparent
* .. of the adapter. It just has some cache-header related logic - nothing major related to "primary response generation"; (3) and, mainly.. there
* .. is call to invokeHandlerMethod() which returns "ModelAndView" object that plays central role in dispatcherServlet. THUS, this method is 
* .. analyzed. ALSO RECALL FROM PREVIOUSLY, handlerExecutionChain provides reference to actual handling method which is given to handlerAdapter,
* .. and that is the one used in this call
* -- In invokeHandlerMethod(): A ServletInvocableHandlerMethod object is made (check spring docs). It seems like a simple "invocable" 
* .. representation of handler-method. HOWEVER, 2 points to note: (1) main point is that it can post-process response from handler-method 
* .. (notice the terminology we are talking of handlerMethod - the actual method in class that does the handling; Not of HandlerMapping, 
* .. or HandlerAdapter, or HandlerExecutionChain); and, (2) there is a call to its invokeAndHandle() method, which, for normal case, does
* .. the actual method invocation and then pass the returned-result to returnValueHandler. In this case, most likely, there is a 
* .. RequestResponseBodyMethodProcessor returnValueHandler which handles it.
* -- Looking in RequestResponseBodyMethodProcessor, it is seen that it is used with incoming request when there is @RequestBody annotation, AND,
* .. with outgoing response when there is a @ResponseBody, or if the containing type (i.e. Generic) is ResponseEntity! This verifies that the
* .. returnValueHandler is indeed getting used in most cases. It is also seen as having constructors that use various RequestResponseBodyAdvice 
* .. (**IMPORTANT: NOTE: The registered advice(s) aren't just responseBodyAdvice BUT ALSO can have RequestBodyAdvice!)
* -- And the main answer.. When processing JSON response, the RequestResponseBodyMethodProcessor.handleReturnValue() writes the response 
* .. (using Objectmapper, etc), and sets the modelAndView.setHandled(true), but does not attach a view object. So when it now comes to 
* .. RequestMappingHandlerAdapter.invokeHandlerMethod(), at end it calls its' getModelAndView() - which sees that modelAndView is handled, and
* .. so returns null value for ModelAndView - to be then sent to dispatcherServlet; ..and in dispatcherServlet.doDispatch(), when it sees a
* .. null ModelAndView value, it simply makes a log and proceeds - understanding that everything is handled - which it is! Had it been a different
* .. returnValueHandler, a view would have existed and a non-null ModelAndView would have come to dispatcherServlet. **This also means that when
* .. returning JSON, then a call to handlerAdapter.handle() in disaptcherServlet.doDispatch() comes with a fully formed response -- so invoking
* .. the interceptor method immediately called after this step can still not change the response, since it it already written by this time!
* -- One more related question.. looking at View api-docs one of the implementation is "AbstractJackson2View" implemented by "MappingJackson2JsonView", etc
* .. SO what is this? As best said in javadoc of "MappingJackson2JsonView", this comes up when trying to return a modelMap as JSON... probably
* .. when accept type is JSON instead of text/html. And this brings up an important design point -- in that "don't do this!" While it seems
* .. possible, don't use modelMap to return JSON - because it breaks the "Object model" design and architecture! So, when wanting to return
* .. JSON, use ResponseEntity or @ResponseBody with a suitable DTO object. DON't TRY TO BYPASS BY USING MODEL-MAP, or, MODEL object!
*
*
*
* RELATED:
* -- (1) The terms: HandlerMapping, HandlerMethod, HandlerInterceptor, HandlerExecutionChain, HandlerAdapter
* --|---- A HandlerMapping represents collection of  "encapsulation" of details that identify an endpoint that needs to be handled, and the 
* .. handling-method. For example, for "RequestMappingHandlerMapping", its elements can be an encapsulation of Requestmapping-url (on class + on method), 
* .. the method (GET, PUT, POST, etc), and constraints, like, mandatory requestParam, requestHeaders. See the api-doc for "RequestMappingHandlerMapping", 
* .. where such terms are identified within class RequestCondition
* --|----|---- A side note: When making RequestMappingHandlerMapping instance, only one instance is made that contains collection of mappings. The mappings are contained in object of "AbstractHandlerMethodMapping.MappingRegistry" class 
* --|---- The "handling-method" as mentioned above (for HandlerMapping) is the HandlerMethod, i.e. it is the actual logic that needs to be "handled"
* --|---- HandlerExecutionChain is a combination of HandlerMethod, and the interceptors (i.e. HandlerInterceptor) that should be applied to the method
* --|---- Now, HandlerMethod is a generic representation of a logic that needs to be run. But, being non-specific, it means we need to also identify
* .. how to run that logic, and that is the work of HandlerAdapter. So, note that (as example), while different RequestMappingHandlerMapping
* .. are asociated to correspondingly different HandlerMethod, all those will use same "RequestMappingHandlerAdapter" to run!
* --|----|---- **IMPORTANT**: Above definition of HandleAdapter means that, say, between request coming as http-request, and it getting sent 
* .. to the controller-method, everything else that happens in between is done by HandlerAdapter. So, for common case of using @RequestMapping, 
* .. it is the RequestMappingHandlerAdapter that does job like conversion of json body to object, calling validation, etc. Looking at 
* .. RequestMappingHandlerAdapter, one can see various methods corresponding to such processes. Particularly, note the methods: 
* .. setSynchronizeOnSession (described below), setMessageConverters() -- used to convert json to object, 
* .. setCustomArgumentResolvers() -- to convert a bunch of query params to an object and/or to also add formatting, 
* .. setCustomReturnValueHandlers(), **AND, IMPORTANT** setWebBindingInitializer (see next). **VERY EVRY IMPORTANT ..DO REALIZE**: that any setting 
* .. done here is GLOBAL in nature, and will apply to all @RequestMapping methods, since the handlerAdapter is itself getting customized!
* --|----|---- **IMPORTANT**: See setWebBindingInitializer(WebBindingInitializer) of RequestMappingHandlerAdapter. Can add validator, 
* .. converter/formatter -> via ConversionService, propertyEditor, etc. **DO NOTE**: That this is an initializer, so it control configuration 
* .. of Web-Binder.. and so, it applied globally to any web-binder created for any requests. This also identifies the difference in using 
* .. @InitBinder("...") method in controller, which is limited to only that controller, or even less, to a particular object alone. ALSO, 
* .. looking at methods used there, note that one generally uses addValidator(), rather than setValidator() --> because WebBindingInitializer 
* .. sets global validator, and @InitBinder add extra validator on top of it for local use
* --|---- **IMPORTANT**: An important digression is to understand how dispatcherServlet identifies HandlerMethod to use - from HandlerMapping's 
* .. MappingRegistery. Essentially dispatcherServlet calls its getHandler() method -> which will iterate and finally call RequestMappingHandlerMapping's 
* .. getHandler() -> which is defined in AbstractHandlerMapping.getHandler() parent class -> which delegates to AbstractHandlerMethodMapping.getHandlerInternal(), 
* .. which is child of AbstractHandlerMapping, but parent of RequestMappingHandlerMapping -> it starts with getting lock on MappingRegistry 
* .. (to prevent it from changing while a matching is done), and calls its lookupHandlerMethod(), which searches for direct url matches in 
* .. registry (in best case), else iterates over all urls in registry, and returns best match. Note that even a single url can have multiple 
* .. handlerMethods associated to it, for example, those corresponding to GET, POST, PUT, etc., or requiring certain headers, etc. To do 
* .. matching from a list of possibilities, AbstractHandlerMethodMapping.addMatchingMappings -> calls getMatchingMapping() defined in RequestMappingInfoHandlerMapping
* .. which is parent of RequestMappingHandlerMapping. This method, getMatchingMapping, asks the corresponding "RequestMappingInfo" (taken 
* .. from a mapping entry in mappingRegistry) to identify is request "matches". As part of doing so, RequestMappingInfo uses a PatternsRequestCondition 
* .. to see whether incoming url (given by UrlPathHelper) matches the @RequestMapping url pattern, with the matching done by PathMatcher. If this is so, 
* .. then that HandlerMethod is accepted as a match
* --|---- Useful links: 
* --|----|---- https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/handler-mapping.html
* --|----|---- (Some good unit-test examples) https://www.programcreek.com/java-api-examples/index.php?api=org.springframework.web.util.UrlPathHelper
*
* 
* -- (2) When are different components, like the handler, mapping, etc. configured? How does DispatcherServlet knows of all it?
* --|---- First, note that there are various methods with names starting with prefix "init". So, dispatcher-servlet calls all of them to initialize 
* .. them, as part of getting configured. I'm guessing this is also the time when the requestmapping-handlerMapping list is created
* --|---- Note that things like LocaleResolver, ThemeResolver, etc.. they are done by dispatcherServlet, and are associated to incoming request 
* .. as attributes within doService() method of DispatcherServlet, before the request goes through full processing. That is how when the request 
* .. enters the Spring MVC architecture, it has access of all those thread-local values, accessible via static method calls. **IMPORTANT**: For same reason, 
* .. they are not available to filters - which get applied before dispatcherServlet (if needed, add extra filters). Same is also true for exception-advice
* --|---- In context of dispatcherServlet and request-handling.. 
* --|----|---- (a) Look at HandlerMapping interface. It defines multiple static field name which are used as name of attributes associated with 
* .. request, and they contain important values related to link betwen incoming url and values that go to endpoint handled by controller
* --|----|---- (b) Note that dispatcher servlet may contain a list of handlerMapping objects.. but recall from above, that it does not mean that
* .. we get different requestMappingHandlerMapping object for each RequestMapping annotation; only 1 such class is made, and all request-mapping 
* .. annotation-mappings are stored in it
* --|----|---- (c) In line with (b), and combining concept from WebMvcConfigurationSupport (which is important, and discussed later), see the java 
* .. code configuration part in https://stackoverflow.com/questions/12684183/case-insensitive-mapping-for-spring-mvc-requestmapping-annotations/12734702#12734702
* .. The idea is that one can define their own handlerMapping classes.. or even modify behavior of "RequestMappinghandlerMapping" class. The bean 
* .. defined in this class is picked up and populated when dispatcherServlet is initialized. So, before it gets populated, it core properties itself 
* .. can be modified this way. A practical example along this line is in Pg62 of Spring-Mvc-docs, which says that one should change 
* .. RequestMappingHandlerAdapter 's "synchronizeOnSession" flag to "true" if multiple requests are allowed to access a session concurrently,
* .. say, by passing it as argument to requestMapping-handler-method; Else session access will not be thread safe. **RELATED TO IT**: See 
* .. discussion above on "RequestMappingHandlerAdapter" and settings inside it; And realize that it (i.e. RequestMappingHandlerAdapter) 
* .. can be configured via WebMvcConfigurer
*
* 
* -- (3) A collection of independent, small-length points:
* 
* --|---- (3.1) **IMPORTANT**: Say you want to do case-insensitive match to an endpoint? How do you do it? 
* --|----|---- Answer: you modify the PathMatcher passed to mvc-config (there is a direct configurer to do so). DO ALSO NOTE that in the overall execution,
* ..  there is also a UrlPathHelper -- whose job is to provide url-pieces from incoming request. The PathMatcher determines whether the incoming-request-url
* .. (as provided by helper) matches the pattern written in controllers. If instead of PathMatcher you instead change the UrlPathHelper to always return,
* .. say, lower case String, even then: (a) match can fail if patterns in controller have mixed cases, (b) it will wrongly lower-case user provided values 
* .. in path-params and query-params. 
* --|----|----|---- RELATED#1: See above on how dispatcherServlet identifies HandlerMethod/HandleExecutionChain. In doing the last bit of 
* .. matching, PathMatcher gets used
* --|----|----|---- RELATED#2: recall from notes in Spring-Mvc-doc, that one must disable semi-colon in UrlPathHelper if one wants to use MatrixParams 
* .. (recall from Spring-Security-docs, semi-colon need to be disabled in HttpFirewall also). 
* --|----|----|---- RELATED#3: If you want case-insensitivity, you'll need to put different matcher in Spring-security also
*
* --|---- (3.2) @EnableWebMvc vs WebMvcConfigurationSupport
* --|----|---- Look at the api for both. @EnableWebMvc is a shortcut for importing the configuration file "WebMvcConfigurationSupport", thus adding 
* .. default behavior. This is good for many cases, except when you want to make really advance changes, then use latter (This is also mentioned in
* .. Spring-mvc-doc pdf file, and, also see https://www.javacodegeeks.com/2013/01/spring-mvc-customizing-requestmappinghandlermapping.html)
* --|----|---- **VERY VERY IMPORTANT**: This explains why, when using the WebMvcConfigurer interface to control mvc-behavior, if you want to add 
* .. extra messageConverters, then it is necessary to first call same method on super(), and then add new converters. This is because call to super 
* .. keeps the default already set, and then adds new entries to it. If you don't call super(), then default entries set by WebMvcConfigurationSupport 
* .. will get removed! -- unless, of course, if that is the intention. OTHER ALTERNATIVE IS to instead use extendMessageConverters()
*
* --|---- (3.3) **VERY IMPORTANT**: Understanding sevletPath -- see https://stackoverflow.com/questions/15385596/how-are-servlet-url-mappings-in-web-xml-used
* --|----|---- FOR THIS REASON: Realize that if dispatcherSevrlet is mapped to /* url, then best to register other servlets before mapping dispatcher
* .. servlet. If not done, then requests to other servlet will first go through dispatcherServlet -- failing all before going to next one and this will
* .. be non-performant. STILL, doing so could still be a viable option if the other servlet is to be rarely used (like swagger mapping)
* 
* --|---- (3.4) An extreme example of creating new HttpMethod, see https://stackoverflow.com/questions/33302397/custom-http-methods-in-spring-mvc
* .. BUT, NEVER DO SO in practice, because (1) that is not restful, (2) would not translate nicely to using other HttpClients!
*
* --|---- (3.5) **VERY VERY IMPORTANT**: Note from FrameworkServlet javadoc/code that it creates events based on if request is successful or failed. 
* .. This can be useful for logging!
*
* --|---- (3.6) RequestContextListener 
* --|----|---- ( Note that Spring has many "listeners", that listen to events and do related task! One such important one is "RequestContextListener"
* .. which can listen to incoming request and bind it to thread, such that the incoming request object can be obtained through executing thread, 
* .. and as per javadoc, even through all spawned threads. See https://stackoverflow.com/questions/35652457/what-do-contextloaderlistener-and-requestcontextlistener-do 
* --|----|----|---- NOTE: In spring boot, we already use the "RequestContextHolder" and its static utilities to get the request, BUT without making such configuration - because dispatcher servlet already does this for you (see docs). See https://stackoverflow.com/questions/30254079/configuring-requestcontextlistener-in-springboot
* --|----|----|---- NOTE: There is something called RequestContextUtils class!
*
* --|---- (3.6) How to change the default page? Maybe, use something like this when setting up your mvc. Then, "/" is mapped to proper html
* .. HOWEVER, I'm not sure if this breaks mapping for dispatcherServlet! STILL.. good to know about such configurations
* .. @Configuration
* .. public class YourWebConfig extends WebMvcConfigurer {
* ..   @Override
* ..   public void addViewControllers(ViewControllerRegistry registry) {
* ..     registry.addViewController("/").setViewName("forward:/index.html");
* ..   }
* .. }
*
* === END ===
* 
* 
* 
* MultipartResolver , MultipartConfigElement and MultipartFilter
* --|---- Before Servlet-3, the configuration values like location, max size, etc. were put here. The "resolver" reads the incoming multipart request
* .. (by noting the multipart type in Content-Type RequestHeader) and breaks it into Multipart object for use later (See Javadoc for MultipartResolver)
* .. Also see https://www.baeldung.com/spring-file-upload
*
* --|---- With the coming of Servlet-3, it is now necessary to provide the configuration values at servlet level and no longer at resolver level. So,
* .. the "MultipartConfigElement" comes in picture, where the settings are done, and then this bean is attached to dispatcherServlet (or corresponding
* ..servletRegistration bean)
* --|----|---- As mentioned above and also early-on in EtceteraController2, for Servlet-3 configuration, we can use WebApplicationInitializer and classes
* .. implementing it, like, AbstractAnnotationConfigDispatcherServletInitializer. In such cases, we need to configure multipart registration programmatically.
* .. See example given in StandardServletMultipartResolver javadoc. Also see https://stackoverflow.com/questions/23570014/spring-4-java-config-for-multipartresolver-for-servlet-3-0
* --|----|---- For Spring boot: (1) these can also be controlled via properties; (2) If config and resolver beans are made, they will be auto-picked up 
* .. for use at correct place
*
* --|---- MultipartFilter: This is a convenient place to configure the invocation of "MultipartResolver" on a request in the overall processing chain. 
* .. For example: when csrf header is passed in security, or when using HiddenHttpMethodFilter, then that may conflict with multipart-resolver
* .. (See https://github.com/spring-projects/spring-boot/issues/2958#issuecomment-103009500) In such cases, issues can be navigated by defining the 
* .. multipart-filter much earlier in the chain.
*
*
*
* **VERY VERY VERY IMPORTANT**: Note that Spring has something called FormContentFilter and HiddenHttpMethodFilter. BEST KEEP THEM DISABLED! else it
* ..may be a security risk
* --|---- (Also mentioned in EtceteraController2): Spring-boot enables them by default. HOWEVER, for security, best keep them disabled.. 
* .. specially, keep HiddenHttpMethodFilter disabled
*
*
*
* **VERY VERY VERY IMPORTANT**: (Also discussed in ReqRespController.. search for keyword "HeaderWriterFilter")
* .. See definition of "SecurityContextPersistenceFilter", BUT mainly note its function on how it does something at 
* .. beginning of request.. AND.. then at also at end of request!! BUT SPRING DOES NOT HAVE POST PROCESSING FILTERS!! And this is the idea.. 
* .. "if you want to do something at end of filter --AND ALSO-- that does not require changing the response", then, after doing chain.doFilter(...) 
* .. in a filter implementation.. don't immediately return, but instead add a logic after that which now gets executed after request has been 
* .. processed, and control comes back to filter chain but in opposite sequence. 
* --|---- JUST TO ADD.. if above does not work, then maybe try adding a finally block in filter just as in code of SecurityContextPersistenceFilter, 
* .. maybe that works! (For code, see github)
* --|---- A RELATED THING TO NOTE: When making own filter and chain.doFilter() is not the last line in method.. then DO WRITE "return;" after the 
* .. call to move request down the chain.. else the control will come back to the filter after chain has processed - and maybe that is not desirable!
* 
* 
* 
* **VERY VERY VERY VERY IMPORTANT**: Note that many of Spring beans for jpa, jms, etc. have a method "afterPropertiesSet()". It is important to
* .. call these whenever the option is given, and not just return constructor-returned object. The "afterPropertiesSet()" invoked @PostConstruct
* .. so that any pending initializations are also done!! See https://stackoverflow.com/questions/30726189/spring-why-is-afterpropertiesset-of-initializingbean-needed-when-there-are-st
* 
* **VERY VERY IMPORTANT**: Remember that spring-profile can be both INCLUSIVE, like, @Profile({"dev","sit"}), and also EXLUSIVE, like, 
* .. @Profile("!test"). See https://www.baeldung.com/spring-profiles. 
* 
* 
* 
* Properties :
[[NOTE that the example below seems to heavily rely on Spring-Boot. This is not counter to main-philosophy of entries in EtceteraController (which is kept generic for Spring) vs EtceteraController2 (which is specific for Spring Boot). It's just that the distinctions in annotations and concepts get more clearly exemplified by Spring boot]]
* -- Spring boot follows a chain of logic when trying to infer properties. See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-vs-value
* -- (Same page) @ConfigurationProperties, @PropertySource, @EnableConfigurationProperties -- and related @Value, PropertySourcesPlaceholderConfigurer
* --|---- @ConfigurationProperties tells spring-boot how to construct a class from properties; But, it does NOT tell which properties to read, 
* .. nor that such a bean should actually be made!
* --|---- Say there is some bean defined with @ConfigurationProperties, then @EnableConfigurationProperties(class-name), tells spring to make a 
* .. bean out of class, populating it by reading properties. It still does not tell where the properties should be read from! Also note, that if
* .. the class with @ConfigurationProperties is also given @Component, then spring makes a bean out of it - property location still not defined
* .. An example of using @EnableConfigurationProperties and not @Component is if the class with @ConfigurationProperties is in dependency jar!
* .. Like for SpringLiquibase made using LiquibaseProperties! 
* .. **IMPORTANT**: Instead of @EnableConfigurationProperties do NOTE that if @Bean is made that just returns new LiquibaseProperties(), 
* .. even then a correct bean with all properties-set is returned.
* --|---- As given in web-page, Spring boot has an order of defining properties. But, in addition to it, @PropertySource can be defined with a 
* .. @Configuration class, and now when @EnableConfigurationProperties is added, then it is known from where to read properties when making bean
* --|---- A common PropertySourcesPlaceholderConfigurer bean can be made, adding to it all the propertySources. This way, it will not be necessary
* .. to add different @PropertySource annotation everywhere. NOTE:
* --|----|---- **IMPORTANT**: This bean needs to be static-type (see examples)
* --|----|---- With this bean, you can define how to deal with missing and null values!
* --|----|---- For a good example, see http://blog.codeleak.pl/2015/09/placeholders-support-in-value.html or https://www.baeldung.com/properties-with-spring
* .. Also see https://stackoverflow.com/questions/46284451/how-to-use-springs-propertysourcesplaceholderconfigurer-to-read-environment-v
* --|---- @Value("") annotation
* --|----|---- As general practice, always mention what the default values should be!
* --|----|---- **VERY VERY IMPORTANT**: It is observed that when a @Value("{tag}") is used, but the tag property-key is not defined, then the 
* .. corresponding variable is assigned value "{tag}" (without quotes)!!! If you want default to be null - which should be assigned when that 
* .. property-key is absent in property file (..and I mean absent, and not present with property-value of empty), then use @Value("{tag:#{null}}")
* .. See https://stackoverflow.com/questions/11991194/can-i-set-null-as-the-default-value-for-a-value-in-spring
* --|----|----  **VERY VERY IMPORTANT**:  NOTE that @Value, or even @Autowired on members are done after the constructor is finished.
* .. So, if you try to access this within constructor, you'll get null. Maybe, use @PostConstruct?!
* --|----|---- String inside @Value, and even more generally.. like say, inside @Profile for some bean.. can be defined by using property of
* .. other bean!! Or even expressions. See http://www.baeldung.com/spring-expression-language
* 
* **VERY VERY IMPORTANT**: Distinction between Enviroment and PropertySourcesPlaceholderConfigurer objects: See https://stackoverflow.com/questions/21100729/propertysourcesplaceholderconfigurer-not-registering-with-environment-in-a-sprin
* --|---- Essentially, latter allows access to properties which by default aren't loaded in Environment.. but it can be made to read those 
* .. properties out of PropertySourcesPlaceholderConfigurer. 
* --|---- Also note:
* --|----|---- Since ApplicationContext has access to PropertySourcesPlaceholderConfigurer, so best try to let the app-context resolve properties 
* .. inside code by using annotations like, @Value{..}, rather than doing it manualy and using Environment for it
* --|----|---- the use of "PropertiesLoaderUtils" and "ResourcePropertySource"
* 
* Spring allows instantiation beans on conditionals - like, if no such bean exists, or if some other bean exists, or if property is some value
* .. etc. See http://www.baeldung.com/spring-boot-custom-auto-configuration   
* .. (NICE) As another example, see https://stackoverflow.com/questions/43168881/can-i-negate-a-collection-of-spring-profiles 
* .. used in "ProfileCondition" to define classes where a profile does not match. This is then used in FaoConfig to trigger bean when multiple
* .. profiles are not found. This is not what @Profile("!profile1", "!profile2") does.. where a condition is triggered is it is not-profile1
* .. "OR" not-profile2. By using list of conditionals, they are AND'd, so they trigger when not-profile1 AND not-profile2
* 
* 
* 
* VERY VERY VERY IMPORTANT: Enabling Autowiring in non-Spring managed class
* --|---- In short, see https://stackoverflow.com/questions/18347518/spring-autowiring-not-working-from-a-non-spring-managed-class -- The best solution is the 
* .. one where it says to use @Configurable annotation on class that is not made into a bean, but you'd want autowiring in it. And add @EnableSpringConfigured 
* .. over a class that has Spring;s @Configuration, so that Spring starts looking for such classes
* --|---- Also note the other answers in the the post.. particularly, the one of capturing ApplicationContext and exposing it via static utility. I think Spring should  
* .. already have it. See https://stackoverflow.com/questions/21827548/spring-get-current-applicationcontext   mentioning of "WebApplicationContextUtils", 
* .. "SpringBeanAutowiringSupport"
* --|---- See Pg.126 of HiernateValidator pdf.. where it says that a custom "ConstraintValidatorFactory" could be used to provide autowiring. Maybe, for validators,
* .. make new factory as needed. It can then be associated with LocalValidatorFactoryBean (in Spring Boot)
*
*
*
* REQUEST-DISPATCHER:
* One item not discussed above is the RequestDispatcher. 
* --|---- Its best definition is from api-docs (https://tomcat.apache.org/tomcat-5.5-doc/servletapi/javax/servlet/RequestDispatcher.html)
* .. which says that it "Defines an object that receives requests from the client and sends them to any resource (such as a servlet, HTML file, 
* .. or JSP file) on the server. The servlet container creates the RequestDispatcher object, which is used as a wrapper around a server resource 
* .. located at a particular path or given by a particular name. This interface is intended to wrap servlets, but a servlet container can create 
* .. RequestDispatcher objects to wrap any type of resource"
* --|---- Based on above, realize that this object is one level above the servlet. Its implementation class are hard to find because that is 
* .. dependent on the server (i.e. servlet-container) being used, see https://stackoverflow.com/questions/22039398/which-class-provides-implementation-for-requestdispatcher-forward-and-include
* .. I am "guessing" that RequestDispatcher is to Servlet, what a HandlerAdapter is to HandlerMethod -- in that RequestDispatcher control flow of 
* .. request to Servlet, and related management of request/response object
* --|---- Note that servletContext has method to find RequestDispatcher. You provide as argument the target-path you want, then the servletContext 
* .. looks up the registered servlets and identifies the one to handle the request. The corresponding requestDispatcher is provided
* --|---- Note that since RequestDispatcher's job is to manage an incoming request, passing it to servlet.. that's why when one servlet wants to 
* .. pass method to other servlet on server-side only (via forward or include commands -- which are totally server-side, unlike redirect command 
* .. which is client/browser side), then it can artificially emulate as a local-client sending request -- which then means that a RequestDispatcher
* .. should handle it. That's why, the way to process such requests is to go via RequestDispatcher!
* --|---- **IMPORTANT**: A good use of request-dispatcher is seen in PC code.. when the incoming request-url is modified to a different form.. 
* .. and then the request is "forwarded" to old form. This way, users can use both old and new form. 
* --|----|---- Another option would have been to add a custom converter.. but then that'll apply everywhere. If that is not the desire, then using 
* .. requestDispatcher can be an option
*
*
*
* JBOSS-DEPLOYMENT:
* -- In deploying to JBoss: Make changes to add following line, and also see that the profile is indeed set. Jboss shows it when starting.
*       rem set java opts for spring profile
*       set JAVA_OPTS=%JAVA_OPTS% -Dspring.profiles.active=local
* -- Use jboss-deployment-structure.xml in WEB-INF to control if some dependencies from jboss should be added/removed when deploying
* -- Use jboss-web.xml to control context path. Use standalone.xml to control http-port
* --|---- NOTE: In springBoot, this is done in properties, and it works since Spring starts by reading them. But when deploying in server, the
* properties aren't the first item read.. and so it is not auto-set
* 
*
* === [END] CONFIG RELATED COMMENTS ===
*/
// @formatter:on

public class EtceteraController {

}
