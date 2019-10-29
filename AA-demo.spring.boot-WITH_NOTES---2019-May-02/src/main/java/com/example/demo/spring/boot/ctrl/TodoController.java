package com.example.demo.spring.boot.ctrl;

/**
 * 
 * This is not really a controller, but a placeholder class containing comments on various TO-DO items, most likely,
 * IN-ORDER-OF-PREFERENCE
 * 
 * @author KunjPrasad
 *
 */
public class TodoController {

    // ..spring JPA
    // just like we needed @EnableJpaRepositories.. do we also need @EnableTransactionManagement - to check, 2 ways
    // .. first, use @Transacitional and refer to a tx-manager which is not present! See if it errors
    // ..See https://stackoverflow.com/questions/40724100/enabletransactionmanagement-in-spring-boot/41643269
    // Check what happens if you start tx with one manager then go to another and then return! -- Even if it works do
    // write to not do so
    // ..or start a service, save it, and at end throw exception. If it is indeed transactional, it will rollback

	// **IMPORTANT**: See https://dzone.com/articles/how-does-spring-transactional  -- It says that one entityManager
	// .. per application-call is the default strategy (and best) to use!
	// --|--- REALIZE that this is why no logic should be kept at controller level!! Make all DB calls in one 
	// .. transactional context if possible
	
    // look at shadow table


	
    // allow multiple commit in one session without using requires-new.. BUT, is it good idea!! because requires new
    // .... explicitly tells of new transaction getting made and so that if it fails then original can still continue!
    // --|-- mention that requires_new is what in lingo is called enabling parent-child relation in transaction.. and
    // that it "need not be" enabled in all JTA providers (probably due to complications of 2 phase commit +
    // requires-new)
    // --|-- Is it that this observation is due to fact that when method leaves @transactional, then the bean gets
    // non-managed. What if we were to call another transcational method and make the bean managed again by calling
    // repository.get() method? Does this work - (1) if the 2nd service is in same @transactional class as first, if 2nd
    // is in different @transactional class -- note that this 2 scenarios arise because of @Transactional being aspect -
    // so that will (..or may) close any top level transaction if the method does indeed correspond to top level tx
	
    // when trying to get id of a foreign key.. not full data, just the id, does it still trigger a fetch. How to stop
    // it

    // make quartz job for reports

    // sorted result, paginated result

    // ehcache + hibernate 2 level caching -- references:
    // .. https://stackoverflow.com/questions/337072/what-are-first-and-second-level-caching-in-hibernate
    // .. https://www.baeldung.com/hibernate-second-level-cache#cacheConcurrencyStrategy
    // ..Also related to SharedCacheMode of persistence.xml, see
    // https://stackoverflow.com/questions/7770847/understanding-persistence-xml-in-jpa

    // Hibernate Search - integration!!

    ////////////////////

	
	// Spring Security -- How to do csrf protection in cloud environment where one cannot make a session!? How do you check the request coming from outside
	// There is also the case of login-csrf protection, logout csrf protection..
	
	
	
	
	
	///////////////////
	
    // set up logging properly!! See https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html
    // --|---- also logging properly for embedded, test, local deployment.
    // --|---- also logging for liquibase

    // Spring cache.. BUT WHY.. when Hibernate already has a cache.. maybe when we use non-Hibernate systems (noSql)
    // Session Management
    // sessionRepository, CookieSerializer

    // VERY IMPORTANT: https://www.baeldung.com/spring-retry

    // Try antivirus with clamAV - then uninstall it

    // ..Security
    // @PreAuthorize, PostFiletr, etc
    // limit number of sessions a user can have, session timeout, etc..
    // From https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#domain-acls-overview
    // ---- "Complex applications often will find the need to define access permissions not simply at a web request or
    // ---- method invocation level. Instead, security decisions need to comprise both who (Authentication), where
    // ---- (MethodInvocation) and what (SomeDomainObject). In other words, authorization decisions also need to
    // ---- consider the actual domain object instance subject of a method invocation."
    // ..and see https://www.baeldung.com/spring-security-acl :: To have object level security!!

    // ..also to do
    // make unit test; integration test!
    // ----|---- See https://www.baeldung.com/spring-boot-testing
    // ----|---- See https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html
    // ----|---- use of @RunWith(SpringRunner.class), @SpringBootTest, @AutoConfigureMockMvc
    // ----|---- how to have just one mockMvc context for full check
    // ----|---- use of @Value in test-setting to read test-specific system variables (as in StaticSwaggerGeneratorTest)
    // jacoco - see https://www.baeldung.com/jacoco
    // make "local level" integration test! - configure it via spring boot maven plugin

    // ..add sample of A/B testing via MVC

    // ..for JS:
    // submitting multipart/form-data and multipart/mixed. And receiving such response
    // reading data from post call
    // get zipped json and unzip it
    
	// use of jwt; (JWE - json web encryption, provided by JOSE - there is also a spring security package for it)
	// ---- why JWT got famous.. because in cloud environment - it is a stable way to have "sessions"!! Cannot otherwise make 
	//      sessions on servers since since subsequent requets may not come to same server; or same server may die!
	// ---- Or use spring-session, which is backed by a DB and is distributed! Best, use NoSQL DB for speed
	
    // typescript plugin
    // jasmine plugin
    // obfuscation and compression plugin
    // local storage, session storage, db, web workers, cache
    // check file size before allowing upload
    // cookie; See https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies -- in contrast are jwt (See
    // .. https://auth0.com/blog/cookies-vs-tokens-definitive-guide/ --- important page)
    // **ALSO add in resume** autocomplete with disable lag.. if someone is just typing.. then don't keep on sending
    // .. request.. wait till they stop

    // configure https


	/*
	
	
	Use of matrix-variables when wanting to filter out at multiple stages in REST path.. queryParam are best suited only to filter out at end
	
	
	
	1.1) RestTemplate error handling : https://www.baeldung.com/spring-rest-template-error-handling
	-- Problem for Spring..!

1.2) If the DB contains columns1-10, but hibernate only contains @Column for columns1-5, then Hibernate will only be doing partial mapping. HOWEVER, if hibernate has annotations for columns1-5, and, say, column-4 is missing, then that raises error

1.3) How to make hibernate query for only 1 column -- but is that even necessary.. if you already add lazy loading! - because even to read a single cell, DB
will have to do a row level access. It just reduces network latency -- which matters if this table is called multiple times and/or if the table row
is very large
---- QUESTION: Will the object get cached in Hibernate session such that any future calls will cause full object to not get returned?!

1.4) If you have auditing in your db design.. and also allows for async processing.. then when system is doing async processing, it should pick up the last-mod-userId. This way, whether the method runs in sync (by user) or async (by system).. it’ll see same behavior. Also have system set security token in SecurityContext when running in async!
	
	
	https://docs.microsoft.com/en-us/dotnet/standard/microservices-architecture/architect-microservice-container-applications/communication-in-microservice-architecture

https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing

https://simplicable.com/new/smart-endpoints-and-dumb-pipes

https://12factor.net/ 
-- The "backing service as attached resource" when done properly -- gives internationalization
-- IMPORTANT: realize the importance of "Statelessness" for cloud deployment.. Server can go up/down and request can change. So, having staful session bean
is just useless. So, there needs to be statelessnee; And maybe use JWT (sent via header) to communicate session variables bac/forth with user!?
--|---- Question: Mabe look into ability to keep JWT secret dynamic.. be able to change it, say, every 5-10 minutes

https://martinfowler.com/articles/microservices.html

Spring Cloud; Kubernates; Spring-Kafka; Spring-cassandra?!; Spring-solr?

CQRS pattern: https://docs.microsoft.com/en-us/azure/architecture/patterns/cqrs

https://stackoverflow.com/questions/36083504/database-connection-pool-strategy-for-micro-services



spEL -- https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#expressions




Microservice -vs- library module-ing



https://jmesnil.net/stomp-websocket/doc/
https://www.devglan.com/spring-boot/spring-boot-actuator-tutorial-guide
https://www.devglan.com/spring-mvc/writing-junit-tests-in-spring4-mvc
https://www.google.com/search?q=stomp+vs+jms&oq=stomp+vs+jms&aqs=chrome..69i57j0.17793j1j7&sourceid=chrome&ie=UTF-8
https://stomp.github.io/stomp-specification-1.2.html
https://www.devglan.com/spring-mvc/spring-ehcache-cacheable-example-javaconfig
https://memorynotfound.com/spring-boot-ehcache-2-caching-example-configuration/
https://spring.io/guides/gs/caching/
https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/integration.html#cache
https://www.devglan.com/spring-boot/spring-boot-jms-activemq-example
https://www.devglan.com/spring-mvc/spring-jms-activemq-integration-example
https://spring.io/projects/spring-session
https://www.google.com/search?q=javascript+upload+large+files&oq=javascript+upload+la&aqs=chrome.0.0j69i57j0l4.4663j1j7&sourceid=chrome&ie=UTF-8
https://tus.io/demo.html
https://github.com/23/resumable.js/



https://www.baeldung.com/spring-security-remember-me
https://www.baeldung.com/spring-security-persistent-remember-me
https://www.troyhunt.com/how-to-build-and-how-not-to-build/
https://stackoverflow.com/questions/244882/what-is-the-best-way-to-implement-remember-me-for-a-website
https://www.baeldung.com/spring-security-oauth2-remember-me
https://maven.apache.org/ref/3.6.0/maven-model/maven.html
https://maven.apache.org/ref/3.6.0/maven-settings/settings.html
https://stackoverflow.com/questions/12433120/creating-a-new-phase
https://stackoverflow.com/questions/26258158/how-to-instrument-advice-a-spring-data-jpa-repository
https://stackoverflow.com/questions/34332046/aspect-advice-for-spring-data-repository-doesnt-work
https://stackoverflow.com/questions/2593722/hibernate-one-to-one-getid-without-fetching-entire-object
https://docs.jboss.org/ejb3/app-server/HibernateAnnotations/reference/en/html_single/#d0e1955
https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/last-modified.html




Learn SQL function + stored-procedures:
https://www.techonthenet.com/oracle/functions.php
https://www.mkyong.com/oracle/oracle-plsql-create-function-example/
https://thoughts-on-java.org/use-database-features-hibernate/#function

-- There is something called "Java source" : can be used in liquibase also..
-- sys_guid can be used to make guid entry in DB
-- After making function.. when you say "UPDATE [table] set [column] = functionCall() where [CONDITION]"; and condition selects more than 1 column, then the function is applied individually and separately to all columns. So, in case of guid, a sepaate guid will be assigned to alll rows






SPRING BOOT ACTUATOR:
-- On back side, an actuator is just like an endpoint. BUT.. the good thing of actuator is that it can be toggled on/off. Not sure if that can be kept dynamic by combining it with Spring-cloud. 
-- Just like an url, @Selectors can be added in actuator endpoint to provide only a smaller section of data. 
-- If not using endpoint, this behavior can be emulated by adding "tags" to each controller, such that when the switch for the tags are on.. they controller works - else it is off. ****VERY VERY IMPORTANT****: This may be a way to incorporate switch-like security behavior within the governor service itself!
-- Look at code of HealthEndpont to get a better idea

Documentation + code sample:
https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html
https://docs.spring.io/spring-boot/docs/current/actuator-api/html/
https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/health/HealthEndpoint.java






Design patern for RestTemplate: Add retry, standard request structure with header, body and to give response; common error response during failure


Spring-cloud
@RefreshScope refreshes bean by calling appContext.refreshAll(). So, even if there is no bootstrapping, one can still do a refresh.
Further, the @Value in beans being refreshed.. can be modified such that it holds an expression based on autowired bean instead of a value-reference.. with the behavior that the bean also gets refreshed (code has to do so).. if that is done, then one no longer needs any kind of config/bootstrap server.. but can still refresh


**IMPORTANT**: See security point mentioned under Patentcenter - in your resume. Add those all!!
Also look at points with "basic developer efforts"






2.a) JavaScript review + unit test  
---- Dynamically load unload a javascript
----|---- See JSPatterns.com
----|---- Any corollary of classloader in JS
----|---- Adding script with id - so it can be replaced/removed
----|---- See https://stackoverflow.com/questions/950087/how-do-i-include-a-javascript-file-in-another-javascript-file
----|---- See https://stackoverflow.com/questions/9092125/how-to-debug-dynamically-loaded-javascript-with-jquery-in-the-browsers-debugg
---- Role-based javascript provisioning + A-B testing : These are not possible without JSP in the app itself

https://exploringjs.com/es6/ch_modules.html
https://darkpatterns.org/
https://css-tricks.com/simple-social-sharing-links/


**IMPORTANT**
https://www.computerhope.com/issues/ch000977.htm  -- this discussion started with Salesforce application html page having different right click menu.. how?!
HAR file, Waterfall chart :: UI optimization




**VERY VERY VERY VERY IMPORTANT**:
* Integration of Jenkins (running on AWS) with Github: https://www.youtube.com/watch?v=1JSOGJQAhtE
 Integration of Jenkins with CodeDeploys/AWS: https://www.youtube.com/watch?v=LFkGtg-ZTko
On aws: java application + DB + S3: such that DB can be both remote or local - triggered by environment variable
Saving machine instance on AWS.. into S3




//Spring data related
https://www.google.com/search?q=spring+data+gets+ingle+field+for+row&oq=spring+data+gets+ingle+field+for+row&aqs=chrome..69i57j33.9175j0j4&sourceid=chrome&ie=UTF-8
https://stackoverflow.com/questions/22007341/spring-jpa-selecting-specific-columns
https://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts
https://www.petrikainulainen.net/programming/spring-framework/spring-data-jpa-tutorial-introduction-to-query-methods/
https://www.google.com/search?q=spring+jpa+get+foreign+key+instead+of+object&oq=spring+jpa+get+foreign+key+instead+of+object&aqs=chrome..69i57.11934j0j7&sourceid=chrome&ie=UTF-8
https://stackoverflow.com/questions/6311776/hibernate-foreign-keys-instead-of-entities
https://stackoverflow.com/questions/49831492/how-to-perform-a-query-in-spring-data-jpa-with-a-foreign-key
https://stackoverflow.com/questions/36105036/spring-data-jpa-get-entity-foreign-key-without-causing-the-dependent-entity-lazy


// Look at Java's "CompletionService" and "ExecutorCompletionService" -- good api for chaining
// CompletableFuture is another option (https://www.baeldung.com/java-completablefuture     https://stackoverflow.com/questions/39472061/executorservice-submittask-vs-completablefuture-supplyasynctask-executor)


Understand UML diagrams: https://www.cs.bsu.edu/~pvgestwicki/misc/uml/


checkout java collator: https://docs.oracle.com/javase/tutorial/i18n/text/collationintro.html  -- nice for text comparision. Also, on that point, recall the use of "Normalization" in strings


IMPORTANT: DB designs for inheritance: https://stackoverflow.com/questions/190296/how-do-you-effectively-model-inheritance-in-a-database  -- TablePerType; TablePerHierarchy; TablePerConcrete -- ALSO.. what is the Hibernate equivalent?!


IMPORTANT: Look at api and usage of Lock, Semaphone, CountdownLatch, CyclicBarrier. Also see https://winterbe.com/posts/2015/04/30/java8-concurrency-tutorial-synchronized-locks-examples/  -- for StampedLock and "optimistic locking" using it



TODO - when discussing "productionizing" code, also discuss of easy change between sql/nosql (i.e. abstracting out normalization); and, how to change code to accommodate different level of requests/second




--|----|---- The String.length() method returns the number of code units, or 16-bit char values, in the string. If the string contains supplementary characters, the count can be misleading because it will not reflect the true number of code points. To get an accurate count of the number of characters (including supplementary characters), use the codePointCount method.
--|----|---- Use the String.toUpperCase(int codePoint) and String.toLowerCase(int codePoint) methods rather than the Character.toUpperCase(int codePoint) or Character.toLowerCase(int codePoint) methods
--|----|---- When invoking the StringBuilder.deleteCharAt(int index) or StringBuffer.deleteCharAt(int index) methods where the index points to a supplementary character, only the first half of that character (the first char value) is removed. First, invoke the Character.charCount method on the character to determine if one or two char values must be removed.
--|----|---- When invoking the StringBuffer.reverse() or StringBuilder.reverse() methods on text that contains supplementary characters, the high and low surrogate pairs are reversed which results in incorrect and possibly invalid surrogate pairs.
*/
}
