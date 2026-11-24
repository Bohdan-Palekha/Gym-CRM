package com.gymcrm.client;

import com.gymcrm.client.config.AsyncConfig;
import com.gymcrm.client.event.TrainingAddedEvent;
import com.gymcrm.client.event.TrainingsRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

//TrainingService/TraineeService's events to TrainerWorkloadNotifier
//workload service os only told about add/delete once transaction is commited AFTER_COMMIT
//@Async so notifications never adds latency to the request that triggered it, runs after response is out
//if no transaction is active during event publishing, spring doesn't invoke this listener
@Component
@RequiredArgsConstructor
public class TrainerWorkloadEventListener {

    private final TrainerWorkloadNotifier trainerWorkloadNotifier;

    @Async(AsyncConfig.WORKLOAD_NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrainingAdded(TrainingAddedEvent event) {
        trainerWorkloadNotifier.trainingAdded(event.getTraining());
    }

    @Async(AsyncConfig.WORKLOAD_NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrainingsRemoved(TrainingsRemovedEvent event) {
        event.getTrainings().forEach(trainerWorkloadNotifier::trainingDeleted);
    }
}
