package com.example.demo.spring.boot.config.jta;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// @formatter:off
/*
 * **IMPORTANT**: Some basic JTA code:
 * --|---- https://spring.io/blog/2011/08/15/configuring-spring-and-jta-without-full-java-ee/
 * --|---- http://fabiomaffioletti.me/blog/2014/04/15/distributed-transactions-multiple-databases-spring-boot-spring-data-jpa-atomikos/
 * --|---- https://github.com/mtorak/disttrans
 * --|---- https://github.com/YihuaWanglv/spring-boot-jta-atomikos-sample
 * --|---- **IN PARTICULAR** Note the SO post: https://stackoverflow.com/questions/20681245/how-to-use-atomikos-transaction-essentials-with-hibernate-4-3
 * .. and also the codes provided above. The point is JTA setup requires providing an implementation of AbstractJtaPlatform to Hibernate when
 * .. making the LocalContainerEntityManagerFactory. This is an extra to remember comparing JTA configuration to that for JPA
 * --|----|---- In this regard, note the configuring jumble in jta as compared to that in Jpa. For Jpa, the transactionManager has option to 
 * .. attach the EntityManagerFactory; But this setting is not available for JtaTransactionManager - because by definition Jta works as tx-manager
 * .. in independent scope from EntityMananger. HOWEVER, in reality, the entitymanager factory is slightly dependent on Tx-Mananger because 
 * .. Hibernate requires information of user-transaction and Jta-Tx-Manager being used (like.. the actual Manager, not Spring's PlatformTxManager).
 * --|----|----|---- ALSO, note the way coding for "AtomikosJtaPlatform" is done.. it is slightly different from that in code in first
 * .. reference, but mostly similar. Also, additional comments have been added to help with understanding the coding
 * 
 * Basic details about JTA, particularly 2-phase-commit process: https://dzone.com/articles/xa-transactions-2-phase-commit
 * 
 * On note of TransactionManager: As mentioned in EntityManagerConfigUserOne, it seems that use of nested transaction is a very weird 
 * .. programming paradigm and it best avoided. On top of it see the SO post saying that for JTA, support of "Nested" transaction is not mandatory
 * .. So to prevent inconsistencies, best avoid it. See https://stackoverflow.com/questions/12552198/why-nested-transactions-are-not-supported-in-jta
 * 
 * **VERY IMPORTANT**: Note that generally we'd want that EntityManager, and XA-Entity management, etc. starts after the DB tables are already 
 * .. made by Liquibase (..and not before). Maybe Spring takes care of it automatically. One way to ensure things happen "after".. is to use
 * .. @DependsOn annotation from Spring  
 */
//@formatter:on

@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.spring.boot.entity.jta",
        entityManagerFactoryRef = "jtaJpaUser1EntityManagerFactoryBean",
        transactionManagerRef = "jtaPlatformTransactionManager")
public class JtaConfiguration {
    // In essence this is just a placeholder class for comments - except for the main annotation enabling the
    // repository, etc.
    // This is understandable because in JTA, configuration is totally separate for DB /JMS/ transaction side, so all
    // can be made separate
}
