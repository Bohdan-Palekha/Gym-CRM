package com.gymcrm.client.event;

import com.gymcrm.model.Training;
import org.springframework.context.ApplicationEvent;

import java.util.List;

//published after a trainee delete cascades
//list is captured before the deletion
public class TrainingsRemovedEvent extends ApplicationEvent {

    private final List<Training> trainings;

    public TrainingsRemovedEvent(Object source, List<Training> trainings) {
        super(source);
        this.trainings = trainings;
    }

    public List<Training> getTrainings() {
        return trainings;
    }
}
