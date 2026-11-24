package com.gymcrm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

//used by TrainerService. Allows service to delete data without knowing about training
@Getter
public class TraineeDeletedEvent extends ApplicationEvent {

    private final Long traineeId;

    public TraineeDeletedEvent(Object source, Long traineeId) {
        super(source);
        this.traineeId = traineeId;
    }

    @Override
    public String toString() {
        return "TraineeDeletedEvent{traineeId=" + traineeId + '}';
    }
}
