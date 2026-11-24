package com.gymcrm.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
//separete from workload service
public record TrainerWorkloadRequest(

        String trainerUsername,

        String trainerFirstName,

        String trainerLastName,

        @JsonProperty("isActive")
        boolean isActive,

        LocalDate trainingDate,

        int trainingDuration,

        ActionType actionType
) {
}
