package com.gymcrm.workload.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//demo class for DLQ demo
//ActiveMQ web console's send and ActiveMQ's rest message api have no way to attach a serviceAuthToken or payloadType to a message
//so they cannot reach TrainerWorkloadUpdateListener validation/auth check, message sent is missing payloadType and fails conversion
//has a ROLE_SERVICE requirement
@Slf4j
@RestController
@RequestMapping("/api/trainer-workloads/_demo")
@Profile({"local", "dev"})
public class DemoMessagePublisherController {

    //must match TrainerWorkloadUpdateListener's SERVICE_AUTH_TOKEN_PROPERTY and JmsConfig's PAYLOAD_TYPE_ID
    private static final String SERVICE_AUTH_TOKEN_PROPERTY = "serviceAuthToken";
    private static final String PAYLOAD_TYPE_PROPERTY = "payloadType";
    private static final String PAYLOAD_TYPE_ID = "TrainerWorkloadRequest";

    private final JmsTemplate jmsTemplate;
    private final String destination;

    public DemoMessagePublisherController(
            JmsTemplate jmsTemplate,
            @Value("${trainer-workload.jms.destination.workload-updates:trainer-workload.updates}")
            String destination) {
        this.jmsTemplate = jmsTemplate;
        this.destination = destination;
    }

    @PostMapping(value = "/publish-raw-message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> publishRawMessage(
            @RequestBody JsonNode body,
            @RequestHeader(name = "X-Simulated-Auth-Token", required = false) String simulatedAuthToken) {
        log.info("Demo: publishing a hand-crafted message to '{}' (simulated auth token present: {})",
                destination, simulatedAuthToken != null);
        jmsTemplate.convertAndSend(destination, body, enrich(simulatedAuthToken));
        return ResponseEntity.accepted().build();
    }

    private org.springframework.jms.core.MessagePostProcessor enrich(String simulatedAuthToken) {
        return (Message message) -> {
            setPayloadType(message);
            setSimulatedAuthToken(message, simulatedAuthToken);
            return message;
        };
    }

    private void setPayloadType(Message message) throws JMSException {
        //forced to the correct value regardless of type of the request body deserialized to
        //JsonNode isn't a TrainerWorkloadRequest, converter's automatic type-id stamping gets this wrong
        message.setStringProperty(PAYLOAD_TYPE_PROPERTY, PAYLOAD_TYPE_ID);
    }

    private void setSimulatedAuthToken(Message message, String simulatedAuthToken) throws JMSException {
        if (simulatedAuthToken != null) {
            message.setStringProperty(SERVICE_AUTH_TOKEN_PROPERTY, simulatedAuthToken);
        }
    }
}