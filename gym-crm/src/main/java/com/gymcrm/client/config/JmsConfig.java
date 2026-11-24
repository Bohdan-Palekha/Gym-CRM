package com.gymcrm.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.client.dto.TrainerWorkloadRequest;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

//spring-managed ActiveMQ wiring for the outbound trainer-workload notification queue
//gym-crm only produces
//JmsTemplate is auto-configured by boot if a MessageConverter/ConnectionFactory bean is present;
//no direct contact with jakarta.jms primitives
@Configuration
public class JmsConfig {

    //must be identical to trainer workload service's JmsConfig.PAYLOAD_TYPE_ID:
    //string travels on the wire, per jacksonJmsMessageConverter
    static final String PAYLOAD_TYPE_ID = "TrainerWorkloadRequest";

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory(
            @Value("${spring.activemq.broker-url}") String brokerUrl,
            @Value("${spring.activemq.user:}") String user,
            @Value("${spring.activemq.password:}") String password) {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        if (StringUtils.hasText(user)) {
            factory.setUserName(user);
            factory.setPassword(password);
        }
        return factory;
    }

    //JmsTemplate sends 1 notification per training add/delete
    //caching keeps a pooled connection/session/ producer around(no fresh one per send)
    @Bean
    @Primary
    public CachingConnectionFactory jmsConnectionFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(10);
        return cachingConnectionFactory;
    }

    //plain JSON on the wire for serialization per task
    //gym-crm and trainer-workload-service have independent DTO class (different qualified names),
    //id that travels on the wire is a string ("TrainerWorkloadRequest"), never theirs class name
    //each side maps the string in their own class via setTypeIdMappings.
    //setTypeIdPropertyName needs a proper property name.
    //spring's converter uses it on incoming messages to look up the type id
    //even if receiving @JmsListener method's parameter type is already known
    //null doesn't disable lookup, thus lookup will throw (ActiveMQMessage rejects a null property name),
    //uses boot's shared ObjectMapper, LocalDate serialize like rest api
    @Bean
    public MappingJackson2MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("payloadType");
        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put(PAYLOAD_TYPE_ID, TrainerWorkloadRequest.class);
        converter.setTypeIdMappings(typeIdMappings);
        return converter;
    }
}

