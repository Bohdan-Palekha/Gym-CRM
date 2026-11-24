package com.gymcrm.client;

import com.gymcrm.client.event.TrainingAddedEvent;
import com.gymcrm.client.event.TrainingsRemovedEvent;
import com.gymcrm.model.Training;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

//@Async/@TransactionalEventListener only take effect through spring proxying
//since AFTER_COMMIT and off-thread is standard spring behaviour this test checks only listener's logic
@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerWorkloadEventListener")
class TrainerWorkloadEventListenerTest {

    @Mock
    private TrainerWorkloadNotifier trainerWorkloadNotifier;

    private TrainerWorkloadEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new TrainerWorkloadEventListener(trainerWorkloadNotifier);
    }

    @Test
    @DisplayName("onTrainingAdded forwards the event's training to the notifier")
    void onTrainingAdded_forwardsToNotifier() {
        Training training = new Training();
        training.setId(1L);

        listener.onTrainingAdded(new TrainingAddedEvent(this, training));

        verify(trainerWorkloadNotifier).trainingAdded(training);
        verifyNoMoreInteractions(trainerWorkloadNotifier);
    }

    @Test
    @DisplayName("onTrainingsRemoved forwards every training in the batch to the notifier")
    void onTrainingsRemoved_forwardsEachTraining() {
        Training t1 = new Training();
        t1.setId(1L);
        Training t2 = new Training();
        t2.setId(2L);

        listener.onTrainingsRemoved(new TrainingsRemovedEvent(this, List.of(t1, t2)));

        verify(trainerWorkloadNotifier).trainingDeleted(t1);
        verify(trainerWorkloadNotifier).trainingDeleted(t2);
        verifyNoMoreInteractions(trainerWorkloadNotifier);
    }
}
