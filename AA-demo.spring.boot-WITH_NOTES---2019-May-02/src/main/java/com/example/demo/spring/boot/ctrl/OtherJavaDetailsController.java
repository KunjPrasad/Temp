package com.example.demo.spring.boot.ctrl;

// @formatter:off
/*
* **NOTE**: THIS FILE CONTAINS "OTHER JAVA DETAILS"
*
*
*
*
* 1) **VERY VERY VERY IMPORTANT** SMALL DETAILS:
*
* 1.A) With introducing of modules in Java-9, minor adjustments are needed to be able to do testing. See https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world.html
* .. https://stackoverflow.com/questions/41366582/where-should-i-put-unit-tests-when-migrating-a-java-8-project-to-jigsaw/41367802
*
*
* 1.B) With Thread management getting separate in executor.. most of thread commands are almost useless to know. However, the 2 webpages make a very strong 
* .. case to know about thread-interrupts and InterruptedException.
* --|---- The webpages are: https://www.informit.com/articles/article.aspx?p=26326&seqNum=3   and   https://praveer09.github.io/technology/2015/12/06/understanding-thread-interruption-in-java/
* --|---- **IMPORTANT**: Second page shows that if you catch InterruptedException and stop it, then you MUST mark the thread as interrupted
* --|---- **VERY VERY VERY IMPORTANT**: This bring out a very important design principles if wanting to run long-execution jobs. 
* --|----|---- (1) BEST, don't run long running job. You want to know early on and often if there has been failure. Split jobs and merge results if possible
* --|----|---- (2) If wanting to run a long running job - that does have breaks.. then periodically check to see if thread is interrupted. If it is, then return
* --|----|---- (3) As said in the page, if you are running from async thread that cannot throw exceptions, you must catch InterruptedException and then set the 
* .. interrupted flag for the thread before returning. This resets interruption
* --|----|---- (3) Could it be that even 1 job is too much? I am guessing that may conceptually relate to the circuit breaker pattern. This shows that concept of
* .. "interrupt" is indeed powerful and useful.. even now when we have ExecutorService to do thread-management!
*
*
* 1.C) Use of absolute vs relative url: Particularly see this answer: https://stackoverflow.com/questions/2005079/absolute-vs-relative-urls/21828923#21828923 
* .. shows how to relativize just the scheme part (For example, if you are a site on https, and want to load image from another site, also on https)
*
*
* 1.D) On need for the Unicode flag in java for pattern matching: See https://stackoverflow.com/questions/4304928/unicode-equivalents-for-w-and-b-in-java-regular-expressions 
* .. Also see Javadoc for Pattern -- https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html -- search for /p{Lower} on page.. And notice how it
* .. matches a bigger group of characters when Unicode flag is given in pattern. 
* --|---- NOTICE, also on the page, it says that:: “[a-d[m-p]]” is a valid pattern which means “a through d, or m through p (..and is equivalent to) [a-dm-p] (union)”
*
*
*
* 2) Java Design: Aggregation (has-a) vs Inheritance (is-a) vs Abstract-Class
* --|---- Consider the case where 2 classes have implement an Inheritance. This means that both classes can be assigned to the common class type. This is why 
* .. inheritance is applied for a "behavior". HOWEVER, the more important point is that this is valid only if the "behavior" has a common processor. It is only then
* .. that we want to DELAGATE "behavior"-processing to the common "behavior-processor". Instead, consider the case of aggregate. In this case, we need a common
* . delegate class that can look at the runtime-type of class, and for each type, it can pull out the object-member holding the "behavior". A separate delegate can be
* .. made to identify the processor to be applied. This shows that aggregates are good in a "evolving"/"not-fixed" type of design, where we know/want a behavior,
* .. but the specifics of it are not yet fixed. 
* --|----|---- In notes of "OtherWebDetailsController", it is mentioned on how standard-data-value fields could be used as context setting variables. Delegation can
* .. be done on such variables - and methods defined inside delegates to pull specific information out from each type.
* --|----|---- Coming back to "philosophical" meaning: It is generally said that inheritance is best used when a "group" of classes have some "Identity" behavior. 
* .. However, once should change it to "..when there is a identity behavior under a common processor". For example: marking animals as "listenable". This seems to 
* .. work good for different classes categorizing human-race.. but works badly when taken to other animals - since they may have different ears, listen different range.
* .. In modern web-development, since scrum methodology is adopted, it means things can change in future.. and so, instead of using inheritance, it is best to use 
* .. an aggregate design, followed by a delegate mechanism - because behind the scenes.. this is actually what the "inheritance" pattern does!
*
* --|---- Abstract class: Difference between abstract class vs inheritance: It is best to use abstract class for strategy pattern only, i.e. when you have an inheritance, 
* .. but execution of certain methods are always similar except a few changes here and there, or, if one method depends on other under all cases! Apart from that,
* .. it is always better to chose inheritance than abstract class - best reason being that the former allows emulating "multiple inheritance" - by having private
* .. abstract class members and passing outer request to inner members!
*
*
*
* 3) Classloader
* https://docs.oracle.com/javase/tutorial/deployment/jar/apiindex.html (An example of classloader can be seen here)
* See https://www.journaldev.com/349/java-classloader   and   https://www.baeldung.com/java-classloaders
* 
* Note the following:
* --|---- (From Baeldung) "Class loaders are responsible for loading Java classes during runtime dynamically to the JVM (Java Virtual Machine) ...the JVM doesn’t 
* .. need to know about the underlying files or file systems in order to run Java programs thanks to class loaders ....Java classes AREN'T loaded into memory all 
* .. at once, but when required by an application. This is where class loaders come into the picture. They are responsible for loading classes into memory."
* --|---- (From JournalDev) There are 3 hierarchy of classloader: 
* --|----|---- Bootstrap Class Loader – It loads JDK internal classes, typically loads rt.jar and other core classes for example java.lang.* package classes
* --|----|---- Extensions Class Loader – It loads classes from the JDK extensions directory, usually $JAVA_HOME/lib/ext directory.
* --|----|---- System Class Loader – It loads classes from the current classpath that can be set while invoking a program using -cp or -classpath command line options.
* --|---- (From JournalDev) "Java ClassLoader is hierarchical and whenever a request is raised to load a class, it delegates it to its parent and **IMPORTANT** "..in 
* .. this way uniqueness is maintained" in the runtime environment. If the parent class loader doesn’t find the class then the class loader itself tries to load the class."
* --|---- (From JornalDev) "Classes loaded by a child class loader have visibility into classes loaded by its parent class loaders. So classes loaded by System  
* .. Classloader have visibility into classes loaded by Extensions and Bootstrap Classloader (**IMPORTANT** : realize that vice versa does not hold. This behavior 
* .. is expected. for example, it means that some Special application class can see jre classes like String, but vice versa is not possible). If there are sibling class 
* .. loaders then they can’t access classes loaded by each other."
* --|---- (From JornalDec.. IMPORTAT) "Why write a Custom ClassLoader in Java? Java default ClassLoader can load files from the local file system that is good 
* .. enough for most of the cases. But if you are expecting a class at the runtime or from FTP server or via third party web service at the time of loading the class 
* .. then you have to extend the existing class loader. For example, AppletViewers load the classes from a remote web server."
* --|----|---- Consider above in relation to Pg133 of Hibernate-Validator Reference-doc pdf : "Problems with modular environments and JSR 223 come from the 
* .. class loading. The class loader where the script engine is available might be different from the one of Hibernate Validator. Thus the script engine wouldn’t be 
* .. found using the default strategy." This highlights the need to understand classloader. In a modular environment where classes can eb coming from different 
* .. places.. we should be considerate to different classloader getting used
*--|----|---- Example of cistom classloader: https://docs.oracle.com/javase/tutorial/deployment/jar/apiindex.html
*
* Classloader in App-Servers:
* .. In contrast to above, note that for app-servers, since they can support multiple applications, each with their own dependency-libraries which must be preferred 
* .. over the ones provided by app-server, so the order reverses. When loading a class, it is first search for in the deployed application, and then in the app-server
* .. See https://docs.jboss.org/jbossweb/7.0.x/class-loader-howto.html
*
* Java's ServiceLoader Mechanism:
* ClassPath represents the locations from where the classes are loaded. However, slightly different is concept of ServiceProvider - where the interface definition is 
* .. loaded from classPath, but the implementation of interface is loaded from service-provider. By doing so, one restricts to using only the definitions available from
* .. interface. Also, anything not matching the interface is not made available. This is in contrast to classpath where all files in the location are loaded
* --|---- To begin, see https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html     https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
* --|---- "java.ext.dirs" - See https://stackoverflow.com/questions/5039862/classpath-vs-java-ext-dirs   -- See in relation to above "spi.html" oracle docs link
* .. saying that "The java.util.ServiceLoader class helps you find, load, and use service providers. It searches for service providers [[**NOTE**] on your application's 
* .. class path] or [[**NOTE**] in your runtime environment's extensions directory]. It loads them and enables your application to use the provider's APIs. 
* .. [[**NOTE**] If you add new providers to the class path or runtime extension directory, the ServiceLoader class finds them]." Putting this together with 
* .. StackOverflow answer -- realize that classPath is for actual loaded classes. Sure putting service provider there works.. but it is more dangerous.. why would you
* .. want to allow loading class for execution from unknown. Instead, with extension route, you just load implementation of classes loaded from classPath. Note that
* .. this point is also mentioned earlier
* --|---- Also note from oracle docs that "The ServiceLoader class requires that the single exposed provider type has a default constructor, which requires no 
* .. arguments. This enables the ServiceLoader class to easily instantiate the service providers that it finds."
*
* META-INF:
* Related to above discussion is the concept of meta-inf. See https://stackoverflow.com/questions/70216/whats-the-purpose-of-meta-inf    and    
* .. https://stackoverflow.com/questions/17531625/how-to-include-a-config-file-in-the-meta-inf-services-folder-of-a-jar-using-ma
*
* "processorpath" in javac: Just for completeness regarding usage of "jar" file.. notice that it can use "-processorpath" to identify the annotation processors!
*
*
*





Annotation Processing Java. RELATED: How Lombok works

 * **VERY VERY VERY IMPORTANT**: Lombok gives @ToString. It'd be good to be able to add an "Encoder" such that it encodes the class before 
 * .. doing toString. This way, for certain unsafe classes, whenever they're written on logs, they'll always be encoded. HOWEVER, better idea 
 * .. is to never write unsafe values out to log - and exclude those fields fro toString!
 * --|---- RELATED: Use of Lombok's @NonNull (not validation's @NotNull).. means that Lombok will do a check that provided field is not empty 
 * .. else will throw exception.. this is good ..specially with Builder pattern
 * 
 See
 https://stackoverflow.com/questions/6107197/how-does-lombok-work
 https://notatube.blogspot.com/2010/12/project-lombok-creating-custom.html
 
 Also realize that generally when talked about "Annotation-Processing", what is meant is something different: See 
 https://www.baeldung.com/java-annotation-processing-builder
 
 See Chpt.13 of Hibernate-validator on annotation processing.. It seems annotation-processing is a good tool to use when you want to bring in runtime checks/failure to compile time! Any checks based on class-type can possibly use it.
 **IMPORTANT** Section 13.4.1 shows how to add annotation-processor via Maven itself!! Realize that Lombok is not exactly same as this.. it is different in that it does codegen - so, it also needs to be added in Eclipse itself! On same line.. Notice section 13.4.2 for Eclipse, where it says that nothing extra is needed after step 13.4.1 is done for maven - once again showing difference between ombok vs a proper annotation-processing
 

 
 
 Difference between >> and >>> operator: See https://javarevisited.blogspot.com/2015/02/difference-between-right-shift-and.html  -- Former does signed shifts, so the MSB bit that is 0 or 1 for positive or negative is the one that gets introduced as new bit on shifting. Latter does unsigned shifts. So if the earlier bit had 1 in MSB because of being negative, then unsigned shift will add 0 in shifting, making it positive

On generics:
1) If you define interface like: public interface Intfr<T extends Comparable<T>>{...}; then, writing classes should be like: public class IntfrImpl<T extends Comparable<T>> implements Intfr<T>{...}
2) When mixing generics with array.. be very very careful, or better use ArrayList<> instead! Use the 2 rules:
2.1) NEVER has any method that returns a generic-type-array. Even if you'll get to be without compile time error, it will fail at runtime. See https://stackoverflow.com/questions/34939499/how-to-properly-return-generic-array-in-java-generic-method ; Also, related are ideas that, say if you want to return T[], then you'll have to make it - but Java doesn't allow doing "new T[size]" on right side of assignment. What it allows is to create an array after type-erasure.. so one can do "new Object[size]", or in case of above "new Comparable[size]" - which is then casted to (T[]). This works because due to type erasure, (T[]) changes to (Comparable[]) in runtime ..However, Java will complain of unsafe type casting, but runtime errors don't happen. Now, this all works, up until very end where you're writing the code implementation for generic method that returns generic array and was defined in interface. Due to erasure, the array returned by interface-method-implementation is (Comparable[]), but the "calling" code would be putting a type in RHS for assignment that will be some type that implements Comparable, like (Integer[]). So, at runtime, the code ends up trying to do "Integer[] intArr = methodCall()" --> which changes to --> "Integer[] ... = Comparable[]...", and this breaks the covariant nature of arrays!! Thus, best to never return a generic array type!!
2.2) As mentioned above, if you want to make a generic array in intermediate processing, then create an Array of type after erasue, then cast it back to (T[]), and this should work!



Use of SOLID design principles ..particularly, the "O" (open/closed) -- this works closely with use of "delegation" for a complex logic.. allowing for easy future extension!
Also.. note the "S" or single responsibility.. which is useful in identifying method breakdown
L = Liskov substitution -> interface over implementation
I = Interface separation -> make 2 interfaces for particular behavior, and then define a third as extending both, if needed
D = dependency injection












Javascript:

https://stackoverflow.com/questions/30665395/how-to-remove-replace-cached-css-and-js-file-from-client-browser
https://www.google.com/search?q=expire+browser+javascript+on+logout&oq=expire+browser+javascript+on+logout&aqs=chrome..69i57.17094j0j4&sourceid=chrome&ie=UTF-8
https://security.stackexchange.com/questions/76813/what-is-the-state-of-the-art-for-forcing-logout-on-browser-quit
https://www.google.com/search?q=remove+javascript+source+file+from+browser&oq=remove+javascript+source+file+from+browser&aqs=chrome..69i57.9370j0j4&sourceid=chrome&ie=UTF-8
https://stackoverflow.com/questions/591685/how-can-i-dynamically-unload-a-javascript-file
http://www.javascriptkit.com/javatutors/loadjavascriptcss2.shtml




2.a) JavaScript review + unit test  
---- Dynamically load unload a javascript
----|---- See JSPatterns.com
----|---- Any corollary of classloader in JS
----|---- Adding script with id 
----|---- See https://stackoverflow.com/questions/950087/how-do-i-include-a-javascript-file-in-another-javascript-file
----|---- See https://stackoverflow.com/questions/9092125/how-to-debug-dynamically-loaded-javascript-with-jquery-in-the-browsers-debugg
---- Role-based javascript provisioning + A-B testing : These are not possible without JSP in the app itself

https://exploringjs.com/es6/ch_modules.html
https://darkpatterns.org/
https://css-tricks.com/simple-social-sharing-links/


See https://snyk.io/blog/after-three-years-of-silence-a-new-jquery-prototype-pollution-vulnerability-emerges-once-again/
For JS safety.. look at tips at bottom of page: (1) Ensure you are using safe recursive merge implementations... Always, when possible, use JS methods - So instead of $.extend(), use Objects.extend() (2) Consider creating objects without a prototype, such as Object.create(null) to avoid them being susceptible to prototype pollution attacks. (3) Avoid using square bracket notation when working with user-controlled data, and at all if possible. Consider using the Map language primitive for map-based structures. (4) Consider using Object.freeze().



See index.html and app.js code on https://spring.io/guides/gs/messaging-stomp-websocket/
-- There is a html <noscript> tab that can be used to warn users if javascript is not enabled -- good practice!!
-- In app.js, look at last set of line.. on how jquery can be used to change default behavior. $(function(){...})), where one can now put desired behavior in ellipses section
--|---- See #3,4 in https://www.sitepoint.com/5-ways-declare-functions-jquery/   --and-- notes in https://learn.jquery.com/plugins/basic-plugin-creation/  -- for better explanation of the syntax!!
-- Also, in terms of general design, see how nicely each components on webpage work together to keep features disabled in a workflow unless necessary connections are made. Having a UX where only suitable buttons are available to user is a good UX



*
* 
*
*/
// @formatter:on

public class OtherJavaDetailsController {

}
