package com.gymcrm.client.event;

import com.gymcrm.model.Training;
import org.springframework.context.ApplicationEvent;

//published after a training is saved
//consumed by TrainerWorkloadEventListener, AFTER_COMMIT for workload service
public class TrainingAddedEvent extends ApplicationEvent {

    private final Training training;

    public TrainingAddedEvent(Object source, Training training) {
        super(source);
        this.training = training;
    }

    public Training getTraining() {
        return training;
    }
}
