package com.example.demo.spring.boot.config.jpa;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * This class provides utility method to EntityManagerFactory configuring classes to assist in setting Hibernate
 * implementation
 * 
 * @author KunjPrasad
 *
 */

@Configuration
// @Configuration is to enable access to property -- and if Beans are needed in future
// NOTE that the methods are NOT marked with @Bean annotation, so that separate clas can be made every time they are
// requested
// NOTE that "jpaHibernateProperties" method was initially not public. It was then made public because it is also used
// by JTA now
public class JpaHibernateConfiguration {

    @Value("${spring.jpa.hibernate.hbm2ddl.auto}")
    private String generateDdl;

    @Value("${spring.jpa.hibernate.show_sql:false}")
    private boolean showSql;

    @Value("${spring.jpa.hibernate.format_sql:true}")
    private boolean formatSql;

    @Value("${spring.jpa.hibernate.max_fetch_depth:0}")
    private int maxFetchDepth;

    @Value("${spring.jpa.hibernate.default_batch_fetch_size}")
    private int collectionResultSize;

    @Value("${spring.jpa.hibernate.jdbc.fetch_size}")
    private int jdbcResultFetchSize;

    // Utility method to provide common hibernate adapter
    public HibernateJpaVendorAdapter vendorAdaptor(String dialect) {
        HibernateJpaVendorAdapter hibernateAdapter = new HibernateJpaVendorAdapter();
        if (StringUtils.trimToNull(dialect) != null) {
            hibernateAdapter.setDatabasePlatform(dialect);
        }
        hibernateAdapter.setShowSql(showSql);
        return hibernateAdapter;
    }

    // utility method to provide common hibernate properties
    public Properties jpaHibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", generateDdl);
        properties.put("hibernate.format_sql", formatSql);
        properties.put("hibernate.max_fetch_depth", maxFetchDepth);
        properties.put("hibernate.default_batch_fetch_size", collectionResultSize);
        properties.put("hibernate.jdbc.fetch_size", jdbcResultFetchSize);
        return properties;
    }
}
