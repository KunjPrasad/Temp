package com.example.demo.spring.boot.ctrl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.spring.boot.dto.DualMessageDTO;
import com.example.demo.spring.boot.entity.jta.ProductJta;
import com.example.demo.spring.boot.entity.jta.ProductJtaRepository;

//@formatter:off
/*
 * **VERY IMPORTANT**: Notice datasource definition in "JtaJpaUser1HelperConfiguration" -- particularly that init and destroy methods can be 
 * registered when making beans which in itself does not have @PostConstruct and @PreDestroy.
 *
 *
 *
 * **VERY VERY VERY IMPORTANT**: Why the need for JTA and XA-transactions? 
 * --|---- Ideally, using JTA is best and recommended way to move information between 2 separate XA-compatible datasources, like DB-to-DB, 
 * .. JMS-to-JMS, DN-to-JJMS! It is the last option of DM-to-JMS (and vice versa) that is most relevant in an Enterprise setting when different 
 * .. data needs to be handed-off from one domain to other as part of the workflow. See https://www.atomikos.com/Blog/ToXAOrNotToXA
 * --|---- HOWEVER, with the Microservice architecture in particular, it is necessary that each module be kept "fully" independent from other,
 * .. and communicating with each other only using REST-ish protocols. Allowing XA transactions require coupling of datasource on either side.
 * .. It may still be possible in most organizations - and may be the simple route moving forward. HOWEVER.. it is good to have a situation worked
 * .. out on how to proceed when it is not possible/allowed to have XA-transactions.
 * --|----|---- Consider case of e-commerce with "order" and "payment" microservice. Customer starts by creating order with item-list. Obtaining
 * .. cost of each item requires a sync-REST-call made from "order" to "inventory". If it fails, it means "inventory" is down, and customer is asked
 * .. to try later -- SO, no issues here! Now, consider when order is done and a data hand-off needs to be done from "order" to "payments". Following
 * .. can happen: 
 * --|----|----|---- (1) "order" tries to make call to "payment" to give it orderNumber and paymentDetails, but is unable to even get connection. 
 * .. This can be the case when "payment" is down even before "order" tried sending it the request. So, no data got created in "payments" DB. 
 * .. Either "order" retries then-and-there, or, tells customer (..on "order" side) that "Fees is down, but we have your order. We'll keep retrying
 * .. and will send you email on how to proceed when it is up". [[Digression: Actually, even when "payment" is up, the email could be sent to confirm]]
 * .. Also, this same applies when connection to "payment" is there, but no data could be persisted on "payment" side.
 * --|----|----|---- (2) MORE IMPORTANT AND ONE WITH ISSUES: "order" gets connection to "payment". "payment" is able to create an entry on its side - 
 * .. meaning, it has received an order request, and can process it if any customer comes and quotes order# on its website. It responds back to "order"
 * .. with success, after which, "order" needs to correct the status from "PAYMENT_CREATE_PENDING" to "PAYMENT_PENDING". And this fails! So, there is
 * .. a "payment" ready, but "order" doesn't know that. [[Digression: Had this been XA, "payment" would have also rolled back, so there wouldn't have
 * .. been an issue]]. So, order retries. 
 * --|----|----|----|---- **IMPORTANT**: This gives first requirement for handoff. We want the "payment" to make an entry in its database. This "act of creation"
 * .. should not have any race condition associated to it. An example when there can be a race condition is if the message to trigger the entry-creation is sent 
 * .. via Kafka queue, and on "payment" code, there are multiple consumers reading from that queue. Now, each can make a new entry. This would be wrong
 * .. design.  Alternately, note that such behavior won't be seen in a JMS-queue.
 * --|----|----|----|---- **IMPORTANT**: A second requirement for handoff is seen next - that when "payment" is asked to create an entry which already 
 * .. exists, then it should not throw error, but simply return 200! This prevents any unnecessary "side-effects" from "order" side retrying. 
 * --|----|----|----|---- **VERY VERY VERY IMPORTANT**:  In the step#2, it is said that some status needs to change from "PAYMENT_CREATE_PENDING" to 
 * .. "PAYMENT_PENDING", and also that "order" was trying to push data to "payment". This is motivated by your experience of the design in PatentCenter, but 
 * .. this does not make it correct! 
 * --|----|----|----|----|---- First-question: Why should "payment" get a handle to "order-id", a variable from "order" data-domain? If not, then how do they 
 * .. coordinate? Note that the underlying concept joining order and payment is the process of "ecommerce". I suggest making a new table, preferably in new 
 * .. schema and DB, and call it ecommerce. This is the representation of data that flows through multiple independent units/micro-services. Starting an "ecommerce"
 * .. should be seen as different from starting an "order". However, it is the start of "order" that also has the job to trigger start of "ecommerce" route. One way
 * .. could be that when order starts (i.e. POST /order), it starts with making an entry in e-commerce (POST /ecommerce) that returns an ecommerce-id. "Order"
 * .. also makes an "order-Id" which is its own primary-key and different from e-commerce; but it also stores ecommerce-id in its table - although that is never 
 * .. revealed to user ever. So, at that this point, an order with order-Id is made about which user knows, an ecommerce with ecommerce-id is made about which the
 * .. user does NOT know. [[DIGRESSION: As part of "POST /order" itself, it is suggested to make a Transactional-Outbox entry to update ecommerce with order-id 
 * .. and to set ecommerce status to ORDER_STARTED. Regarding why that is done is discussed later - at which point the relation of this step to a "Transactional 
 * .. outbox pattern" will become clear. One way to bypass it could be to change "POST /order" in having it return ecommerce-Id, order-Id and also set ecommerce
 * .. entry status to ORDER_STARTED, and have the "order" DB use the "order-id" as returned from "POST /order" call]]. Now, user can use order-id to perform 
 * .. various REST operations on order. REALIZE that in this whole process, the user does not, and never will, know of ecommerce-id. It is just something internal to 
 * .. business process. Also, throughout the step, "order" application just has handle on single-DB, the "order"-DB.
 * --|----|----|----|----|---- Second-question: Should it be the job of "order" to push data in payment? How is the process coordinated? Let's say user just finished
 * .. the order and now wants to pay. The last call on "order" would be something like "PATCH /order/{orderId}" -- close order for edit. Start payment--. First, this 
 * .. should set a flag in "order" that it is now closed to edits. Next, it should add a Transactional-Outbox entry to update ecommerce entry (with ecommerce-id)
 * .. to set its status as ORDER_DONE. Some async-processor in "order" will now pick this outbox entry and make call to ecommerce. However, that should fail if
 * .. the ecommerce-id is not found, or if the status isn't ORDER_STARTED [[This is where the transactional-outbox from previous step comes in. ALSO, both order
 * .. and ecommerce can also have background "cleaning" process to delete ecommerce entries if status are not set, implying data that is no longer in use]]. On same 
 *  .. lines, the call will be silently accepted if the ecommerce status is PAYMENT_STARTED or something more [[This ensures "clean retrials" if a previous call to 
 * .. workflow-manager from a component-system failed earlier, and so it is retrying now.  This also means that ecommerce-status is ordinal, not categorical]]. Now,
 * .. in "ecommerce", we have a background processor, which, when it sees an entry with status of "ORDER_ENDED", then it makes a REST call in "payment" to 
 * .. create a Transactional-Inbox entry that payment should be initiated for ecommerce entry of ecommerce-Id [[Alternative: Have the ecommerce-processor 
 * .. pass in both ecommerce-id and a payment-Id, and also set the ecommerce-status to PAYMENT_STARTED]]. **VERY VERY VERY IMPORTANT**: Do not pass
 * .. "order-id" to payment. The entire idea behind using the concept of "ecommerce-Id" is to clearly establish a parent-child relation between different stages and 
 * .. "workflow-manager". Passing "order-Id" to payment causes a leak of sibling information. One example where this could be bad is if we make payment-Id same
 * .. as order-Id, which will leak payment-details to user even before payment system could get ready and sync'd up with workflow manager to accept user payment.
 * .. BEST AVOID SUCH SITUATIONS! Now.. the payment-processor in "payment" can start processing the transactional-inbox, wherein, it makes a new payment DB
 * .. entry with payment-Id and same ecommerce-Id. It also contacts "order" with "ecommerce-Id" to get payment-details. Once done, it creates a transactional 
 * .. outbox entry to send email to user that it is ready for payment! LASTLY.. in order to make above as synchronous: best is to expose the async-processors as 
 * .. a webservice, maybe by taking the processing id as input. This way, "order" can do a sync invocation of its own processor (that changes ecommerce status 
 * .. to ORDER_ENDED); call ecommerce-processor by passing ecommerce-id (Since ecommerce is the workflow manager, so at a time it should only have 1 task 
 * .. for execution per ecommerce-id; and it can then return the processing-id and processing-name of system where it makes an entry); And then call the "payment" 
 * .. based on processing-id from previous step. This should send email to user, and maybe also return the url that can be used to make payment at that time. Hence, 
 * .. a sync-processing of an otherwise async pipeline is acheived. 
 * --|---- FEW MORE THINGS TO NOTE: 
 * --|----|---- To ensure that hiccups in achieving data-consistency during handoff - done as part of workflow - are soon identified and not left out
 * .. for long time-gap until customer starts grumbling, it is necessary to have log-analysis that can identify errors. Also, it is necessary to have
 * .. standard logging. **ALSO**: REQUIRE that each service/micro-service expose a "process-critical" async process also an an endpoint. This way, the services
 * .. can be called sync-ly as part of remediation without depoyment if the async processing keeps breaking for some reason. (As seen above, these endpoints are 
 * .. also needed to convert async to sync processing if dictated by UX)
 * --|----|---- **VERY VERY VERY IMPORTANT**: Realize that with  Spring, even these fringe cases could possibly be tested - if designed properly!!
 * --|----|---- **VERY VERY VERY IMPORTANT**: NOTE that all processes above are asynchronous and robust against failure, while still  not needing access by a
 * .. system to more than a SINGLE-DB at a time. Cross-system interaction is only via REST call, and is configured to be redone by a periodic processor, in case 
 * .. the previous call fails.
 * --|----|---- With this design there can never be a downstream process start unless all upstream data has come to consistent state -- this is enforced because of
 * ..  "workflow-manager" acting like a latch-pattern.
 *
 *
 *
 * **VERY VERY VERY IMPORTANT**: ALTERNATE IMPLEMENTATION FOR ABOVE: Microservice's Transactional-outbox pattern
 * See (https://microservices.io/patterns/data/application-events.html)
 * --|---- The idea is that when you want, say, writing simultaneously to DB and Message-Queue, then, you instead write to DB - and also write in a "Outbox" table
 * .. in same DB that is seen as a marker to make the message-queue call later on. At a later time, some other process can now read from "Outbox" table and 
 * .. make the entry.
 * --|---- HOWEVER, realize that above is not quite a JTA-replacement, but more like an alternate to above implementation. For example, the async process that reads
 * .. from Outbox-table and writes to Message-Queue can fail after writing to queue. What then? It will try doing so again! This bring us back to data-handoff situation
 * .. discussed above to ensure that operations are smooth. THE ACTUAL GOOD THING ABOUT THIS IMPLEMENTATION is that it decouples the various logic and also
 * .. forces asynchronization when dealing with "cross-service" operations which is a very good thing to do. Ideally, same should also be done on other system, 
 * .. via a "Transactional Inbox", enabling: (1) separating the processes of reading from Message-Queue vs execution of logic, (2)  Creates symmetry between 
 * .. systems, thereby enabling code reuse, (3) separating the processes means that one can easily be upgraded without affecting other -- IN FACT, the enterprise
 * .. may now take control of library responsible for sending-data-to / receiving-data-from queue, and thereby ensure standardization
 * --|---- **IMPORTANT** : This bring out one more point. The JMS/queue-system might be an enterprise wide infrastructure. In this case, giving the control of its
 * .. management to the enterise itself should be a logical design. With above, that is nicely possible!
 * --|---- USE CASES: Look at notes in "OtherDetailsController" -- a good use of Transactional Inbox/Outbox is when dealing with File updates/deletion. That note
 * .. points to another note #9.vi regarding files/multipart in ReqRespController. That note is important in that it shows how the Transactional Inbox/Outbox can
 * .. be streamed in certain cases!
 *
 */
//@formatter:on

@Transactional(transactionManager = "jtaPlatformTransactionManager")
@RestController
public class JtaController {

    @Autowired
    private JmsTemplate jtaJmsTemplate;

    @Autowired
    private ProductJtaRepository productJtaRepository;

    @RequestMapping(value = "/jta1/{msg1}/{msg2}", method = RequestMethod.GET)
    public void getJms2TestMessage(@ModelAttribute DualMessageDTO dualMsg) {
        System.out.println("JTA#1: Sending message");
        jtaJmsTemplate.convertAndSend("mailbox3", dualMsg);
        System.out.println("JTA#1: Making DB entry");
        ProductJta prod = new ProductJta();
        prod.setTitle(dualMsg.getMsg1() + "-" + dualMsg.getMsg2());
        prod = productJtaRepository.save(prod);
        System.out.println("JTA#1: Throwing exception");
        throw new RuntimeException("Inducing JTA fail");
    }

    // JTA#1 consumer
    @JmsListener(destination = "mailbox3", containerFactory = "jtaJmsUser1ListenerContainerFactory")
    public void receiveMailbox3Message(DualMessageDTO messageDTO) throws InterruptedException {
        Thread.sleep(1000);
        System.out.println("MAILBOX#3 - Received: " + messageDTO.getMsg1() + "," + messageDTO.getMsg2());
        System.out.println("Checking JPA Repo - printing all");
        productJtaRepository.findAll().forEach(s -> System.out.println(s.getId() + "," + s.getTitle()));
    }
}
