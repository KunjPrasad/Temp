package com.example.demo.spring.boot.ctrl;

/*
*
* **VER VERY VERY IMPORTANT**: See Pg.150 of Spring-Security-docs. The idea is that you need to take care of all security in websocket! So, if trying to use it,
* .. then do add proper security
* -- As said in Spring-Mvc docs, one can use ChannelInterceptor for security
*
*
*
 * **VERY VERY IMPORTANT**: Realize the difference between websocket and STOMP.. https://stackoverflow.com/questions/40988030/what-is-the-difference-between-websocket-and-stomp-protocols
 * .. The first answer says that websocket is like TCP and STOMP is like HTTP. But, it's slightly different! WebSocket is underlying protocol 
 * .. for full-multiplex communication between server and client. STOMP is a particular example of protocol for clients and servers to 
 * .. communicate with messages. So, Websockets sort of lays road, but STOMP are traffic signs and rules. Someone may have as well used 
 * .. different rule - but STOMP is one such rule. Now, note that STOMP is rule for message based communication - so if it were a different 
 * .. messaging-queue, STOMP protocol could also apply there.. as in if it is dirt road instead of paved one, the same traffic rules could still 
 * .. apply. In fact, if you look at Active-MQ, they have their own AMQP (Active-MQ-Protocol).. but can also support STOMP!
 * .. Just to add, SockJS is totally different. It is a library that provides helpful utility in working with websockets. So, you can try working 
 * .. with websockets.. using SockJS library, and send messages over websockets using STOMP protocol
 * 
 * 
 * 
 * **VERY VERY VERY IMPORTANT**: NOTE that websocket is not the only optimal way for long term communication, see https://spring.io/blog/2012/05/08/spring-mvc-3-2-preview-techniques-for-real-time-updates/ 
 * Particularly note: 
 * .. (i) HTTP/2 has "Push" events that can allow servers to send multiple files etc in single request! 
 * .. (ii) There are "Server Sent Events" which can be used if we want instant update from server. It might be necessary to have heartbeats to keep connections open.. or to re-establish it if connection gets broken. Server-sent events can also do with custom made streaming response in servlet api; See https://golb.hplar.ch/2017/03/Server-Sent-Events-with-Spring.html    https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events
 * .. (iii) There are "Long Pollings", ..but generally Long Polling is not good because long running threads can occupy memory and server's max thread count - thereby doing a Denial attack! Even if not so, long polling is not a very good model when server is updating very fast
 * .. (iv) Particularly regarding Websocket and STOMP, note that the connection can get broken by service carrier, so whether application uses long polling or websocket.. it should be robust against such breaks. With websocket this is the case.. but with Long Polling, this needs to be taken care of (See https://stackoverflow.com/questions/25930285/spring-websocket-timeout-settings)
 *
 *
 *
 *
 * **NOTE**: The comments below are helpful in further trying to understand about Websockets. However, these were made at a later date.
 * .. so, not sure what would be the correct place to put it. So it is being written separately here.
 *
 * (1) Note the difference between Pg160 and Pg166 of Spring-mvc doc. 
 * --|---- (a) When using Websocket without STOMP (can use sockJS), then: (1.a.1) need to add @EnableWebSocket to a configuration file, 
 * .. (1.a.2) have it implement WebSocketConfigurer, and (1.a.3) override method registerWebSocketHandlers() [[seen in example below. 
 * .. Note that Spring-docs suggest overriding registerStompEndpoints.. not sure if that is correct since Stomp is not being used]] 
 * --|---- (b) On other hand, when using STOMP, then (1.b.1) need to add @EnableWebSocketMessageBroker, (1.b.2) have it implement
 * .. WebSocketMessageBrokerConfigurer, and (1.a.3) override 2 methods: registerStompEndpoints() through which initial handshake is done,
 * .. and also override configureMessageBroker() -- because STOMP works on messages,  so need architecture to control messages. Why still 
 * .. should one want to use STOMP - see benefits of doing so in section 4.4.2 on Pg166
 *
 * (2) When working with STOMP over websockets, it is said above that configureMessageBroker() needs to be overwritten, which configures 
 * .. MessageBrokerRegistry. There are a few methods that should be understood.. and best to look at the javadoc:
 * --|---- (2.a) setApplicationDestinationPrefixes(String... prefixes) vs setUserDestinationPrefix(String destinationPrefix): The point is that the message-brokers
 * .. and application is getting message, and it needs to identify which messages should be handled by application vs which ones are for users
 * --|---- (2.b) setApplicationDestinationPrefixes(String... prefixes) 
 * --|----|---- Configure one or more prefixes to filter destinations targeting application annotated methods. For example destinations prefixed 
 * .. with "/app" may be processed by annotated methods while other destinations may target the message broker (e.g. "/topic", "/queue"). 
 * .. **NOTE**: When messages are processed, the matching prefix is removed from the destination in order to form the lookup path. This means  
 * .. annotations should not contain the destination prefix. Prefixes that do not have a trailing slash will have one automatically appended.
 * --|---- (2.c) setUserDestinationPrefix(String destinationPrefix)
 * --|----|---- Configure the prefix used to identify user destinations. User destinations provide the ability for a user to subscribe to queue names 
 * .. unique to their session as well as for others to send messages to those unique, user-specific queues. For example when a user attempts to 
 * .. subscribe to "/user/queue/position-updates", the destination may be translated to "/queue/position-updatesi9oqdfzo" yielding a unique queue 
 * .. name that does not collide with any other user attempting to do the same. Subsequently when messages are sent to 
 * .. "/user/{username}/queue/position-updates", the destination is translated to "/queue/position-updatesi9oqdfzo".
 * .. **NOTE** : The default prefix used to identify such destinations is "/user/". [[That's why most examples use /user/ prefix for user-messages]]
 * .. (See section 4.4.13 on Pg182 of Spring-mvc-pdf)
 *
 * (3) When working with STOMP over websockets : one can use a local broker or use an external broker, like RabbitMQ. How to configure? Best.. see
 * .. comments on Pg.169 of pdf. This comes down to enableSimpleBroker() vs enableStompBrokerRelay(). Note that in either case, spring makes a
 * .. local "MessageBrokerRegistry" object. However, in latter case, when messages with suitable path-prefix comes to it, then it gets forwarded/pulled
 * .. from the external registry.
 * **VERY VERY IMPORTANT**: So why the need for the MessageBrokerRegistry at all when working with external configurations? This goes into how 
 * .. Spring/STOMP works -- See Pg.170 -- "When messages are received from a WebSocket connection, they’re decoded to STOMP frames, then turned
 * .. into a Spring Message representation, and sent to the 'clientInboundChannel' for further processing. [[As an example of this processing]] STOMP 
 * .. messages whose destination header starts with '/app' may be routed to @MessageMapping methods in annotated controllers, while '/topic' and 
 * .. '/queue' messages may be routed directly to the message broker. An annotated @Controller handling a STOMP message from a client may send a 
 *.. message to the message broker through the 'brokerChannel' [[which first goes to a handler in Spring that forwards to external broker]], and the 
 * .. broker will broadcast the message to matching subscribers through the 'clientOutboundChannel' [[Again, there is a handler which pulls message from 
 * .. external broker and returns to user]]. [[Once a broker is configured, messages can be put in it even via a HTTP call, and doesn't have to websocket 
 * .. initiated only -- See Section 4.4.6 on Pg.174; By autowiring SimpMessagingTemplate]]
 * **VERY VERY IMPORTANT** See this also broken step-by-step on Pg.171
 *
 * (4) **VERY VERY IMPORTANT**: One of the best ways to enforce CSRF like security in Websocket is add a channel-interceptor and add processing 
 * .. there. See Pg.182 of mvc-docs-pdf. Since all message goes through incoming channel before  forwarded to system or to Relay, so adding security 
 * .. here address all concerns. Also, as said, put it at higher order than Spring_Security-config. (Also shown in Pg186)
 *
 * (5) **VERY VERY IMPORTANT** Note on Pg.183 - regarding exceptionHandling, and @SendToUser in Websocket, "broadcast=false" option
 * --|---- Pg.182 of spring-mvc-docs pdf, it mentions of "UserDestinationMessageHandler" that does the job of translating "/user/.." endpoints to correct
 * .. endpoint on application's side. QUESTION: Where is the "UserDestinationMessageHandler" registered? Point#1 above mentions how configuring classes
 * .. extend WebSocketConfigurer or WebSocketMessageBrokerConfigurer. In either case, there is a class that contains default configurations which can be 
 * .. used unless overriden. "AbstractMessageBrokerConfiguration", by Javadoc, provides method to configure handling messages, and this is where 
 * .. "UserDestinationMessageHandler" comes in. "WebSocketMessageBrokerConfigurationSupport" specializes it for case of Websocket+STOMP; an extension
 * .. of which is "DelegatingWebSocketMessageBrokerConfiguration" -- which gets imported when using @EnableWebSocketMessageBroker
 *
 * 
 * 
 * Nice STOMP examples:
 * 
 * 1) https://www.devglan.com/spring-boot/spring-websocket-integration-example-without-stomp
 * Since websockets and STOMP are 2 different things, so it is possible to use WebSockets without STOMP. Since SockJS is a library for 
 * .. websocket, so cient side javascript coding - to use websocket, is done using SockJS. The codes involved are also discussed in 
 * .. Spring-Mvc-docs. Essentially, the idea is to associate a handler that handles the incoming request. 
 * .. ALSO NOTE: the example shows how to change from TextMessage to an object class using deserializations by self.. it starts by calling 
 * .. getPayload() method to get the text payload
 * 
 * **VERY VERY VERY IMPORTANT**: In this and other examples, the client side javascript uses "ws://" protocol. In productin case, DO NOT do 
 * .. that, instead use secure websocket, so "wss://" protocol, not just "ws://" protocol. See https://stackoverflow.com/questions/22758360/websockets-over-https-with-spring-boot-1-0-0-rc5-and-tomcat-8-0-3
 * .. Also, to configure additional security regarding websockets, see https://www.baeldung.com/spring-security-websockets    
 * .. https://docs.spring.io/spring-security/site/docs/current/reference/html/websocket.html
 * 
 * **VERY VERY VERY IMPORTANT**: Specific to code in example.. note that the example chosen by user shows how any message is sent to all 
 * .. subscribed sessions. In order to do so, it is needed to keep handle on all sessions up to the time ..and then when a message comes, 
 * .. the session-list is iterated over and value is sent. PARTICULARLY NOTE -- that this model can cause synchronization issues, where list 
 * .. is changed (to add new session) while it is being iterated over (to send a message). To not have synchronization overheads, the author 
 * .. uses "CopyOnWriteArrayList" -- This is a nice example of the use of this array!
 * 
 * **VERY VERY IMPORTANT**: Note the difference between Pg160 and Pg166 of Spring-mvc doc. When using Websocket without STOMP (can use sockJS), 
 * .. then just need to add @EnableWebSocket to a configuration file. But when using STOMP, then need to add @EnableWebSocketMessageBroker!!
 * 
 * 
 * 2) https://www.boraji.com/spring-mvc-5-handling-websocket-message-example
 * **VERY VERY IMPORTANT**: Start by looking at Pg149 of Spring mvc docs, last para of section 4.2.1, "WebSocketHandler".. where it says that 
 * .. the configuration should be used when registering websockets with spring mvc. NOW, see the example on this page - here for initializing 
 * .. the dispatcher servlet both config files are provided (for mvc, and for websocket). 
 * .. A related question is how would Spring Boot work? MAYBE.. no other changes are needed, and spring boot will automatically take care of 
 * .. things when it sees @EnableWebSocket annotation. BUT.. if not, then just see notes in EtceteraController (and/or notes below on spring 
 * .. boot servlet initialization), and it is possible to also configure dispatcher servlet accordingly!
 * 
 * 
 * 3) https://www.devglan.com/spring-boot/spring-boot-websocket-integration-example 
 * This gives an example where websocket is used along with STOMP protocol. While the main-code-example is better shown in other sites like, 
 * .. https://www.baeldung.com/websockets-spring    https://www.callicoder.com/spring-boot-websocket-chat-example/    https://stackoverflow.com/questions/37340271/how-to-send-message-to-user-when-he-connects-to-spring-websocket
 * .. this example shows a few more interesting objects that can be used in coding!
 * .. ALSO NOTE.. that as with Spring boot Json deserializer is registered by default. Not sure how to explicitly control it. Else other option 
 * .. could be to directly request for text payload without any deserialization (see below)
 * 
 * As also described in Spring docs (..and also in https://www.baeldung.com/spring-websockets-sendtouser ), there is concept of Interceptor 
 * .. that can be associated with Stomp-registration-endpoint, and interceptor does certain task. In this case, it retrieves the session-id and 
 * .. makes it available in attributes associated to STOMP-message (remember.. the interceptor is registered with Stomp-registry because it is 
 * .. invoked during initial handshake!). Spring docs list many other interceptors provided by spring
 * 
 * The ".withSockJs()" in STOMP registry shows that client is working with SockJS, so can downgrade if client browser does not have websockets
 * 
 * **IMPORTANT**: Also note.. difference between Stomp-Endpoint-Registry and Message-Broker-Config. Now, we are saying that we're using 
 * .. websocket with STOMP. So, client connects with STOMP endpoint only, and then keeps sending it messages (per STOMP protocol). The protocol 
 * .. defines how to further route the message to approrpiate location for processing - and that is how Message-Broker-Config's 
 * .. application-destination-prefix is matched with url associated with STOMP message to identify endpoint. (NOTE: This example is better done 
 * .. in baeldung url; In the code on devglan, the new /app/message url is not used when sending message from client to server, which is wrong!)
 * 
 * **IMPORTANT**: Note the "SimpMessageSendingOperations" autowired object.. which is websocket+STOMP equivalent of JMSTemplate!
 * 
 * 
 * 4) https://www.devglan.com/spring-boot/spring-session-stomp-websocket
 * Nice example going more detailed in STOMP + Websockets. Explicit use of Interceptor with STOMP is shown.
 * ALSO NOTE: It shows that if the @Payload annotation is used then the entire text body is provided.. this is useful when wanting to take 
 * .. message through custom deserialization
 * 
 * A new thing shown in this example is use of "Listeners" with websocket. Some "events" are defined in url#2 above, but here is an example 
 * .. showing on how to attach listeners to the event (which in this case are associated with Websocket related events). The listeners fire 
 * .. async-ly. Also note, use of "StompHeaderAccessor" in listener method definition to access headers. Compare it to the controller-method 
 * .. written below which accesses header from the message, and note that the object used is "SimpMessageHeaderAccessor", which is a parent of 
 * .. "StompHeaderAccessor". This is because message-processing is more generic.. but listeners are particularly using Stomp-Headers, so using 
 * .. StompHeaderAccessor there is better
 * 
 * **Just note.. frankly, I don't know if "SimpMessageHeaderAccessor" would work equally well in Listeners!! Maybe.. maybe not!
 */
public class WebsocketController {

}
