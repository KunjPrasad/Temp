package com.example.demo.spring.boot.config.jpa;

import java.util.Properties;

import javax.persistence.ValidationMode;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.demo.spring.boot.entity.first.Product;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This class configures the JPA datasource - for user1 only
 * 
 * @author KunjPrasad
 *
 */
// @formatter:off
/*
 * **VERY VERY VERY VERY IMPORTANT**: Understand the difference between JNDI vs setting datasource in application through properties.
 * .. related to it is also the understanding of why NOT to connection pool the datasource obtained from JNDI!!
 * .. and related further are discussion on container-managed or application-managed EntityManager!
 * === START ===
 * A corollary would be like whether to throw exception at database level or at services level when a record is not found in DB!
 * REALIZE: When record is not found, the DB methods generally return null. BUT, if it is agreed throughout the application that such scenarios
 * should throw runtimeException, then it might be better design to simply push the exception logic itself at DB level - so that the burden
 * of repetition is lifted from services. HOWEVER, if services need to deal with it separately, on case-by-case basis, then such push of exception
 * to DB level is not a good code.
 * 
 * *SIMILARLY*, when you have a single app-server on which multiple applications are hosted, and all of them need to use same DB, then in terms
 * of DB management and connection, it is better that the app-server maintains it rather than individual applications! However, if the application
 * uses DB on ad-hoc level, because the application is a POC, then DB connection configuration should be left to application and not via JNDI.
 * **OTHER INFERENCE**: This is why, (1) For Spring-boot in embedded mode, it doesn't make sense to configure datasource via JNDI.. this is a very 
 * low level effort and so can be done via properties; (2) When receiving datasource from JNDI, the connection pool should be set there that signifies
 * the total connection made available to all applications hosted on server. Adding another layer of connection pool is just useless now.
 * --- MORE ON IT LATER BELOW ---
 * 
 * Now, similarly, one can think of EntityManager - when ORM frameworks persist objects in database.. this can be done at application level,
 * .. but, if the same objects are being used in other web-application on same app-server, then it would be preferable to have both the 
 * .. entity-objects (this makes concept of EJB-beans) and the entityManager be handled by container (like Jboss); instead of being done at 
 * .. application level. The advantage is that this prevents redundancy in defining EJB and its mapping onto DB tables. However, this cannot,
 * .. by design, be a feature provided by JBoss (like they do with datasource/JNDI) - because EntityManager needs to be specific to the entity
 * .. beans - which changes with application. Thus, when making a container-managed entityManager, it needs to be an EAR, wherein a jar deploys
 * .. and creates an EntityManagerFactory, and registers it with JNDI; And then the war starts, and uses the JNDI EntityManager! *DO NOTE* that
 * .. additional reading seems to imply that EE containers may have a bootstrapping method to create a container managed EntityManager by reading
 * .. each application's persistence.xml.. but then why have this rather than programmatically make it in spring - using container datasource!
 * .. So, that option, whether it exists or not, is not explored
 * 
 * **VERY VERY IMPORTANT** ..and this bring up the question that in current environment where Spring does everything and we don't want to deal
 * .. with complexity of EAR, why then still the need for container-managed-entityManager! This is why it is ok to just have application itself
 * .. manage the entityManager
 * AND EVEN MORE IMPORTANT - why the need to get database via JNDI?! This now had pros and cons. Cons: If we are letting container manage the 
 * .. pooling, then cannot use HikariCP (unless using Tomcat..) Pros - and reason to still do it - is because even for same DB, it can have
 * .. multiple tables related to different user/applications - such that an application only sees a portion of entire DB. In this case, it is
 * .. still worthwhile to let the container be singular point to manage DB connections for more efficient use of pool. Also, from security
 * .. point of view, this prevents app-developer from knowing system details!
 * 
 * A related question is : Should TransactionManager be kept with Java EE container or with application. Considering how JTA provides separate
 * .. transaction-manager, it may seem that transaction-managers are something different from entity-manager; And so they can be pooled at 
 * .. container level. However, it seems to be tedious to have app-level entity-manager and then container-level transaction-management.
 * .. Might as well combine both and get a full streamlined effect. (Also remember that in JPA, a transaction object is actually obtained from
 * .. entityManager/session)
 * --|---- On note of TransactionManager: See https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#tx-propagation-nested
 * .. in that DO NOT allow nested transactions ... it seems that use of nested transaction is a very weird programming paradigm! Best avoid it
 * .. Also remember that "requires_new" is not nested
 * 
 * === START EXTRA RELATED INFO ===
 * -- Meaning of adding jar in jboss
 * --|---- See answer in https://stackoverflow.com/questions/26624036/how-to-deploy-a-spring-managed-jpa-application-on-jboss-6-3-eap
 * .. the initial prep steps shows the need to add jar and module.xml in jboss. This is the reason: let's say we are leveraging container's
 * .. (jboss') ability to give datasource via JNDI. But in order to do so, Jboss will need a connector-jar file that it can use while starting
 * .. up. And that's why we need to add jar to jboss. And module.xml give additional related information. This is done to assist startup
 * --|---- Finally, to actually add driver, see https://zorq.net/b/2011/07/12/adding-a-mysql-datasource-to-jboss-as-7/  or    
 * .. https://access.redhat.com/documentation/en-us/jboss_enterprise_brms_platform/5/html/brms_administrator_guide/configuring_a_datasource_for_jboss_enterprise_application_platform_6
 * 
 * -- understanding persistence.xml
 * --|---- See https://stackoverflow.com/questions/7770847/understanding-persistence-xml-in-jpa  ..essentially it a name, collections of classes
 * and a jpa-provider that changes entities to db-statements. (For SharedCacheMode, see later when describing about properties)
 * --|---- Suggested to keep validation-mode as always NONE ..because proper validation should be done at controller level, not DB!!
 * 
 * -- understanding JPA properties
 * --|---- VERY IMPORTANT and somewhat confusing are the various places in which same configurations seem to show up. For example, see
 * --|----|---- https://stackoverflow.com/questions/18649583/jpa-provider-vs-dialect-vs-vendor-in-the-spring-contaniner-configuration ..which
 * .. shows the various locations providing jpaDialect ..but it can be picked from hibernateJpaProvider! This is probably because of overall
 * .. integration - because JPA can have a dialect (this is a setting in persistence.xml), but then if we associate an overall provider with 
 * .. Spring's LocalContainerEntityManagerFactoryBean's method.. then that auto-sets the jpa-dialect, rendering the dialect-setter method 
 * .. provided by JPA as useless! And then there is JTA.. which, if it uses some other API, then that method is useless again. And if we use
 * .. HibernateProvider, then that should be auto-set. So, overall there are multiple redundancies from integration
 * --|----|---- https://stackoverflow.com/questions/10440444/hibernate-dialect-issue-with-spring-configuration  ..NOTE that the DB specific 
 * .. dialect to use by Hibernate can either be set in HibenateJpaVendorAdapter, or passed to it as property, ..or even bizarrely, it is not 
 * .. needed because it can be auto inferred! This is because Hibernate can be set on its own, or following the standards, a set of properties
 * .. can be passed to it! Also, shown in https://stackoverflow.com/questions/38581074/how-do-i-set-the-hibernate-dialect-in-springboot
 * --|----|----|---- **VERY IMPORTANT**: "Hibernate can determine the correct dialect to use automatically, but in order to do this, 
 *                  .. it needs a live connection to the database. Since this is not possible with H2, so it is suggested to add the dialect 
 *                  .. property. See: https://stackoverflow.com/questions/26548505/org-hibernate-hibernateexception-access-to-dialectresolutioninfo-cannot-be-null
 *                  .. **IMPORTANT** BUT, this is possible when deploying on JBOSS.. so that property is not needed there. And this makes
 *                  .. sense because in scenarios when using JNDI, the application should NOT be having any other dependency to related property!
 * --|----|---- Not discussed, but same also holds on "show_sql". - which can be configured in HibernateJpaVendorAdapter, or passed as property
 * --|---- Example of various hibernate properties: https://www.petrikainulainen.net/programming/spring-framework/spring-data-jpa-tutorial-part-one-configuration/
 * 
 * -- Particularly understanding Hibernate Properties
 * --|---- For full list of Hibernate properties, see https://docs.jboss.org/hibernate/core/4.3/manual/en-US/html/ch03.html
 * --|---- **VERY VERY IMPORTANT** : because apparantly official Hibernate docs don't show this info.. on how to disable Hibernate from making
 * .. missing tables automatically. See https://stackoverflow.com/questions/3179765/how-to-turn-off-hbm2ddl    The answer is to give an unknown
 * .. property that causes the table-generation operation to fail, and hence stop!!! **IMPORTANT**: Note that HibernateJpaVendorAdapter also has 
 * .. method .setGenerateDdl(boolean) which can be set to false to prevent Hibernate from doing so
 * --|---- **VERY IMPORTANT**: To understand hibernate.max_fetch_depth, see https://stackoverflow.com/questions/25146492/understanding-hibernate-hibernate-max-fetch-depth-and-hibernate-default-batch-fe
 * .. In ideal design, set it zero, so that lazy loading is strictly enforced! (But some places of non-zero use could be seen.. so set accordingly)
 * .. Also, this explains "default_batch_fetch_size" setting, which is used when a single result has a collection associated to it, like @OneToMany
 * --|---- Another important property is "hibernate.jdbc.fetch_size", which controls how many results are obtained in a go!
 * --|---- NOTE one of property is "hibernate.default_schema" -- which raises the question of what is "schema"? Simply, "Schema" is the 
 * .. blueprint according to which DB are made. Many places say that username is same as schema (saw that in oracle). At least in MySQL, when 
 * .. using the workbench, one can make database - and schema takes that name, and then different users can be attached to it with different 
 * .. authorities. so essentially, schema is a very DB related concept -- since H2 is fleeting, so "schema" can be removed when using H2-db
 * --|----|---- Refr: See https://stackoverflow.com/questions/880230/difference-between-a-user-and-a-schema-in-oracle   
 * .. https://dba.stackexchange.com/questions/37012/difference-between-database-vs-user-vs-schema
 * 
 * **IMPORTANT**: Meaning of some Java-EE terms: EntityManagerFactor, EntityManager, PersistenceContext
 * -- See pages:
 * --|---- EntityManager is the main actor. It takes an entity-bean and manages it - loading from DB, modifying, saving, deleting, etc. In order
 * .. to do so, it uses configurations defined in <persistent> tag in persistence.xml. The entire collection of beans inside it makes up the 
 * .. persistenceContext, i.e. there are beans such that they map to a given identifier-value and are made from DB-table per annotations on
 * .. class. See https://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html
 * --|---- While EntityManager is main player.. realize that the even same class-beans corresponding to same id can be in a different state in
 * .. different web request. Thus, by its very construct, an EntityManager is NOT thread-safe. Thus, an EntityManagerFactory is needed to create
 * .. EntityManagers as needed!! (I am not sure if this is true.. but seems so.., and is easy to remember). For some more insight, see javadoc
 * .. of Spring's LocalContainerEntityManagerFactorBean, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/orm/jpa/LocalContainerEntityManagerFactoryBean.html
 * 
 * -- Getting JPA EntityManagerFactory in Spring: 
 * --|---- See section 4.4.1 in https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html  This states that 
 * .. LocalContainerEntityManagerFactoryBean IS the most powerful and flexible way to configure JPA
 * --|---- Note that LocalContainerEntityManagerFactoryBean corresponds to a "single" persistenceUnit in persistence.xml. When making 
 * .. the factoryBean, if you provide it a path to persistence.xml, you should also set the persistenceUnit-name that it should pick up
 * .. and make different LocalContainerEntityManagerFactoryBean for each persistence unit. This is also logical because, this bean represents
 * .. a more intricate EntitymanagerFactory - and so it should be made separate for each persistenceUnit corresponding to THAT EntityManager
 * .. See https://jira.spring.io/browse/SPR-6781
 * --|---- **IMPORTANT**: Though it seems to be not necessary, but.. as a good coding practice, always give a name to PersistentUnit of the bean
 * 
 * -- To bind an object to JNDI
 * --|---- Say, you want to create an object and bind it to JNDI so it can be used by others.. the way to do is.. See https://nozaki.me/roller/kyle/entry/how-to-bind-lookup-a
        final Context context = new InitialContext();
        try {
            context.createSubcontext("java:");
            context.createSubcontext("java:comp");
            context.createSubcontext("java:comp/env");
            context.createSubcontext("java:comp/env/jdbc");
            context.bind(JNDI, ds);
        } finally {
            context.close();
        }
 * 
 * === END ===
 * 
 * **IMPORTANT**: Note that the jdbc properties used in application-{..}.properties do NOT correspond to default properties used by 
 * .. Spring to configure datasource. This is done so that a Datasource bean is made programmatically, and so the same bean can suffice 
 * .. if say, a different environment has other DB. Similarly, same bean abstract away whether the datasource is from JNDI or from properties 
 * An example of using direct spring boot properties to make H2-db is: https://memorynotfound.com/spring-boot-spring-data-jpa-hibernate-h2-web-console/
 * 
 * --|---- For info on H2's jdbc url, see http://www.h2database.com/html/features.html 
 * --|---- For creating DB, users and privileges in Mysql, see http://webvaultwiki.com.au/Default.aspx?Page=Create-Mysql-Database-User-Workbench&NS=&AspxAutoDetectCookieSupport=1
 * **IMPORTANT**: NOTE: Make 2 set of users: one admin and other user type (like in PatentCenter). ONLY "ADMIN" will have ddl rights to DB 
 * --|---- For information on DataSource implementation provided by Spring, see #13.3 in https://docs.spring.io/spring/docs/4.0.x/spring-framework-reference/html/jdbc.html 
 * --|---- To see various ways to make DataSource in Spring-boot, see https://docs.spring.io/spring-boot/docs/current/reference/html/howto-data-access.html 
 * .. and https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html 
 * --|---- **VERY VERY IMPORTANT** Do google search.. it seems HikariCP is fastest connection-pooled datasource and also allow various combinations
 * --|---- **VERY VERY IMPORTANT** It seems making HikariCP via Jboss is not available, see https://stackoverflow.com/questions/25260811/how-to-configure-jndi-datasource-in-jboss-using-hikaricp
 * .. in which author makes Jboss connection pool in JNDI, and then again puts it in HikariCP in Spring.. which is double CP and is discouraged (explained above)
 * 
 * See here on difference between JndiObjectFactoryBean and JndiDataSourceLookup --- https://stackoverflow.com/questions/36575383/why-use-jndiobjectfactorybean-to-config-jndi-datasource-did-not-work
 * Former is used in codes of Patentcenter; Latter is recommended way by Spring-boot-docs
 * 
 * **VERY VERY IMPORTANT**: When making own/multiple LocalContainerEntityManagerFactoryBean -- then remember to add @EnableJpaRepositories with
 * .. basePackages, entityManagerFactoryRef and transactionManagerRef -- for each Bean. Else, Spring-boot will no longer automatically make repositories
 * --|---- If not done, spring fails with error message like: "Spring data jpa- No bean named 'entityManagerFactory' is defined..."
 * 
 * LIQUIBASE:
 * --|---- See LiquibaseConfiguration.java for Liquibase related configurations.
 * --|---- **IMPORTANT**: do note that when making admin datasource for liquibase (in JNDI), give it a min-thread-count of 1 (or 0),
 * .. because after the initial liquibase setup, that admin datasource is no longer needed!
 * 
 * JNDI-Datasource lookup
 * Provided below is one way to lookup for datasource. There are 2 other ways which are generic and can be used for other lookups:
 * --|---- JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
            jndiObjectFactoryBean.setJndiName(jndiName + suffix);
            try {
                jndiObjectFactoryBean.afterPropertiesSet(); // **IMPORTANT** Note the use of this call.. This makes the definition get pulled
                                                            // .. from JNDI. Before this, it is only some bean, and not linked to JNDI as yet!
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
            result = (DataSource) jndiObjectFactoryBean.getObject();
 * --|---- return (DataSource) new JndiTemplate().lookup(env.getProperty("jdbc.url"));
 * 
 * **IMPORTANT** Excluding auto-configurations: Note that when there are multiple datasource (or multiple sources of any kind), then default 
 * .. spring boot config would fail. That's why the code below explictly wires datasource to use with where it is needed. Just for knowledge, 
 * .. note note that there is also a related annotation which is: 
 *          @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, //if you are using Hibernate
                    DataSourceTransactionManagerAutoConfiguration.class
            }) 
 * 
 *
 *
 * **VERY VERY VERY IMPORTANT**: 
 * .. Basic things first, with HikariCP, the configs itself allow have a max-lifetime setting on a connection in pool. So, this can prevent long 
 * connections -- for security reason! Also, Hikari is shown to be very fast.
 * NOW.. the important part.. with Hikari, it is possible to have different password at different times! So this also enhances security by preventing
 * .. use of passowrds for long terms. Let's see how..
 * --|---- The first thing to notice is from this Github/issue: https://github.com/brettwooldridge/HikariCP/issues/879  -- that rather than making a 
 * .. HikariConfig which is then used for HikariDataSource; If you just make a HikariDatasource with the settings, then it is possible to change 
 * .. password in future and have that get reflected
 * --|---- In terms of code, the way it works.. is that HikariDataSource starts by making a HikariPool, which itself extends PoolBase. [[Digression: When 
 * .. datasource is now asked for connection, it gets that from the pool.]] Now, as part of PoolBase constructor, it creates the datasource that actually
 * .. connects to DB and is used to get more connection. When HikariPool wants to create new connection, it calls its createPoolEntry(); which uses 
 * .. newPoolEntry() in PoolBase, which uses newConnection() is poolBase, which then gets a connection using the username/password combo from the 
 * .. database that was made at beginning. Also, createPoolEntry() in HikariPool also adds a clearing-time if a max-time-to-live is defined. 
 * .. SO: WHAT YOU NEED TO DO IS: 
 * --|----|---- (a) Start by making a user/password which has almost no privileges in DB, but is useful to make that initial connection - it will be a 
 * .. static user but with no role or privilege 
 * --|----|---- (b) Have a program that periodically creates new username/password and deletes old combinations. So, say, application started at 00:00am 
 * .. there was {user1, pwd1, expiry@00:30am}; At 00:20am, it makes {user2, pwd2, expiry@01:00am}; At 00:30am, it deletes {user1, pwd1} user.
 * --|----|---- (c) In the application (war file), say, at 00:00am, it configures HikariDS with {user1, pwd1, length=(00:30am - currentTime - offset, 
 * .. say, 4 mins -minus- max-transaction-time, say, 1 minute )}, so, for 00:00, that will mean making entry of {user1, pwd1, 25 mins length}. 
 * .. Next update cycle runs faster, at 00:02am, it changes to {user1, pwd1, 23-mins length}, and so on. At 00:20am, it sees in DB some flag to show 
 * .. a total count of "2" - meaning a new combination is available at which time it will need to N-factor authenticate itself with program (in Step(b)) 
 * .. to get the new user/password/expiry combo. When that it done, the application now uses new {user,pwd,expiry}.
 * ***THIS WAY*** any pools made during the period will always keep on using new username and password. Also, there will only have lifetime before the
 * .. configuration is deleted. SO.. there will never be a case where user will see broken connection!!
 * ***IMPORTANT ISSUE.. NOTE***: Current Hikari code prevents getting a lock before making simultaneous changes to username and password; So it is  
 * .. possible to have some concurrency issues. (I'm guessing) A way around is to set configurations, allow pool to be "suspended", when you make the 
 * .. username and password changes, and then allow pool to resume!
 * 
 *
 *
 * **VERY VERY VERY IMPORTANT**: 
 * REALIZE that it is possible to do a 2F DB access restriction. See https://www.dbarj.com.br/en/oratotp-oracle-time-based-one-time-password/   and
 * https://github.com/dbarj/OraTOtP 
 * .. This can be done for non-Java-application-users (like support staff). For Java applications, we already have HikariCP based route discussed above. 
 * .. If needed, it can further be expanded, where each jboss-app can contact a central service to get new token that should be appended to updated
 * .. password in DB, or, that can itself act like new DB -- but this may become a bit too much. Best, just use the HikariCP based solution as above,
 * .. alongwith, giving jboss applications their own user name to connect, different from other users
 *
 * --|---- SIDE NOTE: The github sql file is a good and detailed example of how complex function / procedure can be done in SQL. It can even be used in 
 * .. Hibernate Learn it!      https://www.techonthenet.com/oracle/functions.php      https://www.mkyong.com/oracle/oracle-plsql-create-function-example/
 *
 */
// @formatter:on


@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.spring.boot.entity.first",
        entityManagerFactoryRef = "user1EntityManagerFactoryBean",
        transactionManagerRef = "user1JpaTransactionManager")
@EnableTransactionManagement
public class EntityManagerConfigUserOne {

    @Autowired
    private JpaHibernateConfiguration jpaHibernateConfiguration;

    // Datasource connection details
    @Value("${user1.jdbc.db.jndi-name:#{null}}")
    private String jdbcUser1JndiName;

    @Value("${jdbc.db.url:#{null}}")
    private String jdbcUser1Url;

    @Value("${jdbc.db.driver-class-name:#{null}}")
    private String jdbcUser1DriverClassNm;

    @Value("${spring.jpa.properties.hibernate.dialect:#{null}}")
    private String jdbcUser1Dialect;

    @Value("${user1.jdbc.db.name:#{null}}")
    private String jdbcUser1Name;

    @Value("${user1.jdbc.db.password:#{null}}")
    private String jdbcUser1Password;

    // PersistentUnit name to use
    @Value("${user1.spring.jpa.persistentunit.name}")
    private String user1PUName;

    // Default schema to use - for JPA
    @Value("${user1.spring.jpa.hibernate.default_schema:#{null}}")
    private String user1DefaultSchema;

    // Transaction properties
    @Value("${spring.jpa.transaction.timeout.seconds:30}")
    private int transactionTimeout;

    /**
     * This method prepares the jpa datasource bean for USER#1
     * 
     * If JNDI string is provided, then that is used, else a Datasource from jdbc url is made.
     * 
     * @return
     */
    @Bean
    public DataSource user1DataSource() {
        if (StringUtils.trimToNull(jdbcUser1JndiName) != null) {
            JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            return dataSourceLookup.getDataSource(jdbcUser1JndiName);
        } else if (StringUtils.trimToNull(jdbcUser1Url) != null
                && StringUtils.trimToNull(jdbcUser1DriverClassNm) != null
                && StringUtils.trimToNull(jdbcUser1Name) != null
                && StringUtils.trimToNull(jdbcUser1Password) != null) {
            HikariDataSource hds = new HikariDataSource();
            hds.setJdbcUrl(jdbcUser1Url);
            hds.setDriverClassName(jdbcUser1DriverClassNm);
            hds.setUsername(jdbcUser1Name);
            hds.setPassword(jdbcUser1Password);
            return hds;
        }
        throw new RuntimeException("User-1 Datasource not configured");
    }

    /**
     * LocalContainerEntityManagerFactoryBean and JpaTransactionManager for user#1
     * 
     * @return
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean user1EntityManagerFactoryBean() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(user1PUName);
        entityManagerFactoryBean.setDataSource(user1DataSource());
        entityManagerFactoryBean.setJpaVendorAdapter(jpaHibernateConfiguration.vendorAdaptor(jdbcUser1Dialect));
        entityManagerFactoryBean.setPackagesToScan(Product.class.getPackage().getName());
        Properties hibernateProps = jpaHibernateConfiguration.jpaHibernateProperties();
        if (StringUtils.trimToNull(user1DefaultSchema) != null) {
            hibernateProps.put("hibernate.default_schema", user1DefaultSchema);
        }
        entityManagerFactoryBean.setJpaProperties(hibernateProps);
        entityManagerFactoryBean.setValidationMode(ValidationMode.NONE);
        return entityManagerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager user1JpaTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(user1EntityManagerFactoryBean().getObject());
        transactionManager.setDefaultTimeout(transactionTimeout);
        transactionManager.setNestedTransactionAllowed(false);
        return transactionManager;
    }
}
