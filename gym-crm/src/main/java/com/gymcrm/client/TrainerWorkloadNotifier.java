package com.gymcrm.client;

import com.gymcrm.model.Training;

//port toward the trainer-workload-service per task.
//TrainingService/TraineeService depend only on this, are testable and mockable
public interface TrainerWorkloadNotifier {

    void trainingAdded(Training training);

    void trainingDeleted(Training training);
}
