package com.example.demo.spring.boot.config.jpa;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import com.zaxxer.hikari.HikariDataSource;

import liquibase.integration.spring.SpringLiquibase;

/**
 * This class defines configuration used by Liquibase
 * 
 * @author KunjPrasad
 *
 */
@Configuration
public class LiquibaseConfiguration {

    @Value("${admin.jdbc.db.jndi-name:#{null}}")
    private String jdbcAdminJndiName;

    @Value("${jdbc.db.url:#{null}}")
    private String jdbcAdminUrl;

    @Value("${jdbc.db.driver-class-name:#{null}}")
    private String jdbcAdminDriverClassNm;

    @Value("${admin.jdbc.db.name:#{null}}")
    private String jdbcAdminName;

    @Value("${admin.jdbc.db.password:#{null}}")
    private String jdbcAdminPassword;

    // No need to read admin default-schema, since that goes to Liquibase-properties - if set

    /**
     * This method prepares the datasource bean for ADMIN. This is done in LiquibaseConfiguration - because admin
     * datasource is only used by liquibase to make tables
     * 
     * If JNDI string is provided, then that is used, else a Datasource from jdbc url is made.
     * 
     * @return
     */
    @Bean
    public DataSource adminDataSource() {
        if (StringUtils.trimToNull(jdbcAdminJndiName) != null) {
            JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
            return dataSourceLookup.getDataSource(jdbcAdminJndiName);
        } else if (StringUtils.trimToNull(jdbcAdminUrl) != null
                && StringUtils.trimToNull(jdbcAdminDriverClassNm) != null
                && StringUtils.trimToNull(jdbcAdminName) != null
                && StringUtils.trimToNull(jdbcAdminPassword) != null) {
            HikariDataSource hds = new HikariDataSource();
            hds.setJdbcUrl(jdbcAdminUrl);
            hds.setDriverClassName(jdbcAdminDriverClassNm);
            hds.setUsername(jdbcAdminName);
            hds.setPassword(jdbcAdminPassword);
            return hds;
        }
        throw new RuntimeException("Admin Datasource not configured");
    }

    /**
     * This makes a bean with liquibase properties set using properties which will then be used to configure
     * SpringLiquibase isntance
     * 
     * @return
     */
    @Bean
    LiquibaseProperties getLiquibaseProperties() {
        // Instead of using @EnableConfigurationProperties(LiquibaseProperties.class) on class and having member
        // @Autowired private LiquibaseProperties liquibaseProperties -- just using this method
        return new LiquibaseProperties();
    }

    /**
     * This makes the liquibase bean used by Spring
     * 
     * @return
     */
    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        LiquibaseProperties liquibaseProperties = getLiquibaseProperties();
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDataSource(adminDataSource());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        return liquibase;
    }

}
