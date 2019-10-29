package com.example.demo.spring.boot.ctrl;

/*
 * IMPORTANT.. NOTE:
 * -- This file has comments pertaining to general Spring-Boot. For best understanding, read it in conjugation with notes in EtceteraController , which
 * .. pertains to general Spring
 * **NICE TIP**: Running Spring boot locally, if Spring log level is set to DEBUG, then it gives many interesting logs that can be used to 
 * .. identify failures and/or process flow
 *
 *
 * 
 * **VERY VERY VERY IMPORTANT**: (Also mentioned in EtceteraController) Note that Spring has something called FormContentFilter and 
 * .. HiddenHttpMethodFilter. BEST KEEP THEM DISABLED! else it may be a security risk. 
 * --|---- NOTE: Spring-boot enables them by default, so do configure it to be disabled.. specially, keep HiddenHttpMethodFilter disabled. The properties
 * .. are "spring.mvc.formcontent.filter.enabled" and "spring.mvc.hiddenmethod.filter.enabled"
 *
 *
 *
 * Programmatic configuration replacing web.xml in Servlet-3:
 *
 * .. The first thing to understand regarding Spring-Boot is that it uses Servlet-3 and so, instead of web.xml, it can be configured programmatically. 
 * ---- BY J2EE SERVLET-3 SPECS.. programmatic configuration is done by scanning for classpath and looking for "ServletContainerInitializer" class. 
 * .. When found, its onStartup() method is called. 
 *
 * ---- Spring provides "SpringServletContainerInitializer" for this work. The class has annotation of "@HandlesTypes(WebApplicationInitializer.class)", 
 * .. which, by meaning of annotation, instructs server to pass all WebApplicationInitializer class to initializer (SpringServletContainerInitializer). 
 * .. The initializer's logic is to call the onStartup() method of all the WebApplicationInitializer-implementation passed to it. 
 * ----|---- Also, as said in "SpringServletContainerInitializer" javadoc, if the WebApplicationInitializer-implementation have @Order annotation, 
 * .. or implement ordered interface.. then they are executed in order. 
 * ----|---- NOTE that WebApplicationInitializer is itself different from ServletContextInitializer. Latter is used to configure ServletContext in
 * .. a manner independent from WebApplicationInitializer
 * ----|---- Do see SpringServletContainerInitializer's javadoc for nice information, and if needed, its github code
 *
 * ---- One "abstract class" that implements WebApplicationInitializer, and so is picked up by "SpringServletContainerInitializer" -- when bootstrapping
 * .. Spring Boot's application-context is "SpringBootServletInitializer". So, when deploying Spring-Boot to an existing server (like, JBoss), then this
 * .. class should be extended. 
 * ----|---- It has almost all starup logic. User-dependent configuration can be done by overriding the configure() method and nothing extra should
 * .. be needed. Realize that when overriding, generally we code it as:
    @Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}
    ..but realize that in addition to "sources", the SpringApplicationBuilder has many other properties that can be configured. The app-context initialized getting configured is shown in https://dzone.com/articles/spring-1
 * ----|---- One downside of using it (..mainly of using Spring boot) is that the context which gets made does not have a parent-child structure between
 * .. dispatcher servlet and root-context. There is just one context made. However, within it, you can configure multiple servlets.
 *
 * ---- AbstractAnnotationConfigDispatcherServletInitializer: This is another such class that eventually implements WebApplicationInitializer. And so,
 * .. it can be used for programmatic configuration to replace web.xml when launhing webservice. 
 * ----|---- This class is to be used by itself [[no Spring-boot even in discussion when using it]], and is used when using Servlet-3. Example of using 
 * .. it for Spring-web-app startup, see https://joshlong.com/jl/blogPost/simplified_web_configuration_with_spring.html  REALIZE that there are many 
 * .. setting that can be overwritten for a much better control
 * ----|---- While nothing seemingly seems to stop it from being used with Spring-Boot, not sure why someone should do it!! It will just be confusing 
 * .. and may have unexpected behavior. That being said, REALIZE that Spring boot does many other configurations in addition to making the 
 * .. Spring-app-context. So, just using AbstractAnnotationConfigDispatcherServletInitializer does NOT automatically replace all those settings/tasks
 * ----|---- **IMPORTANT** REALIZE that the when using AbstractAnnotationConfigDispatcherServletInitializer, then the root-context and dispatcher
 * .. servlet context have a parent child relation. This is NOT the case when Spring boot makes them (see discussion below on "SpringBootServletInitializer" 
 * .. and startup procedure). Referred from http://www.kubrynski.com/2014/01/understanding-spring-web-initialization.html    
 * ----|---- One downside of using it (..apart from fact that you don't get all extra things configured by Spring-boot) is that you can only have one
 * .. dispatcher-servlet
 * 
 * ---- Own implementation of WebApplicationInitializer: As shown in links above, note that it is even possible to make own implementation of WebApplicationInitializer, rather than trying to use other classes. 
 * ----|---- DO NOTE: When making app-context by own.. So you start by creating blank appContext of a type, and now you want to associate a 
 * .. @Configuration class to it.. then use the appContext's register() or scan() method, rather than using setLocation().. this is as per the 
 * .. api-docs. I am "guessing" that the former methods also refresh the web-app-context and that it might not be done for latter method!
 * ----|---- Look at javadoc for WebApplicationInitializer. It contains 2 example of programmatic configuration: First one replaces web.xml, but loads
 * .. Spring configurations via xml. The second one even loads Spring configurations via code -- so is 100% code-based.
 * ----|---- The downside of using it (..apart from fact that you don't get all extra things configured by Spring-boot) is that you'll need to write extra code, so is sort-of reinventing the wheel. HOWEVER, you can now have multiple servlets! 
 *
 * 
 *
 * A FEW SMALL DETAILS before taikng lengthy diversion to Spring-Boot initialization:
 * -- Note that in case of vanilla spring boot, the @EnableAutoConfiguration - makes spring boot automatically make the dispatcher servlet, and 
 * .. register all necessary beans! So, this is how everything comes up together nicely in Spring boot -- with just one annotation!
 * --|---- HOWEVER, if customization is needed, then selected points from the annotation should be disabled.
 * --|---- When running Spring-boot in embedded server, then SpringApplication.run(..) passes the class with this annotation (i.e. EnableAutoConfiguration)
 * .. which then enables auto configuration. When deploying Spring in external server, then SpringBootServletInitializer's configure() method identifies 
 * .. the class with this annotation, and SpringBootServletInitializer itself gets eventually picked up by container via Servlet-3's programmatic 
 * .. bootstrapping mechanism as mentioned above
 * 
 * 
 * 
 *
 * On "SpringBootServletInitializer" and startup procedure:
 * [[Follow along in Github code for understanding]]
 *
 * -- onStartUp() method:
 * --|---- The overall method is very simple, with the creation of app-context via createRootApplicationContext() [which is discussed later in detail]
 * --|---- Next: note the addition of contextLoaderListener such that the contextInitialized() is empty. Why? -- Because that job of the listener has 
 * .. already been done in the previous step, i.e. the context is already made. And so, the contextInitialized is left empty. However, another job of
 * .. listener is to destroy context, i.e. contextDestroyed() method, and to be able to do that.. so a listener is still added.
 *
 *
 * -- createRootApplicationContext() method:
 * [[Looking at github code, following happens]]
 *
 * .. i) Make a new, empty SpringApplicationBuilder object
 * --|---- SpringApplicationBuilder: (from Javadoc) Builder for SpringApplication "AND" ApplicationContext instances with convenient fluent API 
 * .. "AND CONTEXT HIERARCHY SUPPORT".
 * --|---- SpringApplication: (from Javadoc) Class that can be used to bootstrap and launch a Spring application from a Java main method. By default,
 * .. class will perform the following steps to bootstrap your application: (1) Create an appropriate ApplicationContext instance (depending on your 
 * .. classpath); (2) Register a CommandLinePropertySource to expose command line arguments as Spring properties; (3) Refresh the application context,
 * .. loading all singleton beans; (4) Trigger any CommandLineRunner beans
 *
 * .. ii) Check if there is an applicationContext associated as root-context in the servletContext. If that is the case, then: (a) remove that 
 *        association from servlet context, (b) add an "application context initializer" that would programmatically change the newly made app-context 
 *        after its initialization to associate the previous app-context as parent, particularly, using the ParentContextApplicationContextInitializer 
 *        (https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/api/org/springframework/boot/builder/ParentContextApplicationContextInitializer.html) 
 *        --|---- NOTE and REMEMBER: removing the root-app-context from servlet-context happens "now and immediately"; But the new root-app-context 
 *                .. that will do the replacement.. it hasn't yet been made yet. We are still just configuring the SpringApplicationBuilder! That's 
 *                .. why we are adding an initializer with reference to parent-context, so that it can be added when new context is made
 *
 * .. iii) Add ServletContextApplicationContextInitializer which will associate the newly made appContext as the root-app-context in 
 *        servletContext. **IMPORTANT**: This is done by binding root app-context to attribute name 
 *        "WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE" of servletContext
 *        --|---- NOTE and REMEMBER: the new root-app-context to which he servlet context will get bound hasn't yet been made. We are still just 
 *                .. configuring the SpringApplicationBuilder! That is why we are adding an initializer to run at end, which stores reference of 
 *                .. servletContext, so that can be added at that time
 *
 * .. iv) A "listener" is added that responds to ApplicationEnvironmentPreparedEvent (Javadoc: Event published when a SpringApplication is starting 
 *         up and the Environment is first available for inspection and modification). So, the environment has just been made.. at this time, this 
 *         listener looks for "stub property source" in environment corresponding to servletContext and replaces that with original servletContext, 
 *         so that properties could be read from there as needed. Why is this needed? because Spring boot says that it makes available the servletContext
 *         source. See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html 
 *        --|---- Just to mention.. the listener is WebEnvironmentPropertySourceInitializer -- and it is also defined in SpringBootServletInitializer's code
 * 
 * .. v) The context class is set to AnnotationConfigServletWebServerApplicationContext
 * --|---- v.i) See java-doc for ServletWebServerApplicationContext 
 *       (https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/web/servlet/context/ServletWebServerApplicationContext.html), 
 *        .. which is the class extended by it. This app-context class is special in that it can make a new "embedded" webserver, host servletContext 
 *        .. in that server, and then attach itself to the server that it-itself created. This is extra feature compared to simple WebAppContext, and
 *        .. so this is used [How this happens, we'll come to it in Point#(v.ii)]
 * --|---- v.ii) Regarding how the context is made and how it starts "embedded " server -- if needed -- See code for SpringApplication (https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/SpringApplication.java)
 *        .. for the "non-static" run(..) method [Even the static run(..) methods that are called when running Spring boot from main() method rather than
 *        .. hosting from external server, eventually end up calling the non-static run(..) method. So, just studying it is sufficient]. In the logic, it
 *        .. prepares the appContext and then it calls the "refresh" method of app-context it. Now, look at code of ServletWebServerApplicationContext, 
 *        .. where a call to refresh() invokes createWebServer()
 * --|----|---- v.ii.A) When running embedded webserver, since there is no servletContext already available, so the 1st branch runs in createWebServer(), 
 *              .. that starts a webserver (by searching for ServletWebServerFactory bean, whose specific implementation would be available based on maven
 *              .. dependency added to project (like, tomcat or Jetty). [DIGRESSION: (1) For extra details, see https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-embedded-container-application-context ;
 *              .. (2) REALIZE (from above link) how the embedded server properties can be controlled, or even new beans can be made to do the control] 
 * --|----|---- v.ii.B) When running in external container, the 2nd branch runs since a servletContext is already available (didn't check how..), and so,
 *              .. a new embedded server is not made!!!
 * --|----|---- v.ii.C) The code then runs getSelfInitializer() method -- which, by comment on method, "Returns the ServletContextInitializer that will 
 *              .. be used to complete the setup of this WebApplicationContext". [Note: it gives "ServletContextInitializer", not "WebApplicationInitializer",
 *              .. where the latter is executed by web-container and is replacement for web.xml, BUT, the former is Spring specific only and is used to
 *              .. configure servlet-context. The point being.. it is not auto-picked by container, but is picked by Spring. It is a bit confusing though,
 *              .. since both have onStartUp() method!!] It is at this point that Spring boot registers the filters and dispatcherServlet, etc. - as confrmed
 *              .. in javadoc of ServletWebServerApplicationContext. 
 *        .. [DIGRESSION: Few more things to note in SpringApplication class: 
 *        .. --|---- (1) As mentioned, even the static call SpringApplication.run(..), later ends up calling the non-static run(..) method. So, the above
 *                   .. also applies when running spring-bbot from main() method, in which case, it ends up starting the embedded server, and making context
 *                   .. based on the provided source class (..most likely, the one with @EnableAutoConfiguration configuration)
 *        .. --|---- (2) See createApplicationContext() - in that default app-context-class for web application is 
 *                   .. AnnotationConfigServletWebServerApplicationContext
 *        .. --|---- (3) Continuing on #2 above, Notice that there is an app-context-Type of AnnotationConfigApplicationContext, when SpringBoot is to  
 *                   .. make an appContext that is not used for any web-application at all
 *        .. --|---- (4) See child(..) method in SpringApplicationBuilder -- it has a webType of NONE. From above, we now understand that Spring won't 
 *                   .. be able to bootstrap itself to a server it creates by its own self if using this configuration. **IMPORTANT**: This means that 
 *                   .. Spring Boot loads both the root and web-context as one. It does NOT make 2 different contexts as done in examples in 
 *                   .. WebApplicationInitializer-javadoc.
 *        .. --|---- (5) Continuing on #4 above, realize that one can still register multiple dispatcher servlets in Spring boot by making them as beans 
 *                   .. and registering them using ServletRegistratioBean
 *        .. END-OF-DIGRESSION]
 * --|---- v.iii) Maybe also see https://www.baeldung.com/spring-web-contexts
 *
 * .. vi) "registerErrorPageFilter": NOTE that there is "registerErrorPageFilter" property which is set to true for basic Spring boot startup. This adds
 * .. "ErrorPageFilterConfiguration" file in source, which, by its Github code, just add the "ErrorPageFilter" bean. So what?
 * --|---- vi.i) We know by Spring boot docs that it is able to handle exceptions also raised at filter level where there won't be access to
 *               @ControllerAdvice (See https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-error-handling)
 * --|---- vi.ii) We know by another Spring boot doc link that this filter is configured very high up in chain, so that it is able to catch everything
 *               (See https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-embedded-container-servlets-filters-listeners-beans)
 *
 * .. vii) run() is called which runs the SpringApplication. As mentioned earlier, the static SpringApplication.run() method also end up calling non-static run() method.
 * .. So just studying the non-static method is sufficient
 * .. NOTE: Fore more on SpringApplication, see: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-spring-application.html
 * --|---- vii.A) To begin with, a bunch of listeners that listen to SpringApplication.run are identified and a message is sent to them that the process is starting. 
 * .. This is before the try{..} block starts within the run()
 * --|---- vii.B) prepareEnvironment() is called, which:
 * --|----|---- vii.B.1) getOrCreateEnvironment(): Instantiates a configurable environment if one is not already present
 * --|----|---- vii.B.2) configureEnvironment() is called: 
 * --|----|----|---- It starts by getting conversion-service which may be necessary to convert properties. 
 * --|----|----|---- configurePropertySources() is called. It starts by including any default properties added to SpringApplication while building it. Then it adds 
 * .. command line properties, if provided to SpringApplication via the builder
 * --|----|----|---- configureProfiles() is called which sets profile  as provided in system variable or in command line arguments. NOTE that this still does not 
 * .. read the application.properties file
 * --|----|---- vii.B.3) listeners are told that the environment is prepared, i.e. it is available. HOWEVER, note that it won't have all properties as yet. 
 * .. Probably the info on active profile is what'd be the most useful information it has
 * --|----|---- vii.B.4) bindToSpringApplication() is called -- which is just this code: "Binder.get(environment).bind("spring.main", Bindable.ofInstance(this));"
 * .. "Binder" is defined as "A container object which Binds objects from one or more ConfigurationPropertySources." -- Recall how spring has 
 * .. @ConfigurationPropertySource which can be used to read properties which are then made into objects. Essentially, what it does is any properties 
 * .. provided in environment so far (via command line, or as default args, but not in application.properties).. those are read, and any of those starting with
 * .. "spring.main" prefix are use for configuring the SpringApplication. Notice that same properties (i.e. starting with "spring.main") are also available to be
 * .. read from application.properties and can be used for configuring SpringApplication. See https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html
 * --|----|---- vii.B.5) "ConfigurationPropertySources.attach(environment);" -- I'm guessing that this makes "environment" as source for configuration-property
 * .. that can be used by any other classes down the line
 * --|---- vii.C) The Spring-banner is made - that shows up at start of deployment
 * --|---- vii.D) createApplicationContext(): Instantiates the empty application context that will be used. NOTE: the application.properties haven't been read as yet.
 * --|---- vii.E) getSpringFactoriesInstances(): Loads the spring-boot- exception reporters. Per jaadoc, they are "Callback interface used to support custom
 * .. reporting of SpringApplication startup errors."
 * --|---- vii.F) prepareContext() is called, which:
 * --|----|---- vii.F.1) set the environment to app-context
 * --|----|---- vii.F.2) postProcessApplicationContext() is called which makes few more settings.. but nothing yet related to application.properties, so not going deeper
 * --|----|---- vii.F.3) applyInitializers() is called to apply any ApplicationContextInitializer made (See point#vi above. Spring boot adds a few app-context-initializers)
 * --|----|---- vii.F.4) SpringApplication listeners are told that app-context is "prepared"
 * --|----|---- vii.F.5) Few more things done.. not of much interest.. so ignoring it. Before starting with next call made to load() method, notice that it tries to
 * .. get all "sources" for the SpringApplication. In this case, the source is the class with @SpringBootApplication annotation (We explicitly did this change, i.e. added 
 * .. the source, in class extending SpringBootServletInitializer, with the intention to bootstrap). load() is when Spring reads the various **bean-definition** from 
 * .. sources provided to it. I'm not sure if the properties are also read.. I think they are! **HOWEVER** the good thing is that Spring-Application initialization has added 
 * .. logic to call refersh() on the app-context after that point. But before it does so, the environment can be changed by "EnvironmentPostProcessor" -- and so, when
 * .. refresh() is called on app-context, then it loads new properties in @Value annotation.
 *
 *
 *
 *
 * DYNAMICALLY CHANGING SPRING PROPERTIES:
 * --|---- On general topic of refreshing spring beans when configuration is changed, see
 * .. @RefreshScope : https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_refresh_scope
 * .. EnvironmentChangeEvent : https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_environment_changes
 *
 * --|---- Note that in Spring cloud, there is the concept of "Bootstrap" application context and corresponding bootstrap-properties, i.e. the properties loaded to a 
 * .. server "BEFORE" an application-context is made. See https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_the_bootstrap_application_context
 * --|----|---- REALIZE that same concept can even be understood in terms of DAG chart in Spring Boot. In this case, it essentially means that we keep the
 * .. SpringApplication source class in a different package, and pass packageNames to it identifying @Configuration classes that should be invoked. Generally, this is
 * .. bypassed by keeping ApringApplication-source class at top of package structure, in which case, when Sprig-Boot runs, the app-Context makes all classes. This 
 * .. is unlike the 2-level structure as suggested for app-context vs servlet-context. BUT ALSO REMEMBER that in Spring-Boot, having 2 layer structure is hard since
 * .. App-context also has job to load itself when running in embedded mode!!
 * --|----|---- To be able to modify it or add more properties to it before extra "application" properties are added, see
 * .. EnvironmentRepository : https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_environment_repository
 * .. PropertySourceLocator : https://cloud.spring.io/spring-cloud-static/spring-cloud.html#customizing-bootstrap-property-sources)
 *
 * --|---- **VERY VERY VERY IMPORTANT**: In the Spring-Boot load step, if you want to read a property from application.properties file, and use a property in it 
 * .. to load other properties (say, from DB, or another file), and want those new properties to also show up in final deployment, then add an implementation of 
 * .. "EnvironmentPostProcessor". See https://docs.spring.io/spring-boot/docs/current/reference/html/howto-spring-boot-application.html#howto-customize-the-environment-or-application-context
 * .. and particularly the note below the example where it says "The Environment has already been prepared with all the usual property sources that Spring Boot 
 * .. loads by default. It is therefore possible to get {some-property-as-needed} from the environment."
 * --|----|---- ALSO HEED THE WARNING BELOW which says to NEVER put @PropertySource() on same class as having @SpringBootApplication. That's useless!!
 *
 * --|---- On general topic of loading properties from database just one time, it can be done via SpringBoot without needing SpringCloud. See
 * .. https://stackoverflow.com/questions/46407230/load-spring-boot-app-properties-from-database
 * .. https://stackoverflow.com/questions/40465360/spring-boot-use-database-and-application-properties-for-configuration 
 *
 * --|---- One can also use ApplicationContextInitializers - but realize that they are called much earlier and before SpringBoot configuration starts. So, if you want to 
 * .. depend on properties in application.properties file, then using the initializers won't be useful
 *
 * --|---- **IMPORTANT**: Another generic way to make application wide changes was made by Maria-M (and if you recall, also by you for CMS' dynamic log-level
 * .. change). The idea is as follows:
 * --|----|---- (a) Make classes that are extending some interface, it you want each such class to have certain method (that needs to be executed)
 * --|----|---- (b) Make a BeanPostProcessor that, based on some criteria (like, package name, class name, class extend an interface), registers the beans with
 * .. some "central-service"
 * --|----|---- (c) A "central-service" that keeps a registration information, nd has logic for update or triggers
 * --|----|---- (d) An endpoint that exposes the "central-service". Once "central-service" is triggered, it invokes some method in all registered beans causing the 
 * .. application wide desired change!!
 *
 *
 *
 *
 * Changing servlet context values in Spring boot
 * --|---- Either when deploying boot in embedded server or other container, it is possible to customize the servletContext by using 
 * .. ServletContextInitializer bean. As example, see https://stackoverflow.com/questions/26639475/how-to-set-context-param-in-spring-boot
 * --|---- Note that another easier way to make changes is to just add properties in application.properties file
 * 
 * 
 *
 * What is Spring Boot Banner?
 * --|---- Note that when Spring boot starts, it shows an "image" of SpringBoot made with ascii symbols. That is the banner. It can be 
 * .. configured whether that is shown or not, and what to show
 * 
 * 
 *
 * **IMPORTANT** NICE.. ApplicationRunner and CommandLineRunner
 * THE MAIN REQUIREMENT here is to run certan pieces of code after the applicationContext has been configured.
 * --|---- When calling Application.run(..) for Spring Boot, it takes the initializing class and also a bunch of arguments. Those are the arguments 
 * .. that are passed down to ApplicationRunner and CommandLineRunner for running. See https://dzone.com/articles/spring-boot-applicationrunner-and-commandlinerunne   
 * .. and    https://www.concretepage.com/spring-boot/spring-boot-commandlinerunner-and-applicationrunner-example
 * --|---- Another way to do such thing would be to make a listener on ApplicationReadyEvent. See https://stackoverflow.com/questions/27405713/running-code-after-spring-boot-starts
 * --|---- DO NOT DO THESE:
 * --|----|---- Adding the logic-to-be-executed after static SpringApplication.run(..) statement. It may run in embedded mode but not when deploying on server
 * --|----|---- Make a class and autowire application-context or dispatcher servlet in it. This may run improperly, since the bean may be attached before 
 * .. all properties are configured and bean refreshed
 *
 *
 *
 * ON SPRING-BOOT DEPLOYMENT:
 * ---- See https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-programmatic-embedded-container-customization   
 * .. and   https://stackoverflow.com/questions/20405474/add-context-path-to-spring-boot-application   on being able to customize Spring boot embedded 
 * .. server - if needed. However, best is to do it via properties! - So that the same changes go to all environments
 * --|---- **IMPORTANT**: It is possible to enable JNDI when running Spring boot in embedded server. See https://stackoverflow.com/questions/24941829/how-to-create-jndi-context-in-spring-boot-with-embedded-tomcat-container
 * ----|---- Can also see https://www.baeldung.com/spring-boot-application-configuration   and   https://stackoverflow.com/questions/47832999/embeddedservletcontainercustomizer-and-configurableembeddedservletcontainer-in-s
 * 
 * ---- **IMPORTANT**: Note that Spring makes "BuildProperties" bean through which one can get build info (can also do it via maven-properties-plugin 
 * .. as is done in your code.. but this is also an option!). See https://docs.spring.io/spring-boot/docs/current/reference/html/howto-build.html 
 * .. and https://www.vojtechruzicka.com/spring-boot-version/
 *
 * References on deployment:
 * ----|---- See end comments in EtceteraController for deployment in JBoss
 * ----|---- https://spring.io/blog/2014/03/07/deploying-spring-boot-applications
 * ----|---- https://docs.spring.io/spring-boot/docs/current/reference/html/howto-traditional-deployment.html
 * ----|---- https://thepracticaldeveloper.com/2018/08/06/how-to-deploy-a-spring-boot-war-in-wildfly-jboss/  (..shows deployments with Java 10) 
 *
 *
 *
 * **OTHER IMPORTANT ITEMS**:
 * ---- **VERY VERY IMPORTANT**: See last para in https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-embedded-container-servlets-filters-listeners  
 * .. regarding FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER -- as per docs, "If a specific order is required, you should avoid 
 * .. configuring a Filter that reads the request body at Ordered.HIGHEST_PRECEDENCE, since it might go against the character encoding 
 * .. configuration of your application. If a Servlet filter wraps the request, it should be configured with an order that is less than or equal 
 * .. to FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER." -- Related to it, see the jira: https://github.com/spring-projects/spring-boot/issues/3613
 * ----|---- ON RELATED NOTE (to filter ordering in Spring Boot): Note that as shown in the documentation url.. Spring boot adds certain
 * .. filters with highest_order precedence. It is best that if you want to add some high order filters, then at least add it at HighestOrder "less" 2 (meaning, 
 * .. Integer.MIN_VALUE+2) precedence so as to not mess with Spring Boot's order (unless you are sure that going higher in precedence is ok)
 * ----|---- **VERY VERY IMPORTANT**: See the stackOverflow post asking on how to order filter after security-filter in SpringBoot
 * .. https://stackoverflow.com/questions/25957879/filter-order-in-spring-boot     The post (..along with the JIRA post referred) mentions that SpringBoot 
 * .. creates SpringSecurity filter with Order of lowest. Following inferences/relations can now be made (not all of which is visible from post). Can also refer to
 * .. https://stackoverflow.com/questions/24364436/adding-a-custom-filter-to-be-invoked-after-spring-security-filter-in-a-servlet-3
 * ----|----|---- First, the direct result is that the post shows that: (a) Spring Boot makes Spring-Security filter with Lowest-Order; (b) It is possible to change order
 * .. by making custom filter-registration for the security filter. This way, user can now control filters applied before or after the Spring-security filter bean
 * ----|----|---- Now.. realize that SpringSecurity makes a FilterChain.. so one way to add own filter in a given order is by controlling its location in the Security
 * .. chain. DO RECALL that if following this route then your own filters should NOT be given @Component or not explicitly registered in SpringBoot - because
 * .. they are now invoked as part of SpringSecurity chain
 * ----|----|---- Other thing to realize is that the post says SpringSecurity Filter is added at Lowest-priority order. This automatically makes it comply with 
 * .. FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER because SpringSecurity works with wrapped request/response
 * ----|----|---- See above for notes on "AbstractAnnotationConfigDispatcherServletInitializer". Realize that SpringSecurity has "AbstractSecurityWebApplicationInitializer"
 * .. to register DelegateSecurityFilter to web.xml. This class if of type WebApplicationInitializer, and so is auto-picked during Spring web-app deployment. So, just like
 * .. how we discussed about "AbstractAnnotationConfigDispatcherServletInitializer", same applies also to "AbstractSecurityWebApplicationInitializer", i.e. in a 
 * .. web-deployment that in Servlet-3 based, but does not use SpringBoot, then "AbstractSecurityWebApplicationInitializer" can be used to control filter registration.
 * .. However, in Spring Boot, there won't be a need to have "AbstractSecurityWebApplicationInitializer", since SpringBoot does the correspnding job
 
 * 
 * ---- **IMPORTANT**: See the difference between interface vs cglib proxying mechanism (https://stackoverflow.com/questions/10664182/what-is-the-difference-between-jdk-dynamic-proxy-and-cglib)
 * 
 * ---- Nice article on Spring boot's auto configuration: https://www.springboottutorial.com/spring-boot-auto-configuration
 *
 */

public class EtceteraController2 {

}
