package com.example.demo.spring.boot.config.jta;

import java.util.Properties;

import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.lang3.StringUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jta.atomikos.AtomikosDataSourceBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import com.example.demo.spring.boot.config.jpa.JpaHibernateConfiguration;
import com.example.demo.spring.boot.entity.jta.ProductJta;
import com.example.demo.spring.boot.util.ApplicationConstants;

/**
 * This helper configuration class creates beans corresponding to JPA side of JTA
 * 
 * @author KunjPrasad
 *
 */
@Configuration
public class JtaJpaUser1HelperConfiguration {

    @Autowired
    private JpaHibernateConfiguration hibernateConfiguration;

    // Datasource connection details
    @Value("${jta.jpa.user1.db.jndi-name:#{null}}")
    private String jtaJpaUser1JndiName;

    @Value("${jta.jpa.db.xa-url:#{null}}")
    private String jtaJpaUser1Url;

    @Value("${jta.jpa.db.xa-type:#{null}}")
    private String jtaJpaUser1DbType;

    @Value("${jta.jpa.db.hibernate.dialect:#{null}}")
    private String jtaJpaUser1Dialect;

    @Value("${jta.jpa.user1.db.name:#{null}}")
    private String jtaJpaUser1Name;

    @Value("${jta.jpa.user1.db.password:#{null}}")
    private String jtaJpaUser1Password;

    // PersistentUnit name to use
    @Value("${jta.jpa.user1.persistentunit.name}")
    private String jtaJpaUser1PUName;

    // Default schema to use - for JPA
    @Value("${jta.jpa.user1.hibernate.default_schema:#{null}}")
    private String jtaJpaUser1DefaultSchema;

    /**
     * This method prepares the jpa datasource bean for USER#1
     * 
     * If JNDI string is provided, then that is used, else a Datasource from jdbc url is made.
     * 
     * @return
     */
    @Bean(initMethod = "init", destroyMethod = "close")
    public DataSource jtaJpaUser1DataSource() {
        if (StringUtils.trimToNull(jtaJpaUser1JndiName) != null) {
            JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            return dataSourceLookup.getDataSource(jtaJpaUser1JndiName);
        } else if (StringUtils.trimToNull(jtaJpaUser1Url) != null
                && StringUtils.trimToNull(jtaJpaUser1DbType) != null
                && StringUtils.trimToNull(jtaJpaUser1Name) != null
                && StringUtils.trimToNull(jtaJpaUser1Password) != null) {
            // Note that Hikari docs say "XA data sources are not supported" - so it is not used here
            AtomikosDataSourceBean xaDataSourceBean = new AtomikosDataSourceBean();
            xaDataSourceBean.setXaDataSource(getXADatasourceForType(jtaJpaUser1DbType, jtaJpaUser1Url, jtaJpaUser1Name,
                    jtaJpaUser1Password));
            xaDataSourceBean.setUniqueResourceName("user1XADatsSource");
            xaDataSourceBean.setMinPoolSize(1);
            xaDataSourceBean.setMaxPoolSize(5);
            return xaDataSourceBean;
        }
        throw new RuntimeException("User-1 XA-Datasource not configured");
    }

    // Utility method to get XADatasource, so that it can then be wrapped as AtomikosDataSourceBean
    private XADataSource getXADatasourceForType(String dbType, String dbUrl, String userName, String password) {
        if (ApplicationConstants.H2_JPA_PROVIDER_NM.equalsIgnoreCase(dbType)) {
            JdbcDataSource h2XaDataSource = new JdbcDataSource();
            h2XaDataSource.setURL(dbUrl);
            h2XaDataSource.setUser(userName);
            h2XaDataSource.setPassword(password);
            return h2XaDataSource;
        }
        throw new RuntimeException("XA datasource not found for db-type=" + dbType);
    }

    /**
     * LocalContainerEntityManagerFactoryBean and JpaTransactionManager for user#1
     * 
     * @return
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean jtaJpaUser1EntityManagerFactoryBean() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPersistenceUnitName(jtaJpaUser1PUName);
        entityManagerFactoryBean.setDataSource(jtaJpaUser1DataSource());
        entityManagerFactoryBean.setJpaVendorAdapter(hibernateConfiguration.vendorAdaptor(jtaJpaUser1Dialect));
        entityManagerFactoryBean.setPackagesToScan(ProductJta.class.getPackage().getName());
        // extra hiberate properties
        Properties hibernateProps = hibernateConfiguration.jpaHibernateProperties();
        if (StringUtils.trimToNull(jtaJpaUser1DefaultSchema) != null) {
            hibernateProps.put("hibernate.default_schema", jtaJpaUser1DefaultSchema);
        }
        // extra properties for JTA
        hibernateProps.put("hibernate.transaction.jta.platform", AtomikosJtaPlatform.class.getName());
        hibernateProps.put("javax.persistence.transactionType", "JTA");
        // set JPA properties made above
        entityManagerFactoryBean.setJpaProperties(hibernateProps);
        entityManagerFactoryBean.setValidationMode(ValidationMode.NONE);
        return entityManagerFactoryBean;
    }

}
