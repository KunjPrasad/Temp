package com.example.demo.spring.boot.ctrl;

import org.springframework.web.bind.annotation.RestController;

//@formatter:off
/*
 * === CONFIG RELATED COMMENTS ===
 * 
 * **VERY VERY VERY VERY IMPORTANT** See IMPORTANT comments in EntityManagerConfigUserOne.java!!
 * **VERY VERY IMPORTANT** See comments in FaoConfig.java
 * 
 *
 *
 * NOTE: The @SpringBootApplication annotation also include @EnableAutoConfiguration -- This annotation tells Spring to automatically configure 
 * .. your application based on the dependencies that you have added in the pom.xml file. For example, If spring-data-jpa is in the classpath, 
 * .. then it automatically tries to configure a DataSource by reading the database properties from application.properties file
 * **IMPORTANT**: This why with SpringBoot, the maven-dependencies get automatically activated.. and so they need to be kept disabled
 * .. or @EnableAutoConfiguration needs to be changed
 * 
 * NOTE: These are some other JPA-implementation providers besides Hibernate: Oracle TopLink, Apache OpenJPA, DataNucleus, and ObjectDB
 * 
 *
 *
 * **VERY VERY IMPORTANT** : As mentioned in EntityManagerConfigUserOne.java, the EntityManager is not a thread-safe object. So, if instead of 
 * .. using SpringData, you want to make own dao methods by using EntityManager.. then ALWAYS remember, to create a request-scope, method-bound
 * .. EntityManager and use that.. and after query is done, destroy it. See https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#orm-jpa-dao
 * --|---- **VERY IMPORTANT**: Same link also describes use of @PersistenceContext annotation.. that has an optional attribute "type", which defaults 
 *         .. to PersistenceContextType.TRANSACTION. Another allowed value is PersistenceContextType.EXTENDED -- which should not be used. Apart
 *         .. from theory, this explains why a bean gets un-managed as soon it exits a @Transactional method.. because EntityManager has 
 *         .. PersistenceContextType of Transactional. The other option would be to make it extended, which is wrong.. because then it will hold
 *         .. over multiple requests and will not be thread-safe!! LAST, realize that if you want to make a non-managed bean back as managed,
 *         .. then again invoke a find() operation from EntityManager, or if using Spring-Data, then from Repository method! Also, related, see
 *         .. https://stackoverflow.com/questions/2547817/what-is-the-difference-between-transaction-scoped-persistence-context-and-extend
 *
 *
 *
 * **VERY VERY VERY VERY IMPORTANT**: See https://stackoverflow.com/questions/45176310/transaction-management-in-spring-boot-and-spring-data-jpa
 * .. in that even when @Transactional is made on Service layer, a transaction is opened again when request hits dao layer. But this time, it 
 * .. uses the transaction-manager defined in @EnableJpaRepositories! THUS, it is necessary that when using 2 different EntityManagerFactory,
 * .. that the Request-flow stays segregated, i.e. same tx-manager is used for @Transactional-on-service and for Repository-Bean
 * .. If you see yourself mixing the two.. you need JTA!!
 *
 *
 *
 * VERY VERY VERY IMPORTANT: When using @Transactional, Spring keeps the session for only as long as the transactional is there. Is there a way to keep
 * .. a hibernate-session open for longer period that a single transactional-method (..even though it may be actually be nightmare to handle and better
 * .. not done)?? Yes - See https://stackoverflow.com/questions/13531122/multiple-transactions-in-a-single-hibernate-session-with-spring  
 * --|---- HOWEVER, better than just answer, note the introduction of concept of Transaction-Synchronization and related class Manager, Adapeter, etc. 
 * .. The best "common word" explanation of what they are is provided in https://stackoverflow.com/questions/46047264/what-does-it-mean-by-transaction-synchronized-session-with-reference-to-spring
 * .. where it says "transaction-synchronized Session is a Session whose state is synchronized with underlying transaction. For example: after 
 * .. transaction is completed the session closes."
 * --|---- THE BEST OVERALL EXAMPLE on its usage is given in https://azagorneanu.blogspot.com/2013/06/transaction-synchronization-callbacks.html  
 * --|----|---- **VERY IMPORTANT.. NOTE..** It shows how, by registering TransactionSynchronizationAdapter, one can achieve a transactional "callback" like
 * .. behavior. This way, if something needs to run after a transaction is successful, it can be done then. BE CAREFUL THOUGH.. not sure how it'll
 * .. behave when there are multiple entity updates in transaction
 * --|----|---- IMPORTANT.. NOTE: The definition of afterCompletion() in Adapter where the thread-local variables are cleared. It is important to remember
 * .. to clear thread-local when it is done
 * --|----|----IMPORTANT.. Maybe "beforeCommit" can be used to check that fields to which a user does not have access to because of their permission level
 * .. are not updated by them
 * --|---- Can also see https://stackoverflow.com/questions/15026142/creating-a-post-commit-when-using-transaction-in-spring
 * --|---- **IMPORTANT**: See javadoc for afterCommit() https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationAdapter.html#afterCommit--
 * .. where they say that one can even do GET calls at the point, but cannot save them!! Maybe, if needed.. could this be a strict way to ensure that a 
 * .. transaction is read-only!!
 *
 *
 *
 * **VERY VERY VERY VERY IMPORTANT** DB-related data security
 * ---- For use of sp-el to get more complex queries -- leveraging auth-principal, etc. THIS IS BETTER THAN SERVICE BASED APPROACH because there are 
 *      lower chances of error when making service!! See https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions#spel-evaluationcontext-extension-model 
 * ---- @PreRemove (hibernate callback) may be enhanced to look for user role.. and, say, if it is not admin, then invoke deletion only as soft delete, 
 *      while making a new audit entry listing who did the delete. If it is admin, then allow hard-deletes
 * ----|---- On similar lines, add a listener, or change Spring's base-repository behavior (https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.custom-implementations) 
 *           to only pull data that has not been soft-deleted; Or pull all data only for admin users (alternately, can make another method that does this). 
 *  	     Doing so will apply this logic in a consistent and system wide manner
 * ---- We can start with a data-model design, where portions of data that are accessible only to higher authority users are kept. For example, say, there is 
 *      medical data of user. This can include details that public can read (A), that can be shared with other partner companies (B), that can be shared with 
 *      insurers (C), and to be kept only with doctors (D). In this case, patient entity can be composed by using foreign key of A,B,C,D.. and say, for the 
 *      entity representing (D), add a @PreUpdate and @PrePersist such that when anyone other than doctor uses it.. then it throws error. **ALSO**, same logic
 *      when entity is read. The primary entity can still be read with lazy loading on A-D. It is just that if the logic becomes such that (D) is actively 
 *      read, then an error gets thrown. A question can be -- when will @PreUpdate even get invoked in this case? -- Answer -- First, realize that @PreUpdate
 *      is going to get called in response to some PUT call, based on some user input. In ideal case, at time of this PUT call, the deserialization
 *      process should be such that it should ignore user-provided json if user does not have sufficient privilege to make such changes (or throw
 *      validation-exception, maybe by using validation-groups)! But if that does not happen, then this acts as second level check on such codes. 
 *      Along with modifying spring's base-repository read() method to throw error when called from disallowed user, the need for @PreUpdate check 
 *      should not arise.. but is a good fail-safe!
 * ----|---- SO.. a related question is whether it is possible to do a user-dependent deserialization!! -- Yes - best use @JsonComponent, while also 
 *           leveraging Spring-wiring (see in ReqRespController, and also in https://www.baeldung.com/spring-boot-jsoncomponent and 
 *           https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-json-components), or alternately use custom 
 *           serializer-module, so that the setting applies system wide, without continuously using @JsonSerialize and @JsonDeserialize..
 * ----|---- ALSO see "RequestBodyAdvice" (in ReqRespController, extra details #2.5) -- that may be better suited here
 * ----|---- It may be asked that why not to just control serialization/de-serialization, and not worry about DB access. The problem in such cases is that
 *           incoming request may be able to run operations using disallowed fields and then store result in some other allowed field, and that should not 
 *           happen. So, DB level restriction is needed.
 * ----|---- It may be asked that why not to just fully deny access.. while that is possible and easy to implement, it is highly restrictive in nature
 *           and may not always apply
 * ----|---- Why not have annotations on fields.. such that when @PrePersist or @preUpdate is called.. then read the field-annotation and allow update or not.
 *           This still suffers from same issue that disallowed fields could be used to make data that is then stored in allowed fields. ALSO.. this would
 *           actually be harder to implement since it requires that user deftly puts annotations in all necessary fields. If same table is reference via
 *           foreign key in two oher tables, and one place has annotation and other does not.. then it bad coding. So, best design is to make different tables.
 *           WHILE, OF COURSE, UNDERSTANDING.. that this does cause a hit in performance due to need for join.
 * ---- VERY VERY IMPORTANT: Above discussion may also be seen to match CQRS pattern for microservice -- see ReqRespController#10 for holistic view
 * ---- VERY VERY IMPORTANT: See comments in "OtherDetailsController" on how @OneToOne could be nicely used for such operations!!
 * ----|---- REALIZE that if the need is for "time-based" security, i.e. an information is available only up to a certain time from creation, then one do not need
 *            @OneToOne based table-breaks. Instead @PostLoad (for full list of listeners, see https://docs.jboss.org/hibernate/orm/4.0/hem/en-US/html/listeners.html)
 *             can be used to simply hide the columns or entire data. FURTHERMORE, at time the data is initially added, an entry can be made in Transactional Outbox
 *             to delete it once the time-span has elapsed.
 *
 *
 *
 * **VERY VERY VERY IMPORTANT** ON DB Audit via Hibernate:
 * ---- Realize that if wanting to store audit-values in DB (even via, say @MappedSuperclass) -- there should be 2 different levels of  
 *      doing so, as it was done in Patentcenter. This is because the tables containing "standard-data" will have additional fields of start and end date 
 *      when the standard data is in use. THIS IS IN ADDITION TO other fields like version, created-date, create-user, etc. will go for both standard data 
 *      and also user-data
 * ---- See https://stackoverflow.com/questions/16622073/same-jpa-callback-method-in-mappedsuperclass-and-child-class  -- A good auditing strategy could be to 
 *      add @PrePersist, @PreUpdate, @PreRemove - under the audit class that automatically updates the lastModUser, time, etc. This way the auditing logic 
 *      implementation would become consistent, uniform, and not to be shouldered by code. For a full list of available listeners in Hibernate, see
 *       https://docs.jboss.org/hibernate/orm/4.0/hem/en-US/html/listeners.html (chcek recent versions for updated list)
 * ----|---- In terms of design, rather than change lastModUser, one may want to append an audit-entry in a list, so as to keep running entry of all 
 *           changes. However, this will create a lot of data. A benefit of using separate method to do so can now be seen.. we can programmatically select 
 *           for different cases where a running entry should be kept versus when only a single netry be kept -- depending on importance of target object
 * ---- See https://techblog.bozho.net/spring-managed-hibernate-event-listeners/  and  https://dzone.com/articles/spring-managed-hibernate-event-listeners
 *      on registering listeners for Hibernate.. in Spring boot. One thing not shown explicitly in these pages.. you can cast the 
 *      localContainerEntitymanagerFactorBean (..which is sub-type of EntityManagerFactory) as HibernateEntityManagerFactory. It is then also possible
 *      to get SessionFactoryImpl by doing (SessionfactoryImpl) hibernateEntitymanagerFactor.getSessionFactory
 * ---- IMPORTANT: Look at Domain-Event pattern for microservice: https://microservices.io/patterns/data/domain-event.html -- just like updating 
 *      audit variables like setting modified user and time for any changes, implementing the domain-event can also be done by these listeners!!
 *      "UP TO AN EVENTUAL CONSISTENCY", this pattern provides a nice way to have simulatenous commits in Database and Message-Queues!
 *
 * 
 *
 * IMPORTANT EXTRA INFO -- RELATED TO SPRING-REPO:
 * ---- See a good example on how to make a query in abstract class that becomes generic for all sub-class. See
 *      https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query.spel-expressions   ALSO on same page, see use of @Modifying
 * ---- For ability to join Spring repository with custom repository interface, see 
 *      https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.custom-implementations
 * ----|---- NOTICE how this allows updating base-repository behavior on case-by case basis.
 * ---- HOWEVER, For ability to globally change base-behavior of spring-repo.. for example, throwing exception when entity is not found!! See 
 *      https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.custom-implementations
 *  ----|---- **VERY VERY VERY IMPORTANT - FROM SECURITY**: NOTE the presence of "JpaEntityInformation " in constructor. This can be used to identify the specific
 *                java-type of the entity. This way, once can add behavior, like, if a Entity has a member-field with annotation @ManyToOne, and that member field is 
 *                not a reference to standard-data, then disable findById() method. Essentially, this happens in case when we have parent -> child relation in DB table,
 *                (excluding standard-data as parent, which will apply everywhere). In such cases, access of child-table entry is done in logic when endpoint call like
 *                webContext/parent/{parentId}/child/{childId} are made. HOWEVER, in such cases, we should not just try to find child-entity by child-id and return
 *                it. We should also establish that child-resource with {childId} is also linked to parent-resource with {parentId}. Thus, findById() should be disabled!!
 * --|----|---- In actual implementation, a concurrent-map can also be kept that stores the name of entity(s) for which findById can be safely called. Given a rest-style,
 *                   this is likely to be classes whose name start with "Stnd" prefix.. or just one data Entity -- the central entity in model.
 * --|----|---- In these notes , we say that @OneToOne could be used to break down an otherwise big-table into smaller parts, say for security concern (search for 
 *                   "DB-related data security"). For such cases, we can now require that findById() is again disabled. Since the one-to-one is conceptually based on "id" 
 *                   of common table, so one should always use that id only, and never the id of particular table.
 *  ----|---- From code-management perspective, this is useful to constrain coders to use only a particular set of functions - giving better code uniformity
 *
 * === [END] CONFIG RELATED COMMENTS ===
 *
 *
 *
 * === DB-DESIGN COMMENTS ===
 *
 * IMPORTANT: NOTE: Look at comments regarding Package-ordering in OtherDetailsController. It has comments on entity-design also
 *
 *
 * **VERY VERY IMPORTANT**: See in connection to JMS: sometime it may be necessary to just get a sequence so as to get uniqueId (..like, 
 * .. associating uniqueId to each new message put on JMS) 
 * --|---- The way to do so: create a sequence, and table with just one entry. It seems that accessing sequence without a table is not possible!
 * .. Various posts suggest that Oracle provides special DUAL table through which the sequence can be identified without making a table
 * .. BUT, I don't know if this would be portable to other DB. Also, don't know how to integrate Spring-data repository with it!
 * --|---- In Spring data repository add: (Refer: https://stackoverflow.com/questions/46240529/getting-next-value-from-sequence-with-spring-hibernate)
 *              public interface EventRepository extends JpaRepository<Event, Long> {
 *                  @Query(value = "SELECT seq_name.nextval FROM dual", nativeQuery = true)
                    Long getNextSeriesId();
 * --|---- Another important thing to note is that in this case, we just need uniqueness of Id - but we don't need them to be strictly monotonic
 * .. among different requests. In such cases, change "incrementBy" option for efficiency!! See https://royontechnology.blogspot.com/2010/04/note-on-allocationsize-parameter-of.html
 * 
 *
 *
 * **IMPORTANT**: Understanding fetch strategy. See https://docs.jboss.org/hibernate/core/3.6/reference/en-US/html/performance.html#performance-fetching
 * .. and https://stackoverflow.com/questions/32984799/fetchmode-join-vs-subselect
 * --|---- The idea is that when doing FetchType.SELECT, it triggers 1 query for each entity obtained in first query. For FetchType.SubSelect, it
 * .. does a query to load result of first query in memory and then loads all associated values using other query. FetchType.Join actively loads 
 * .. everything using join. So, if the design is such that the result of the first query will itself always be very small, then you can simply 
 * .. actively load using join. As the result from first query starts becoming big and/or say you'll run filtering on first result a lot before
 * .. calling lazy-load of related entities and collections, then start going towards subselect/select fetch
 * 
 *
 *
 * **IMPORTANT**: See under ReqRespController#10 - On Async-processing - and mention of Domain-Driven-Design and Domain-aggregates
 * --|---- "Doman Driven Design" and "Bounded Context": see https://martinfowler.com/bliki/BoundedContext.html -- Essentially the idea is that when doing
 * .. DB design, have a sort of broad cluster in table structure that matches with similar clustering of functionality. This brings in "bounded context"
 * .. with minimal tables at intersection of different domains. 
 * --|----|---- DO NOTE: Separate from above would be the "constant data" tables, probably hosted under different schema - but, they can join to different
 * .. tables in different context. ..And this is fine, because these are effectively constants that are stored in DB
 * --|---- In context of "domain design", note that sometimes, due to legacy splitting or just the way implementation is done, there can be data with 
 * .. similar use (i.e. Domain Aggregates, see https://martinfowler.com/bliki/DDD_Aggregate.html) but which are scattered across different services. A 
 * .. nice implementation to have a coherent aggregate could be as mentioned in reqRespController#10 - i.e. querying transparently and via async-processing
 * .. Additionally, logic can be put within default implementation of Spring repositories (which could/could-not be Entity-class specific), or by using
 * .. aspects and annotations.
 *
 *
 *
 * NEVER DO THIS: Look at "Event Sourcing" design pattern (https://microservices.io/patterns/data/event-sourcing.html) - maybe i'm misunderstanding, but 
 * .. it says to keep running updates and then compose current state later. This is what Cassandra does.. or what Blockchain would do! SO.. best, don't
 * .. try to do it yourself because it can get super messy -- plus, there may not really be a need for it!!!
 * --|---- **IMPORTANT**: One place where I can remotely see this pattern "sort-of" coming in.. is in a stituation as discussed in ReqRespController #10.iv. 
 * .. Consider that you have a microservice that prepares a DTO by doing CQRS on multiple repositories. User updates something and now you need to push
 * .. the update to repositories. However, let's say one the update hasn't gone through.. so the data is in stale-state. Now the user asks for data again.. what to do?
 * --|----|---- FIRST, model the data being pulled in from other repositories and having @OneToOne with your data. Following notes from OtherDetailsController,
 * .. this makes it easier to imlement security concerns, wherein, if someone is not authroized to view a data, then they don't get to see it!
 * --|----|---- SECOND, for sending updates to each repository, start that processing chain by making a Transactional-Outbox entry. This will at least create 
 * .. pseudo-transactional behavior in updating the primary entity. AT SAME TIME, it is suggested to have boolean valued fields within primary-entity that are 
 * .. marked at "DIRTY". Now, everytime a new data-call comes that tries to access a DIRTY field, then that call fails. But if it only tries to make partial call to 
 * .. clean fields, then it succeeds. So, it behaves effectively as if we tried to do event-sourcing and returned a success when it was successful, and a failure if
 * .. any operation is still pending. 
 * --|----|---- THIRD, just to explicitly mention.. everytime a tranactional outbox message is asynchronously processed, it also removes the "DIRTY" mark on
 * .. the entity along with removing the message in transactional outbox. ONE MROE THING TO NOTE.. the processing logic in remote repositories should be such
 * .. that it should not raise error if the same process is retried after it already succeeded once - but should silently send a success response without any changes
 * 
 * === [END] DB-DESIGN COMMENTS ===
 */
 
 
 
 
 
 /*
 TODO:
 
 1) See https://docs.jboss.org/hibernate/stable/annotations/reference/en/html_single/
 ---- There can be cases where more than 1 class uses same id -- but why? Also, is it concurrency safe? 
      ----|---- @Related to above is @MapsId, which allows for use of same-id in the other linked entity
 ---- Other annotations: @JoinTable - to create associations via intermediary table. Although it can be used for @OneToOne to @ManyToMany.. this is
      best used in case each entity can develop independently and then need to join.. Example: accepting resume via file or by web-entry and creating
	  relation between them so that the files and metadata then go as one
 ---- [[@Cache, @Cachable]] ; [[@Embeddable, @EmbeddedId, @AttributeOverrides, @JoinColumns (when there are multiple column that combine to form 
      a complex foreign key object)]] ; [[@Lob, @Version]] ; [[@OrderBy -- to control ordering of list-type in children]]
 
 ---- (**VERY VERY VERY IMPORTANT**) @Filter, @FilterJoinTable -- can this be used to control what fields are retrieved / updated based on 
      user permission level?
 ----|---- If someone reads an entity where some columns are filtered out.. what happens if they add new columns, will it break previous association
 
 
2) NICE ARTICLE on why the connection pool size should be between ~2-2.5 times the core size. If coding is not proper and there are transactions that are held back behind large-time-consuming operations (like file upload), then one may possibly go higher. See https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing


3) Use of @Where clause.. and a better option: use of @Filter clause in JPA to enforce common baseline clauses when performing retrieval. 
--|---- See https://www.baeldung.com/hibernate-dynamic-mapping    https://dzone.com/articles/row-level-security-in-hibernate-using-filter
--|---- Realize that this can also be leveraged and used with Spring spEL  https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions
--|----|---- **VERY VERY VERY IMPORTANT**: The mechanism being referred here is a combination of Spring-data and Spring-security. See Pg165-170 of 
* .. springSecurity pdf notes - and also related information added in Spring-Security java notes
--|---- How to combine @Filter with Spring-Data: 
--|----|---- (a) **NICE TRICK**: add an aspect where the session in entity-manager is modified everytime it is created. See https://stackoverflow.com/questions/32228031/access-to-session-using-spring-jpa-and-hibernate-in-order-to-enable-filters/32230857#32230857
--|----|---- (b) Either create new base repository, or modify the base-behavior of Spring-repository! See https://stackoverflow.com/questions/30430187/filters-for-spring-data-jpa

4) **VERY VERY VERY IMPORTANT**: Look at comments under "Domain Object Security using ACLs" in Section 5.7 - in SecurityController.java. It shows how
* .. methodSecurity can be used to control object access; AND; in doing so, creates a DB-model equivalent mapping of "aspect" like behavior from service - 
* .. such that it is even applicable for audit!

 
 5) NICE EXTERNAL EXAMPLE: https://vladmihalcea.com/how-to-update-only-a-subset-of-entity-attributes-using-jpa-and-hibernate/
 ---- use of "updatable" to prevent a column from being updated
 ---- use of @Transient to prevent storage and to make "related" values that could be used
 ---- use of @DynamicUpdate (**VERY VERY VERY IMPORTANT** for performance!!)
 ----|---- See https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.modifying-queries -- use of @Modifying in spring-data to force execution of 
               custom query -- REad around the section. Note: (a) Use of escape(); (b) comparison of using @Modifying for delete vs deleting-by-JPA. Best, DO NOT use
			   @Modifying and stick to proper JPA
 
 
 6) NICE EXTERNAL EXAMPLE: https://vladmihalcea.com/the-best-way-to-soft-delete-with-hibernate/
 ---- NOTE THE USE OF: @SqlDelete, @Loader, @Where -- for controlling global behavior!! **QUESTION**: How to make it such that it is different for 
      ordinary users versus for admins? Is there a way to change it programmaically for admins?
 ---- Would a better way to implement above be to just add listeners -- as mentioned in DB-security section (#2) and in DB-audit section (#3) so that
      it remains dynamic. OR, maybe, make expicitly different access methods
 ---- **IMPORTANT**: See https://www.baeldung.com/hibernate-dynamic-mapping -- when using dynamic mapping, like @Where.. then it is only evaluated at time of
      data pull. Should the entity be changedmidway in transaction.. then the value won't get reflected! 
	  **NOTE**: Also, it shows the use of @Any -- to use for inheritance
 
 
 7) Question: How to ensure that a column is updated only if it is null - and that once it gets a non-null value, then it is never changed?
 Ans) Change its setter method to do the logic only if existing value is null. If it is non-null, then throw error. Hopefully, same will work properly
      when reading entity from the DB
	  
 

 */
// @formatter:on

@RestController
public class JpaController {

}
