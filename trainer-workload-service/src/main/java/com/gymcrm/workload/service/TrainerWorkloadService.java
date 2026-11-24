package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadResponse;

public interface TrainerWorkloadService {

    //apply one training event to the trainer's monthly summary
    void applyWorkloadChange(TrainerWorkloadRequest request);

    //total training minutes for one trainer, one month
    MonthlyTrainingHoursResponse getMonthlySummary(String username, int year, int month);

    //months history for one trainer
    TrainerWorkloadResponse getFullSummary(String username);
}
