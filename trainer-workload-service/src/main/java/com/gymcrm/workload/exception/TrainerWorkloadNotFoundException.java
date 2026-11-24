package com.gymcrm.workload.exception;

//when a GET references a trainer this service has never received
public class TrainerWorkloadNotFoundException extends RuntimeException {

    public TrainerWorkloadNotFoundException(String message) {
        super(message);
    }
}
