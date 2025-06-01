package com.pps.bs.consumer;

import java.util.List;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.pps.bs.service.BSService;
import com.pps.common.model.PaymentMessage;
import com.pps.common.model.PaymentProcessingEvent;

import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import lombok.extern.slf4j.Slf4j;

/**
 * A JMS Consumer to receive payment for BS processing.
 * 
 * @author shashank
 *
 */
@Component
@Slf4j
public class JmsBSConsumer implements MessageListener {

	@Autowired
	private BSService bsService;
	
	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	@JmsListener(destination = "${active-mq.bs-event-topic}")
	public void onMessage(Message message) {
		try {
			log.info("Received Message to BS->: " + message.toString());

			ActiveMQTextMessage objectMessage = (ActiveMQTextMessage) message;
			String paymentProcessingEventStr = objectMessage.getText();

			PaymentProcessingEvent paymentProcessingEvent = bsService.covertJsonToPaymentProcessingEventPojo(paymentProcessingEventStr);

			if("PROCESSING_STARTED".equalsIgnoreCase(paymentProcessingEvent.getEventType())) {

				PaymentMessage paymentMessage = new PaymentMessage(
						paymentProcessingEvent.getPaymentCanonical().getPaymentInfo().getTransactionId().toString(), 
						paymentProcessingEvent.getPaymentCanonical(), "SENT");
				mongoTemplate.save(paymentMessage);
			} else if ("PROCESSING_COMPLITED".equalsIgnoreCase(paymentProcessingEvent.getEventType())) {
				BasicQuery basicQuery = new BasicQuery("{transactionId : \"" + paymentProcessingEvent.getPaymentCanonical().getPaymentInfo().getTransactionId() + "\" }");
				List<PaymentMessage> paymentMessageList = mongoTemplate.find(basicQuery, PaymentMessage.class);

				if (!paymentMessageList.isEmpty()) {
					PaymentMessage paymentMessage = paymentMessageList.get(0);
					paymentMessage.setPaymentCanonical(paymentProcessingEvent.getPaymentCanonical());
					paymentMessage.setStatus("COMPLITED");
					mongoTemplate.replace(basicQuery, paymentMessage);
				}
			}
		} catch (Exception e) {
			log.error("Received Exception : " + e);
		}
	}
}