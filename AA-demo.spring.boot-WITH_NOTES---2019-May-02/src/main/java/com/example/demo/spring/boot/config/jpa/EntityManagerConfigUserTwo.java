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

import com.example.demo.spring.boot.entity.second.ProductSecondUser;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This class configures the JPA datasource - for user2 only
 * 
 * @author KunjPrasad
 *
 */
// IMPORTANT: NOTE -- all relate config comments are in EntityManagerConfigUserOne.java

@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.spring.boot.entity.second",
        entityManagerFactoryRef = "user2EntityManagerFactoryBean",
        transactionManagerRef = "user2JpaTransactionManager")
public class EntityManagerConfigUserTwo {

    @Autowired
    private JpaHibernateConfiguration jpaHibernateConfiguration;

    // Datasource connection details
    @Value("${user2.jdbc.db.jndi-name:#{null}}")
    private String jdbcUser2JndiName;

    @Value("${jdbc.db.url:#{null}}")
    private String jdbcUser2Url;

    @Value("${jdbc.db.driver-class-name:#{null}}")
    private String jdbcUser2DriverClassNm;

    @Value("${spring.jpa.properties.hibernate.dialect:#{null}}")
    private String jdbcUser2Dialect;

    @Value("${user2.jdbc.db.name:#{null}}")
    private String jdbcUser2Name;

    @Value("${user2.jdbc.db.password:#{null}}")
    private String jdbcUser2Password;

    // PersistentUnit name to use
    @Value("${user2.spring.jpa.persistentunit.name}")
    private String user2PUName;

    // Default schema to use - for JPA
    @Value("${user2.spring.jpa.hibernate.default_schema:#{null}}")
    private String user2DefaultSchema;

    // Transaction properties
    @Value("${spring.jpa.transaction.timeout.seconds}")
    private int transactionTimeout;

    /**
     * This method prepares the jpa datasource bean for USER#2
     * 
     * If JNDI string is provided, then that is used, else a Datasource from jdbc url is made.
     * 
     * @return
     */
    @Bean
    public DataSource user2DataSource() {
        if (StringUtils.trimToNull(jdbcUser2JndiName) != null) {
            JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            return dataSourceLookup.getDataSource(jdbcUser2JndiName);
        } else if (StringUtils.trimToNull(jdbcUser2Url) != null
                && StringUtils.trimToNull(jdbcUser2DriverClassNm) != null
                && StringUtils.trimToNull(jdbcUser2Name) != null
                && StringUtils.trimToNull(jdbcUser2Password) != null) {
            HikariDataSource hds = new HikariDataSource();
            hds.setJdbcUrl(jdbcUser2Url);
            hds.setDriverClassName(jdbcUser2DriverClassNm);
            hds.setUsername(jdbcUser2Name);
            hds.setPassword(jdbcUser2Password);
            return hds;
        }
        throw new RuntimeException("User-2 Datasource not configured");
    }

    /**
     * LocalContainerEntityManagerFactoryBean and JpaTransactionManager for user#2
     * 
     * @return
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean user2EntityManagerFactoryBean() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(user2PUName);
        entityManagerFactoryBean.setDataSource(user2DataSource());
        entityManagerFactoryBean.setJpaVendorAdapter(jpaHibernateConfiguration.vendorAdaptor(jdbcUser2Dialect));
        entityManagerFactoryBean.setPackagesToScan(ProductSecondUser.class.getPackage().getName());
        Properties hibernateProps = jpaHibernateConfiguration.jpaHibernateProperties();
        if (StringUtils.trimToNull(user2DefaultSchema) != null) {
            hibernateProps.put("hibernate.default_schema", user2DefaultSchema);
        }
        entityManagerFactoryBean.setJpaProperties(hibernateProps);
        entityManagerFactoryBean.setValidationMode(ValidationMode.NONE);
        return entityManagerFactoryBean;
    }

    @Bean
    public PlatformTransactionManager user2JpaTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(user2EntityManagerFactoryBean().getObject());
        transactionManager.setDefaultTimeout(transactionTimeout);
        return transactionManager;
    }
}
