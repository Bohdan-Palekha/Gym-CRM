package com.gymcrm.workload.messaging;

import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.logging.TransactionLoggingFilter;
import com.gymcrm.workload.security.ServiceTokenValidator;
import com.gymcrm.workload.service.TrainerWorkloadService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//authenticates sender, validates payload, then applies the change
//messaged that fail check are not redelivered, moved to dead letter queue
//failure like runtime exception out of the service layer is rethrown
//so container's transacted session rolls back and  redelivery retries it before, it ends up dead-lettered
@Slf4j
@Component
public class TrainerWorkloadUpdateListener {

    //must match gym-crm's ServiceAuthTokenMessagePostProcessor.SERVICE_AUTH_TOKEN_PROPERTY
    static final String SERVICE_AUTH_TOKEN_PROPERTY = "serviceAuthToken";
    static final String DLQ_REASON_PROPERTY = "dlqReason";
    static final String DLQ_ORIGINAL_DESTINATION_PROPERTY = "originalDestination";
    static final String DLQ_FAILED_AT_PROPERTY = "failedAt";

    private final TrainerWorkloadService trainerWorkloadService;
    private final ServiceTokenValidator tokenValidator;
    private final Validator validator;
    private final JmsTemplate jmsTemplate;
    private final String sourceDestination;
    private final String deadLetterDestination;

    public TrainerWorkloadUpdateListener(
            TrainerWorkloadService trainerWorkloadService,
            ServiceTokenValidator tokenValidator,
            Validator validator,
            JmsTemplate jmsTemplate,
            @Value("${trainer-workload.jms.destination.workload-updates:trainer-workload.updates}")
            String sourceDestination,
            @Value("${trainer-workload.jms.destination.workload-updates-dlq:trainer-workload.updates.dlq}")
            String deadLetterDestination) {
        this.trainerWorkloadService = trainerWorkloadService;
        this.tokenValidator = tokenValidator;
        this.validator = validator;
        this.jmsTemplate = jmsTemplate;
        this.sourceDestination = sourceDestination;
        this.deadLetterDestination = deadLetterDestination;
    }

    @JmsListener(
            destination = "${trainer-workload.jms.destination.workload-updates:trainer-workload.updates}",
            containerFactory = "trainerWorkloadJmsListenerContainerFactory",
            concurrency = "${trainer-workload.jms.listener.concurrency:1-3}")
    public void onWorkloadUpdate(
            @Payload TrainerWorkloadRequest request,
            @Header(name = SERVICE_AUTH_TOKEN_PROPERTY, required = false) String serviceAuthToken,
            @Header(name = TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY, required = false) String transactionId) {

        boolean mdcOwnedHere = transactionId != null && !transactionId.isBlank();
        if (mdcOwnedHere) {
            MDC.put(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY, transactionId);
        }
        try {
            if (request == null) {
                log.warn("Rejected workload update: empty message body, dropping (nothing to dead-letter)");
                return;
            }

            Optional<String> callerServiceId = tokenValidator.validate(serviceAuthToken);
            if (callerServiceId.isEmpty()) {
                log.warn("Rejected workload update for trainer={}: missing or invalid service auth token",
                        request.trainerUsername());
                deadLetter(request, serviceAuthToken, transactionId, "Missing or invalid service authentication token");
                return;
            }

            Set<ConstraintViolation<TrainerWorkloadRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String reasons = violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.joining("; "));
                log.warn("Rejected invalid workload update for trainer={}: {}", request.trainerUsername(), reasons);
                deadLetter(request, serviceAuthToken, transactionId, "Validation failed: " + reasons);
                return;
            }

            applyChange(request);

        } finally {
            if (mdcOwnedHere) {
                MDC.remove(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY);
            }
        }
    }

    private void applyChange(TrainerWorkloadRequest request) {
        try {
            trainerWorkloadService.applyWorkloadChange(request);
        } catch (RuntimeException ex) {
            log.error("Unexpected failure applying workload change for trainer={} action={}: {}",
                    request.trainerUsername(), request.actionType(), ex.getMessage(), ex);
            //rethrow rolls back the transacted, redelivery
            //message can succes
            throw ex;
        }
        log.info("Applied workload change: trainer={} action={} trainingDate={} durationMinutes={}",
                request.trainerUsername(), request.actionType(), request.trainingDate(),
                request.trainingDuration());
    }

    private void deadLetter(TrainerWorkloadRequest request, String serviceAuthToken, String transactionId,
                             String reason) {
        jmsTemplate.convertAndSend(deadLetterDestination, request, message -> {
            message.setStringProperty(DLQ_REASON_PROPERTY, reason);
            message.setStringProperty(DLQ_ORIGINAL_DESTINATION_PROPERTY, sourceDestination);
            message.setStringProperty(DLQ_FAILED_AT_PROPERTY, Instant.now().toString());
            //trainer-workload-service's dead letter queue, not a public/inter-service
            if (serviceAuthToken != null) {
                message.setStringProperty(SERVICE_AUTH_TOKEN_PROPERTY, serviceAuthToken);
            }
            if (transactionId != null) {
                message.setStringProperty(TransactionLoggingFilter.TRANSACTION_ID_MDC_KEY, transactionId);
            }
            return message;
        });
    }
}
