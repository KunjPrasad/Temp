package com.example.demo.spring.boot.ctrl;

// @formatter:off
/*
* **NOTE**: THIS FILE CONTAINS "OTHER J2EE DETAILS, NOT PARTICULAR TO SPRING"
*
*
*
*
* **VERY VERY VERY VERY IMPORTANT** (Data-Governance): PACKAGE-ARRANGEMENT : Directed-Acyclic-Graph (DAG)
* 
* -- The most ideal structure of packages, both within a monolith, and in a microservice, would be to have classes and packages in a DAG. While breach of
* .. class level DAG is raised as a circular refrence error in compiler; the breaking of package-level DAG is not identified and leads to jumbled code.
*
* --|---- CHECKS:
* --|----|---- (1) create a directed link between a class and all classes that it call, with the link pointing inwards to the former class. 
* --|----|---- (2) Do this for all classes in the application. 
* --|----|---- (3) Replace class name with package names, and join duplicate package-name nodes into a single "super-node".
* --|----|---- (4) Check#1: Ensure that there are no cycles within the set of nodes and links within any "super-node". 
* --|----|---- (5) [Digression] Check#2: Ensure that all links within the "super-node" have package-default modifiers, and not public. Related: If a method 
* .. is called with a class itself, check that it is package default, unless it is also called by some other outside class. Also do check#1 in point-4. 
* --|----|---- (6) Check#3: MOST IMPORTANT: The links among "package" level super-nodes should not have a cycle!
*
* --|---- IMPLEMENTATION:
* --|----|---- While sonar and check-code does allow for "package" level tests, realize that these perform tests on one class at a time. For example, they
* .. can probably check that for all classes in entity package, the class name should have suffix "Entity", and that vice versa. But, they won't be able
* .. to check how this package forms a DAG with other packages. Probably that's why such "data governance" tool is missing. BEST would be to have a code
* .. that is probably bound to a maven phase after the "compile", wherein it first creates the full DAG, and finally does above analysis.
*
*
*
* -- Package DAG structure for a general web-project with dto, entity, service, controller, utility and configuration classes. 
*
* --|---- (1) INITIALLY, it was suggested that DTO(s) define the way services communicate, and so they should be top-most in DAG hierarch, above the entities.
* .. While this is true in terms of design (..because customer contracts are made first), REALIZE that DTO also consists of validation-annotations, which are 
* .. backed by validation-implementation. If these implementations are independent of any repository or service, then DTO will continue to be topmost. However,
* .. if validation-implementations autowire service and repository classes, then DTO gets pushed to just one above service's hierarcy level in DAG. Since DTO are
* .. independent cluster from Entity/Repository, so this change isn't something that suddenly breaks DAG, even though the position of DTO is shifted.
* --|----|---- That being said.. realize that an Entity can store information beond that in DTO (like, lastModUser, lastModTime, etc). So, for DTO to depend on Entity
* .. is not unlike; And can be seen as a logically sound criteria. **VERY IMPORTANT**: In fact, this would be natural if DTO validation depended on some history. It's 
* .. just that such criteria are preferable to put in Service, and then they'll occur in one transaction!!
*
* --|---- (2) Since a large-DTO can contain multiple small-DTO as fields, so the small-DTO(s) go higher up in class level DAG than #1, and if they are 
* .. associated with different functionality, then they also go in a separate package, which cwould go at higher level in package-DAG than #1
*
* --|---- (3) Once the DTO are formed, we need for a way to store them in DB. So the Entity classes come in picture. These are independent of DTO. So, a 
* .. separate "super-node" section should start for them, mking them as new set of parents. Just to mention: These are DTO in #1 and #2 are disconnected 
* .. as of now, and we'll being in the connections as the analysis develops.
*
* --|---- (4) For entities also, a large-Entity can be comprised of small-Entity(s). However, the reasoning flips here copared to DTO. 
* --|----|---- (4.1) When there is a child-entity to a parent-entity, it means that the "parent" needs to be persisted first in DB, so that its 
* .. primary-key could then be given to the "child" which can then persist. So package with "child"-entity should go at lower level than "parent"-entity.
* --|----|---- (4.2) Since "child"-entity are at lower class level than "parent"-entity, and also that child-entity are free to be in a different package,
* .. this implies that a "parent"-entity should "never" contain references to child-entity. SO, WE SHOULD NEVER HAVE @OneToMany annotation in ANY entity
* --|----|----|----- **IMPORTANT**: realize that this design only works when parent-Id, which is used by children as foreign-key.. that "id" never changes.
* --|----|----|----- **VERY VERY VERY IMPORTANT**: Realize that even when there is a natural unique element, like a category name in standard-data-table, 
* .. one should still use numeric id(s). This way, in future, if business comes back to say that they want to change category name, but it'll still be 
* .. unique, then such a change can be easily handled - without messing any foreign key update/mapping
* --|----|----|----- Contrast this with DTO structure in #1 and #2, where the parent-DTO will always contains child-DTO
* --|----|----|----- ALSO NOTE: That with this entity structre, a @ToString call on parent will NOT automatically cover child. So, logs need to have 
* .. @ToString call on child-entity to cover all details.
* --|----|----|---- REALIZE that absence of @OneToMany means that any entity class cannot have a collection type member!
* --|----|---- (4.3) Realize that this structure also helps with proper auditing. When lastModUser/lastModTime in an entity is changed, one can simple continue
* .. recursively over all parent entity (by searching for @ManyToOne and @OneToOne member in the entity and continuing up till no more are found) updating
* .. the values. Depending on use case, one may add 2 fields "lastModTime" and "lastChildModTime". This way, if you have a GET request which does not ask
* .. for child metadata then you can simply ask them to continue using whatever they have. As said in ReqRespController#10.iii, the lastModUser and lastModTime
* .. could be stored in threadlocal map variable so that the same value gets applied for entire request
*
* --|---- (5) Given above design of parent/child entity in #4, can effective parent and child service structure be made? 
* --|----|---- SHORT ANSWER: YES - and that is the good design. **ALSO** this explains why child-service should be kept in distinctly different package 
* .. than child-entity
* --|----|---- (5.1) How do we go about making a child-entity - when parent is already present and was made separately? (i) Call comes to childService. 
* .. (ii) ChildService invokes parentRepository (or some utility) to see if an entry with parent-id exists. (iii) ChildService makes "unbounded" 
* .. child-entity object, with its @ManyToOne reference to parent set appropriately, and then persists it. (iv) It calls some utility to convert persisted 
* .. child-entity to DTO. **NOTE** that with this design:
* --|----|----|---- The "ChildService" comes at a DAG level lower than Child-Entity, and so, also lower than Parent-Entity. 
* --|----|----|---- Since the method logic also involves making a DTO, it also comes at a lower level that Child-DTO. 
* --|----|----|---- Realize that if we are dealing with multiple hierarchy levels, like, call to "POST /grandparent/{gId}/parent/{pId}/child" endpoint,
* .. then it needs to make call to grandparent-repository with {gId} and to parent-repository with {pId, gId} to verify the relation up to all levels
* .. of hierarchy up to immdiate parent. Also note that except the first in hierarchy, all other calls should be made using both the corresponding entity-id
* .. and using the foreign key - to ensure the existence of entity and of foreign-key-relation. **HOWEVER** REALIZE THAT when making the actual child-entity,
* .. only the reference to last parent is saved in child - so maybe move that last step out in separate method. See #5.3 on why that is good design
* --|----|----|---- It is discussed later on how to form relation between ChildService and repository / utility classes such that it fits DAG structure 
* .. for the use cases mentioned above.
* --|----|---- (5.2) With design of #5.1, consider the case if we would instead have @OneToMany List type in parent. Had it then been necessary
* .. to take parent through additional processing, then that would be bad - because we'd have to manually update the @OneToMany list after creating-custom
* .. the new child-entity, and append that child-entity to parent. Even worse, iterating over @OneToMany list would invoke multiple JPA calls in backend
* .. which destroys performance. With this design, the child can be added/updated/deleted independently, and without affecting the parent, which can then
* .. be taken for additional processing! AND should it become necessary to retrieve this child object again later, that will also be quick because it 
* .. would have already been cached in Hibernate's entityManager.
* --|----|----|---- **IMPORTANT**: REALIZE that with this design, it naturally fosters to make findBy.. methods in child-repository that takes child-Id and all parentId
* .. like, findByIdAndParentIdAndParentGrandparentId(long childId, long parentid, long grandparentId)
* --|----|----|---- **IMPORTANT**: See notes in JpaController (search for "repositories.custom-implementations"), where one can now easily disable the 
* .. findById() method call for child-entity, while still allowing it only for primally-parent entity, or the standard-data-entity
* --|----|---- (5.3) Unlike #5.1, How do we simultaneously make a parent and child - when parent wasn't already present? FIRST, BEFORE STARTING, do verify 
* .. if this is even a valid business requirement. Considering that there are separate parent and child entities, it means parent can exist independent 
* .. from child - So why are both being simultaneously made? One case why it might be is if we want to make a "default" child entry for every parent. In 
* .. this case, call is handled by "parentService", which should (i) make unbounded parent-entity, (ii) save it in repository, (iii) send saved-parent-entity 
* .. to "childService" which binds it to new unbound "child"-entity and saves it. 
* --|----|----|---- **IMPORTANT**: Realize the design consideration that have come up to now: (a) This works whether or not Hibernate does the 
* .. entity-management as long as it is in one transaction. (b) Our initial conceptual design of "parent" being made firsta dn existing before "child" entity
* .. still holds even in this multi-step process. (c) This mixes with another design consideration in #5.1 to have the method that makes new child-entity
* .. as being separate one and accept the parent-entity as an argument
* --|----|----|---- **VERY VERY IMPORTANT**: This means that "ChildService" should lie at higher DAG precedence than "ParentService". 
* --|----|----|---- **VERY VERY IMPORTANT**: This also means that if multiple children needs to be made as part of processing, then it is job of parentService
* .. to make calls to each of them one by one as part of their own method
* --|----|---- (5.4) Knock-on effect of #5.1-5.3: How do we read / update / delete child-entities based on query-param criteria? Say, the endpoint
* .. is "DELETE /parent/{id}/child?type=certainChildType". This request should technically be handled by childController. It can work as already shown in
* .. #5.2 but now, it retrieves a list of child-entities and deletes them. 
* --|----|----|---- **VERY VERY IMPORTANT**: Realize that if this were required as part of some processing on parent-entity, then parentService can call 
* .. childService to execute these operations, and then still continue working on parent-Entity. As said in #5.2, that would be real hassle.
* --|----|---- (5.5) Knock-on effect of #5.2: When wanting to delete entire hierarchy, it is best to configure casacade in parent - in table design, since
* .. it won't be possible to do so on Hibernate's side (since Entity table won't have reference to children-entities). Then, simply delete parent entity
* .. in a parentService method, and the DB should automatically delete entire hierarcy.
* 
* --|---- (6) Given above parent/child design for dto, entity and service: Where do Util classes lie? Do they handle converting from entity to DTO?
* --|----|---- When wanting to handle "GET /parent/{id}" call, it may be necessary to pass information of parent and all children in DTO. By the REST
* .. structure, thiscall should come to ParentController, and then go to ParentService. ParentService, as we know above, can call childService, and so, 
* .. it can collect all child-DTO (from corresponding child-service(s)) that can then be used to form the main, big parent-DTO. The main question here  
* .. is that whether this be done in serviceclass or in Util class?
* --|----|---- (6.1) We know that ParentDTO will never be used by any lower childService - so there seems to be no point in making separate "Util" class 
* .. to hold that logic
* --|----|---- (6.2) The main reason up until now to have a "Util" class is to wrap the logic from repository and throw exception. But, idealy, that should
* .. best be done by repository-implementations or general application-wide repository config
* --|----|---- (6.3) **VERY VERY IMPORTANT**: Wih this understanding - What actually is a "Util" class?
* --|----|----|---- Best way to think: It is a class such that ideally all its method should be static. Meaning, it should do basic things and be able to 
* .. function without need of any autowiring, etc. Example: converting list of files into zip, or tar, or pdf!
* --|----|----|---- If you want "util" equialent to "any" class, be it parent/child-entity, or parent/child-dto, it should work sufficiently with that 
* .. class itself and not need any other changes
* --|----|---- With above undestanding, "Util" classes should be seen as floating around in application, either being connected to JDK class and thus lying
* .. at top of hierarchy; Or, just having a single connection to a class and no more - and so lying at its DAG level
*
* --|---- (7) **VERY VERY IMPORTANT** Given above parent/child design for dto, entity and service: How does a childService be arranged, when its execution 
* .. depends on standard data associated to parent? 
* --|----|---- Example: Let's say that we have categorization in some standard-data-field like "std1", "std2"... "stdN". The parent-entity can be 
* .. associated with some category from the standard-data-field. So, technically, the standard-data-field is higher in hierarchy that parent - because
* .. in this case, parent-entity contains a @ManyToOne pointing back to standard-data-field. The standard-data-fields can also be considered as controlling
* .. the environment or context within which parent-entity exists. Now, if a child-service needs to do processing, how should the DAG be?
* --|----|---- (7.1) We've previously established that childService should not call parentService. However, since standard-data sets the "context" for all
* .. data, this means that it comes higher-up in chain, and so, standard-data-field-service should be callable by childService / parentService. Think of it
* .. like being able to use SecurityContextService in Spring-security. Similar situation holds here.. just that we are wiring the standard-data-field-service
* .. in each bean and using as needed.. rather than wiring it in request-level
* --|----|---- (7.2) Realize that a particular value of standard-data-field could control multiple functions. For example: "std1" type means that the
* .. parent-entity of the type remains active for 1 day and then expires, it accepts mime1 type, and should store it in location1. "std2" means that 
* .. parent-entity of the type never expires, it accepts {mime1, mime3} type3, and should store it over http instead! In such cases, it is best to use
* .. a backing "delegation pattern" when implementing standard-data-field-service. This way, "childService" will call "delegation" logic in "standardService"
* .. to get handle to a particular delegate, and will then call methods on it.
* --|----|----|---- REALIZE that sometimes the logic may itself delegate to a visitor pattern to go through multiple steps -- Like "file-save" strategy
* .. being to save in multiple locations one after other
* --|----|---- (7.3) **VERY VERY IMPORTANT**: Realize that as described above, standard-data-field lies much above in DAG rank. So, it shouldn't really reference
* .. to any other entity/dto objects in data-domain. However, same standard-data-field could act as a "selector" to effect a particular processing branch for the 
* .. corresponding special case. 
* --|----|----|---- Thus, the standard-data-field-service should lie high up in hierarch, so that all entity-repository or service methods can use it. 
* --|----|----|---- HOWEVER, the stndard-data-field-delegate is something that should lie after all repository, but before all service. This way, all service can use
* .. it to effect particular application specific data movement based on some strategy. 
* --|----|---- (7.4) **VERY VERY VERY VERY IMPORTANT**: Realize that for standard-data-entity, you should NOT mindlessly cascade-on-delete "ANYWHERE".. 
* .. so, you don'tableend up deleting a bunch of standard-child-data, or user-data when higher level standard-data gets deleted. A better design would 
* .. be to add an "expiry" date, after which that standard-data could not be used
* --|----|---- (7.5) REALIZE: Since standard-data is part of "context", we may also have cases where they form some kind of "security-role". For example,
* .. send out a particular child-data piece only if the standard-data associated to parent is a certain value. To be uniform and consistent in handling such context
* .. based branching behavior, it is good to consider them as a "security-role", and then use the security-role(s) associated to a request to control
* .. serialization / deserialization, auditing, allowing read/write from DB. The only difference now is that these "context-based" roles are internal to application and
* .. not business-wide
* --|----|----|---- NOTE that this is also a good way to realize that just like we want security-roles to be transferred in a sync processing, we should also emphasize
* .. on properly transferring the context data if wanting to do async processing
* --|----|----|---- **ON SECOND THOUGHT**: It is suggested that while one can "think of them as Roles in certain aspects".. you should NOT think of them as role
* .. relating to Governor-Service (discussed later). The use of Governor-service is to integrate DevSecOps across organization and bring in standard-value based 
* .. roles makes the entire process very complicated. Best, use the standard-value based roles to define validation logic exposed via Standard-value Delegation-service
* .. or just for purpose of logging.. But do not integrate it with Governor service (or features used to integrate with Governor Service)
* --|----|----|----|---- **VERY VERY VERY IMPORTANT**: Best to add a Post-Processing-Aspect.. and have it contain a list of "standard values" that can get associated.
* .. This "Post-Processing" (not pre-processing) aspect can be associated with public @Service methods to look for the standard values and throw exception if an
* .. operation is disallowed for standard values.. or if the standard-value is missing. One thing to note is that such a logic should only be done for outermost transaction
* .. and not redone when a @Transactional method refers to other.. so, it should be synchronized with Transaction, but happen before Transaction is committed
* .. **AND** this brings out an important use of TransactionUtils [[Or, alternately, have this be done by Hibernate-listener when trying to create/update/delete an 
* .. entity and in the process, looking at the URL and recursively reading parent-entity, and using associated standard values throughout the chain to check if update 
* .. should be allowed]]
*
* --|---- (8) Expanding on #7 above: Consider case when behavior of child2-service depends on value of child1 of parent-entity. How is this handled?
* --|----|---- (8.1) FIRSTLY, realize that the situation presented in example is likely very wrong.. for reason, how will child2 know which "child1-id"
* .. to use among different child-1-entities under parent-entity? So, the only available option is if there is a one-one mapping between parent-entity
* .. and child-1 entity [[Example: as discussed in JpaController, to enforce data security, we break an otherwise single record in multiple tables and
* .. allow retrieval only if user has correct permission. Since the break is artificially done on otherwise single record, this becomes a one-to-one
* .. mapping]]. With this understanding.. Realize that we have already established that a childService can use any "repository" method, so child2-Service 
* .. can invoke child1-repository, quering for "single" child-1-entity based on parent-id (which is available with child2-service) that would be a foreign
* .. key in child1 record.
* --|----|----|---- **IMPORTANT**: Realize how the rule of "putting parent-entity reference in child-entity-class" rather than other way round also works
* .. nicely with security-centered-DB-design with breaks. In this case, the basic entity queries will be one with small data; And it becomes burden of
* .. service to make additional calls depending on what is user's access level. **ALSO**: This simplifies the logic even when user is trying to update - 
* .. where they will only be allowed to update per their permission level.
* --|----|----|---- **IMPORTANT**: Realize how this rule (of putting annotation with child entities) enables integrating with CQRS pattern.. wherein, we
* .. keep flexibility to simply move child to some other service -- but the application will still see the whole entity as if it is all local!
* --|----|----|---- Although one can place child1-entity (ones with @OneToOne annotation) in a different package, it is greatly suggested to instead keep
* .. such entities in same package as parent-entity - to emphasize the point that in some other design realization, these might have been just 1 entity,
* .. but are made separate for now
* --|----|---- (8.2) Realize that since child1 is related to parent via one-to-one mapping, So, it should be treated effectively like parent-entity by 
* .. other children of parent. Meaning.. chil2-service should not be allowed to make calls to child1-service; But child-1 service should be able to call
* .. child2-service. Even better.. both parent and child-1 should be handled uniformly in parent-service
*
* --|---- (9) Study the following case: ParentDTO has a field "child-MultiType-DTO", whose actual class-respresentation depends on some standard-field value
* .. associated to parent-entity (let's assume that is ensured by processing, else some exception is thrown). Now, user gives parent-DTO with expectation 
* .. that "child-MultiType-DTO" be correctly casted, value read from it, and used to update parent-entity, and then the processing goes to update 
* .. corresponding child-MultiType-entity. Finally, the updated child-MultiType-Entity is converted to DTO and is returned back.
* --|----|---- (9.1) Request comes, the default deserialization would most likely convert the user-provided data into a Map type and save it in "object" type
* .. field under the parent-dto
* --|----|---- (9.2) Based on parentEntity's already set value of standard-data-field, the standard-data-field-service returns a delegate, which identifies 
* .. the correct dto class type to use for "child-MultiType-DTO" field.
* --|----|---- (9.3) Util classes provide serialization/deserialization to convert the map-type to correct data-type, which is then attched to parent-dto
* --|----|---- (9.4) Some other util classes are used to make calls to external system to get extra information -- which is then used to update the parent
* entity and/or the "child-MultiType-DTO"
* --|----|---- (9.5) Updated "child-MultiType-DTO" is sent to child-MultiType-service to update child-entity and return a updated "child-MultiType-DTO",
* .. which is then attached to parent-service's response-dto.
*
* --|---- (10) How do validators fit into this DAG structure. 
* --|----|---- In answering the question.. REALIZE THAT: (a) validators are made on DTO and not on entity, (b) parent-DTO validators should also validate
* .. the child-DTO -- and so, child-DTO-validators take higher rank in DAG graph than parent-DTO-validators, (c) One may equally shift the validation logic
* .. to service and so, most likely, validator should exist at same level as corresponding service!
* --|----|---- (10.1) The point#c from above very succinctly captures the location level for a validator, i.e. it should be at same level as service. So,
* .. it can access all repository classes, and all child-services (for nodes that are child to it and not its peer). Making it a bit stricter, since
* .. validator pull out validation logic that could have been in corresponding service, so it is a good practice to not have it depend on service at the 
* .. corresponding level.
*
* --|---- (11) How do diamond-patterns (i.e. @ManyToMany mapping) fit into the DAG structure.? Specifically: How do CRUD operations on such structure 
* .. operate while also maintaining a DAG structure
* .. Example: School-webservice. School has different classes. School has different teachers. Each employee has a resume.
* -->-->-->
* --|----(11.1) Case#1 : There is a "Allocation" table that allocates teachers to classes. One class may be taught by multiple teachers (say there
* .. is quarterly rotation) and one teacher can teach multiple classes. In this case, when a teacher-class relation is broken, both of them continue to
* .. stay independent. However, when a teacher is removed, all subsequent allocations should also be deleted - but classes should not get deleted.
* --|----|---- This is easiest and most straight forward of all 3 cases.
* --|----|---- Following above pattern, make a new entity to represent the child. Add reference to all "its" parent entities, each with @ManyToOne annotation
* --|----|---- BEST, it is suggested to make a new REST call, like "POST /school/{sId}/assignments", rather than "POST /school/{sId}/teachers/{tId}/classes/
* .. {cId}/assignments". Latter is unnecessarily long and also confusing.. why teachers before classes? Also, with former, you can pass a proper DTO which 
* .. is id for class and teacher, so as to make that assignment
* --|----|---- When creating this child-entity, make a cascade from its multiple parents. This way, when any of them gets deleted, the assignment also 
* .. automatically goes away.
* --|----|---- This particular sub-section fits nicely into the design-paradigms made so far, and is easily extendible to also include multiple parents for
* .. same child entity!
* -->-->-->
* --|---- (11.2) Case#2: Contrast above with teacher-resume. Each teacher should have a resume. Either they upload file and metadata is parsed from it. 
* .. Or, they give metadata and resume is made from it. Either case, one cannot exist without the other. When teacher is deleted, then their resume is also 
* .. deleted, and at same time, the teacher-resume association is also deleted.
* --|----|---- First, start by realizing that file and metadata are aletrnate representations of same "concept".. and they will always be "same". So, the
* .. REST call should also be same - since we are always accessing same resource.
* --|----|---- Above is important realization. If there is just one REST call, then the only way to proceed would be to encapsulate "file" and "metadata"
* .. into a single object and use that for communication. Thus, in such cases, we should communicate using Multipart with file + json component! When
* .. someone asks for file, give them file.. or if they ask for metadat json, give them json.. or in default case, give both! Also, when they upload, they
* .. can choose to send either a file or json.. but either way, both fields get simulatneously updated! 
* --|----|---- Along the same lines, when a delete call is made, it needs to simulatenously delete both the file and the metadata. Realize that since files
* .. don't have transaction with DB, so this needs to be done via Transactional Outbox pattern (see notes in JTA-Controller). Also see related comments
* .. on files/multipart in ReqRespController #9.vi
* -->-->-->
* --|---- (11.3) Case#3: There is "Employee" section, and there is "Document" section. Both exist independently. But, when a "Teacher" employee comes, a
* .. resume is automatically made and linked to in documents. Similarly, when a "resume" comes, a Teacher profile is automatically made and linked. Once linked, 
* .. deletion/update of one should also delete/update the other. This linking should not happen for other cases.   
* --|----|---- Realize the difference with previous case (#11.2) regarding retrieval. In previous case, when a "retrieval" of "resource" is invoked, it retrieves 
* .. both the file and metadata, since both are representation of same information. In this case, for most "Employee" type, only the json is single representation of 
* .. data and for most "Documents", only the file-response is the single representation of data. So, in this case, it might be ok to have the retrieval-endpoint give  
* .. either the json or file only; And requiring correct endpoint call if a file or a json is needed for the "Teacher" where data can live in multiple equivalent forms. 
* .. Furthermore, looking at it in an Agile development environment, it is likely that the service gets initially developed with either a json or a file only  as output. 
* .. And later on, the requirement is put that for "Teacher", the 2 are equivalent representations. However, by this time the API is set and already has customer-usage.
* --|----|---- In terms of DB design, this is a case with a "diamond pattern". But, it differs from case#11.1 in that the diamond pattern exists strictly for create,
* .. update and delete operations. More crucially, the diamond pattern here is an implicit one - whereas in case#11.1, there was an explicit endpoint made for it.
* ..  While the services code will use it, this is not something that will be exposed via an endpoint as done in case#11.1. For these behavior, even a different
* .. cascade rule might be necessary. 
* --|----|---- IMPLEMENTATION (CREATE): Start first as if there wasn't need for any such linking. This models the time period in development before the requirement 
* .. was put. So, the service to add metadata to DB just does that; And the service to add files just does that. Now the requirement comes to create link between 
* .. metadata and files for "Teacher", and also that in future that may get further extended. Since "Teacher" is a particular category, i.e. a context value, so the 
* .. best suggestion if to create a "Processor"-delegate, a call to which is added at the end of existing logic for metadata-creation or file-creation. For other categories,
* .. the default behavior of this delegate could be to do nothing. However, for Teacher category, this delegate can have logic to (a) make files and make entry in 
* .. Transactional Inbox (see #9.vi in ReqRespController), (b) link the file location to metadata via a new entry in some join-table, (c) remove entries from transactional 
* .. inbox so that the files don't get deleted. NOTE that another method can be made in the delegate that now does same work when files are loaded and it needs to 
* .. connect the loaded files with parsed metadata. This works for the DAG structure defined earlier; As said in #7.3, the standard-data-field-delegate come after 
* .. all repositories but before all services. 
* --|----|---- IMPLEMENTATION (DELETE): As described in CREATE, the ideal implementation should involve bringing in a standard-data-field-delegate to execute the 
* .. logic. It works in opposite order to delete. HOWEVER, THE MOST IMPORTANT THING TO NOTE is that one should not use cascade pattern for the join table. Here,
* .. we want to explicitly gather the linked metadata and file id(s) that needs to be deleted, then delete join-table-entry. And then delete the metadata and files by 
* .. using the id(s) that were just gathered. A casacde structure is explicitly discouraged because we don't want to inadventently delete the join table entry by 
* .. deleting the file or metadata first. ALSO NOTE.. as discussed earlier, by not having @OneToMany in parent-entity, then can be processed without any issue after
* .. the join table entry is deleted. This couldn't have been done in a clean way if there were @OneToMany entries in parent-entity(s)
* --|----|---- IMPLEMENTATION (UPDATE): As described in CREATE, the ideal implementation should involve bringing in a standard-data-field-delegate to execute the 
* .. logic. Update can be considerded as a sequential mix of delete-old and create-new. So, it should work expected.
*
* --|---- (12) **VERY VERY VERY VERY IMPORTANT**: Summary
* --|---- Branch#1: Standard-Data-Field-DTO == Standard-Data-Field-Entity >> Standard-data-field Repository >> Standard-data-field validator >> 
* .. Standard-data-field service >> Standard-data-field controller
* --|---- Branch#2: Child-DTO >> Parent-DTO (as long as DTO validators depend on class only, and not on repository/service) >> Parent-Entity (not having any 
* .. @OneToMany annotation) >> Parent-Repository >> Child-Entity (who should contain @OneToOne or @ManyToOne annotation) >> Child-Entity-Repository >> 
* .. Grand-children (formed to handle @ManyToMany mapping between children) >> Grand-children-repository 
* --|----|---- NOTE that the @Query method can invoke joins - in writing which we may use fieldNames of parents. For this reason, parent-Repository is placed
* .. at higher rank than child-repository
* --|---- Branch#3: All repository >> standard-data-field-delegate >> grand-child validator >> Grand-child DTO (if it contains validators that are based on repository) 
* .. >> grand-child service >> child validator >> child DTO (if validator based on repository) >> child service >> @OneToOne-Child validator >> @OneToOne-Child 
* .. service >> parent validator >> parent service
* --|---- Branch#4: Util classes float around as needed
* --|---- Branch#5: Since child-service is >> parent-service, it lends to: child-controller >> parent-controller. 
* --|----|---- **VERY VERY VERY IMPORTANT**: For child-controller to be a higher priority than parent-controller is also understandable, in that one can think of
* .. some parent operation, like, update, being equivalent to calling update on all its child. So, child-controller should be at higher up level in DAG chart than parent
* .. controller. Realize that this also meshes nicely with Jersey provided functionality where url can be forwarded to child controller for processing
*
*
*
*
*
*
* **VERY VERY VERY VERY IMPORTANT** (Data-Governance): CHECK-STYLE/SONAR IMPLEMENTATIONS
*
* -- Require every "if" statement to be preceeded by a comment. Every "if" implies a logical branching, and so, it should be backed by a reason - which should be 
* .. in the comment. Even better, if the user-story is also mentioned. NOTE that "if" could also be used for null check, but that really shouldn't be done a lot in code
* .. and instead be handled in custom serialization/deserialization logic. If above condition seems too constraining, it could be limited to class with names ending
* .. wih Service, Controller, Repository of DTO
*
* -- Require that only the following class-name-suffixes are allowed : DTO, Entity, Controller, ControllerImpl, Service, ServiceImpl, Repository or Dao, RepositoryImpl or 
* .. DaoImpl, Util, Config, Factory, Delegate, Visitor, Builder, Servlet, Filter, Interceptor, Aspect, Validator, SecurityVoter, Authenticator, Authorizer, User, Searializer, 
* .. Deserializer, Editor, Resolver, Converter (..add more as necessary)
* --|---- Exclude custom annotations from this check
*
* -- For class names ending with Entity:
* --|---- Require that each Entity has a no-arg constructor (Good practice)
* --|---- require that each Entity has Boxed-primitives (like Boolean, Integer, etc) and not the primitives (like boolean, int, etc). This is because the boxed fields can 
* .. store null values which the primitives cannot.
* --|---- Require that it has an @Entity annotation (question: how does this work for CSRF.. where the backing database is a REST call? Maybe divide "Entity" classname prefix to "DbEntity" and "RestEntity")
* --|---- Require that if there is an Entity class, then there is a repository class with name "{Entity-class-name}(Repository|Dao|DaoImpl)", and vice versa
* --|---- Require that @Id annotation is on field of type Long (this is good for standardization.. but can be relaxed)
* --|---- Require that if the DB-table name starts with STND, then class name starts with "Stnd", and vice versa
* --|---- Require that if entity-table name starts with "STND_", then all its members having @ManyToOne should also start with class name Stnd
* --|---- Require that Entity class doesn't have any member whose type-name ends with DTO
* --|---- Require that Entity classes do not get used within public controller method for any return type or method parameter
* --|---- **VERY VERY VERY IMPORTANT**: Necessary for DevSecOps + Data-Governance:
* --|----|---- Require that each table @Entity has a @Role marker on class such that if an entity-class has a @ManyToOne or @OneToOne field, then the 
* .. role-annotation on parent entity should be larger or equal to role-annotation on child-entity class. 
* --|----|---- Require that the custom base-repository-factory used in Spring allows getForId only for classes of type @Entity, and those which have classnames 
* .. starting with "STND_", or those not having any @ManyToOne annotations in it. Couple this by providing a base repository factory implementation that all 
* .. projects in the enterprise need to use/extend. Since that is a standard, so that's how the test will know about it by scanning class.
* --|---- See https://stackoverflow.com/questions/3805584/please-explain-about-insertable-false-updatable-false -- the constraint is that all entity fields with 
* .. @ManyToOne should have insertable=false in @Column annotation. Updatable need not be false, since as part of child-service workflow, it might be needed to
* .. update parent, like, updating child-last-mod-time in all ancestors
*
* -- For class names ending with DTO:
* --|---- Require that each DTO has a no-arg constructor (Good practice)
* --|---- require that each DTO has Boxed-primitives (like Boolean, Integer, etc) and not the primitives (like boolean, int, etc). This is because the boxed fields can 
* .. store null values which the primitives cannot.
* --|---- Require that class ending with DTO does not have @Entity annotation (this may get slightly restrictive in that DTO class may be used to query another 
* .. microservice. Maybe, call such classes as CsrfEntity)
* --|---- Require that DTO class doesn't have any member whose type-name ends with Entity
* --|---- Require that they define an isEmpty() method to help identify if a DTO is empty. See later under Data-Governance for how it could be useful
* --|---- Require that each Object type field (except boxed primitives) is non-null after no-arg constructor runs. See under ReqRespController (i guess) where this
* .. can be helpful to initialize fields and prevent Null pointer exception. On a down-side, this increases memory consumption
* 
* -- For each Controller:
* --|---- **IMPORTANT** Disable Http.TRACE method. This is a security vulnerability. See https://security.stackexchange.com/questions/56955/is-the-http-trace-method-a-security-vulnerability
* --|---- Require that Controller classes don't have any dependency on Repository method
* --|---- Require that it has at most 5 public methods for GET, POST, PUT, DELETE, OPTIONS. Make exclusion for PATCH calls (reasoned below)
* --|---- Require that there is @RequestMapping url on a class. At method level, the only allowed @RequestMapping annotations are no-path-url for POST-Type, 
* .. and only "/{identifier}" for other HTTP-method type (NOTE that it shouldn't be made manadatory that only "identifier" text is there in url-path. It can be anything,
* .. just as long as it is consistent). This ensures proper grouping of all related logic, and only of them, within a controller. Also, like above, make exclusion for PATCH
* --|---- Require that all public methods (except PATCH call) have same return type, or if the return type is ResponseEntity, then the generic-type inside it should be
* .. same as others. NOTE that this also works for DELETE which returns No-content. Alternately, it could be required that delete-calls have ResponseEntity<Void> 
* .. which is stricter.
* --|----|---- **IMPORTANT** For above 3 points, one may want to maybe start with exception for PATCH call, realizing how the operations like Split-file/join-file, 
* .. that cause a "count" difference between initial and final metadata state won't fit within this paradigm. 
* --|---- Require that if the response type is not a generic, then it should extend BaseDTO
* --|---- Require that the class has Swagger annotations. The responseType in swagger should be void or an Object.. never a container type
* --|---- Require that each public method has Swagger annotations, javadoc, and is at most 10-15 lines long
* --|---- Require that each public method is not called by any other class throughout the code. Controller methods shouldn't call other controllers, it should call service!
* .. This is also good from a security viewpoint
* --|---- For POST call, require that the response-type is same as one of the input-type. It is rather suggested to relax the criteria allowing for "Creation-Request"  
* .. type to be different from object-representation of "Created-Object". This is good for security purpose. If using same object-type for request and response,  
* .. add RequestBodyAdvice to ensure that users are unable to pass in fields that they shouldn't have access to - for influencing object creation process.
* --|---- For PUT call, require that the response-type is same as one of the input-type; And unlike for POST, this criteria should be strictly followed here. In terms of
* .. coding, it is also suggested to add RequestBodyAdvice for all PUT calls that make the "id" in DTO same as that in the path-param
*
* -- For a method:
* --|---- Require every public method has a javadoc (excluding constructor)
* --|---- Require that each method that is not public, also has a comment on top of it
* --|---- Require every method is coded within 80 lines, including comments (including constructor)
* --|---- Require that every method have at least 10% comment-lines (excluding constructor)
* --|---- Each method should be in camel-case. When broken and first word used, it should be a verb.
* --|---- Each method in Controller, Service, Respoistory class should:
* --|----|---- Only start with verbs: is, get, update, delete, save, create. If it is complex, start with "process"
* --|----|---- End with generic-container name if the return type is a generic, like, Collection, Map, Set, List, Optional
* --|----|---- If method name starts with "update", the return type should be void. If method name starting with "is" or "get" there should be a return type. For other
* .. cases it may become too restrictive on coding to add rules.
*
* -- For a class:
* --|---- Require every class has a javadoc
* --|---- Require every class has less than 1200 lines, including comments
* --|---- Require that every non-static field be private. (Don't put constraint on static field -- that is left on to developers)
* --|---- If the class ends with "DTO" suffix, require that it has @ApiModel annotation, and every field of it (either declared or inherited) has @ApiModelProperty
* .. annotation on it
*
* -- #7 under Dag-structure discussion under "Directed-Acyclic-Graph (DAG)" topic says that application-dependent standard-fields are like Context-variables, and
* .. any processing based on their value should be done via a "Delegate Pattern". Thus, require that within the Controller, Service and Repository class, one should 
* .. not have getter/setter of such values within an if-condition.
*
* -- To ensure that there is just a single-DB that gets used throughout the codebase, and any request to external gets routed through it.. one may think of making a
* .. custom annotation, like, @RepoGroup(...) and setting it to have a value. Then iterate over all service-class and ensure that all repository used by it, or repositories
* .. used by other services wired to it have same @RepoGroup() -- and this constrains that one DB is used throughout the application [[ALSO NOTE to add exceptions 
* .. for @RepoGroup used by standard-data-field, since they might be coming from elsewhere. Similarly, add exception for security-details]]. **HOWEVER**, I'm not
* .. very sure whether this will give more benefit than constraints. So, enforcing this criteria isn't vigorously suggested.
*
*
*
*
*
*
* **VERY VERY VERY VERY IMPORTANT** (Data-Governance): SWAGGER + JSON-INTERNATIONALIZATION + SECURITY + UI-LABELS!! (Yup, all these..)
* -- This applies both to monoliths and to microservices
*
*
* (1) In previous section, we require that the DTO-class, and each of its member-field have a Swagger annotation. It is suggested to not put actual text values in 
* .. Swagger annotations, but placeholders, like {dto1.description}, {dto1.field1.description}, etc. [[Continue to read why this is suggested. It will eventually tie up
* .. with https://springfox.github.io/springfox/docs/current/#property-file-lookup ]]. For simplicity, this can be same as field name. 
* --|---- Same is also suggested for controller-class, controller-method(s), query-param(s) in controller-method. I believe there are no other cases where Swagger
* .. annotation is even needed
*
*
* (2) At organization-wide level, have a "Governor" service+UI through which developers in any team can register field-name, placeholder-name, field-description,
* .. project-name, requestor/developer-name. Any entry being made is tagged with "UNCONFIRMED", null-value for reviewer-Id and reviewed-date.
* --|---- Possibly, add a unique constraint over "field-name", "placeholder-name", and combination of {fieldName, placeholderName}
* --|---- Teams can also provide an approval-deadline [[It is explained later on how this service gets used in a meaningful manner. For now, just accept it]]
* --|---- OPTIONALLY, a "BUSINESS-DATA-CONTEXT" field can be added. The idea is to use this field to identify data that belong to a particular business context. 
* .. It is hoped that in long term, this field will be useful to ensure that business-context matches closesly to application accessing the field, i.e. there aren't cases
* .. where one field in a business context is handled by one team/application, but a related field is handled by other team/application. This can happen in a large
* .. organization where projects can start at different times and their scope evolves.
*
*
* (3) The core idea to prevent introduction of unreviewed field-names is as follows:
*
* (3.1) When developers are making new controllers and DTO, they need to register values as identified in #2 above with the enterprise-wide "Governor" service
*
* (3.2) As part of unit-test, one of the test will go over all controllers/dto and make sure that annotations are given (Or maybe this is part of checkStyle plugin)
*
* (3.3) As part of unit test, one of test will make Swagger doc of system's dto and controller, parse it, and then send "confirmation-requests" to Governor [[More
* .. on "confirmation-requests" later]], with the understanding that if "Governor" denies any of these requests, then the build fails. 
* --|---- (3.3.1) What request is sent to the Governor service? Request to "Governor" service consists of a name and corresponding placeholder.The name 
* .. provided can be the name of controller, or DTO-name, or DTO-field name. 
* --|----|---- DO NOT pass team name when making request. Why? Because same DTO could be imported as a library by other teams. All we want is to make sure
* .. that a combination has been reviewed, but not where it is coming from.
* --|----|---- There will be cases where same DTO gets used within multiple other DTOs in same codebase. Or, maybe, your tam is importing others' DTO. In this 
* .. case, the reused DTO's fields will get multiply checked against the Governor service. This causes extra calls but won't fail as such. This also works when a team 
* .. uses others' DTO. HOWEVER, one condition that must be checked is if the field-name for this DTO inside the parent-DTO is same as the fieldname annotation
* .. added to class itself. Example, if DTO-1 is defined with @FieldName("dto1Data"), and DTO-1 gets used within DTO-2 as : "private DTO1 dto1Data;", then
* .. the unit-test-based-checker should verify that the value given under @FieldName (i.e. dto1Data) is same as fieldname in DTO-2 (i.e. dto1Data) - as is the case
* .. here. A related question is that why should @FieldName annotation even be made and used? That is because it brings greater consistency between json, yml
* .. with the xml-format. In XML, the outer-tag-name is defined in same class. So, best if the same convention continues. Also, this way, the dto-field-name to 
* .. use becomes consistent across its use in different classes.
* --|---- (3.3.2) When can Governor service send a denial? There can be various cases for failures. Some may hold for every deployment environment, while 
* .. other may hold in higher lab environments only.
* --|----|---- If the field-name and placeholder-name combination is not found, that may cause a failure. It means that  someone from a team is trying to introduce
* .. a variable without presenting it for review. This denial-criteria could be enabled by default for all environments.
* --|----|---- If the combination is there, and the corresponding entry if not yet tagged with "CONFIRMED", then that can raise a failure. It means that a team is trying
* .. to introduce a variable that is not yet reviewed for correctness of name. This can be kept in higher environment, like PVT. However, a slightly more lax version of
* .. same constraint can be put in lower environments, requiring that the tag be "CONFIRMED" if today's date is after the deadline date, or, say, 30 days after 
* .. entry-create date. Properties can be used to toggle on-off on which requirments should be enabled.
* --|----|---- REALIZE that same above 2 points can also be used with @ApiResponse in Swagger, requiring that users register every error response possible
* .. with a non-200 response type. This allows in standardization of failure messages across the enterprise. However, unlike former, any inconsistencies couldn't
* .. be strictly caught -- meaning -- It is possible for a dev team to raise a runtime error with message that is not registered; And that won't be caught unless
* .. another team starts using the API and sees the unregistered error message. Even then, they might choose to work around it rather than report it!! So, while
* .. this is an enforceable platform, the inconsistencies may not get caught until later. This is not the case with DTO-naming enforcement which is strict and caught
* .. then and there!!
* --|----|---- The above-outlined tests should end by telling Governor service that they just did checks for {team, project} combination. Governor service
* .. can now iterate over all entries belonging to {team, project} and see if there are any entries which were NOT recently checked. This means that the team
* .. using a particular field / DTO / controller, but has now stopped using it. If the Governor sees that for any of such "deleted" entries, there are other projects
* .. subscibed to it, then it will fail. This prevents a team from randomly deleting "features" and breaking cross-team integration. On same logic, the data-type of
* .. a field could also be provided to ensure that DTO field data type isn't suddenly changed. This denial-criteria could be enabled by default for all environments.
* --|---- (3.3.3) Extension of denial for DB check: For all "DbEntity" type class, we have already put the requirement in above section on "CHECK-STYLE/SONAR 
* .. IMPLEMENTATIONS" that they should have @Entity annotation. Extend Governor service by also passing the {"table-name", "Entity-class-name"} to ensure
* .. that DB-table names are also reviewed. They can also have @Role to identify the roles that can access data in this DB-table
* --|----|---- Some data-governance aspects are discussed in previous section on "CHECK-STYLE/SONAR IMPLEMENTATIONS". However, since they don't relate to
* .. central Governance-service (as mentioned in this section.. so they are not included here)
* --|----|---- Apart from above.. one may also start requiring teams to provide link between DTO class name and table name. The problem is that now the paradigm
* .. starts becoming too restrictive; Even worse, there is no way to cross-check this information, and it may soon get stale! So, best, avoid it. **MORE IMPORTANT**:
* .. Realize that DB table name and column name is something internal to an organization/microservice. So, a service consumer doesn't really see it, and so they 
* .. shouldn't care about it. As such, adding governance here does not provide any utility, and is likely to be a constraint!! Governing DB-table-names seems like a 
* .. good stopping point because it shows that DB-tables are getting reviewed. But there's no need to go further on DB-side governance.
*
* (3.4) For a failure to never happen, some Enterprise-Data-Architecture team is needed which reviews the "UNCONFIRMED" entries and confirms it. 
* --|---- (3.4.1) They can also suggest a new-name for a field, and make it enforcable by changing field-name in DB independently via another service by Governor. 
* .. This will obligate the developer/team to make the same change in their code, else deployment will not happen. This ensures that only correctly named fields are 
* .. allowed to deploy. It also controls enterprise-wide naming convention. When doing so, the Governor service will also change the tag from "UNCONFIRED" to 
* .. "CONFIRMED", and also add reviewer-Id and reviewed-Date
* --|---- (3.4.2) Additionally, reviewers can also update associated description [[It is covered later on how this is used -- and this is used in multiple ways!!]]
*
*
* (4) Governor Service:
* (4.1) Earlier, we've seen Governor service provided to team to register new field-names, and to reviewers to update field-name and field-description as part of
* .. review process and to confirm the changes.
* (4.2) Service giving admin users the ability to delete an entry
* (4.3) Reviewers could be provided with a "Search" service that searches the DB to identify fields with similar name / description. The idea is that a field is 
* .. representative of a data-column. In a microservice design, data should be stored at just a single location to prevent duplication and for proper control. So,
* .. if same field-name shows up as being requested by 2 different teams, then that signals improper cross-team communication and design, and should be 
* .. resolved by only one team holding access to data, and other team using CQRS to get indirect access to data. Since reviewers have access to requesting-teams,
* .. a communication can be initiated to get the duplication resolved.
* (4.4) Service allowing other teams to subscribe/unsubscribe to an entry created by other teams -- to ensure cross-team integration
* --|---- When a team uses DTO/controller from other team, they can add a "subscribe" before doing import. NOTE: When using controller, they should subscribe to
* .. both the controller-method and also to the DTO
* 
*
* (5) Adding role-based dependency in DTO: A business has various enterprise-level roles. Some DTO fields could be accessible to a role, while not accessible to other.
* .. How to communicate this information? Realize that this feature comes in to inter-team communication only after service-to-service authentication has been put
* .. in place. But even without that, having this feature is very useful in controlling that what user-role is able to see which-fields when it interacts with a service. This
* .. means that same DTO/controller will end up creating different Swagger based on user-role - so that it can then be distributed to allow external client integration
* .. rather than forcing everyone to use your UI
*
* (5.1) In Governor DB, copy the Business-wide roles. Most common: anonymous-user, logged-user, admin, client-type-1, etc.
*
* (5.2) Require that each DTO and controller have a @SwaggerRole() annotation that takes a list of @AndRole entries. There is also an understanding that unless 
* .. a role is added to controller's method or DTO's property, the same role will copy throughout the class. When role is copied, an AND of class and method level
* .. roles are taken.
* --|---- (5.2.1) Add check that if method/field-level role is laxed than class-level role, then raise error. Similarly, if a DTO (say, DTO-1) is used as a member of other 
* .. DTO (say, DTO-2), then the overall DTO-2 {class + DTO-1-field} level role should not be more laxed than role at DTO-1 itself. 
* --|----|---- The overall DTO-2 level, i.e. {class + DTO-1-field} is a particular-case of some possible role that can come over DTO-1 being used in certain context.
* .. We want this case-specific role on DTO-1 to be stricter than what it ideally should be. This condition means : DTO-1 on itself is recognized to have a certain 
* .. auth-level, and should never be given to any lower authorization. HOWEVER, to err on side of safety, even for users who are authorized to see it - may get 
* .. denied - in certain cases. This is done to err towards stricter authorization than necessary than to err on other side.
* --|----|---- Ideally, best-inter-team-communication-scenario is when the role from overall DTO-2 should be at same level as DTO-1. Maybe, an environment variable 
* .. can be used to determine whether error should be raised in case it is not strictly equal. REalize that this behavior also appies for DTO imported from other libraries

* --|---- (5.2.2) This is useful for customer-contracts design, when a new field is added in response json for a particular type of user. We need a system to be able to
* .. capture and highlight this extra information/constraint, and that is provided by DTO-field-role. Even more, the entity-to-DTO translator (or response body advice)
* .. can use the annotation to dynamically hide information.
*
* (5.3) Now, step-2 gets modified in that a role is also needed when making entry. This field cannot be empty and default will be "anonymous" role use authorization
*
* (5.4) Step-3's checks get modified in that it is also checked whether the field-role coming from unit-test is same as that in Governor-DB. Error is raised if it is 
* .. different while there is some other team that is subscribed to it.
*
* (5.5) Step-4's list of Governor services expand in that it also allows admin-users to add role, and modify role-list associated to an entry
*
* (5.6) #7 under Dag-structure discussion under "Directed-Acyclic-Graph (DAG)" topic says that application-dependent standard-fields are like Context-variables
* .. and can be thought to be defining application specific role. In context of current discussion, those roles should NOT be sent to Governor-service, sicne Governor
* .. should only work with enterprise-wide roles and not application-specific roles. Such application-specific roles can be understood to communicate that the 
* .. corresponding field will provide a null-value for certain cases. If the client desires that level of information - maybe give it to them.. or just put "[Conditionally]"
* .. in Swagger field description.. but no more! YOU CAN, HOWEVER, use those application dependent roles to guide the entity-to-DTO conversion (or vice versa)
*
*
* ==========
* [[REALIZE THAT: Up until this point Swagger is just used for data-architecture validation and cross-team collaboration. Taking it further]]
* ==========
*
*
* (6) SWAGGER INTEGRATION:
* (6.1) Instead of a single Swagger, we make a bunch of Swagger documents now, each associated with a role.
* (6.2) If a particular role is not allowed to see a DTO/DTO-field or controller/method, then remove them from the swagger json. For  example, in reference to
* .. consumer-driven contracts, don't show a field to other users which is made specifically for a particular client
* (6.3) **VERY VERY VERY IMPORTANT**: Now, users can provide their language, and you can query Governor-service for description of a particular placeholder, 
* .. in that language, and then add that value in Swagger. THUS, you achieve "internationalization" of Swagger!! 
* --|---- REALIZE, that the amount of effort needed to extend same service to other language becomes as low as adding another placeholder-description in a 
* .. language. And this can be done independently without requiring any other developers.
* (6.4) **IMPORTANT**: NOTE: Use Swagger plugin for extensions:: https://springfox.github.io/springfox/docs/snapshot/#plugins
*
*
* (7) **VERY VERY VERY IMPORTANT** HATEOAS/UI/FORM INTEGRATION:

* (7.1) In previous section on "CHECK-STYLE/SONAR IMPLEMENTATIONS", it is suggested that a Controller only have a few public method for each type of HTTP 
* .. call. One of them being OPTIONS call. For OPTIONS, it is suggested that response-body have HATEOAS map with key as name of all fields that could be found
* .. in json response, and value as being the corresponding "internationalized" placeholder-description. REALIZE that this solves the problem of "internationalizing"
* .. the json. Now, the json-fields simply become tags, and people can easily understand in their own language what that tag means. Before this solution, the
* .. json field-name used to act both as a tag and also to communicate business-meaning of the tag. 
* --|---- (7.1.1) For similar discussion, see #8.iv) of ReqRespController
* --|---- (7.1.2) Should the return type of controller be a generic, then custom annotations can be added (in controlled method for HTTP OPTIONS call) to help 
* .. the "Heateoas Map Generator" in identifying the DTO-classes it should refer to for getting the field-description. 
* --|---- (7.1.3) Scope of HATEOAS can be expanded to also provide details about the request-body class (if applicable), and meaning of GET, POST, PATCH, PUT,
* .. DELETE calls. These values can themselves be collected from Governor-service
* --|---- (7.1.4) Most common OPTIONS response could be cached and the cache refreshed, say, once a day. This way Governor-service could be hit for only 
* .. those languages which aren't very popular
* --|---- (7.1.5) Annotations can be added to certain DTO fields (which are object type and not primitive) to identify headers that user can pass if they want
* .. that field-information provided or hidden. This helps with performance by cutting down unnecessary database calls to add information in DTO that are 
* .. not needed. These custom headers can also be provided in OPTIONS' Heateoas map. There is no need to pass it out to Governor service, but, it is necessary
* .. to ensure that these don't get passed on for unauthorized roles.
*
* (7.2) Just like HATEOAS, UI can also do an OPTIONS call and the values can be used to populate the labels for form-fields. **HOWEVER** this may not be a 
* .. more popular thing to do because there is a slight security leak. UI page has to render itself for all users. What if we have a user who should not be allowed
* .. to see even the existence of certain fields. That is not possible in this case because the pages are already generated and rendered - it's just that the labels
* .. aren't filled yet. But the user can still make out that there are certain hidden fields.
* --|---- (7.2.1) Should UI page be a jsp that is coded to not show fields if user does not have proper authorization.. then such structure would mesh nicely
* .. with the HEATEOS based UI design, along with internationalization. ALSO REALIZE.. you'll need to change both the UI and the scripts accordingly. You don't
* .. want the user to have slightest of inkling that there could be hidden fields to which they are not allowed.
*
* (7.3) Just like above 2 cases, consider the case where user can access some endpoint to receive a blank pdf form (which is a pdf representation of metadata
* .. that the user could have filled on web. For example, to have backward compatibility with pdf forms before web-option got available). In this case, again, 
* .. the service can refer to internationalized placeholder-description to make a form in the language as asked by user. Also, if certain language forms are downloaded
* .. most frequently, they could simply be cached - and cache refreshed, say, once a day
*
*
* (8) VERY VERY IMPORTANT: Realize that there are 2 more conditions that tremendously improve the security aspect of the deployed service.. However, currently
* .. I'm not sure how to include it as part of automation, and to add checks on it such that failure of checks prevents deployment of unsafe application.
* --|---- One is the requirement to add a @PostRetrieve listener that throws an error if this unauthorized user role (as defined on @Entity class) is trying to retrieve
* .. data. Second is the requirement to add a ResponseBodyAdvice to nullify DTO fields and Hateoas map during OPTIONS call for field that are disallowed to a user
* .. of a role. Third is to have a RequestBodyAdvice that nullifies the fields that don't match the role of user. 
* --|----|---- Does this add overhead to performance - yes.. But, in return it streamlines security - which a huge deal!!
* --|---- Maybe.. require that during unit test, a running Spring App-Context is formed. Then check that its entitymanagerFactory has registered a @PostRetrieve
* .. that is a visitor type class that runs over list of listeners, and that the role-rejecting listener is the first one in the list (..and list is not empty). If this is the case,
* .. the register a successful check in Governor service for the project. SIMILARLY, from the dispatcherServlet, get the RequestMappingHandlerAdapter, and from it,
* .. get the list of responseBodyAdvice, and verify that field-nullifying advice is the last one there. If this is the case, then register another success in Governore-service 
* .. for the project.
* --|----|---- On side note.. maybe also check that @PreUpdate, @PreCreate, @PreDelete sets the proper audit trail..
* --|----|---- On side note.. maybe also check that the 2nd last response-body-advice is the one that converts empty objects/lists to null. Maybe, to help with this 
* .. method, have each DTO define a isEmpty() method. Similarly, check that requestBodyAdvice has 2nd object which changes each type with an empty object, or
* .. maybe require that in DTO definition itself.
* --|---- **NOTICE** that this integrates "Security" within "DevOps".. this is a DevSecOps!!




===start of todo for package structure===

Data governance security based on condition -- goes in translator logic?


Auditing -- 2 processes can be followed: (1) whenever an entity is updated, all its predecessors are also updated with lastModUser and lastModTime. Also, there can be 2 separate "modify" values, one signifying when the particular entity was changed, and other signifying when any of its child was changed. The drawback of this approach is that it causes a DB fetch for all parents -- which means multiple more DB calls just to support auditing. (2) Second option is to just keep record when that entity's metadata is changed. Now, based on flags from user, if none of child-dto values are needed, then just compare with the entity's lastModTime.. else always fetch all children data. I think 2nd one is better option
--|---- [[UPDATE]] #7.5 in (Package DAG structure control) saying "Since standard-data is part of "context", we may also have cases where they form some kind of "security-role"".. mentions that one should check if an endpoint is enabled/disabled based on standard values association as could be identified up to the child-entity and all its ancestor. Now, this seems important.. but if we do this, then it may in every endpoint-call we have to retrieve from DB the entire ancestor-entity chain... And if we are doing that, then we may as well, update each of their lastModTime!


**A very good practice is to look at HttpHeaders class in Spring, and associated fields. Maybe, when sending response out, use those to set as much values possible. Looking at it:
--|---- See "Date" response header to be same as date-time when the value was committed in DB. Or, maybe send date and version as Hateoas response


See Pg.145 of SpringSecurity-docs-pdf. It says that having Sessionregistry could be used to list all users who are there.. this may be used if a strict control is needed on the users.
--|---- Other option.. instead have a proper and unified logging code that logs the user who come and the session and request they make! -- maybe also log concurrent session associated with them! For the user-related id that stays same throughout the session, maybe use their session-id


release DTO as separate jar for use as library by other microservice. This helps in adding Data-Governor across the enterprise
-- One related issue seen is that if a "war" (say, war1) is used as a dependency to another "war" (say, war2).. then the @SpringApplication run in war2 will also pickup endpoints from war1. This creates issues because now same endpoint is exposed through 2 different applications.. So, if your application is like that.. either keep the controller separate so it does not get packed in war1.. or put conditional on it, say, using "layer-name" so that it does not get redeployed in both cases

Object-oriented-programming is good for data. But Aspect-oriented-programming good for management. So, need for using strategy pattern when wanting an implementation of aspect oriented but with an object oriented way (This is logging with different levels based on error)

Blue-Green: when wanting to test a server group at once (as in PatentCenter)


standard value "must" be considered as role - in case we want certain endpoints to be disabled for the role (i.e. for certain standard values). This requirement now points to need for delegtion. The reason to be careful: Such standard-value based roles are "process scope".. so, smaller than session scope - since in 1 session, user may do multiple processes dealing with different value for same satndard-value-field, but these are bigger than request-scope. So.. such roles cannot be actually made - and they cannot be in granted authority, but they should be "thought of" as existing.. and be checked in each request - for security purpose

consider each package as a module - to get full DAG structure




Use updatable=false on columns that are set just once initially when entry made. Otherwise modify setter to set value only when it is not-null

Entity should no have even a list-type object - since that is representative of one-to-many mapping. NOTE: this applies only to DB-entities and not to REST-entities!







if we are trying to make full REST structure, then it's a good design if HEATEOAS also shows operation for current and next layer. This means:

(1) registering an OPTIONS for "/" endpoint, which tells user they can call "OPTIONS /v1/parents" to get more details about parent. The action of "more detail on parents" can be internationalized, and pulled from @ApiOperation / Governor. In a proper microservice design, there should just be 1 such case for DTO that joins all the bounded context (See https://martinfowler.com/bliki/BoundedContext.html and mentioned in JpaController). 
--|---- For monolith, or just "not-yet-split" microservice, there can be many such cases. 
--|---- Also, there can be multiple entries if there are many versions of a service; 
--|---- Or, if you want to give a GET link on "/" that gives information on "About-us" of business (which is best kept internationalized), and POST link that is universal customer-complaint site (which can capture user language setting to be used by customer-representative in responding). The point is, when user lands on "/", they should get an idea of all options available to them.

(2) (2.a) So now user calls "OPTIONS /v1/parents". This should tell them that they can do POST, GET, etc. - with corresponding operations pulled from @ApiOperation Swagger, and internationalized. (2.b) Do note that "POST /v1/parents" creates a parent-item, so you'll need to give 2 each of GET, PUT, DELETE, PATCH (and only 1 POST); Also, you'll need to cover all requestParams. This is Heateoas details for this particular hierarch structure. (2.c) Now, in line of operation as done in #1 above, it should also provide list of OPTIONS url available to do extra operations (with internationalized message). This shows that each Hateoas is comprised of 2 independent sections: 1 describing about "current" url and other describing about "immediate future possibilities". Realize that with this structure, the leaf node will not show any future possibilities ..and root node will not show any "current" possibilities (2.d) Note that above tells that we should describe all get/put/post/patch url, along with the queryParams to user. Thus, it also becomes necessary to explain the DTOs involved - to keep in line with this design. This is one more motivation to always keep the request and response object the same.. because now you won't have to explan multiple fields. (2.e) **ALSO** realize that you are just explaining the url, its purpose, etc - but not the validations done, or requirements modeled. That is not the purpose here. (2.f) If you want to be finicky.. then you can also give link to parents allowing users to go back one step -- though it seems more of a fancy addition than a necessity
--|---- In Implementation of option mapping.. have each rest controller identify its parent, rather than have each controller point to all its child.. because latter is not maintainable. Then, during deployment, create an inverse map of controller identifying all its children, which is stored statically and can now to used to answer OPTIONS call
--|---- For this implementation, modify swagger.json string by breaking into parts, split by {...} string. This way, when creating mapping, you can do it in a more memory efficient manner, than repeatedly calling string.replace() method on full swagger json - creating bunch of immutable string in the way.

(3) **VERY IMPORTANT**: With above exercise, your rest links are in themselves informative enough to not need a UI!!! 
That said.. how does the UI integrate now?
As said.. above brings in a self-consistent and closed structure among REST calls. And it is important to realize this. This means that UI should be considered a totally different domain in itself, not subscribed to a java-app deployment. In ideal case, one can now think of UI and REST as now becoming 2 separate microservices, each with different host url. For small organizations, they may wish to integrate the 2 by exposing UI on a different path. Either way, UI can set session-values (say, for A-B testing).. and these values forming part of application-side log can help with analysis. Any user-UI interaction done, clicktrace, etc. can be logged by this system (which is clean and separated from REST serving system)
---- On these lines, maybe use combination of sessionId, a/b-test-id, session-start-time, list of [time since last endpoint access using the session] {..of size (say) 100; Or, start fresh everytime there is an access after a long pause} -- and log it -- to then analyze to identify clicktrace, user-interaction and engagement, or if simply to identify web-scraping. Either way, note that this keeps a clean separation between REST-server and UI-system. Maybe later you'd want to move UI to javascript in Node.js.. even that is freely possible now!
---- Also note that when UI and REST is decoupled.. it promotes more security because both REST and UI are considered as separate system. If they instead kept as one system, then developers or management may force code-development to compromise security, or break architecture patterns in name of "..UI willhandle this, so let service not worry, or vice versa"
---- Above it is said that UI won't be needed. In reality, EVEN BETTER - you can now have front end "as a service". You can say that different organizations deal with REST and UI and can save yourself from liability. Can stop UI independent of services if an issue is found.

-- Have you application provide a request and session specific param that can be set by user. this can help integrate the 2 sets of otherwise different data.. useful for data analysis. But why have such design? - because this is a collection of processes, that mutually co-occur, but don't have a common parent-process they can refer to. 
--|---- Why is it that UI sets value in REST design? - because UI comes earlier in process than service. Should UI not have set this.. then let REST take the responsibility for setting it and communicating to UI. As an example, think of it like how the "locale" setting from user is treated




* Design of GET, PUT, POST, PATCH url

---- For PUT, POST, PATCH.. allow DTO to pass a version number also... helps with testing if not modified and change with optimistic locking. 
----|---- More importantly, this can better help differentiate between POST vs PUT vs PATCH. POST is when no version is given because entry is not yet made. PUT can also have no version since entire object (which is already existing) is being updated in an idempotent way - So, although the @Version column changes, user is not restricted in anyway from making the change. PATCH has to have version greater than 0 - and change is applied only if user's version matches DB value

---- For PUT, POST, PATCH.. automatically nullify any child-DTO in it.. because ideally user should be calling HTTP verb on those corresponding child-resource to add that. For example: parent-DTO also has reference to child-DTO. But if user wants to create child-DTO, they should explicitly call POST on child-resource, not calling POST on parent-resource only with a parent-DTO having child-DTO also.
----|---- NOTE: For PUT and PATCH call, the "id" value exists both in DTO and in path. So, best to also add a requestBodyProcessor that either deletes the "id", or, even better, sets it from url. By code design, one may require that pathParam holding id value has same name in url and dto. Furthermore, a custom annotation can be made to identify the Id property. (See these notes.. that annotation can also be used to constrain design of repository method that fetches-by-id)

---- Realize that it is not a good design to have POST, PUT, PATCH accept a list of DTO ..and say that when given a list all of them are updated. Why? (a) if you give a list then POST-REDIRECT-GET pattern cannot be used. (b) POST /resource corresponds to GET /resource/{id}. It'll be weird architecture that POST can handle a list of resources but corresponding GET only handles 1 resource-id. Realize that if client side really wants to add multiple resources.. they can just make multiple async calls! With this view, the adantage of allowing list-dto upload is that it gives guarantee that either all changes are done or none are done. But if that is a guarantee that we need.. then we are designing for relation between sibling objects without there being a parent - and that is bad design.




* For http calls: how to allow passing warnings.. not just error? Warning means.. it will go through.. but you may be doing something wrong
-- maybe its not part of REST but shows up when doing workflows! In that regards, should we call it warning-message or "business-message"
This can also be temporary events, like when user buys something, they get coupons! **QUESTION** How do you model that user gets coupon on purchase. Maybe its coupon now, lottery later, recall warnings later and so on.. should it be a separate notification board or or personal profile?




Governor service- allow user to pass a default where the swagger, or OPTIONS call just return placeholder values as is.. this allows UI to put their own values




Note that in ideal microservice structure.. user-details should be exposed via a different endpoint, like "/user".. which can be used to establish a authentication (based on an OAuth like model). It can also be used to save custom user session values (like, saving a/b test value, or some custom session-id that is logged and is matched with UI logs to then help with log data analysis). 
--|---- Either this can serve as central endpoint for business-wide authentication.. or, maybe there can be different end-point for different business-realms (as long as they don't repeat common fields). Other option is to have a common user-endpoint, but divide the DTO itself into realms.. and when a realm contacts the central user-data-service (2nd part of oAuth mechanism), then it is only given access to particular data fields. This can give an enterprise-level control on data-field-sharing between different components of an organization. 
--|---- **VERY VERY IMPORTANT**: When using realms -- (a) do note to use different rememberMe-service for each realm.. maybe even have remember-me-key be associated to realm in some manner. (b) do note to use different csrf token for different realms so that you prevent leak of csrf token to outside. Even this, it is best if csrf token has a finite lifetime after which it expires and needs changing





* use of view controller, request forward controller.. see https://stackoverflow.com/questions/27381781/java-spring-boot-how-to-map-my-app-root-to-index-html

* It is said earlier that dto and web-url interfaces are to be packed separately. Based on it: (1) also pack model attributes that combine queryparams, (2) pack handlerresolver object, (3) most important.. this gives a guideline on what to put in validator. Since dto are packed separately and contain annotations of javax.validation, so, it is suggested that any custom validators should only have logic not needing repository wiring. If repository wiring is needed, best put it in service layer

* Add check to verify you don't have any extra unaccounted requestmappinghandlermapping in your dispatcher servlet. This can be if you loaded a jar as dependency, and it exposes endpoints.
---- on same line.. either.. (1) make separate jar with method logic if you know it'll be consumed by other group in organization, and don't just pass single war file, and/or (2) if you know that there is a common url that every microservice needs and so it should be in common jar, then at least add conditional properties to enable disable that endpoint

* Try coding and see if you can make interface web url work




See https://www.wired.com/story/netflix-interactive-bandersnatch-hackers-choices/
The idea is that when working with progression in a "finite state machine".. then the size of response itself may be strongly correated to the outcome, and can be clustered to identify how user is progressing. In such cases, one can add "_sizeObfuscator" element in both the request (sent from client) and response (sent from server). The idea is to just fill this json with garbage. Sure, it causes some wasted bandwidth but it breaks correlation of size with outcome.
---- On same line, for video services.. it would be better if video(s) are padded so that almost al of them become same length. This way, just by tracking video length, one won't be able to understand the video being seen by user



Use of @JsonAnySetter to identify extra fields being sent by user.. to identify vulnerabilities. For example, to do prototype pollution in JS, attackers would be sending __proto__ key. Once you identify it being sent multiply, you can actively block it by requiring that the field is not stored.. or just becoming more aware!



Use Governor service to internationalize exception message. 
-- At this point, the base-message in logs are the only ones that don't internationalize - which is ok.. since these are application specific and don't go out to consumers. Best practice would be to separte log messages in "static final String" in each class. This way, if codebase goes to different language, it'll be easy for them to identify changes
--|---- An advantage of explicitly associating a key or particular base-log message, is that it can then be searched for in application to identify hack attempts. For example, when a parent-child association is being queried that is actually absent. This means that a hacker is trying to guess random parent/child association - which shouldn't have happened for a valid case and access via UI
-- same for business messages (like warning messages etc) - should be internationalized. 




Have /user service, endpoint be followed by realm name. This way, you’ll have user details by realm, and enterprise can use various realm
Or, maybe, start with /realm, then have /user – this means realms are different and users are part of it.. On back end.. you can still merge details on user from different realm in 1 db
Also, now within realm, you can add /realm/roles – to define all roles available within realm.. and this could be referred by Governor service to identify roles available to a realm. When doing tests now the classes need to pass realm name also.. which for a service would be constant and can be provided by maven properties


Set up a logging framework that logs user-details for each request, along with request url, and business related details (standard field values) associated with "topmost" REST resource accessed - so that they are available for all REST requests made. Now by looking at ip-address rate, valid inter-request rate from valid user, once can identify if the application is being farmed. Also, look at whether the request was success or failure.. If a user is making high failures at quick rate - that could be a potential issue and stop the user





Start with the observation that a class-with-fields gets json serialized in same way as a Map-with-key-value-pair. 

--|---- Realize that when we make a class with fields, it is as if we started with a map, but restricted it to contain only certain keys which are always present, and also have them have a default value (null, primitive defaults, or those in constructor)
--|----|---- Now, let's go opposite route. Just like a map can have a map again as one of its key, a class can also have a class-type as a member. The thing to note is that one shouldn't always aim to make each and every class as a public class. If a class (say, class2) is always only used as a member of another class (say, class1), then: (a) class2 can be made package-default and not public, and the class1-constructor be changed to initialize class-2, and no setter should be provided for class2-type member in class1; or, (b) class2 can be made as a static class inside class1. Both ideas communicate that class2 should be seen in relation to class1 ONLY!! [[I'm not sure if route-a would work with objectMapper serialization/deserialization]]

--|---- Same inference can now be transferred onto DB tables. Instead of talking of a DB table(Table1) with (id + N-columns), one can model it as combination of: (a) DB table (Table1) with single id-column, (b) DB stnd-value table (StndTable2) with rows containing name of columns that can go in Table1, (c) D table (Table3) containing ForeignKey to Table1 in #a, StndTable2 in #b and corresponding value. This design could be considered when one is not sure of how many fields to put in a table and if, as a requirement, it needs to be expandale
--|----|---- Now, let's go opposite route, which is much simpler. It is just that one can add extra columns in same table if it is not needed to break those in a separate table. Even if doing so, the columns can always be aggregated and modeled separately on java side. So, this does not cause any java-modeling restriction

--|---- Combining above with REST endpoint call: Above idea, when carried to REST, is to make the point that one shouldn't always arbitrarily try to break REST endpoints! Say, there is resource, modeled by class (Class1), having an id(class1-id), a class (class-2) and a list-of-class3 (List<Class3>) as its members. The question is whether we should have /class1/{class1-id}/class2 or /class1/{class1-id}/class3/{class3-id} like fields?
--|----|---- For /class1/{class1-id}/class2 : 
--|----|----|---- One may start by thinking that there is no "id" portion in url after class-2, and so such endpoints should be disallowed. However, a diferent case can be made, that since there is no "id" portion, it means that associated calls should return a collection type response. If the resource has just one of it, then simply return that single value in a list. And this keeps the design open to future extensions where this field becomes something like List<Class-2>, then one can return a list (In terms of DB design, this might be possible. It maybe that class-2 was modeled by entity-2 have entity-1 foreign key reference. So all that's needed is instead of @OneToOne, you allow @ManyToOne in entity-2. Point being, such a modification is not infeasible). Also, note that one can give a {class2-id} - its just that for now it is trivially removed since there is just one class-2 in class-1. So, this argument fails. 
--|----|----|---- One may also think of class-1 as a map, and so this could have instead been /class/{class-1}/sub-resource/{sub-resource-id = class2}. However, such design precludes extension from single valued to list-valued class2
--|----|----|---- Based on above discussion, one can see an "object" as a container. So, maybe one may want to use queryParams. Note that queryParams traditionally act as filter. So, they are applied to endpoints **NOT** ending with "id" portion and which should return a collection-type response. And then, the queryParams filter out the collection to give a smaller response. However, we now see even a single object as collection. So, the idea is that when applied on single object (i.e., url ending with "id" portion), then the queryParam can be used to filter. So GET /class1/{class1-id}?resource=class2 gives class1 DTO as response but where only its class2 attribute is filled. Similarly, GET /class1/{class1-id}?resource=class3 gives class1 DTO as response where only the list-of-class3 items are filled. Note that we understood queryParams to do filtering, so when put on urls, the corresponding should be a "collection" type response. Here, the single DTO class is the collection. So, we don't need to return list-of-class1 in response, with list containing just 1 class1 object.. you just return class1 - which itself is the collection here. This concept can also be taken to PATCH and DELETE, where PATCH/DELET-ing is done only using the fields mentioned in resource-list. POST/PUT can be taken to create/update whole object and so the queryParams don't play any role there.
--|----|---- Above discussion bring up an interesting point.. When designing REST url(s), don't immediately jump out to map every class within a DTO as higher level url. 
--|----|----|---- Extend the url for sub-resource when doing so gives more benefit because you need to include multiple business processes for the child-resource and having it in parent would just clutter code. Looking at clase of class3-list within class-1, realize that although it seems on the outset that a URL extension can be made for it.. but if the business requirement can be fulfilled by always deleting entire list and adding a new one back, and this can be done with small performance hit because each entry of class-3 is very small.. then, don't make url-extension, and just access class-3-list via class-1 only. On coding side, this keeps codes simpler rather than making multiple classes with small codes, and jumping between them. While a machine can do that on thread stack.. it will be hard for a new developer to come and understand that.
--|----|----|---- Best, consider expanding url from parent to child-level, only if a grand-child-level resource can exist and have huge complexity (based on observations from case of class-2) 
--|----|----|---- **FINALLY NOTE**: 2 things of interest: (1) Above discussion shows an evolution of design, giving an idea on how to expand code from small to big without undergoing major jump! (2) Realize that whatever design you take, make sure to update the OPTIONS accordingly, and in an internationalized manner, so that users know how to proceed



Few things to follow-up on:
1) In governor service - how do you communicate the login requirements, or any other common error that can cause 401, 403, 400 status - back to user. 
--|---- One way could be using the "parent link". We said that for a resource, it should identify the parent - and that's how a parent-resource hateoas will be able to provide user with all child options available to them after an operation. Now, within parent resource an annotation can be made that contains business messages that are propagated down to every child. So, a 400 on "parent-resource is unavailable", gets sent to child-resource also, on which it can now add 400 status if child resource is unavailable, or if child resource is not related to parent. 
--|---- While above process is good, it does not connect between Spring-Security chain and others. Another way could be that whenever an OPTIONS call is made, then security-filter should add necessary messages in a thread-local map. These can now be read by OPTIONS handler and be added in HATEOAS message. Note that this allows for any preprocessor to add number of custom messages from any preprocessors ..can probably be extended to handle enterprise message - which the gateway can add as headers, and then the code shows it up in Hateoas messages. The only requirement -- a threadLocal should be established in filter.. or naming convention should be added - and then used for naming request-attributes

2) Even when using @RestController, if the return type is ResponseEntity.. you can use it to control the response-status. If not, maybe try @ResponseStatus annotation




**VERY VERY VERY IMPORTANT**
i) For an entity, the corresponding repository method should have 1-only method marked with @IdRetriever (meaning.. it is used to select row-by-id), and that method should have as many arguments as number of fields with @ManyToOne annotations in "entire ancestry" chain [[Pending: (a) how to resolving diamond patterns.. maybe limit to count fo unique class-names in ancestry chain; (b) Also require that the method-arg types match the types of "id" field in ancestor]]

ii) https://medium.com/@cowtowncoder/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062  -- and then for background on general process, see https://www.baeldung.com/jackson-inheritance   and   https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization
--|---- Generally, see webpage: https://github.com/frohoff/ysoserial    and    https://github.com/GrrrDog/Java-Deserialization-Cheat-Sheet#jackson-json
--|---- For same reason as above... configure objectMapper used as having "disableDefaultTyping" -- See https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization
--|---- To mention - json serialization with subtyping requires both annotations @JsonTypeInfo, @JsonSubTypes. 
--|----|---- @JsonTypeInfo tells how to store sub-type information, and @JsonSubTypes gives the information to add. If you look at code, it seems like a chicken/egg scenario where abstract-class is made first but the sub-type information is referred back into it when using @JsonSubTypes. So.. is there a better way? Yes! Instead of using @JsonSubTypes, you can use objectMapper.registerSubTypes(NamedType...) method to register the named types. This way, you'll only need to add @JsonTypeInfo on abstract-class/interface, and not annotations with reference to implementations. ALSO, if you get abstract-class/inheritance from jar, you can extend it trivially in your code and add annotations there. This is possibly one valid use of mixins.. but then why to open the rule allowing mixins to be used in json! 
--|----|---- **VERY VERY IMPORTANT**: If you follow above where subtypes are registered with objectMapper rather than put as annotation on abstract-class/interface, then do make sure to use same objectMapper for both request/response json serialization/deserialization, and if your code logic also requires json text use/manipulation. If you end up using new objectMapper, then that registration won't hold! -- **DO READ BELOW** -- this won't apply if you use annotation.. but then you're getting a chicken/egg in java code!
--|----|---- DO NOTE: The use of @JsonSubTypes is to constrain user-provided data into a particular type. So: (a) the property being used for doing so should not be part of the deserialized Java class. It is just for communicating with external; (b) Even though the java class does not have extra property field.. it is seen that any serialization of the object, whether done as part of response or for internal logic, brings back the field -- this is ONLY if the @JsonSubTypes is put on abstract-class/interface, or if an objectMapper is used where the concrete types are registered as sub-types
--|----|---- How to register these in Swagger.. and in Governor service?! (along with passing subtype name info)

iii) Extend Spring's responseStatusException for application purposes

iv) See https://www.baeldung.com/jackson-annotations - it has a whole bunch of json annotations which could be very useful.. Particularly..
--|---- use of @JsonRootName seems like a nice way to control same name for json and xml!! **ALSO** it can be intergrated with Governor service to ensure that same name is used everywhere. If wrapping is not needed - that can be controlled by objectMapper customization.
--|---- use fo @JsonAlias when one organization wants one name.. and other want other - for same field! Or, maybe to achieve an otherwise non-compatible backward change
--|---- @JsonIgnoreType for @JsonIgnore on entire type. For safety purpose, this can be added to all @Entity classes to prevent them for being mistakenly shown.
--|---- @JsonUnwrapped : A very very nice annotation to keep the json expanded, but to keep the java-side codes having aggregate-class as members. **DO NOTE** if you want to contrl showing standard-field-values -- where you'd like to show only 1 field for entire entity, it is best to use combination of @JsonCreator and @JsonValue - this also applies for enum.
--|---- @JsonView: Use of @JsonView "seems" something that can be used just like validation groups -- HOWEVER, that may not be the case (not sure). But the biggest problem is on how to dynamically change the view to be used by ObjectMapper on a per-request basis - that's why.. best would be to use a requestBodyAdvice.. that can do so based on operation being requested, user roles, etc.
--|----|---- @JsonFilter: Lack of dynamism is also something that makes @JsonFilter useless
--|---- @JsonManagedReference, @JsonBackReference, @JsonIdentityInfo : Require these to be absent from any DTO member!! Its use signals poor design and then trying to rectify it. Similarly, use of Json-Mixins seems like indicating that code has a bad design. But there is no way to check that this is not being done.
--|---- **IMPORTANT** @JacksonAnnotationsInside : Nice way to combine multiple jackson annotations for reuse




** Require that the difference between endpoints registered in dispatcherServlet is same as (..or different by a known offset) the counts of @RequestMapping annotations in code. This prevents unexpected release of controller methods coming from inside the jar-dependencies!



**Can have DB/audit-table structure with values: Operation, ExecutionBy, ExecutionOnBehalfOf: -- this way the creation, update, delete operations can be uniformly handled. Also.. this uniformly handles scenario where a user-request initiated an execution which was eventually handled by a system via an asycn operation. The operation column can be put as a foreign key to a list of allowed operations.. so, for example, file-creation and metadata-creation could be separate. Also, as system evolves, new operations could be brought in.


**Pending: 
(a) to design for async meta-updates to different systems. For example, say microSrv1 collects data from srv#2,3,4 and presents to user. Now user updates microSrv1 (via PUT or PATCH call), and microSrv1 should update srv#2,3,4 correspondingly for that. NOW.. say, srv#2 updates but #3,4 are not yet done.. start by telling user that #3,4 is queued for update.. no need to tell queue status of #2 sicne it is already updated. Say, user now requests for a data that does not need #3,4.. in this case, return it. Now user requests for data needing #3 - in this case, the old data needs to be pulled and they need to be told that it hasn't been updated. Maybe fail the entire request! Now.. how does a different microservice (not #1) knows that 3 is still to be updated - thus there needs to be an enterprise wide event log that can be searched to identify the status of #3 and whether there is a pending requets to update it. Maybe this can be kept of sixe-1 so that at a time there can just be 1 request for update!! Other option could be to use event-sourcing, i.e. to use the existing status, the update reuqest to construct new state of #3 and present it to user.. that is a possibility but now things seem to be becoming complex!!
(b) Making future-files (or metadata, or any data). As done in PC where grant-letters are to be made in future.. what should be returned. Maybe update the metadata showing that a request has been queued but not yet processed. But once the async processing happens, the file should be there. Just like above now.. if there is a request to modify this file, that should be put on hold since it is already in an incomplete state from previous processing.


** Have each DTO extend a "last-modified" interface, and then add a responseAdvice that adds a last-modified header when sending such DTO as json. This will trigger browser to send a "If-Modified-Since" header which if present in request.. and then Spring-controller returns null.. then a 304 response is instead sent back to client


** Is it possible that when making options call.. then user is also allowed to control whether they are using protocol-buffer / json / xml


4) **VERY VERY VERY IMPORTANT**: Look at comments under "Domain Object Security using ACLs" in Section 5.7 - in SecurityController.java. It shows how
* .. methodSecurity can be used to control object access; AND; in doing so, creates a DB-model equivalent mapping of "aspect" like behavior from service - 
* .. such that it is even applicable for audit!



Combine governorService with environment constants - to allow for selective enable or disable of a feature.
When sending Hateoas response, maybe also return also role + role-like environment constraints that was seen by the request at that time


Define Secured<T> Wraper for a class. The wrapper does the following: (1) at time of deserialization, it takes the incoming string to form the object which is kept transient. It also creates a symmetric-encrypt text. (2) At time of serialization, only the symmetric encrypt text is provided. (3) Same as above also holds for DB persistence, and in doing toString(). (4) When updating the object, both the object and the encrypt text gets updated. When outside wants to interact with the object, the wrapper accepts a function, which gets applied on object and result is returned back -- best to restrict even the scope of such methods and only allow cases when the return type is, say, boolean. This is because Personal-Identifying-Info is generally used for matching and nothing else. 



The general idea is that "server" should be brain, html the skin that shows information, css the presentation/beautification. and javascript for coordination of html, css and server. However, consider the scenario of "notifications" in twitter. User may unfollow someone and would like to immediately stop seeing notices from such person. However, the backend is noSQL and lets say that changes don't get immediately reflected. It may be seen that to get a "sudden change" behavior, an index that changes on drop of hat is needed. However, another option could be to let UI remain up to date. The server can just send various notification-events, but the UI now decides whether it should be shown as general news (wall..) or notification to user based on their preference. This brings back to boligical-based-system-modeling where javascript takes that job of automated-neural-behavior that are done without hitting the brain



SQL: use of "SELECT FOR UPDATE.." for pessimistic locking - best don't use it as it slows performance
 
 

One of the examples where having a @OneToMany could make sense is say, if part of doing a PATCH call on parent is to see the "N" type of child entities exist, and if some don't then auto-make it by using some other data. After that, return the dto response back. In here, having @OneToMany could be useful because any changes made in child-list is then used by DTO. However, even in this scenario, the use of @ManyToOne (and NOT of @OneToMany) could be sufficient.. in ParentService method, just add a necessary call to childRepository DAO and get the list of all child entities, then add extra as needed. NOW - IN THIS CASE - the place where one may face issues is that towards the end, when one is wanting to return back the DTO for parentEntity, it requires making list of DTO for childEntity - and question comes whether making same chilD-DAO method call to get list of associated child-entity to the parent will also bring the newly made entity. This is, however, more of a framework question -- one may say that a good entity manager should be able to do so.. and maybe, hibernate already does so.. but if not, then that's a drawback of the entityManager, and not of the rule. DO note though.. if a L2 query cache is being used.. then that definitely will give stale results so be careful. 


Add note that in microservice environment.. 
(1) incorporation of circuit breaker, ambassador and retry patterns is good to be able to give good user experience. 
(2) Note that if you need batch updates in multiple tables with NoSQL db, then you need write-ahead log like behavior and construction of final data state from joining of different individual stages. However, this can create inconsistency if not properly handled - so best to not enter such kind of situations. For example, consider case that user makes reservation on Yelp, and Yelp needs to make reservation on restaurant site and then tell user that reservation is made. The best way to proceed in such cases would be to store user's request on a Transaction Outbox table, returning user with an "id" that can be queried for status.. and also promising that user will get an email at end (this keeps the design responsive, rather than requiring user to always query back). Now, in first set of outbox processing, the server (yelp) tries to contact restaurant api and make a reservation. If successful, it deletes old entry and creates new Transactional outbox entry to now notify the user. There can be case that this new entry wasn't made even though a reservation was made. In this case, 2 things can happen: (a) either restaurant api has another call to cehck that the restaurant reservation at a particular time belongs to user in which case, the retry works fine, or, (b) that api is not there.. so user is never informed of reservation, restaurant gets angry at user for which we apologize to restaurant (..and this becomes a business operating cost). Once the email processing entry goes in outbox table, and then old transaction entry removed, then user is notified via email, and then the processing chain is marked as complete. Just to mention, in case that this didn't happen, user will end up getting one more email... so it's not like, processing has broken. This is how you handle multi-table batch statement in NoSQL. Now, one could have as well used batch statement (like, in cassandra) - that would work if the key in 2nd table in batch statement is one-one/many-one related to key in 1st table. If it is one-many relation, meaning that one entry in 2nd table could be changed by multiple keys in 1st table, then batching from previous failed command may conflict with batching from next successful command, where the 2 batches target different keys in 1st table, but same key in 2nd table. TO SUMMARIZE, it is best to avoid situations requiring batch of multiple nosql db tables because in NoSQL, the table is best thought of as a distributed hashmap, so having "consistency" behavior between 2 different hashmaps is not a good design
(3) As a good service-method design for transactional-db, it is suggested to call DB only towards the end and just once. If there is need to do any external system call, like filesystem store, or external API call, then that is best done by a different transactional outbox call and creating like a process or fileId, which is then linked to main DB tables and removed from outbox (for example, the process for file upload api). This allows one to have housekeeping undo calls to prevent garbage collection. However, in above example of restaurant reservation, such an undo is not possible, because of time-limiting nature of processing. A "reservation" itself auto-expires in utility after certain time has passed. This also relates to "SAGA" microservice pattern.. which can now be better seen as just a seqence of transactional outbox entries. Thus, whenever pressed with such situation, it is better to have a good data model that can account for such cases. For example, in case of credit card processing, it is better to change available credit to user, even if some portion of it is "pending transaction". The pending ones can change in future.. but restricting that credit now is a better solution. maybe it prevents user from possibility to use the "trapped" portion of it.. but that is better than allowing user credit which they weren't authorized to
---- If the row in a table can be changed to get constructed using "event sourcing", or if certain fields are in such manner that they get populated only on certain events, then event sourcing, along with requirement to update an entry in just one table could mean one can almost get transaction like behavior even in NoSQL environment
(4) Remember that version-based optimistic locking of SQL-DB.. when done on NoSQL becomes paxos protocol which is expensive.. so if locking is absolutely needed, either use RDBMS.. or let a machine take care of it by using in-memory locks.. and route the requests in manner so that request corresponding to update of same NoSQL table primary-key from different user goes to same machine. For example, in same resturant reservation app.. maybe Yelp can have a request routing in a manner that all request to reserve for a certain date on a resturant.. all such requests go to same server.. and then that server now takes care of ensuring there aren't 2 concurrent bookings for same time. NOTE that in SQL DB, this ca be solved by optimistic locking on version ONLY if the DB table first creates a version-0 of entry in reservation table corresponding to the time. Then only 1 user can commit that updates the version. If that initial entry is not made, and the reservation entry for each user gets made only when user commits, then we'll need a unique key constraint on DB-table to ensure that there is just one entry for reserving certain table in a resturant for certain date-time
 
 
 
* Using CQRS : If you can change the write to just use write-ahead log, or that every change in DB record is done as "processing" by an entry in transactional outbox pattern - then CQRS can be used there. Even if you think about Cassandra.. writing is done in caches, and then in multiple older persisted files - which are periodically merged.. and eventually at time of read, the full details are merged back to give the read "view" of data.. this is like a CQRS pattern. This means that any DB model can be made into CQRS pattern.. but don't yourself code it again.. let NoSQL DBs do it. You can, if needed, use CQRS, wherein you give a unified view of an object even though you store only a portion of it and read the remaining from another service. ..And also do same while writing

===end of todo for package structure===




 
 
 * Use "POST/REDIRECT/GET" pattern if you think that is better and more useful. However, note that it is not RESTful.. Or, maybe it is!! However, 
 * .. Hateoas links may get screwed up, or if we are returning response time
 *
 
 -- Let's say the user has 'N' sessions on - and wants to close all of them. how to do that?
 Say user changed password? how does JWT account for that
 Say user changed password.. then changed back to the same old password. how to close all accounts and force login again






2) Liquibase:

2.1) Don't break liquibase changelog in multiple.. it'll just cause issue. best keep everything in one folder. If you're adding huge data, then maybe keep the data in 1 file, and keep appending there unless original table has not changed. If it has, or you delete old data.. then make new data-file for it. Otherwise tracking an issue or seeing progression of a column/constraint becomes hard!!

2.2) When giving liquibase id, start with "release name". This can help do the same thing but without chunking the liquibase script file

2.3) When giving contexts, give a context like "production-pre" or "production-post" if you want to control if a script should be run before / after deployment on passive servers in a blue-green architecture (See below)

 

 

3) Blue-Green architecture and backward-compatibility:

3.1) What is it?

---- We have 2 group of servers: Active and Passive pool. Initially, let's say the DB is version-1 and services-war is version-1. Also note that at all times, both pools of server are joined to same DB (**IMPORTANT**)!!

---- Now, for the deployment activities, we pick Passive pool. We do liquibase update - this should not fail because it has been tested so. However, note that doing so now causes version-1 services-war on Active Pool to connect to version-2 upgraded DB. **VERY VERY IMPORTANT**: THIS IS WHERE INCONSISTENCY CAN HAPPEN. AND IT IS NECESSARY TO ENSURE THAT NOTHING WILL STILL BREAK!

---- Now, we deploy version-2 services-war on Passive servers. Should it fail, we'll put back version-1 services-war. **VERY VERY IMPORTANT**: THIS HIGHLIGHTS THAT THE PERIOD OF BACKGROUND-COMPATIBILITY CAN REALLY EXTEND.. SO DO NOT TAKE IT AS SMALL THING!

---- If everything goes fine, then we make Passive pool as Active, and vice versa. Now do deployment on the new set of Passive pools, and then make them Active.

3.2) Backward compatibility of DB:

---- To ensure backward-compatibility, the best way is to: (a) ensure you always "add" new column, (b) remove constraints -- adding constraints only when table/column is newly made [do NOTE to not remove "unique" constraint.. if your code has Spring-data method that expects only one column getting found, but then ends up with multiple columns getting found], (c) when adding constraint on existing column, check the code and also existing data before doing so, (d) add/update comment values on table/column
*
* 
*
*/
// @formatter:on

public class OtherWebDetailsController {

}
