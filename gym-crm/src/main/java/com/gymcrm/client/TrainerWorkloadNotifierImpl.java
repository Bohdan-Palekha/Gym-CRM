package com.gymcrm.client;

import com.gymcrm.client.dto.ActionType;
import com.gymcrm.client.dto.TrainerWorkloadRequest;
import com.gymcrm.client.security.InterServiceTokenProvider;
import com.gymcrm.client.security.ServiceAuthTokenMessagePostProcessor;
import com.gymcrm.client.security.TransactionIdMessagePostProcessor;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Service;

//notifies trainer-workload-service over ActiveMQ
//once per training add/delete destination is a queue (point to point)
//if several trainer-workload-service instances are running, each message lands on one of them
//ActiveMQ load-balances across all consumers per task
//messages have a new service JWT and the caller's transactionId as JMS properties
//send failures are caught and logged, never thrown into the caller
//listener already runs AFTER_COMMIT on a background executor, no request and no transaction left to fail
@Slf4j
@Service
public class TrainerWorkloadNotifierImpl implements TrainerWorkloadNotifier {

    private final JmsTemplate jmsTemplate;
    private final String destination;
    private final ServiceAuthTokenMessagePostProcessor authTokenPostProcessor;
    private final TransactionIdMessagePostProcessor transactionIdPostProcessor;

    public TrainerWorkloadNotifierImpl(
            JmsTemplate jmsTemplate,
            InterServiceTokenProvider tokenProvider,
            @Value("${trainer-workload.jms.destination.workload-updates:trainer-workload.updates}")
            String destination) {
        this.jmsTemplate = jmsTemplate;
        this.destination = destination;
        this.authTokenPostProcessor = new ServiceAuthTokenMessagePostProcessor(tokenProvider);
        this.transactionIdPostProcessor = new TransactionIdMessagePostProcessor();
    }

    @Override
    public void trainingAdded(Training training) {
        send(toRequest(training, ActionType.ADD));
    }

    @Override
    public void trainingDeleted(Training training) {
        send(toRequest(training, ActionType.DELETE));
    }

    private void send(TrainerWorkloadRequest request) {
        try {
            jmsTemplate.convertAndSend(destination, request, enrich());
        } catch (JmsException ex) {
            //on broker unreachable/send failure workload service falls behind, thus reconciliation required if it persists
            log.warn("Could not publish workload update to '{}' - trainer={} action={} not recorded there yet: {}",
                    destination, request.trainerUsername(), request.actionType(), ex.toString());
        }
    }

    private MessagePostProcessor enrich() {
        return message -> transactionIdPostProcessor.postProcessMessage(
                authTokenPostProcessor.postProcessMessage(message));
    }

    private TrainerWorkloadRequest toRequest(Training training, ActionType actionType) {
        Trainer trainer = training.getTrainer();
        return new TrainerWorkloadRequest(
                trainer.getUsername(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.isActive(),
                training.getTrainingDate(),
                training.getDurationMinutes(),
                actionType);
    }
}
