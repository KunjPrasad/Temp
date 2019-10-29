package com.example.demo.spring.boot.config.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.FixedBackOff;

import com.example.demo.spring.boot.util.ApplicationConstants;

// @formatter:off
/*
 * **IMPORTANT**: A simple, yet descriptive example of SpringBoot JMS: https://spring.io/guides/gs/messaging-jms/
 * --|---- Note that the example says to use @EnableJms. This does not seem necessary when only 1 connection-factory is present, even when 
 * .. multiple queues/topics are associated to it.  MAYBE, this is because Spring-boot saw jms in dependency and added it - even when making
 * .. new SpringBoot with only JMS (using Spring Initializr), it does not put @EnableJms in code
 * --|---- Note that this note shows use of many default provisions.. the code done here started with it, and then slowly started to define 
 * .. each bean explicitly
 * 
 * NOTE: Particularly for JBoss, JMS started as HornetMQ, but is recently integrated with ActiveMQ to form new project "Artemis". The point is,
 * .. DO NOT refer to old documentation on how to configure JBoss JMS since it's now useless
 * --|---- Best to configure jms in JBoss, see: https://stackoverflow.com/questions/40015829/how-to-configure-jms-in-jboss-eap-7
 * --|---- Then, see https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.0/html-single/configuring_messaging/ 
 * --|---- Particularly for Artemis, see https://activemq.apache.org/artemis/docs/1.0.0/using-jms.html
 * --|---- When in doubt, this is new schema.. so use it to generate sample xml to understand how to configure the standalone.xml's messaging, 
 * .. urn:jboss:domain:messaging-activemq:2.0 , say, suing https://xsd2xml.com/   For schema, see https://github.com/wildfly/wildfly/blob/master/messaging-activemq/src/main/resources/schema/wildfly-messaging-activemq_3_0.xsd
 * 
 * 
 * Configuring ConnectionFactory (and related)
 * --|---- Creating in-memory JMS: For embedded and test profile, it is needed to create in-memory JMS. The way JMS works is through a port on 
 * .. a server. For in-vm JMS, there is a special string to use. See docs: http://activemq.apache.org/jndi-support.html. In these cases, if you
 * .. just do "@Autowired ConnectionFactor cf", then spring will automatically add an implementation. Also see https://activemq.apache.org/how-do-i-embed-a-broker-inside-a-connection.html
 * --|---- See here for 3 different activeMq connectionFactory implementation.. one if for XA. See https://activemq.apache.org/maven/apidocs/org/apache/activemq/spring/ActiveMQConnectionFactory.html
 * --|---- **VERY VERY IMPORTANT** For the conectionFactory received from standalone.xml, it shows:
 * --|----|---- className=>org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory
 * --|----|---- [Jndi-object].toString()=> ActiveMQConnectionFactory [serverLocator=ServerLocatorImpl [initialConnectors=[TransportConfiguration(name=in-vm, factory=org-apache-activemq-artemis-core-remoting-impl-invm-InVMConnectorFactory) ?bufferPooling=false&serverId=0], discoveryGroupConfiguration=null], clientID=null, consumerWindowSize = 1048576, dupsOKBatchSize=1048576, transactionBatchSize=1048576, readOnly=false]
 * --|----|---- **ALSO NOTE** The Jboss does not quite start till all "jms-destinations" used in the code are also configured
 * --|---- NOTE: It is not necessary to return an instance of Spring's SingleConnectionFactory or CachingConnectionFactory!! The native implementation
 * .. of ConnectionFactory can be returned too!! Do note that this introduces somewhat of a dependency in code on the broker being used for 
 * .. JMS implementation. It was also there in Datasource.. just that Hibernate abstracted it off!!
 * --|---- NOTE: redelivery of failed message is set at ConnectionFactory level, not at JmsTemplate; See https://activemq.apache.org/redelivery-policy.html
 * .. Particularly note that there is something called "Dead Letter Queue" (DQL)!! ..to get failed messages. ActiveMQQueue() object seems to 
 * .. have a property that can make it a DLQ. BUT, ideally this is configured at broker level (not ConnectionFactory, but actual JMS broker. As
 * .. corollary, think of setting in physical database, just like roles are set in database; and not like a DB Connection/Datasource object, 
 * .. which is just a representation of link between Java application and actual database). See https://activemq.apache.org/message-redelivery-and-dlq-handling.html
 * --|---- Note that ActiveMQ connection factory can also be made be various other properties.. see its javadoc where constructor take properties 
 * --|---- ****In prod system, consider setting different timeout settings also!!
 * 
 * 
 * 
 **VERY VERY VERY VERY IMPORTANT**: Understanding acks and session (in jmstemplate and listener); and its relation to retryPolicy (in connectionFactory)
 * --|---- As written in "https://docs.oracle.com/cd/E19587-01/821-0029/aeqbk/index.html" -- an ack is exchanged between "CONSUMER" and broker,
 * not between producer and broker
 * --|----|---- In case of producer, the method .send() blocks and returns only after message is added to broker; And also persisted in broker, 
 * .. if configured to do so. What if application fails after that.. that's why JMS should be sent with id
 * --|----|---- In case of consumer, the consumer has to take the message and do procesing with it. NOTE the difference with producer, in that
 * .. send() message generally happens at end of producer, but receive() happens at beginning of consumer. So consumer has high chance to fail,
 * .. in which case we want the persisted jms message to not be deleted. Thus, the need for ack-mode in consumer.
 * --|----|---- In this code, when tried by failing at @JmsListener, then note that producer does not fail. This is why the webservice invoking
 * .. JMS finishes much early with 200 status. HOWEVER, the pull from JMS into consumer fails, and this triggers the retry-policy set in
 * .. connectionFactor used in making JmsContainerListener!
 * --|----|---- Using @JmsListener in Spring, the ack is sent automatically at end of the method if it finishes normally
 * 
 * --|---- **VERY IMPORTANT**: ON OTHER HAND.. TRANSACTIONS (See https://docs.oracle.com/cd/E19587-01/821-0029/gdydy/index.html)
 * --|----|---- Just like JPA, the JMS-transactions enable combining multiple JMS operations as a single unit. THIS MEANS: various send() on 
 * .. producer are combined as single unit, and various receive() on consumer are combined as single unit. **IMPORTANT**: Note from the webpage
 * .. that "Because the scope of a transaction is limited to a single session, it is not possible to combine the production and consumption 
 * .. of a message into a single end-to-end transaction. That is, the delivery of a message from a message producer to a destination on the 
 * .. broker cannot be placed in the same transaction with its subsequent delivery from the destination to a consumer."
 * --|----|---- Sessions do NOT combine send() and receive() separated by persistence in a JMS destination in between. They only combine send()
 * and receive() in a single method call.
 * --|----|---- It is observed while coding that setting "jmsTemplate.setSessionTransacted(true);" wasn't quite necessary to get transaction
 * .. BUT it is needed to have @Transactional on class/method logic containing transaction unit, and it should use a JmsTransactionMananger.
 * --|----|---- I am ASSUMING.. that just like JPA, when a method is wrapped with @Transactional, then any JMS call made inside automatically 
 * .. ends up using the outer transaction. Else, it runs in its own little transaction when called. This is unless Propagation.REQUIRES_NEW is used
 * 
 * --|---- Continuing transactions.. **VERY VERY IMPORTANT**: Effect of @Transactional-jms on @JmsListeners! 
 * --|----|---- It is noticed that the default config of JmsListenerContainerListenerFactory is to set session-transacted as true and to use the 
 * .. platformTransactionmanager.. However, when using multiple jms transaction manager, it is good to specify it explicitly. 
 * .. HOWEVER, NOTE, DUE TO THIS CONFIG OPTION: even if @Transactional is not used, the methods with @JmsListener will be transactional in nature! 
 * --|----|---- Due to above, if JmsListenerContainerFactory is configured to not use transaction and a transactionManager is not configured
 * .. but @JmsListener is made in a class that has @Transactional on it.. then the listener will still not be transactional in nature
 * --|----|---- NOTE: @JmsListener can in itself use JmsTemplate and further send message. As said above, these will be wrapped in overall
 * .. transaction. EVEN MORE, since a failure in listener prevents sending ack, the listener will get retried - as configured by RetryPolicy
 * .. in connection. Thus, be very careful, a @JmsListener without transaction, and also involving jmsTemplate.send() can become infinite 
 * .. exponentially - sending itself a message and then failing and triggering retry
 * 
 * 
 * 
 * Configuring JmsTemplate
 * --|---- See Spring docs on jms: Note the line "The JmsTemplate class ..simplifies the use of JMS since it handles the creation and release 
 * .. of resources when sending or synchronously receiving messages." -- So, JmsTemplate does NOT play a role when async-ly processing message
 * .. via message-driven-pojo. That's job of jmsContainerListener!! So, in this context, JmsTemplate remains useful only for sending messages!
 * --|----|---- With this understanding, and with understanding from above that acks are important for recivers and not for sender, so it is 
 * .. probably useless to configure acks in JmsTemplate!
 * --|---- JmsTemplate.isExplicitQosEnabled -- note that default jms behavior should ideally be defined programmatically in JNDI. When you
 * .. want to overwrite it at app-level, then, in addition to setting those values, the property "isExplicitQosEnabled" needs to be enabled
 * --|---- From JmsTemplate apidocs: If you want to use dynamic destination creation, you must specify the type of JMS destination to create, 
 * .. using the "pubSubDomain" property. "For other operations, this is not necessary." Point-to-Point (Queues) is the default domain.
 * --|---- Configuring sessionAcknowledgeMode : for different ack modes, see https://docs.oracle.com/cd/E19587-01/821-0029/aeqbk/index.html 
 * .. Also see details above in connectionFactory regarding how exception works! **NOTE** it mentions of a NO-ACK mode which is in different package
 * --|---- **IMPORTANT**: Note the following lines.. probably this explain the ack/transaction behavior of Jmstemplate as mentioned above: 
 * .. "Reusing code across a managed and unmanaged transactional environment can be confusing when using the JMS API to create a Session 
 * .. from a Connection. This is because the JMS API has only one factory method to create a Session and it requires values for the transaction 
 * .. and acknowledgment modes. In a managed environment, setting these values is the responsibility of the environment’s transactional 
 * .. infrastructure, so these values are ignored by the vendor’s wrapper to the JMS Connection. ****When using the JmsTemplate in an unmanaged 
 * .. environment you can specify these values through the use of the properties sessionTransacted and sessionAcknowledgeMode. ****When using 
 * .. a PlatformTransactionManager with JmsTemplate, the template will always be given a transactional JMS Session."
 * 
 * Configuring JmsListenerContainerFactory
 * --|---- A backoff strategy can be made.. such that if listener is unable to pull message from MQ, it tries again. NOTE: This is different
 * .. from "reDelivery" which is set in ConnectionFactory, when delivery of message fails
 * --|---- NOTE: A generic errorhandler can be registered that can handle cases if an error gets thrown by listener. In absence of handler, 
 * .. exception is propagated. **VERY IMPORTANT**: See details in connectionFactory regarding how exception works!
 * --|---- NOTE: Javadoc says that if you want listener to auto-deserialize incoming message to an object, then it is necessary to set 
 * .. "setTypeIdPropertyName" in message converter associated to listenerContainerFactor!!
 * --|---- **VERY VERY IMPORTANT**: For DefaultJmsListenerContainerFactory.. see Javadocs, particularly setCacheLevel(), setConcurrencyLevel()
 * .. which itself falls back on same method definition of DefaultJmsListenerContainer.. specially changing setCacheLevel() when transactions
 * .. are enabled (Or maybe, you don't want caching-level when transactions are involved.. sure performance goes down, but ease goes up) 
 * --|---- **IMPORTANT**: Note that if needed, it is possible to even configure each listenerContainer separately and programmatically!
 * .. Spring says "JmsListenerEndpoint ..allows you to configure endpoints programmatically "IN ADDITION TO" the ones that are detected 
 * .. by the JmsListener annotation. [It is also possible to] ..skip the use of @JmsListener altogether and only register your endpoints 
 * .. programmatically through JmsListenerConfigurer." See https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#jms-annotated-programmatic-registration
 * --|---- See https://stackoverflow.com/questions/39420769/defaultjmslistenercontainerfactory-and-concurrent-connections-not-shutting-down
 * .. in that when setting concurrency level for listenerConstainer thread, note that if "maxMessagesPerTask " is set to -1, that means the
 * .. thread once created will not go down. So if you want threads to go down eventually when load is less (..and if it really is critical), set
 * .. "maxMessagesPerTask" to greater than 0. HOWEVER, do note that doing so means worker threads (..responding to JMS) will start having a 
 * .. lifespan, so it is not good to have this value be too small, else you'll just end up making and then destroying threads!
 * --|----|---- Similarly, there is "prefetch" option that should be set depending on expected workload (the prefetch concept also explain in 
 * .. StackOverflow link). By ActiveMQ docs, "Large prefetch values are recommended for high performance with high message volumes. However, 
 * .. for lower message volumes, where each message takes a long time to process, the prefetch should be set to 1."
 *
 *
 * 
 * **VERY VERY VERY IMPORTANT**: Validation of message
 * In the web-side, there are various ways to configure/call a validator! But not so much on the JMS side
 * Best look at example in api-page for @EnableJms (https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jms/annotation/EnableJms.html)
 * .. and this example code (https://memorynotfound.com/spring-jms-validate-messages-jsr-303-bean-validation/) 
 * .. and this StackOverflow thread (https://stackoverflow.com/questions/34763289/validating-jms-payload-in-spring)
 * .. for details on adding validator. NOTE:
 * ---- From api-page and example: there is just one place to add validator, and it allows for adding one validator. SO.. if you want to auto-enroll
 * .. multiple validator, make a custom validator class which is actually a chain of validators and set it here
 * ---- Only in StackOverflow page is it written that you can use SmartValidator with call to @Validated to invoke group-based validation. Not sure 
 * .. if the writer meant group-based validation after message is parsed (i.e. @Validated on method-arg); or @Validated on class and then on method
 * .. with @Valid on method-param
 * ---- NOTE that unlike web-request, there does not seem to be any static utilities to convert the headers. This could be useful in dynamic validation
 * .. of jms-messages, wherein, the custom-validator looks at header value to set constraints on field! HOWEVER, this could be done by making a custom
 * .. message converter, and adding a logic, say, if the targetClass extends "WithJmsHeader" class (..which has fields of type Map<>), then 
 * .. during serialization, reads the map and populate headers.. and during de-serialization, read the headers from message and write in the map. This way,
 * .. when the message goes next to validator, it will also have acess to headers which can be used to validate. 
 * ----|---- Should advanced configurations to set ThreadLocal be needed.. maybe you can even look into configuring the "DefaultMessageHandlerMethodFactory"
 * .. As said in docs for @EnableJms, it would also be needed to modify the "JmsListenerEndpointRegistry" to set up the thread-local fields at startup of
 * .. listener, and then remove them at end! ..or maybe modify the "JmsHandlerMethodFactory " to do so!
 *
 */
// @formatter:on

//@EnableJms
@Configuration
public class JmsConfiguration {

    @Value("${user1.jms.jndi-name:#{null}}")
    private String jmsUser1JndiName;

    @Value("${jms.url:#{null}}")
    private String jmsUser1Url;

    @Value("${jms.provider-class-name:#{null}}")
    private String jmsUser1ProviderClassNm;

    @Value("${user1.jms.name:#{null}}")
    private String jmsUser1Name;

    @Value("${user1.jms.password:#{null}}")
    private String jmsUser1Password;

    // Transaction properties
    @Value("${spring.jms.transaction.timeout.seconds:30}")
    private int transactionTimeout;

    @Autowired
    private DefaultJmsListenerContainerFactoryConfigurer configurer;

    // This bean defines the connectionFactory
    @Bean
    ConnectionFactory jmsConnectionFactory() {
        if (StringUtils.trimToNull(jmsUser1JndiName) != null) {
            try {
                Object obj = new JndiTemplate().lookup(jmsUser1JndiName);
                return (ConnectionFactory) obj;
            } catch (NamingException e) {
                throw new RuntimeException("User-1 Jms-connection not configured in JNDI", e);
            }
        }
        if (StringUtils.trimToNull(jmsUser1Url) != null && StringUtils.trimToNull(jmsUser1Url) != null
                && StringUtils.trimToNull(jmsUser1Url) != null) {
            if (ApplicationConstants.ACTIVE_MQ_JMS_PROVIDER_NM.equalsIgnoreCase(jmsUser1ProviderClassNm)) {
                ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
                activeMQConnectionFactory.setBrokerURL(jmsUser1Url);
                activeMQConnectionFactory.setUserName(jmsUser1Name);
                activeMQConnectionFactory.setPassword(jmsUser1Password);
                activeMQConnectionFactory.setAlwaysSyncSend(true);

                RedeliveryPolicy mailbox1QueuePolicy = new RedeliveryPolicy();
                mailbox1QueuePolicy.setInitialRedeliveryDelay(0);
                mailbox1QueuePolicy.setRedeliveryDelay(1000);
                mailbox1QueuePolicy.setUseExponentialBackOff(false);
                mailbox1QueuePolicy.setMaximumRedeliveries(1);

                RedeliveryPolicy mailbox2QueuePolicy = new RedeliveryPolicy();
                mailbox2QueuePolicy.setInitialRedeliveryDelay(0);
                mailbox2QueuePolicy.setRedeliveryDelay(1000);
                mailbox2QueuePolicy.setUseExponentialBackOff(false);
                mailbox2QueuePolicy.setMaximumRedeliveries(1);

                // Receive a message with the JMS API
                RedeliveryPolicyMap map = activeMQConnectionFactory.getRedeliveryPolicyMap();
                map.put(new ActiveMQQueue("mailbox1"), mailbox1QueuePolicy);
                map.put(new ActiveMQQueue("mailbox2"), mailbox2QueuePolicy);

                // this line is not quite necessary.. just showing its use
                ConnectionFactory connectionFactory = new SingleConnectionFactory(activeMQConnectionFactory);
                return connectionFactory;
            }
        }
        throw new RuntimeException("User-1 Jms-connection not configured");
    }

    // This bean is the jmsTemplate
    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();

        jmsTemplate.setConnectionFactory(jmsConnectionFactory());
        // jmsTemplate.setConnectionFactory(new CachingConnectionFactory(jmsConnectionFactory()));

        jmsTemplate.setMessageConverter(jacksonJmsMessageConverter());

        // Configuring ack in JmsTemplate is probably useless - see comments above
        // jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        // jmsTemplate.setSessionTransacted(true);

        jmsTemplate.afterPropertiesSet();
        return jmsTemplate;
    }

    // This bean is to serialize message content to json-based-TextMessage
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        // this is important.. to tell a textMessage is being made
        converter.setTargetType(MessageType.TEXT);
        converter.setEncoding(ApplicationConstants.UTF8_CHARSET);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    // This bean is listener-containerFactory to register different listeners
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // This provides all boot's default to this factory, including the message converter
        configurer.configure(factory, jmsConnectionFactory());

        // You could still override some of Boot's default if necessary.
        factory.setMessageConverter(jacksonJmsMessageConverter());

        // setting backoff strategy on failure to pull message
        factory.setBackOff(new FixedBackOff());

        // setting ack
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

        // setting transaction properties
        factory.setSessionTransacted(true);
        factory.setTransactionManager(jmsTransactionManager());

        // caching/concurrency behavior
        factory.setConcurrency("1-5");
        factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);

        // return
        return factory;
    }

    // The transaction manager for JMS
    @Bean
    public PlatformTransactionManager jmsTransactionManager() {
        JmsTransactionManager transactionManager = new JmsTransactionManager();
        transactionManager.setConnectionFactory(jmsConnectionFactory());
        transactionManager.setDefaultTimeout(1); // transactionTimeout);
        return transactionManager;
    }
}
