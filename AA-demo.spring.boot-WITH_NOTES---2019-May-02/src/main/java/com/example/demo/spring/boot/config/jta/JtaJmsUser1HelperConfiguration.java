package com.example.demo.spring.boot.config.jta;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.XAConnectionFactory;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.atomikos.jms.AtomikosConnectionFactoryBean;
import com.example.demo.spring.boot.util.ApplicationConstants;

@Configuration
public class JtaJmsUser1HelperConfiguration {

    @Autowired
    private MessageConverter jacksonJmsMessageConverter;

    @Autowired
    private PlatformTransactionManager jtaPlatformTransactionManager;

    @Autowired
    private DefaultJmsListenerContainerFactoryConfigurer configurer;

    @Value("${jta.jms.user1.jndi-name:#{null}}")
    private String jtaJmsUser1JndiName;

    @Value("${jta.jms.url:#{null}}")
    private String jtaJmsUser1Url;

    @Value("${jta.jms.provider-class-name:#{null}}")
    private String jtaJmsUser1ProviderClassNm;

    @Value("${jta.jms.user1.name:#{null}}")
    private String jtaJmsUser1Name;

    @Value("${jta.jms.user1.password:#{null}}")
    private String jtaJmsUser1Password;

    @Bean(initMethod = "init", destroyMethod = "close")
    public ConnectionFactory jtaJmsUser1ConnectionFactory() {
        if (StringUtils.trimToNull(jtaJmsUser1JndiName) != null) {
            try {
                Object obj = new JndiTemplate().lookup(jtaJmsUser1JndiName);
                return (ConnectionFactory) obj;
            } catch (NamingException e) {
                throw new RuntimeException("User-1 Jta/Jms-connection not configured in JNDI", e);
            }
        } else if (StringUtils.trimToNull(jtaJmsUser1Url) != null
                && StringUtils.trimToNull(jtaJmsUser1ProviderClassNm) != null
                && StringUtils.trimToNull(jtaJmsUser1Name) != null
                && StringUtils.trimToNull(jtaJmsUser1Password) != null) {
            AtomikosConnectionFactoryBean atomikosConnectionFactoryBean = new AtomikosConnectionFactoryBean();
            atomikosConnectionFactoryBean.setXaConnectionFactory(getXADatasourceForType(jtaJmsUser1ProviderClassNm,
                    jtaJmsUser1Url, jtaJmsUser1Name, jtaJmsUser1Password));
            atomikosConnectionFactoryBean.setUniqueResourceName("jtaJmsUser1ConnectionFactory");
            atomikosConnectionFactoryBean.setLocalTransactionMode(false);
            atomikosConnectionFactoryBean.setMinPoolSize(1);
            atomikosConnectionFactoryBean.setMaxPoolSize(4);
            return atomikosConnectionFactoryBean;
        }
        throw new RuntimeException("User-1 XA-JMS-ConnectionFactory not configured");
    }

    // Utility method to get XAConnectionFactory, so that it can then be wrapped as AtomikosConnectionFactoryBean
    private XAConnectionFactory getXADatasourceForType(String jmsType, String jmsUrl, String userName,
            String password) {
        if (ApplicationConstants.ACTIVE_MQ_JMS_PROVIDER_NM.equalsIgnoreCase(jmsType)) {
            ActiveMQXAConnectionFactory activeMQXAConnectionFactory = new ActiveMQXAConnectionFactory(jmsUrl);
            activeMQXAConnectionFactory.setUserName(userName);
            activeMQXAConnectionFactory.setPassword(password);
            return activeMQXAConnectionFactory;
        }
        throw new RuntimeException("XA jms-conectionFactory not found for jms-type=" + jmsType);
    }

    @Bean
    public JmsTemplate jtaJmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(jtaJmsUser1ConnectionFactory());
        template.setMessageConverter(jacksonJmsMessageConverter);
        template.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        template.setSessionTransacted(true);
        return template;
    }

    // NOTE: new listenerContainerFactory for XA aareness
    @Bean
    public JmsListenerContainerFactory<?> jtaJmsUser1ListenerContainerFactory() {
        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();
        // basic config
        configurer.configure(jmsListenerContainerFactory, jtaJmsUser1ConnectionFactory());
        // extra config
        // jmsListenerContainerFactory.setConnectionFactory(jtaJmsUser1ConnectionFactory());
        jmsListenerContainerFactory.setConcurrency("1-2");
        jmsListenerContainerFactory.setSessionTransacted(true);
        jmsListenerContainerFactory.setTransactionManager(jtaPlatformTransactionManager);
        jmsListenerContainerFactory.setMessageConverter(jacksonJmsMessageConverter);
        return jmsListenerContainerFactory;
    }
}
