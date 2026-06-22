package com.strandls.taxonomy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.strandls.taxonomy.RabbitMqConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyEventProducer {

	private static final Logger logger = LoggerFactory.getLogger(TaxonomyEventProducer.class);

	private final Channel channel;
	private final ObjectMapper objectMapper;

	@Inject
	public TaxonomyEventProducer(Channel channel, ObjectMapper objectMapper) {
		this.channel = channel;
		this.objectMapper = objectMapper;
	}

	public void sendTaxonomyUpdate(Object taxonomyObject, Boolean both, Boolean doc) {
		try {
			String message = objectMapper.writeValueAsString(taxonomyObject);

			// Always publish to taxonomy/observation
			channel.basicPublish(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.TAXONOMY_EVENT_ROUTING_KEY, 
			        MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));

			// Only publish to species when both=true
			if (Boolean.TRUE.equals(both)) {
			    channel.basicPublish(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.SPECIES_EVENT_ROUTING_KEY,
			            MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));
			}
			
			if (Boolean.TRUE.equals(doc)) {
				channel.basicPublish(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.DOCSCI_ROUTING_KEY,
			            MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));
			}

		} catch (Exception e) {
			logger.error("Failed to publish taxonomy event: {}", e.getMessage());
		}
	}
}