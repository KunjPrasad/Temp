package com.example.demo.spring.boot.ctrl;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.spring.boot.dto.DualMessageDTO;

// @formatter:off
/*
 * === CONFIG RELATED COMMENTS ===
 * 
 * **IMPORTANT** See comments in JmsConfiguration.java
 * 
 * IMPORTANT: Docs for Basic JMS and Spring-JMS understanding:
 * --|---- **VERY IMPORTANT** JMS doc from Java EE to understand various terms involved: https://docs.oracle.com/javaee/5/tutorial/doc/bnceh.html
 * --|---- Why need XA transaction in JMS: https://www.atomikos.com/Blog/ReliableJMSWithTransactions
 * --|----|---- Once again note that essentially a "JMS-driven JPA action" involves 2 simulatenous transaction, so it brings up the need for XA
 * .. transaction. Same criteria (of having multiple-JPA-transactions simultaneously commit) is what also invoked XA in JPA
 * --|----|---- **IMPORTANT**: The article shows use of XA when message is being consumed. Note that same may also apply when message is being 
 * .. created, in that user supplied input needs to be modified with some entry from DB before putting on queue. One simple reason might simply
 * .. be to use a DB sequence to get the id for message before putting it in queue -- why the need for ID in message (see next comment on how
 * .. this can help prevent XA transaction during message consumption)
 * --|---- Why not use XA transaction in JMS and possible workthrough; Note that the solution relies on there being some primary key associated to 
 * .. message: http://activemq.apache.org/should-i-use-xa.html
 * --|---- Spring docs on JMS: https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#jms
 * --|---- **IMPORTANT**: A simple, yet descriptive example of SpringBoot JMS: https://spring.io/guides/gs/messaging-jms/
 * 
 * --|---- **VERY IMPORTANT**: Note that jms senders and consumers are made from session. Thus a question of session-caching comes, i.e.
 * .. whether a session should be reused/cached to make new producer/consumer. Also note that a JMS session construction also requires knowing 
 * .. the ack-mode and whether it is transacted (See JmsTemplate notes.. in that if used in method/class with @Transactional annotation, it 
 * .. always makes transactional session). Now, refer to https://stackoverflow.com/questions/21984319/why-defaultmessagelistenercontainer-should-not-use-cachingconnectionfactory
 * .. The point is : if you are in a JMS design where the destination-queue (from where message pulled) can change, then don't cache
 * .. consumer-session (there are properties in CachingConnectionfactory to disable this).. which is understandable - because you want to 
 * .. cache to get performance; And not to keep bunch of objects that'll stay in cache and not get used (latter happens if destination changes 
 * .. since there is no guarantee of its use). However, the worst part, as said in post, is that cached consumers will still receive message
 * .. but will not respond to it for long! This doesn't happen for producers. 
 * --|----|---- Either-way, if you are instead in using fixed number of consumer destinations, then this issue may not happen!! 
 * --|----|---- Another bottom line, prefer caching in listenerContainer or listenerContainerFactory rather than at connection level. 
 * --|----|---- Also realize that just like EntityManager in JPA, the producer/consumer should not be thread-shared. So, when using transactions
 * .. maybe, it is preferable to not cache sessions!! Performance goes down, but ease goes up
 * === [END] CONFIG RELATED COMMENTS ===
 * 
 *
 *
 * SENDING MESSAGE:
 * --|---- One option is to use JmsTemplate's convertAndSend -- as shown below. Note that it takes a destination-to-send-to (..if not given, it 
 * .. uses the default destination; The connection factory is set in JmsTemplate definition itself), an object to send (..using converter registered
 * .. with JmsTemplate), and a post-converter callback!! Note that the converter-object is set in JmsTemplate. It can be thought of like objectMapper()
 * .. is converter for JSON request - a generic one and widely applicable! The message-post-processor gives customization specific to each case!
 * --|----|---- Spring doc says "To accommodate the setting of a message’s properties, headers, and body that can not be generically 
 * .. encapsulated inside a converter class, the MessagePostProcessor interface gives you access to the message after it has been 
 * .. converted, but before it is sent. "
 * --|---- Notice that a JMS message has multiple components, see https://docs.oracle.com/javaee/5/tutorial/doc/bnceh.html#bnces
 * .. It has specific headers (just like http request has particular standard headers only), and can have properties which can be arbitrary and 
 * .. user controller (like configurable headers of http), and then the actual message
 * --|----|---- **IMPORTANT**: Meaning of different headers: https://www.ibm.com/support/knowledgecenter/en/SSCQGF_7.2.0.3/com.ibm.IBMDI.doc_7.2.0.3/rg_conn_jms_headersandproperties.html
 * --|----|---- **IMPORTANT**: The code below uses post-process to set custom header on message. Another possibility could be to use "MessageBuilder".
 * .. See https://stackoverflow.com/questions/42367309/spring-jmstemplate-add-custom-property/42368435#42368435
 * 
 *
 *
 * RECEIVING MESSAGE (with Listener):
 * --|---- **VERY VERY IMPORTANT**: Understand the concept for listener. In JMS design-spec: https://docs.oracle.com/javaee/5/tutorial/doc/bnceh.html
 * .. (1) it is a messageConsumer that pulls message; 
 * .. (2) This message pull is done "synchronously"; 
 * .. (3) The messageConsumer is made out of session (and so is limited to JMS session scope); 
 * .. (4) If listeners are made, they are registered with messageConsumer -- consumers listen sync-ly, but after they receive message, they 
 * .. pass it to listen to process it async-ly
 * --|----|---- BRINGING IT TO SPRING: 
 * .. (i) MessageConsumer seems like a boiler-plate code if all logic can be put in listeners. So one can make listener, and a listener-container, 
 * .. which is a JMS-consumer. Spring handles running listener-container (i.e. JMS consumer) in some thread without blocking main application, 
 * .. and if a message is received, then the corresponding listener is run - in that thread. Per docs "A message listener container is used to 
 * .. receive messages from a JMS message queue and drive the MessageListener that is injected into it ...takes care of registering to receive 
 * .. messages, participating in transactions, resource acquisition and release, exception conversion and suchlike."
 * .. (ii) **VERY VERY IMPORTANT**: For message driven POJO (jms-listener), be aware that the pojo will receive message on multiple threads, 
 * .. so it is important to ensure that the implementation is thread-safe.
 * .. (iii) With (i) and (ii), it mean that if there are many JmsListeners, we need multiple ListenerContainer ...and if all these listeners
 * .. are being exposed via @JmsListener annotation, then we are required to provide just one central way to provide listenerContainer for 
 * .. all of them. And thus, we get "listenerContainerFactory" that needs to be provided if we want to use @JmsListener. 
 * .......... (iii#a) NOTE the Spring comment: "The annotated endpoint infrastructure creates a message listener container behind the scenes 
 *                    for each annotated method, using a JmsListenerContainerFactory. Such a container is not registered against the application 
 *                    context but can be easily located for management purposes using the JmsListenerEndpointRegistry bean." ...As mentioned
 *                    above, this is probably because Spring runs the listener-containers in own separate thread and not through appContext
 * .......... (iii#b) NOTE: In this code, we did not go through listenerContainer .. just bypassed to use listenerContainerFactory and @JmsListener
 * .. (iv) Spring doc also mentions MessageDelegate interface and MessageListenerAdapter.. but frankly, if using just one messageConverter
 * .. or at least using similar converter to send and receive.. then it might be unnecessary to get in these. 
 * 
 * 
 *
 * **IMPORTANT** MORE ON @JmsListener:
 * --|---- In @JmsListener annotation.. One option to control listener target is with JMS selectors, see https://docs.oracle.com/javaee/5/tutorial/doc/bnceh.html#bncer
 * .. and https://docs.oracle.com/javaee/5/tutorial/doc/bncgw.html
 * .. **IMPORTANT** For working example, where there are different listener for same queue because of selector, and also to see unit test,
 * .. see https://stackoverflow.com/questions/40932367/how-to-implement-jms-queue-in-spring-boot   (..not sure through why need ActiveMQ's BrokerService)
 * --|---- See https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#jms-annotated-method-signature
 * --|----|---- Note the various parameters than can be injected in method annotated with @JmsListener. 
 * --|----|---- validation can be done using @Valid (Not sure though if needed.. since in use case, Jms-message will be system generated itself!)
 * --|----|---- A message contains various information in addition to just body. With org.springframework.messaging.Message<> abstraction, one
 * .. can directly get message, while also have handle to other information 
 * --|----|---- The docs also mention of DefaultMessageHandlerMethodFactory (also look at the javadoc), but not sure if it'll be needed in a 
 * .. system where JMS message origin and consumer are in same codebase. Why need validtion/conversion in such case
 * --|---- Next section shows how @SendTo("") can be used to chain consumer response to another JMS destination.. also destination can be dynamic
 *
 */
// @formatter:on

@Transactional(transactionManager = "jmsTransactionManager")
@RestController
public class JmsController {

    @Autowired
    private JmsTemplate jmsTemplate;

    @RequestMapping(value = "/jms1/{msg1}/{msg2}", method = RequestMethod.GET)
    public void getJms1TestMessage(@ModelAttribute DualMessageDTO dualMsg) {
        System.out.println("JMS#1: Sending message");
        jmsTemplate.convertAndSend("mailbox1", dualMsg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws JMSException {
                message.setIntProperty("ID", 1234);
                return message;
            }
        });
        System.out.println("JMS#1: Exiting after sending message");
    }

    // JMS#1 consumer
    @JmsListener(destination = "mailbox1", containerFactory = "jmsListenerContainerFactory")
    public void receiveMailbox1Message(DualMessageDTO messageDTO) {
        System.out.println("MAILBOX#1 - Received: " + messageDTO.getMsg1() + "," + messageDTO.getMsg2());
    }

    @RequestMapping(value = "/jms2/{msg1}/{msg2}", method = RequestMethod.GET)
    public void getJms2TestMessage(@ModelAttribute DualMessageDTO dualMsg) throws InterruptedException {
        System.out.println("JMS#2: Sending message");
        Thread.sleep(1000);
        jmsTemplate.convertAndSend("mailbox2", dualMsg);
        System.out.println("JMS#2: Exiting after sending message");
    }

    // JMS#2 consumer
    @JmsListener(destination = "mailbox2", containerFactory = "jmsListenerContainerFactory")
    public void receiveMailbox2Message(DualMessageDTO messageDTO) throws InterruptedException {
        System.out.println("MAILBOX#2 - Received: " + messageDTO.getMsg1() + "," + messageDTO.getMsg2());
        Thread.sleep(1000);
        System.out.println("MAILBOX#2 - End of sleep, returning");
    }

    @RequestMapping(value = "/jms3/{msg1}/{msg2}", method = RequestMethod.GET)
    public void getJms3TestMessage(@ModelAttribute DualMessageDTO dualMsg) throws InterruptedException {
        System.out.println("JMS#3: Sending mailbox-1 message");
        jmsTemplate.convertAndSend("mailbox1", dualMsg);
        Thread.sleep(1000);
        System.out.println("JMS#3: Sending mailbox-2 message");
        jmsTemplate.convertAndSend("mailbox2", dualMsg);
        System.out.println("JMS#3: Erroring...");
        throw new RuntimeException("jms3 - excp at end");
    }
}
