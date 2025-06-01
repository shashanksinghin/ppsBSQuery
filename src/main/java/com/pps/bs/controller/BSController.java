package com.pps.bs.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.pps.bs.service.BSService;
import com.pps.common.model.PaymentCanonical;
import com.pps.common.model.PaymentMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * A rest controller to facilitate the unit testing.
 * @author shashank
 *
 */
@RestController
@Slf4j
public class BSController {
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private BSService bsService;
	
	@GetMapping("/paymentFCS/{transactionId}")
	public ResponseEntity<String> sendToPaymentForFCS(@PathVariable String transactionId) {
		log.info("Received Message to BS->: " + transactionId);

		BasicQuery basicQuery = new BasicQuery("{transactionId : \"" + transactionId + "\" }");
		List<PaymentMessage> paymentMessageList = mongoTemplate.find(basicQuery, PaymentMessage.class);

		PaymentCanonical paymentCanonical = null;

		if (!paymentMessageList.isEmpty()) {
			PaymentMessage paymentMessage = paymentMessageList.get(0);
			paymentCanonical = paymentMessage.getPaymentCanonical();
			String paymentCanonicalJson = bsService.covertPojToJson(paymentCanonical);
			return ResponseEntity.ok(paymentCanonicalJson);
		} else {
			return ResponseEntity.of(Optional.empty());
		}

	}	
}
