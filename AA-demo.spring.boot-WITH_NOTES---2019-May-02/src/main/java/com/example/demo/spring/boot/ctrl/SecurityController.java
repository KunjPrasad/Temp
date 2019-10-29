package com.example.demo.spring.boot.ctrl;

//@formatter:off
/*
* 
*Basic guide: See https://spring.io/guides/gs/securing-web/
*
*
*
* VERY VERY VERY IMPORTANT: BASIC SPRING-SECURITY TERMINOLOGY:
* 
* 1) To start Spring-Seurity, do @EnableWebSecurity on a @Configuration file, and, to make configs, extend WebSecurityConfigurerAdapter class
* 
* 
* 2) Somethings that **maybe** spring security does not do.. but is good to configure in an application (THESE SHOULD BE SEEN "IN ADDITION"
to security identified in Spring-Security Pg15):
* --|---- **VERY VERY VERY IMPORTANT**: (Also mentioned in EtceteraController and EtceteraController2) Note that Spring has something called
* .. FormContentFilter and HiddenHttpMethodFilter. BEST KEEP THEM DISABLED! ..else it may be a security risk. 
* --|----|---- NOTE: Spring-boot enables them by default, so do configure it to be disabled.. specially, keep HiddenHttpMethodFilter disabled. 
* .. The Spring-boot properties are "spring.mvc.formcontent.filter.enabled" and "spring.mvc.hiddenmethod.filter.enabled"
* --|---- (2.1) setting a max-session timeout! 
* --|----|---- Best is to configure following properties for Spring boot [server.servlet.session.cookie.domain=...; 
* .. server.servlet.session.cookie.http-only=...; server.servlet.session.cookie.max-age=...; server.servlet.session.cookie.secure=...;
* .. server.servlet.session.timeout=...; server.servlet.session.tracking-modes=cookie (never use url - that is a security flaw, meaning that 
* .. sessionId will be exposed when doing redirects)]
* --|---- (2.2) when giving csrfToken / localStorage security token.. (and depending on application), accept the token only if user 
* .. returns it within a time-interval, i.e. let it have a timeout - but don't make it a cookie, else cookie automatically gets passed. 
* .. BUT, do realize that with this model, the burden comes on user to always be doing some sort of authentication
* --|---- (2.3) Alongwith #(2.2), if possible, also make sure that the remote ip does not change rapidly within a "small" time interval. 
* .. While this can give security, it means that if users go around proxy connection, then they'll get logged out - maybe just allow them 
* .. to configure this setting! 
* --|---- (2.4) For cookies, always set an expiry. ALSO, always use "HTTPOnly", "Secure" and "SameSite" as default for all cookies. If 
* .. needed by application to not put HttpOnly and/or SameSite - then maybe also name this custom cookies with a naming convention so 
* .. that any defects are easily identified
* --|---- (2.5) Just like #(2.4), For sensitive cookies, use a particular naming cnvention to easily identify it --and-- make sure to encrypt 
* .. the cookie value.. and I mean "encrypt", not just simple Base64 "encode", and specially to never store as is!! -- along same lines, 
* .. sensitive cookies should always be HTTPOnly, Secure ..and maybe always SAMESITE. 
* --|---- (2.6) For control on serialization/deserialization, use @JsonProperty(access=Access. READ_ONLY) if the property should not be 
* .. deserializable. If needed, use @JsonIgnore. HOWEVER, do note that it might get necessary to also add @JsonIgnoreProperties(ignoreUnknown=true). 
* .. This is necessary if UI has logic wherein it takes the json returned by service, makes some mods to it, and then returns it to service 
* .. for extra processing. 
--|----|---- ONE HOLD UP is that when doing unit tests.. it might be necessary to convert the returned json text to an object so that 
* .. assertions can be made on it. However having access-control on fields will prevent such procedure for testing. IN SUCH CASES.. 
* .. it might be useful to instead make tests with jsonPath and related json assertions!
* --|---- (2.7) COntinuing on deserialization control.. it is a good practice that for STRING field entries, necessary validation patterns 
* .. are added as constraints if it is expected that the field will only be alpha, or alphaNum, etc. Another option is to make a totally 
* .. new class and define its serialization/de-serialization/default-value. MAINLY, this prevent html script injection!
* --|---- (2.8) Regarding UI.. use "Content-Security-Policy" meta tags. See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy
* --|---- (2.9) As mentioned in Pg.101 of sepring-security reference doc, rather than relying much on url-level security configuration in 
* .. url-matchers and in single auth-configurer class.. it is better to use method-level security annotations, since url(s) can change!
*
*
* 
* 3) **VERY VERY VERY IMPORTANT**: Difference between "Authentication", "Principal" and "UserDetails" object -- **AND SOURCE OF CONFUSION**
* --|---- To begin with, understand the terms. See https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/glossary.html
* --|---- Now.. See javadoc of "Authentication". It says: "Authentication" object represents the token for an authentication request, or, 
* .. token for an authenticated principal if the request has been successfully processed by the AuthenticationManager.authenticate(...) method. So, Authentication"
* .. is just a representation -- if the request is not yet authenticated, then it contains details sent by user (credentials + no principal information). When request
* .. has been authenticated, then Authentication contains other details along with credentials, like prinicipal, authority, userDetails.. see javadoc
* --|---- The murkiness starts: Technically, Principal is the identification of user based on authentication-process done on provided credentials. However,
* .. as said on Pg.61 of pdf, Spring-Security returns a "UserDetails" object as Authentication token, after successful authentication. What is confusing is that
* .. Authentication interface has a getDetails() method, separate from getPrincipal() - which is a UserDetails type object!
* --|---- Suggestion: ..is to follow the definition of Principal as said in docs-oracle; And to make a custom userDetails object following the interface. If needing
* .. to store extra contextual information.. put that in getDetails() method, either under a separate object.. or such that the underlying object implements
* .. userDetails and also gives extra fields - each served by corresponding methods. Just be consistent in design and interpretation. Even spring-security docs
* .. say so in next section "Being a representation of something from your own user database, quite often you will cast the UserDetails to the original object 
* .. that your application provided, so you can call business-specific methods (like getEmail(), getEmployeeNumber() and so on)."
* --|---- Suggestion#2: Look at javadoc of ProviderManager. Since it can remove password after authentication if the Authentication object implements 
* .. CredentialsContainer interface, so best to also do that and clear password!
*
* 
* 
* 4) **VERY VERY VERY IMPORTANT**: Difference between AuthenticationManager, AuthenticationProvider, AuthenticationManagerBuilder, 
* .. and UserDetailsService. And how does authentication procedure work?
*
* --|---- AUTHENTICATIONMANAGER: is an interface, and is the main Spring object that does the authentication. Spring provides only 1 implementation 
* .. for it called "ProviderManager" (iportant - see javadoc). Take note of name-confusion -- "ProviderManager" is actually a AuthenticationManager 
* .. implementation, but is called so because it is made of list of AuthenticationProviders. **ALSO NOTE**: ProviderManager allows setting to clear 
* .. credentials after auth ..and also messageSource to internationalize fail message
*
* --|---- AUTHENTICATIONPROVIDER : This is an individual autheticating component. ProviderManager works based on rule that if at least one 
* .. of the authenticationProviders (authPrvdr) give a non-null response and not throw exception, then the user-provided authentication is seen 
* .. as "validated". Best way to understand authPrvdr is to see code of AbstractUserDetailsAuthenticationProvider, or, DaoAuthenticationProvider.
* .. Each authPrvdr contains an instance of USERDETAILSSERVICE and PASSWORDENCODER. authPrvdr uses incoming "Authentication" object given by user
* .. to get userName, which is sent to UserDetailsService to get "UserDetails" information - within which is also the password associated to user, 
* .. say, as stored in DB. [[A digression.. it is not a good idea to store raw-user provided password in DB. This is where Password Encoder comes in.
* .. It encodes raw user-password before storing it in DB. And so, same encoder needs to be used when comparing the original-stored encoded user-password
* .. against the one being provided now for authentication. This is why when storing in-memory-users, or jdbc-users, then the encoder should be 
* .. applied to raw password before storing them]]. The authPrvdr then uses the UserDetails, alongwith the associated passwordEncoder to
* .. identify whether the Authentication is correct.. and if so, it should return same/enhanced/other-with-similar-details Authentication object.
* .. See code for AbstractUserDetailsAuthenticationProvider.createSuccessAuthentication(..), wherein, after successful authentication, it returns
* .. new Auth object where principal is the "UserDetails". The advantage of doing so that "getDetails()" method of new object preserves same 
* .. information as that in original auth-object. If not match, then it can return null or throw exception (better choice). 
* --|----|---- SO: (a) When making own logic, you should make a new "AuthenticationProvider", (b) Remember that when saving user-provided password 
* .. for first time, then encode it with encoder before saving, so that you don't ever save raw user password (like, even done in mRemoteNG when 
* .. saving user pwd) - if it leaks, say via logs, it will be hassle. 
*
* --|---- AUTHENTICATIONMANAGERBUILDER: From above, we understand that AuthenticationManager is the important entity, and is configurable.
* .. So, best way to make it is to have this "builder" that takes various options.. so that coders need not worry about making proper AuthManager
* .. Example for above, see https://www.baeldung.com/spring-security-multiple-auth-providers
*
* --|---- **VERY VERY IMPORTANT**:  Regarding how, it all works.. also look at #5.1 and 5.2 below on DelegatingFilterProxy and FilterComparator.
* --|----|---- SIMPLY, look at Pg72, chapter 10 "Core Services" of spring security docs...
* --|----|---- Realize that the actual auth is done by various filters configured by Spring. UsernamePasswordAuthenticationFilter (as example) and other
* autheticating filters actually extend "AbstractAuthenticationProcessingFilter", which has main logic in doFilter() method. HOWEVER, the abstract 
* class allows the sub-classes to implement their own authentication method. This shows how even the different filters on the security chain have
* similar common behavior, but just have difference in how they do authentication.
* --|----|---- A DIGRESSION: Look at "AbstractUserDetailsAuthenticationProvider" abstract class.. this is a clas with core components on how 
* to make a custom auth-token like the usernamePasswordAuthToken. So, to make custom authentication, users can probably extend the base abstract class,
* add code on how to implement userDetails and how to authenticate!
* --|----|---- Joing the above 2 concepts: 
* --|----|----|---- EITHER: coders can write custom AbstractUserDetailsAuthenticationProvider class and add it to AuthMgrBldr, and add a formLogin 
* that uses user-pwd for authentication. When user tries to authenticate to that url, they can now give details in new format. Spring security will 
* call usernamePwdFilter - which will call authMgr as configured by user -- which will have a new authProvider to authenticate the details in new 
* form. REALIZE THAT.. this gives users new way to authenticate, but the authetication will still only be done by usernamePwd filter set on login page
* --|----|----|---- OR: coders can just make a new filter AND add that to filter chain. Here, there is advantage that they can code however they want!
* As long as finally one auth object goes in SecurityContext, Spring is happy!
* --|----|----|---- OR: Can do a mix, where they make new authProviders, and before it, add new filters that prepare auth-token token in correct form
* by parsing user request. See https://stackoverflow.com/questions/25794680/multiple-authentication-mechanisms-in-a-single-app-using-java-config
* --|----|---- HOWEVER, WHAT YOU CANNOT DO: is say that you setup a userPwdAuthFilter, and, say, BasicAuthFilter, then go to /login page and expect that
* either you give username and password or you give basic-auth-header, then both ends up authenticating without requiring any extra setup. This won't work
* because as soon as usernamePwdFilter is hit, it'll want to see username-pasword token, and if those are absent then it will cause error. For above to
* work, there needs to be another filter before username-pwd-auth-filter that takes basic-header and converts it to username-pwd-token!! 
* THE POINT IS.. all the websites that say to just add new authProvider and things will start working.. it won't if auth-mechanism is totally different.
* It only works this way when you want to add new "providers" to authenticate on same auth-token-type
* 
*
* 
* 
* 5) **MORE INFO ON HOW SPRING SECURITY WORKS**:
* 
* ** A VERY NICE LINK TO UNDERSTAND SECURITY:
* --|---- https://spring.io/guides/topicals/spring-security-architecture/
* --|----|---- IMPORTANT: NOTE the fact that all filters internal to Spring Security are internal within the DelegateFilterProxy and are thus unknown to the container. 
* .. This becomes especially important, especially in a Spring Boot application, where all @Beans of type Filter are registered automatically with the container by 
* .. default. So if you want to add a custom security logic in SpringBoot, then either (a) register the filter as bean but have it be outside Spring Security and do NOT 
* .. doubly add that as part of DelegateFilterProxy chain filter, or, (b) add the filter inside the delegate filter proxy, but then don't register it as a bean and also have 
* .. SpringBoot add it outside the security. DO REALIZE that in case-b, the standard bean lifecycle methods will NOT be invoked on the filters - which you'll need to 
* .. manage on own, if you want!
* --|----|---- Processing Secure Methods Asynchronously: Since the SecurityContext is thread bound, if you want to do any background processing 
* .. that calls secure methods, e.g. with @Async, you need to ensure that the context is propagated. This boils down to wrapping the SecurityContext 
* .. up with the task (Runnable, Callable etc.) that is executed in the background. Spring Security provides some helpers to make this easier, 
* .. such as wrappers for Runnable and Callable. To propagate the SecurityContext to @Async methods you need to supply an AsyncConfigurer and 
* .. ensure the Executor is of the correct type:
					@Configuration
					public class ApplicationConfiguration extends AsyncConfigurerSupport {
						@Override
						public Executor getAsyncExecutor() {
							return new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(5));
						}
					}
* --|---- https://stackoverflow.com/questions/41480102/how-spring-security-filter-chain-works
* 
*
* (5.1) DelegatingFilterProxy, FilterChainProxy, and, "security filter chain":
* --|---- See Pg98, sections on DelegatingFilterProxy and FilterChainProxy, in "The Securtity Filter Chain" chapter (in Part-4: 
* .. Web application security), in Spring security refrence pdf
* --|---- DETAILS:
* .. (a) The way security works is that some "operations" should be done for every request. So, security needs to be implemneted by a filter
* .. (b) If it is a filter, then it means it needs to be registered in web.xml. OR, we need to create a filter-registration during the startup, 
* .. when app-context is being made and bound to servlet-context. THIS IS WHERE THE FIRST LEVEL OF SPRING-SECURITY ABSTRACTION COMES!! We tell 
* .. web.xml that we'll apply a filter to all request - but internally, this filter is hollow, and "delegates" the job to do security to another 
* .. filter bean. This way, web.xml registers the filter, and knows to apply it to every request.. but that filter in itself is hollow, and just 
* .. delegates to a bean created by Spring. This is how a "Spring-managed" filter-bean(s) is/are "bootstrapped" to web.xml, via a proxy! This 
* .. proxy-filter is the DelegatingFilterProxy.. that simply delegates the job of security to other filters.
* .. (c) Next in line is "FilterChainProxy" -- which, for a given url-pattern, identifies the filter-sequence to use (i.e. the filters through 
* .. which the request should be passed). This "spring-bean" is the one that takes the task of applying security, delegated to it from DelegatingFilterProxy). 
* .. Be careful in interpreting the name: "FilterChainProxy" is a proxy seen by incoming request.. through which the incoming request is channeled to a 
* .. particular security-filter-chain (each chain consisting of list of spring-managed-beans). And so, the "FilterChainProxy" in itself contains multiple such chains! 
* .. See Pg99 of pdf for a sample xml structure of FilterChainProxy
* .. --|----|---- So how does "WebSecurityConfigurerAdapter" class relate to "FilterChainProxy"? Answer: Based on user provided configuartions.. the 
* .. "WebSecurityConfigurerAdapter" class makes WebSecurityConfiguration, which has the "springSecurityFilterChain" bean which is the the "FilterChainProxy", 
* .. and gets used by "DelegatingFilterProxy" (see WebSecurityConfiguration api, which itself is made by WebSecurityConfigurerAdapter)
* .. --|----|---- We also see an advantage of the "DelegatingFilterProxy --> FilterChainProxy" architecture, in that this helps us to write just 
* .. one filter in web.xml, but generalize it further as needed.. through settings of FilterChainProxy in application-context
* .. --|----|---- Another advantage of this architecture is that we can make diffrent "FilterProxyChain" with different order, to control the precedence 
* .. of rule-applicaton. In line with above.. this means that one can have multiple "springSecurityFilterChain" (corresponding to different intercept 
* .. rule on http-element; NOTE:.. intercept on http, not on authorize!!) from different "WebMvcConfigurer" class, and, "DelegatingFilterProxy" 
* .. collects them.. and so, there is no need to add them all to web.xml, cluttering it.
* .. --|----|---- Each http-element in security-configurer creates a filter chain within the internal FilterChainProxy (i.e. springSecurityFilterChain 
* .. bean) and the corresponding URL pattern that should be mapped to it. These chains are added in the order they are declared, so the most specific
* .. patterns must again be declared first.
* .. --|---- Since above architecture requires that before the first request is served, ..during application startup, there really should be a bean 
* .. named "springSecurityFilterChain", else the "DelegatingFilterProxy" will be unable to find it, and security won't process. In such cases, 
* .. sometime spring may give error message of "No bean named ‘springSecurityFilterChain’ is defined" which happens if configuration is not 
* .. properly made, see https://www.baeldung.com/no-bean-named-springsecurityfilterchain-is-defined
* .. (d) **VERY VERY IMPORTANT**: 
* .. --|----|---- (i) Since filters are added in "FilterProxyChain" proxy.. it is coder's job to ensure that any necessary lifecyle 
* .. method (like init, post-construct, etc) are already done before it is added to in chain. Spring security does not do any lifecycle activities 
* .. on the filter bean.. it just takes the bean, and sets it in chain. 
* .. --|----|---- (ii) Make sure to not auto-register a filter-bean in Spring Boot, else it will get doubly applied, once inside Spring security 
* .. chain, and other time when teh filter is invoked in processing chain by itself.. because it registered itself in SpringBoot app-context
*
*
* (5.2) FilterComparator: See FilterComparator java code in github. It lists the various filters that Spring-Security can choose from when making
* a chain. 
* --|---- The actual chain it end up making depends on how user has done the configuration (See in example of https://stackoverflow.com/questions/41480102/how-spring-security-filter-chain-works)
* --|---- Note that Spring security allows user to add their custom filter before one of the filters in the chain (by method .addFilterBefore(..)  and/or addFilterAfter(..) 
* --|---- **VERY IMPORTANT** See notes in https://spring.io/guides/topicals/spring-security-architecture/ for additional information on ordering of filters.
* .. It also mentions some ordering-constants used by security
* --|---- Additional good read is from spring-security docs: https://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#filter-ordering
* .. also on Pg100 of security-reference pdf
*
*
* (5.3) HOW IT ALL WORKS?
* --|---- In Spring-security-config, note that there are 4 possible settings: (1)permitAll, (2)hasRole, (3)access, (4) authenticated (See Pg 18 of 
* .. the spring security docs). "access" is a combination of other factors, so is not discussed for now. 
* --|----|---- FIRST: permitAll() bypasses security chain altogether, so no need to go further there. This leaves with 2 options to understand, hasRole() 
* .. and authenticated(). 
* --|----|---- Ideally, security discussion requires starting with Spring identifying the security chain to apply based on url.. as identified by delegatingFilter. Let's 
* .. say we are at a point where chain has been identified. Now, how are the filters set by Spring **AND** how it relates to the configurations put in by user.
*
* --|---- The first main step is adding securityContextRepository, which can set SecurityContext for session.. if so configured. Also, as mentioned, 
* .. this is also the one that finally clears context after requets has been processed. Also, it does so based on startegy!!
*
* --|---- NOW.. if this is the form-login authenticating page, then a userNamePasswordFilter is added -- only for that page, which gives web-app the ability to 
* .. read user input, identify Auth token, and save it in SecurityContext if valid. If securityContextRepository is set, then this token finally gets persisted at end 
* .. of request. Thus, when same session user comes back, the token will be pulled when user continues the session, even for "ANY, and ALL" other page
* .. (except those that have permitAll()).
* --|----|---- See Pg 50 of Spring security doc pdf, it says "..the UsernamePasswordAuthenticationFilter which is created by the <form-login> element"
* --|----|---- Important information for security-chain-understanding, see extra-notes below regarding "SessionAuthenticationStrategy"
*
* --|---- There is RememberMe filter
*
* --|---- Now, anonymous-auth is added by default for Spring-Security-3, whose job is to add an anon-security-token, if context has no entry up to that point. 
* .. The behavior of anon-auth can be controlled, or this can even be disabled, by anonymous() portion of httpSecurity (See https://docs.spring.io/spring-security/site/docs/4.0.2.RELEASE/reference/htmlsingle/#nsa-anonymous). 
* .. NOTE that the anon-auth-token set by "AnonymousAuthenticationFilter" has isAuthenticated property set to true (See github code and also https://stackoverflow.com/questions/23340433/spring-using-anonymous-authentication-provider-to-use-guest-user ). So, when the user goes to any other 
* .. endpoint, spring-security loads the same token, which is the anon-token, which has "isAuthenticated"=true. This allows the same anon-user to visit all other
* .. endpoints (as long as they are allowed for anonymous role, or if anon-user goes onto login page and hits the username/pwd filter instead)
* .. [[More on Anon-auth below in "Other details" section]]. ONE MORE THING TO NOTE, the userDetails for anon-token contains the sessionId, 
* .. remote-ip, etc. Check github for createAuthentication() method of AnonymousAuthenticationFilter, which uses WebAuthenticationDetailsSource,
* .. and pick up from there.
*
* --|---- After it, then the authorize-control portion comes in. This is where effectively all checks happen. So, if we configure in Spring-security for a page to 
* .. be secured with ROLE_USER, this does not mean that it necessarily add a usernamePasswordFilter, or any authenticator, etc. Spring-security, essentially 
* .. leaves it be.. until it comes to authorize control. NOW.. authorizer checks!! If a page was indeed to be secured, but instead got an auth-token with 
* .. ROLE_ANONYMOUS from SecurityContext (say, put there by the default anon-filter), then an authorization-error is raised (not authentication-error). 
* .. Similarly, if an user authenticates, but has wrong authority, then this is where they get disallowed! So, all checks added in Spring-security-config, doing so 
* .. does not mean Spring will add an authentication filter to all those pages.. but it does add an authority-check, and if that check fails, then error is raised.
*
* ***AND THIS IS HOW SECURITY CONFIGUARTION given by user MAPS TO FILTER CHAIN of Spring!***
*
* --|---- EXTRA NOTES #1: Another option could be to add BasicAuth using request-header. With this addition, the request can now be made stateless. 
* .. Statelessness is not possible with userNamePassword-auth, since the authenticator is only added on form-page. As said on Pg 44 in last
* .. paragraph before "Setting a Default Post-Login Destination" section, it is possible to have both basic-auth and form-login, where, basic
* .. auth will stay on each page, and form login will only go on one page. However, it is user-responsibiliy to ensure that they interact properly
*
* --|---- EXTRA NOTES #2: Within the filter chain, there are 2 session-managing filters: ConcurrentSessionFilter and SessionManagementFilter
* --|----|---- To see SessionManagementFilter, see page 50 of SepringSecurity-docs-pdf. It may not be shown in filterChain
* --|----|---- ConcurrentSessionFilter has to do with concurrent-session management. See Pg.144 of SpringSecurity-docs-pdf. As said, it is actually a
* .. SessionAuthenticationStrategy class that does the job of marking sessions belonging to same user and crossing a threshold. Once marked, when user
* .. comes back from that session, then ConcurrentSessionFilter logs the user out. Look at its Github code, it uses "SessionInformation", and per the javadoc,
* .. {{"[Spring categorizes] Sessions have three states: active, expired, and destroyed. A session that is invalidated by session.invalidate().. is considered "destroyed". 
* .. An "expired" session, on the other hand, is a session that Spring Security wants to end because it was selected for removal for some reason (generally as it was 
* .. the least recently used session and the maximum sessions for the user were reached). An "expired" session is removed as soon as possible by a Filter}}
* .. The point is, when reading the code of ConcurrentSessionFilter and seeing keyword "expired", that does not mean session.invalidate()!
* --|----|---- SessionManagementFilter is used for Session-management. So, it is understandable that it has SessionAuthenticationStrategy. However, Pg.143 of
* .. Security-docs pdf says "SessionAuthenticationStrategy is used by both SessionManagementFilter and AbstractAuthenticationProcessingFilter". An example of 
* .. "AbstractAuthenticationProcessingFilter" is "UsernamePasswordAuthenticationFilter". So why should "AbstractAuthenticationProcessingFilter" have logical
* .. dependence on "SessionAuthenticationStrategy"? 
* --|----|----|---- FIRST, look at Github code of "AbstractAuthenticationProcessingFilter" - once it does filter and is successful, there is no call to let the filter-chain
* .. continue. This could be understod that the role of endpoint was to do authentication. So, once the filter does the authentication, there is no more reason in
* .. letting the request, and so, "AbstractAuthenticationProcessingFilter" does not continue the filter chain. However, we also need to do session-management after
* .. the login.. and that's why "SessionAuthenticationStrategy" is added as a dependency. Also look at footnote on Pg.143 of SpringSecurity-docs-pdf which says
* .. "Authentication by mechanisms which perform a redirect after authenticating (such as form-login) will **NOT BE DETECTED** by SessionManagementFilter,  
* .. as the filter will not be invoked during the authenticating request. Session-management functionality has to be handled separately in these cases."
* --|----|----|---- NEXT, See Pg.143 of SpringSecurity-docs-pdf, it says "The SessionManagementFilter checks ...to determine whether a user has been authenticated 
* .. during the current request, typically by a non-interactive authentication mechanism, such as pre-authentication or remember-me". In conjunction with above
* .. point, the thing to note is that SessionManagementFilter comes in play for cases where there is no "AbstractAuthenticationProcessingFilter", whose current
* .. subclasses are "CasAuthenticationFilter, OpenIDAuthenticationFilter, UsernamePasswordAuthenticationFilter". This can be during Remember-Me, or if a user is 
* .. verified externally (like Rbac, Kerberos). During such cases, authentication is already done by external system.. so we now want to put responsiblity of only the
* .. session-management on Spring, and that's where this filter comes
*
* --|---- EXTRA NOTES#3: **VERY VERY IMPORTANT** RunAsManager:  See https://dzone.com/articles/spring-security-run-example
* --|----|---- While the Spring-security-pdf docs give idea about it, I feel it does not do as good of a job there. Best, look at Github code of "FilterSecurityInterceptor"
* .. which IS the class that does authorization check. Notice how it calls in sequence: beforeInvocation(), continuation of filter-chain (i.e. moving to controller..), 
* .. finallyInvocation() and afterInvocation() methods of "AbstractSecurityInterceptor". This identifies the actual logic that happens and I feel this gives more abilities
* .. than what is let out in the pdf docs. 
* --|----|----|---- beforeInvocation() does the actual authorization using voters. "THEN", it calls RunAsManager to get token - if a non-null value
* .. is received then that is set in SecurityContext, while creating a way to undo that before control returns - because remember that if a different a different token
* .. makes all its way up the "authentication" filter chain (not "authorization") to SecurityContextRepository - that will be stored there permanently which we don't 
* .. want (since Run-As is not same as changing user authentication permanently).
* --|----|----|---- As said above, we never want the securityContext to hold changed token.. so finallyInvocation() has job to replace the token to original value.
* --|----|----|---- afterInvocation() is where the AfterInvocationManager runs -- which means that this can never work with RunAsManager!! I would suggest to 
* .. best not touch it.
* --|----|----|---- The custom RunAsManager implementation in Spring, i.e., RunAsManagerImpl, has logic to add new ROLE_RUN_AS.. grant-authority. I think
* .. this is not a good logic. We don't just want to give any RunAs.. to anyone. **ALSO** when doing a "RunAs", the original grant-authority of user should get
* .. totally hidden. **VERY VERY IMPORTANT**: I suggest making a new RunAsManager, such that, (i) It reads from a requestAttribute to identify the run-as role
* .. requested by user; (ii) verify that the request runAs-role is allowed because it is of lower hierarchy, either globally, or at least for that call; (iii) In making new
* .. RunAs-Token, it should only put the new runAs-role (i.e. if ROLE_ADMIN wants to RUN-AS-USER, then put ROLE_USER in new token.. don't have a separate 
* .. ROLE_RUN_AS_USER, and don't also put ROLE_ADMIN in grant-authority list). THE REASON BEING - now, as the request proceeds whatever happens will
* .. consistently see the user/token as the Run-As-role, without anywhere seeing the actual role.
* --|----|----|---- **VERY VERY VERY IMPORTANT** : When using a run-as manager to change the role, make sure:
*  --|----|----|----|---- To also change role-related information. For example, let's say there are uder-details fields which are filled for user with certain role, then, 
* .. when role is changed, those fields should be removed in new token.
*  --|----|----|----|---- Realize that JWT-token used for csrf protection should not depend on "run-as" role. Note that there are 2 logic needed for JWT: one comes  
* .. after authentication but before authorization to make sure that JWT is parsable, not expired and user-details details matches those in JWT; The other should 
* .. come somewhere high up in chain but must happen after the filter-chain is called, i.e. on the exit-filter rather than entry filter. It's job is to look for parsed-token
* .. if available, create a new token with new expiration timestamp (maybe also key rotation, etc) and add to response-header. However, if there is error or user gave
* .. a token which isn't parsed, then maybe return null value under header, or better, same value as that given by user. This is done as exit-filter so that the token 
* .. return is not dependent on request execution time. There shouldn't be any other logic in code that depends on JWT token.
*  --|----|----|----|---- Realize that these steps are suggested to prevent a defect like what happened with facebook in 2018 (See
* .. https://www.cnbc.com/2018/09/28/facebook-says-it-has-discovered-security-issue-affecting-nearly-50-million-accounts-investigation-in-early-stages.html )
* --|----|---- NOTE that Method-Security can also be configured to use RunAsManager, see javadoc of GlobalMethodSecurityConfiguration 
*
* --|---- EXTRA NOTES#4: "Switches" is a concept that is probably missing above. After performing Run-As, and before invoking methods, one can think of adding 
* .. switches, like turning on/off certain features; or turning on/off access for certain roles: Essentially pick and role or non-role based environment-constant, or, a
* .. combination of them, and have it be enabled/disabled. This can also be done via a filter (in a way similar to delegating filter model of Spring). 
* --|----|---- Note that special care is needed for POST calls since they don't contain the "identifier" in the URL. So the request needs to be wrapped so it can be read
* .. multiple times. (It does bring in slight processing delays though.. since each request body will be read and parsed twice -- another option is to expose same logic 
* .. but via interceptor -- so it is run after dispatcher servlet has parsed the body, and has made it available to request-handler)
* --|----|---- RECALL: notes in OtherWebDetailsController -- that this can also be integrated with Governor Service
*
*
* (5.4) SecurityContext:
* --|---- As said in Spring-security pdf, Pg62 - SecurityContext holds request specific information on Security. As an example of what distingushes 
* it from simple Authentication, note that there can be a SecurityContextHolderRepository - which applies strategies not available for Auth token!
* --|---- As said, there is a SecurityContextPersistenceFilter which populates SecurityContext at start, and then saves security context back at 
* end of request. As said on Pg67 - last para of Section-9.4, when securityContext is shared by sessions, then change in any single thread will 
* affect all of them.. so code accordingly!
* --|----|---- https://www.baeldung.com/spring-security-async-principal-propagation shows an example where one may need THREADLOCAL_PROPAGATION
* mode of securityContext.. and is useful in async propagation. **VERY IMPORTANT**: FURTHERMORE, such implementation can act as base on which 
* similar developments can be done where necessary info is passed in threadLocal context for async processing.. rather than as method params!
* See https://stackoverflow.com/questions/3467918/how-to-set-up-spring-security-securitycontextholder-strategy
* --|----|---- **IMPORTANT**: If you actually look at code of SecurityContextHolder, then its setContext() method actually delegates to underlying 
* respository object, which means if you are heaving sessions, or stateless auth-implementation, just calling SecurityContextHolder.getContext().setAuthentication()
* is sufficient to set the context on own thread and also others - as per the repository configured! See https://stackoverflow.com/questions/6408007/spring-securitys-securitycontextholder-session-or-request-bound
* --|----|---- Recall that if the isAuthenticated() method of auth-token returns true, then it is not authenticated again!!
*
*
* (5.5) WebSecurity vs HttpSecurity:
* --|---- WebSecurity: Bypassing any url here will bypass spring security totally for the pattern. When using multiple WebSecurityConfigurerAdapter, it is best 
* .. if the webSecurity portion in all are kept same. This ensures commonality in endpoints which should be bypassed by spring-security totally.
* --|---- HttpSecurity: I would say that the main job of this portion is to set authorization for endpoints based on a pattern. Since it relates to authorization, 
* .. so the related configurations needed are also set here. And that's why most authentication things are also set here
* --|---- TIPS:
* --|----|---- Tip#1: You can make a base class setting the common web and httpSecurity, and then extend it for actual implementations on which  
* .. @Configuration annotation is made
* --|----|---- Tip#2: **VERY IMPORTANT**: If a web-url is ignored in Websecurity, it'll bypass spring-security filter chain totally. 
* --|----|----|---- Ideally, skipping in websecurity should only be done for static resources 
* --|----|----|---- **IMPORTANT**: Note that bypassing spring security chain means that there won't be any SecurityContext -- so maybe it would be better 
* .. in terms of logging and tracking to instead have the users be marked as anonymous and keep track of them
* --|----|----|---- Another option is to NOT ignore url-pattern in WebSecurity.. and let it hit HttpSecurity ..at which time all users are allowed to move. 
* --|----|---- Tip#3: How do we setup different HttpSecurity authentication/authorization requirements? To do this: make 2 separate WebSecurityConfigurerAdapter
* .. classes, each annotated with @Configuration, and adding different @Order annotation on them. In each class, make different HttpSecurity as needed - keeping
* .. the configurations to corresponding url-pattern only. In either HttpMethod-configuring class, do not try to explicitly exclude the url of the other.. just focus
* .. on configuring for that url. ALSO.. for both classes,  make sure to not ignore the url of other inside the websecurity. The way this works is: In both classes,
* .. websecurity is configured to not exclude the 2 url-patterns. However, once o HttpSecurity, each adds a restriction only to the corresponding url and not the other.
* .. A good design would be to have a @Order(lowest) configuration class that just restricts everything! The idea is that if there is a url-pattern which is not configured
* .. earlier, then it'll be caught and access-prevented
*
*
* (5.6) METHOD-SECURITY RELATED COMMENTS:
*
* NOTE that enabling of method security needs adding @EnableGlobalMethodSecurity. Look at its javadoc where it says that if users need advanced configuration, 
* .. they should extend "GlobalMethodSecurityConfiguration", but still add the annotation. 
* --|---- ALSO, realize that the proxyMode used is JDK-proxy.. this way, even Spring data methods can be anotated and they will run method-security. 
* .. About JDK vs CGLIB proxy, see https://cliffmeyers.com/blog/2006/12/29/spring-aop-cglib-or-jdk-dynamic-proxies.html
* --|---- Note that configuring "GlobalMethodSecurityConfiguration" requires wiring many other components like authManager, runAsManager, etc. Add as needed
* --|---- **VERY VERY IMPORTANT**: Realize that method-security is done via proxy-ing, so it is done only for the beans which are associated to the 
* .. applicationContext where the method-security is registered. This is no issue fro Spring-boot because it uses just 1 context.. Otherwise, one needs to be careful
* .. whether it is defined in parent appContext, or dispatcher-servlet appContext or both.
*
* SpEL and Spring-Data and Method-security:
* **VERY VERY VERY IMPORTANT**: See Pg165-170 of springSecurity pdf notes. It relates to using SpEL and accessing authentication parameters in it. This
* .. also relates to Spring data.. since the mechanism being referred to is provided by Spring data. See https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions
* .. RELATED: See Pg.232-233 of Spring-security pdf docs
* --|---- Just to mention at beginning.. that there are notes on how to use SpEL::  https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#expressions
* .. To work with SpEL, one mainly needs a parser (spelParser to convert from spel expression to an evaluation-action); and an evaluation-context on which the 
* .. parser acts to create the result (as desired by spel expression). The evaluation-context in itself contains a root-object, and can also register extra properties 
* .. and functions that can be used. Also, for spel use, see https://www.baeldung.com/spring-expression-language
* --|---- REGARDING USE OF SPRING-SECURITY IN SPRING-DATA:
* --|----|---- Note from website.. it says that "Spring Data exposes an extension point EvaluationContextExtension. The interface allows implementors to 
* .. customize the EvaluationContext.." (website also says of "EvaluationContextExtensionSupport" - but realize that it is now deprecated
* --|----|---- Soon after the webpage makes a reference to "@PreAuthorize" annotation which is used for method security.. DO NOT GET CONFUSED. 
* .. Method-security is nowhere involved in this discussion
* --|----|---- For evaluation-context-extension to work (that allows SpEL used in Spring-data to get access to other root objects, properties and methods which 
* .. are otherwise not available via parameters in method).. REALIZE that it is necessary to expose custom EvaluationContextExtension-implementation as bean.
* --|----|---- Note that one of the methods in "EvaluationContextExtension" is "getExtensionId()", which according to Javadoc "..Returns the identifier of the 
* .. extension. The id can be leveraged by users to fully qualify property lookups and thus overcome ambiguities in case multiple extensions expose properties 
* .. with the same name." -- However, I've not seen that method used anywhere in spring codebase, nor any examples using it. Maybe when making spel within
* .. @Query, then instead of using, like, "authentication" property, one should use "#{extensionId}.authentication" -- i'm just guessing. Not sure also whether it is
* .. also necessary that when registering the EvaluationContextExtension-implementation as bean, then this value should be used as bean name.
* --|---- REGARDING USE OF SPRING-SECURITY / METHOD-SECURITY:
* --|----|---- Start by realizing that enabling of method security needs adding @EnableGlobalMethodSecurity. Look at its javadoc where it says that if users
* .. need advanced configuration, they should extend "GlobalMethodSecurityConfiguration", but still add the annotation
* --|----|---- Realize that the annotation also has option to enable @Secured annotations
* --|----|---- See https://www.baeldung.com/spring-security-expressions (particularly towards the end) where it mentions the use of "PermissionEvaluator".
* .. This is alos on Pg.170 of Spring-security-docs. The idea is that "hasPermission()" spel used in method-security is handled by a permission-evaluator.. so a 
* .. custom evaluator can be made to handle complex permission logic specific to the application.
* --|----|---- Look at DefaultMethodSecurityExpressionHandler referred in code and realize that it also has authenticationtrustResolver which can be used to 
* .. differentiate between anon vs logged-in vs remember-me user.. at the method-level
* --|----|---- **IMPORTANT - DON'T CONFUSE YOURSELF**: See Pg.165 of Spring-Security docs where it says that "SecurityExpressionRoot" is used for
* .. method-security. Realize that objects of this class gets involved when spring-security is combined with spring-data, but that is via a mechanism exposed by
* .. Spring-data itself. In case of method-security, it automatically uses securityExpressionRoot and provides that to user. To get clear understanding, see code
* .. of "DefaultMethodSecurityExpressionHandler" which is the goto class that handles method-security invocations. It shows using "MethodSecurityEvaluationContext"
* .. and "MethodSecurityExpressionRoot". If any change is needed, then the handler for method-security needs to be changed.
*
* Domain Object Security using ACLs - combining methodSecurity and SpringData (Corresponding to Pg.172 of Spring-Security-pdf docs)
* .. Good example: See https://www.baeldung.com/spring-security-acl
* --|---- Simply put, the idea with "Spring Security ACL" is just to have a custom "PermissionEvaluator" as discussed above. Herein, through method-security
* .. annotations on Spring-data methods, either before or after doing a DB query, the method-security aspects get run, and corresponding logic gets applied. 
* .. This can be seen in action in Baeldung-example. 
* --|---- **MORE IMPORTANTLY**: This model is built on assumption of: 
* --|----|---- (i) Every logic required as part of endpoint call is occurring within a single-transaction
* --|----|---- (ii) There is inherent separation of data from data-security. Thus, the concept of security-exists only as an "aspect" to data. To some extent, I believe
* .. this is a good model.. and fits nicely with the idea of even having "audit-logs" as an "aspect" to actual data. Furthermore, this also matches with programming
* .. paradigm of using aspect-oriented-programming.
* --|----|---- (iii) That a data-row is either fully accessible or not accessible. If we relax this criteria, i.e. provide only certain subset of information to user based on their
* .. roles or otherwise, then we'll need to form model using @OneToOne mapping as said in JpaController comments
* --|---- As an alternative to this implementation, one may also use @Filter from JPA, and filtering based on security-information. One nice thing about instead using
* .. method-security instead of jpa-filtering.. Then complex logic can be embedded in the custom PermissionEvaluator to define "haspermission" method!
* --|---- Few other things to note:
* --|----|---- **VERY VERY IMPORTANT**: If your business service is such that once the control comes to it, then it runs multiple separate transactions, or can 
* .. launch async-processes, and then go to a data-method which can fail due to ACL.. in such cases, you should in-fact have a filter/access-voter at the very
* .. beginning to fail the request-fast and not to let the side-effects go through before it is failed.
* --|----|---- As a best practice: don't cache the ACLs unless you can also immediately clear entry from cache when user changes permission -alongwith- also
* .. making changes in DB, and if changes made by user affect the ACL for other users, then their cache entries should also change; OR, maybe put an expiration
* .. time in cache which forces cache entry to clear.. or even when retrieving from cache, check that retrieval time versus lastModTime for ACL entry -- but that 
* .. can have an edge case where user deletes a previously existing entry!
*
*
* (5.7) Other details..
* --|---- **VERY VERY IMPORTANT**: regarding configuration of Spring-Security filter and ordering, see comments in EtceteraController2 - notes on SpringBoot.
* .. This also includes AbstractSecurityWebApplicationInitializer - as discussed on Pg.125 of security-docs pdf
* 
* --|---- See https://stackoverflow.com/questions/43838639/spring-security-custom-authentication-failure-handler-redirect-with-parameter  -- in that you
* should always give permission to all on login, loginHandler, logErrorHandler page
*
* --|---- Session properties can be controlled using Spring Security (See https://www.baeldung.com/spring-security-session ). PARTICULARLY, note that one
* of nice things done is "Session Fixation Protection", i.e. the JSESSIONID is changed after the existing session is validated. The way it works is that 
* attacker sends user with a sessionId in url (This is actually related to Session tracking via url params, a different thing that should be stopped by
* security). When user calls the link, the session asks user to validate, and now same sessionId gets associated with valid user.. but attacker still has 
* that session-id, so the attacker proceeds and system treats it like user
*
* --|---- Pg.129 of security-doc pdf -- is very important in that it shows different "safety" response headers that should be utilized!
* --|----|---- Referrer-Policy can be added to help with analysis of your own UI usage internally (same-origin) or also internal/externally (no-referrer-when-downgrade)
*
* --|----Difference between ROLE_ANONYMOUS vs IS_AUTHENTICATED_ANONYMOUSLY
* --|----|---- ROLE_ANONYMOUS is used when RoleVoter is used. IS_AUTHENTICATED_ANONYMOUSLY is used when AuthenticatedVoter is used. 
* .. So what string is used **SHOULD** match the type of voter used
* --|----|---- AuthenticatedVoter is a bit different in that it allows differentiating between fully authenticated user, Remember-me user, or anon-user.
* .. The advantage is that now "remember-me" users can be triggered to actually authenticate again for some portions of webpage!
* --|----|---- See Pg.148 of SpringSecurity-docs-pdf
* 
* --|---- **BEST-PRACTICE #1**: Distinguish your services if you want them to be fully restful or not.. for fully restful, these are generally very high volume.. and not needed to have session (for example, when retrieving static data), then configure spring to not make new session for it.. also don't store authentication in a session-securityContext-reporsitory! (See comments on Pg99 of sping security document). For other cases, save securityContext in session so don't need to always authenticate. ANOTHER OPTION.. for high volume service call serving static data.. maybe not need to have high security!
* --|----|---- Following up: Note that SpringSecurity either disables cache-control or add "no-cache". Maybe a good idea is to have a post-filter that adds caching
* .. for requests to standard data. All other, user-based data can be non-cached. Also, for serving static data, (a) one can cache DB results in app-server for speed,
* .. (b) Another option could be to read all possible data at startup and make, say, json files, and just serve them. This way, the url could be registered in Mvc and 
* .. cache could be set there, as shown in springSecurity-pdf-docs, and also on https://stackoverflow.com/questions/24164014/how-to-enable-http-response-caching-in-spring-boot
* .. (c) See Pg.142 of SpringSecurity-pdf-docs.. add a delegating-header-writer!
*
* --|---- **BEST-PRACTICE #2**: When returning the Auth object after successful authentication, best return one which also impements "CredentialsContainer".
* See the github code for ProviderManager, and towards end of authenticate() method. NOTE that with such auth object, you can ask providerManager to 
* erase the auth credentials after validation. THUS, this reduces chance of misusing the credentials anywhere in the code!!
*
* --|---- When making custom AccessDecisionVoter for endpoint access.. the generic could be "filterInvocation". This way, the accessDecisionVoter can get handle to 
* .. url involved, etc. HOWEVER.. do override the supports(Class<?> clazz) to return false unless it is a filterInvocation-class. This can be when trying to use voters
* .. for method-security
*
*
*
*
*
*
* === BASIC-INFO RELATED COMMENTS ===
* 
* VERY VERY VERY IMPORTANT: Realize that session/cookie based security is not optimal without sticky session.. but even so, in 
* .. a full cloud deployment, it may not be a good idea.. since cloud servers can go down and that can muck things up - or maybe 
* .. the benfit of still using it far outweighs.. since mostly the servers WILL be up.. even in cloud deployment
* 
* XSS (Cross site scripting) and CSRF (cross-site request forgery attack)
* === START ===
* -- XSS is when attacker sends you a link, that points to a valid web-app, but that web-app has a vulnerability wherein it takes 
* .. user-provided-data and returns it as-is. Now, the link send by attacker has script-tag elements in it, so when it renders back
* .. html executes the scripts which then compromises user-data to attacker
* --|---- See https://www.veracode.com/security/xss   and   https://www.owasp.org/index.php/Cross-site_Scripting_(XSS)
* --|---- The solution: NEVER, EVER just take user data and send it back as-is. Also see prevention cheat seet on https://www.owasp.org/index.php/Cross-site_Scripting_(XSS)
* --|----|---- (1) When getting queryParam, reading requestBody - add a converter/serializer that forces a string field to be strictly
* .. alphanumeric -- if that matches requirement, which most likely it will
* --|----|---- (2) This is probably another good reason to not reflect back user provided url back as HATEOAS. That can still be one vulnerability
* --|----|---- (3) If user is allowed to send and receive text with special characters, like <>=();{}, then html encode it before sending
* .. so that it gets displayed as html-safe text rather than triggering up a script evaluation
* --|----|---- (4) Use JSON.parse(..) in client side, if you are reading text data and then converting to json. Also, html-encode the value before
* .. displaying on page
* --|----|---- (5) Maybe add a filter to intercept and stop requests that want a "text/*" mediatype returned! Essentially stop sending text data
* --|---- **VERY VERY VERY IMPORTANT**: See https://www.noob.ninja/2017/11/local-file-read-via-xss-in-dynamically.html -- it is example where
* .. XSS scripts entered by user can make way to pdf and can compromise the hosting server itself!
* 
* -- CSRF attack is when attacker sends you a link that is formatted as a legit request to run on some other web-application. By doing so, the
* .. web server sees that you are logged in, and wanting to take an action, whereas you clicked unwittingly on a link!
* --|---- See https://en.wikipedia.org/wiki/Cross-site_request_forgery
* --|---- The best way to completely eliminate this is by using "localStorage" and using tokens for validation. Thus, even when user clicks,
* .. and url is valid, since you did not send the token; And since the token are only allowed to be sent when you are in context of the page,
* .. i.e., "protocol://host:port" combination - so when you are clicked on malicious link, you were on some other webpage, and so the 
* .. localStorage containing auth token to attack-target webpage is not accessible, and so the attack-target web-app will discard request as 
* .. unauthenticated
* --|---- **VERY VERY VERY IMPORTANT**: See Pg.123 of spring-security pdf. It mentions of a "Ruby On Rails" issue by which (a) CORS preflight checks can be
* .. bypassed for POST request with JSON body, and (b) extra headers can be added in the request made (See
* .. http://lists.webappsec.org/pipermail/websecurity_lists.webappsec.org/2011-February/007533.html  ). This issue is better explained by another page, but it 
* .. does not highlight the ability to change header also (See  https://blog.appsecco.com/exploiting-csrf-on-json-endpoints-with-flash-and-redirects-681d4ad6b31b )
* .. **MOST IMPORTANT TO NOTE**: (i) Don't just rely on static csrf-value in webpage sent as header for csrf protection. (ii) Now, you know of an alternative, i.e.
* .. to instead store token in localStorage. **BUT** note that for localStorage, it is domain-specific to point that it may not be available to subdomains. This means
* .. best way to design, if using localStorage, is to keep a single-page application!!!
* --|---- **VERY VERY IMPORTANT.. NOTE**: One should add csrf protection evn for login and logout url. "Login CSRF" (see
* .. https://stackoverflow.com/questions/6412813/do-login-forms-need-tokens-against-csrf-attacks   and
* .. https://www.netsparker.com/web-vulnerability-scanner/vulnerabilities/cross-site-request-forgery-in-login-form/    is an attack wherein the user starts by being
* .. logged in to system. Now, attacker sends a malicious link, clicking which triggers a login call, made by user, but using attacker's credentials. The ensuing
* .. behavior depends on how web-application is configured. But, if it so happens that doing so logs in the attacker, then whatever the user does now will be stored
* .. under attacker's history. At some time later, attacker can just visit his account again and get those details. The "login csrf" protection requires that there be a 
* .. csrf token even in login form. Now, what happens when user is trying to log in for the very first time?  
* --|----|---- Maybe require the presence of a SameSite cookie. This means that there will be a session-creation even when user tries to access login-page when 
* .. he/she is currently anonymous., and before /login REST endpoint is even called. Since login-step does session-invalidation for protection.. this means that you'll
* .. need to copy the csrf token manually to newly created session (or provide a new csrf-token in new session)
* --|----|---- Other option is to have a csrf value in login-form submitted by user, and have that value match non-SameSite cookie or header
* 
* -- EXTRA INFO:
* --|---- SessionID cookie -- vulnerable to CSRF, not to XSS (partly) -- Note that Java Servers create SessionID cookie and send it to user. This 
* .. is how the server identify a session, even though HTTP itself is stateless. This is vulnerable to XSS attack because -- SessionID is a cookie, 
* .. and the way cookies work is that everytime a request is made to the website, all previous cookies, unless expired, are sent. So, even though
* .. you are clicking on a malicious link made by other person, the SessionID cookie identifying your login will go with request. EVEN MORE, if
* .. the cookie isn't marked as httpOnly, then XSS attach can be made to steal the sessionId, and once obtained, the other user can send it to
* .. identify himself as user! (More about cookies under Spring session management)
* --|---- localStorage is vulnerable to XSS attack.. consider the case that you are made to click a link, that brings back a response from 
* .. target web-app, making script run on your system that sends localStorage information to attacker. Since this script is now running within
* .. the domain of main webpage, so it will indeed have access to the localStorage containing auth-token
* --|---- **VERY VERY IMPORTANT**: Since XSS attack is more controllable by correct application-design, this makes token-use for authentication
* .. as a better and more secure way, since this completely cuts off CSRF attack. For more information, see https://auth0.com/blog/cookies-vs-tokens-definitive-guide/
* --|----|---- Also, as mentioned in the page, this allows for 3rd party token authentication. This will see further adoption considering new 
* ..authentication standards to use phone, fingerprint, etc.
* --|----|---- EVEN MORE.. you can probably add the ip-info, or encoded-ip-info inside the token, and can force user to re-login if the 
* .. ip-address changes of incoming request changes (..since attacker will be sending request from different ip)
* === END ===
* 




* TODO

* See https://www.reddit.com/r/firefox/comments/86sagi/how_sites_use_http_header_etag_to_track_you_and/    --- shows that ETag can be used for tracking.. this shows that if the requirement is to get speed by checking last-update-time.. then just cehck for last update time!!

* See https://www.fastly.com/blog/caching-cors   and   https://www.fastly.com/blog/best-practices-using-vary-header   The idea is to use "Vary" header to additionally control the use of cached data. As a policy feature, it could be made to require that this is present as a response-header -- Additionally, may require that "Origin" be one of the values for "Vary", to prevent cache abuse in CORS request, i.e. a cached value set by one page shouldn't be used for CORS request by other

* See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Clear-Site-Data  -- during logOff.. along with scripts loaded, the site may want to send out a response header to clear site data. Another possibility could be to do a 2-step call.. where at beginning the "remember me" token is store din local variable.. then first request asks to clear everything, and second request asks to set the one particular cookie, so that it can be sent in subsequent requests; Or maybe it could be done in localStorage also, in addition to cookie:: this way the rememberMe gets triggered back when visiting that site only later.. rather than trapping t a csrf defect/attack link
---- This is initially motivated by https://polict.net/blog/web-tracking-via-http-cache-xs-leaks/  -- maybe another thing to note is that when downloading files for a user.. then Personal-Identifiable-Information (PII) should not be provided in any way (for example in fileName). for same reason it is best if any endpoints that can potentially share PII in url or requestParam or responseBody (as json or file) is never allowed CORS with other domain (..or user be warned of potential security)
---- **IMPORTANT**: See https://www.youtube.com/watch?v=vzp7JdezZRU  (Cross-site search attacks are like cross-site request-forgery attack but you're doing cross-origin search request which leaks some critical information.. see https://www.youtube.com/watch?v=HcrQy0C-hEA -- Thus, using a JWT token should stop such requests?!)    FOLLOW-UP:: Should responseTime and responseSize be made homogenous, and is there a need for it? Should the response type be homogenized as "application/json" -- is another reason for always having json communication.. in addition to fostering single page response..  and UI separation from service logic
----|---- Follow up from same.. maybe the handler logic of Exceptions could be modified so that it adds a variable delay based on time when request came to server (or filter, or controller), and average time of response +/- std-dev. Also, a field caould be added in response to give it similar length as valid response length corresponding to such searches.. BUT, is it needed?! If cross-site attack is stopped.. then 403 response will go nonetheless!!

* JWT custom keys - make it more than 3 aphabets - since 3 alphabet keys seem like they are based on standard jwt token
* JWT - when signing; use a long secret key - since if you are using HS256 algo to encrypt it, then don't provide a small signing secret-key
* Question: How often / when to update JWT to reflect if user made back-end changes?! ..Or is there a way to defer it till later by having some redundant logic on service side

* Nice video on JWT use for microservice, service-to-service sync/async communication/security using JWT: See https://www.youtube.com/watch?v=BJlfrAicVtc -- **IMPORTANT** Note that JWT by itself gives CSRF-security because the last-section of it (after the 2nd dot) is computed using payload and secret. So, if someone changes payload, but uses same 3rd part (because theyd on't know secret) - then that won't work. Also, in header, you can define a Key-id (kid), that can be used to look up the secret-key. The idea is that you can rotate and expire key-id(s), so that also gives a security
--|---- However: This, by itself, does not guarantee against a login-csrf attack - but it can be modified. Realize that login-csrf attach is a csrf-attack during login (See https://stackoverflow.com/questions/6412813/do-login-forms-need-tokens-against-csrf-attacks ) A good modification could be that : (a) on services side, require the jwt token be present - where token is of special type, say "login", different from "logged in" type, and this token should contain the username-hash the user is sending (don't keep username since that goes insecure), and the remote-ip of user, and have this token be valid for only, say, 30 secs - Since JWTs are safe against tampering, this should help; (b) on UI side, once user enters username and pwd and clicks sumit, first make a call with username-hash to get corresponding JWT.. then use that JWT in making actual login call; (c) Maybe as response - also return back the username (in service side). On UI side, maybe start with a fade showing username first and last logged in time... and then fade in the remainder of UI (This way the user's attention is drawn to their name first before they get distracted by multiple other information shown to them)
--|----|---- **REALIZE** that CSRF attacks are when servers cannot identify if the reqest is coming from legitimate user or attacker - because of way browser behaves. But before a login happens (..and if we are looking in cloud setting which is supposed to be stateless), there is no user! This is what makes login-csrf different from others - because in terms of architecture, there is no difference in concept of user vs attacker. But in real implementation, there is such a difference - because user and attackers are different. From stackoverflow link above, note that the "attack scenario" is where user clicks a malicious link elsewhere and then expects to be shown their homepage; but instead they are shown attacker's homepage; which user is unable to distinguish either knowingly, or unknowingly (because userName of login is not displayed on page)
--|---- Realize that the good thing about JWT is that it can be used in service-to-service call, service-to-Messaging call, etc. Thus it is much more versatile. If you want, you can add a second layer of also sending a cookie which has JWt token. The advantage of JWT is that by going off of cookies/session-- the code becomes cloud ready where sessions cannot be defined.
** VERY VERY IMPORTANT -- TODO - update comments in Spring security docs

* See https://haacked.com/archive/2012/01/30/hazards-of-converting-binary-data-to-a-string.aspx/ -- to never store bytes as text, if intention is to store bytes
Notice it mentions of "Standard Replacement Character" --> that can be see clearly in https://www.baeldung.com/java-string-to-byte-array
* 






* === [END] BASIC-INFO RELATED COMMENTS ===
* 
* 
* 
* === CONFIG RELATED COMMENTS #1 ===
* 
* Nice Spring-Security examples:
* IMPORTANT: Meaning of various overriden configuration methods in spring security -- https://stackoverflow.com/questions/22998731/httpsecurity-websecurity-and-authenticationmanagerbuilder
* 
* 1) https://www.devglan.com/spring-security/spring-boot-security-custom-form-login-example
* .. Particularly note that just for mvc there is @EnableMvc and MvcConfigurer.. similarly for security there is @EnableWebSecurity and 
* .. WebSecurityConfigurer (to configure security)
* .. This example primarily focuses on stopping csrf attack via configuration in Spring-security. See https://www.baeldung.com/spring-security-csrf  
* .. on how csrf prevention can also be done when sending json request. Recall, another way to prevent is through use of local-storage
* .. IMPORTANT: Look at web-mvc config of this example (not Baeldung example).. it is shown that for "/" url, it is directly associated the 
* .. view controller of login. This is an example of how in Spring mvc, the index page.. and even other pages can directly be made to go to 
* .. login page!
* 
* 
* 2) https://www.baeldung.com/spring_redirect_after_login
* .. This is example for redirect after spring security login. See the presence and use of "RedirectStrategy". 
* .. Related, recall that there is .sendRedirect() method in HttpServetResponse. You can also add redirect-attributes, or flashMap for use in 
* .. redirect (See Spring-mvc-docs about it. In docs-pdf, it is Pg.49, 50). There is also RedirectView; Or just return ModelAndView where url 
* .. starts with "redirect:/..."; Or return string type from controller starting with "redirect:/..."
* 
* .. ALSO.. there is "RequestDispatcher" that can be used to forward request to different component. NOTE that forward() only works when 
* .. sending to another endpoint in same application, and not in other url domain. Or return string type from controller starting with 
* .. "forward:/..."
* .. IMPORTANT: Since forwards happen within same server, so attributes set before forwarding are passed onto the new requestHandler (after 
* .. forward)
* 
* 
* 3) https://www.devglan.com/spring-security/spring-boot-security-hibernate-login-example
* .. This configures custom user-details-service, which contains logic on getting user-details. By making specific implementations, users can 
* .. control how the user corresponding to a particular incoming request is identified!
* .. Also, note from spring api docs, that if the authentication token is present in security-context, then authenticaion will just use that! - 
* .. bypassing usual username-password auth
* 
* 
* 4) https://www.devglan.com/spring-security/spring-boot-security-rest-basic-authentication
* .. This is example of Basic auth in Spring boot. Few things to note: 
* .. (i) since basic auth is standard pssword protocol.. so just mentioning it in web-secuirty-config enables it.. but how to respond properly 
* .. when it fails can change, so the need for custom "AuthenticationEntryPoint". 
* .. --|---- Particularly, within the webpage, note the line "response.addHeader("WWW-Authenticate", "Basic realm=" +getRealmName());". This is a standard way to
* .. respond. Can see github page of DigestAuthenticationEntryPoint  for other values, or see MDN docs for "WWW-Authenticate"
* .. --|---- See https://stackoverflow.com/questions/12701085/what-is-the-realm-in-basic-authentication  -- it explains the meaning of realm.. it is a set of resources
* .. connected by the authentication. Maybe, some other resources would want to use other authentication. Although, best would be an unified design.
* .. (ii) **IMPORTANT**: Note the use of SecurityContextLogoutHandler -- to do logout, rather than just clearing context. Look at the 
* .. implementing code for this handler in github and realize that with this.. one can clear authentication and invalidate session in a 
* .. consistent manner throughout the applictaion.. rather than handcoding it, and/or just clearing securityContext. ALSO NOTE that a good 
* .. practice is to redirect to login page after the logout, 
* .. --|---- **VERY VERY VERY IMPORTANT**: See https://stackoverflow.com/questions/17205841/how-to-end-the-session-in-spring-3  -- the idea that 
* .. when wanting to invalidate a session, then don't do it on a controller annotated with @SessionAttributes, and maybe, not even in a method 
* .. where one of the param has @SessionAttribute annotation  
* .. --|----|---- RELATED#1: Best to invalidate session with same logic as in SecurityContextLogoutHandler .logout() method; Maybe also 
* .. incorporating other LogoutHandler implementations.
* .. --|----|---- RELATED#2: to know about the annotations, see https://www.boraji.com/spring-mvc-4-sessionattributes-example
* .. (iii) **VERY VERY VERY IMPORTANT**: Note the statement that after successful basic auth, the browser keeps sending the value. From this, 
* .. realize:
* .. --|---- **IMPORTANT**: On personal note, this is why you should clear your cache after using webpage with basic auth. Can use 
*            chrome://restart; Or, use incognito - but then don't share open window with sensitive info (like banks), in same incognito as 
*            unsafe webpages - since all incognito shares same cache!
* .. --|---- This is a big reason why basic auth is not safe against CSRF attack! And that's why using localStorage, and a js that adds a 
*            custom header, and custom authentication mechanism reading that custom header is base safeguard against csrf attack
* .. --|---- Maybe, when you do "redirect:/login" after the logout, then instead do something like "redirect:/login?logoutAt={logout-time-in-ms}", 
*            and discard login attempt even with same username and password if done within, say, 5 seconds of "last logout time, in ms"; Also, 
*            on failure again redirect to login page but let it work this time. Also, for extra security, obfuscate the "logoutAt" parameter 
*            with some other name; or, append extra junk and encode the value {logout-time-in-ms}, so that others cannot understand its utility 
*            - say, by randomly putting equally many alphabets in time value. The hope is that even when browser auto-redirects to login page 
*            and sends authentication.. that will fail, prompting broser to clear the cache! ..i guess! This will require special auth-handler. 
*            ALSO.. better idea to tell consumers that they should just close their browsers
* 
* .. On a side note, the example also uses CORS!
* 
* 
* 5) Note that there is X-FRAME-OPTIONS response header to prevent webpage content being shown in an iframe. There is also "Content-Security-Policy"
* .. that can be used
* 
* 
* === [END] CONFIG RELATED COMMENTS #1 ===
*/
//@formatter:on

public class SecurityController {

}
