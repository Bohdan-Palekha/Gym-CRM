package com.gymcrm.workload.exception;

//exception for repeated conflicts for workload update persistence for the same trainer
//goes through TrainerWorkloadUpdateListener catch (RuntimeException) -> rethrow -> JMS redelivery path unchanged
public class TrainerWorkloadPersistenceException extends RuntimeException {

    public TrainerWorkloadPersistenceException(String message) {
        super(message);
    }
}
