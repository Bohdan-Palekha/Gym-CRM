package com.gymcrm.workload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

//spring managed ActiveMQ wiring for the consumer DefaultJmsListenerContainerFactory +JmsTemplate + @JmsListener
// no jakarta.jms primitives
@Slf4j
@Configuration
@EnableJms
public class JmsConfig {

    //must be identical to gym-crm's JmsConfig.PAYLOAD_TYPE_ID
    //string travels on the wire, per jacksonJmsMessageConverter
    static final String PAYLOAD_TYPE_ID = "TrainerWorkloadRequest";

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory(
            @Value("${spring.activemq.broker-url}") String brokerUrl,
            @Value("${spring.activemq.user:}") String user,
            @Value("${spring.activemq.password:}") String password,
            @Value("${trainer-workload.jms.redelivery.max-redeliveries:3}") int maxRedeliveries,
            @Value("${trainer-workload.jms.redelivery.initial-delay-ms:1000}") long initialDelayMs,
            @Value("${trainer-workload.jms.redelivery.max-delay-ms:30000}") long maxDelayMs,
            @Value("${trainer-workload.jms.redelivery.backoff-multiplier:2.0}") double backoffMultiplier) {

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        if (StringUtils.hasText(user)) {
            factory.setUserName(user);
            factory.setPassword(password);
        }

        //broker redelivery for failures like (an unexpected exception from the listener)
        //exhausted retries goes to ActiveMQ.DLQ
        //no broker XML
        //not used for validation/auth failures(those routed to out dead letter queue)
        //queue by TrainerWorkloadUpdateListener, invalid message cannot ever succeed
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(maxRedeliveries);
        redeliveryPolicy.setInitialRedeliveryDelay(initialDelayMs);
        redeliveryPolicy.setMaximumRedeliveryDelay(maxDelayMs);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setBackOffMultiplier(backoffMultiplier);
        factory.setRedeliveryPolicy(redeliveryPolicy);
        return factory;
    }

    //used by JmsTemplate to forward invalid messages to the dead letter queue).
    //not used for the listener container because several concurrent consumers need independent session, thus shared cached session would bottleneck them
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

    //horizontal scaling of consumers
    //concurrency is set per listener (rainerWorkloadUpdateListener @JmsListener(concurrency=)
    //tuned per environment via application properties.
    //running several instances of this service against the same queue name adds more consumers
    //ActiveMQ load-balances deliveries across all of them
    //throughput scales by adding instances and changing this setting.
    @Bean
    public DefaultJmsListenerContainerFactory trainerWorkloadJmsListenerContainerFactory(
            ActiveMQConnectionFactory activeMQConnectionFactory,
            MappingJackson2MessageConverter jacksonJmsMessageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        factory.setMessageConverter(jacksonJmsMessageConverter);
        //listener returns normally = commit/ack;
        //listener throws = rollback
        //redelivery as configured
        factory.setSessionTransacted(true);
        factory.setErrorHandler(loggingErrorHandler());
        return factory;
    }

    //catches failures the listener cannot like message conversion errors on bad payload which never reach try/catch
    private ErrorHandler loggingErrorHandler() {
        return throwable -> log.error(
                "Unhandled error in the trainer-workload-updates JMS listener container "
                        + "(message will be redelivered per the configured RedeliveryPolicy): {}",
                throwable.getMessage(), throwable);
    }
}

