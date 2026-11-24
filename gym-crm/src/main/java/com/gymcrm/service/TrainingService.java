package com.gymcrm.service;

import com.gymcrm.client.event.TrainingAddedEvent;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

//cascade deletes handled by JPA
@Slf4j
@Service
public class TrainingService {


    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private TrainingTypeDao trainingTypeDao;
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setTrainingDao(TrainingDao d) {
        this.trainingDao = d;
    }

    @Autowired
    public void setTraineeDao(TraineeDao d) {
        this.traineeDao = d;
    }

    @Autowired
    public void setTrainerDao(TrainerDao d) {
        this.trainerDao = d;
    }

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao d) {
        this.trainingTypeDao = d;
    }

    //microservices task 4
    //TrainingAddedEvent is picked up (AFTER_COMMIT, async) by client.TrainerWorkloadEventListener for workload service
    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    //task16
    @Transactional
    public Training addTraining(String traineeUsername,
                                String trainerUsername,
                                String trainingName,
                                LocalDate trainingDate,
                                TrainingType.TrainingTypeName trainingTypeName,
                                int durationMinutes) {

        if (trainingName == null || trainingName.isBlank()) {
            throw new IllegalArgumentException("trainingName must not be blank");
        }
        if (trainingDate == null) {
            throw new IllegalArgumentException("trainingDate must not be null");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        if (trainingTypeName == null) {
            throw new IllegalArgumentException("trainingTypeName must not be null");
        }

        Trainee trainee = traineeDao.findByUsername(traineeUsername).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot create training — trainee not found: " + traineeUsername));
        Trainer trainer = trainerDao.findByUsername(trainerUsername).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot create training — trainer not found: " + trainerUsername));
        TrainingType type = trainingTypeDao.findByName(trainingTypeName).orElseThrow(() ->
                new EntityNotFoundException("TrainingType not found: " + trainingTypeName));

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(trainingName);
        training.setTrainingDate(trainingDate);
        training.setTrainingType(type);
        training.setDurationMinutes(durationMinutes);

        Training saved = trainingDao.save(training);
        eventPublisher.publishEvent(new TrainingAddedEvent(this, saved));
        log.info("Created training name={} trainee={} trainer={}",
                trainingName, traineeUsername, trainerUsername);
        return saved;
    }

    //REST task 14
    //no training type in request, trainers only train in their spec
    //type is derived from trainer, delegated to another addTraining
    @Timed(value = "gym.training.creation", description = "Time to create a training session via the REST API")
    @Transactional
    public Training addTraining(String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate,
                                int durationMinutes) {
        Trainer trainer = trainerDao.findByUsername(trainerUsername).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot create training — trainer not found: " + trainerUsername));
        return addTraining(traineeUsername, trainerUsername, trainingName, trainingDate,
                trainer.getSpecialization().getTrainingTypeName(), durationMinutes);
    }

    //OLD
    @Transactional
    public Training createTraining(Long traineeId, Long trainerId,
                                   String trainingName, TrainingType trainingType,
                                   LocalDate trainingDate, int durationMinutes) {

        Trainee trainee = traineeDao.findById(traineeId).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot create training — trainee not found: id=" + traineeId));
        Trainer trainer = trainerDao.findById(trainerId).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot create training — trainer not found: id=" + trainerId));

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(trainingName);
        training.setTrainingType(trainingType);
        training.setTrainingDate(trainingDate);
        training.setDurationMinutes(durationMinutes);

        Training saved = trainingDao.save(training);
        eventPublisher.publishEvent(new TrainingAddedEvent(this, saved));
        log.info("Created training name={} traineeId={} trainerId={}",
                trainingName, traineeId, trainerId);
        return saved;
    }

    //read

    @Transactional(readOnly = true)
    public Optional<Training> selectTraining(Long id) {
        return trainingDao.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Training> selectAllTrainings() {
        return trainingDao.findAll();
    }

    @Transactional(readOnly = true)
    public List<Training> selectTrainingsByTrainee(Long traineeId) {
        return trainingDao.findByTraineeId(traineeId);
    }

    @Transactional(readOnly = true)
    public List<Training> selectTrainingsByTrainer(Long trainerId) {
        return trainingDao.findByTrainerId(trainerId);
    }
}
