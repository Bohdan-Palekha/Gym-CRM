package com.gymcrm.service;

import com.gymcrm.client.event.TrainingsRemovedEvent;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.util.PasswordGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TraineeService {

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private TrainingDao trainingDao;
    private PasswordGenerator passwordGenerator;
    private UsernameService usernameService;
    private GymMetrics gymMetrics;
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setTraineeDao(TraineeDao d) {
        this.traineeDao = d;
    }

    @Autowired
    public void setTrainerDao(TrainerDao d) {
        this.trainerDao = d;
    }

    @Autowired
    public void setTrainingDao(TrainingDao d) {
        this.trainingDao = d;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator g) {
        this.passwordGenerator = g;
    }

    @Autowired
    public void setUsernameService(UsernameService s) {
        this.usernameService = s;
    }

    @Autowired
    public void setGymMetrics(GymMetrics m) {
        this.gymMetrics = m;
    }

    //microservices task 4
    //TrainingRemovedEvent is picked up (AFTER_COMMIT, async) by client.TrainerWorkloadEventListener
    //thus cascade delete training are reported to workload service.
    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    //task2
    //hash is persisted
    //plaintext is in rawPassword field of returned entity for the response
    @Transactional
    public Trainee createTrainee(String firstName, String lastName,
                                 LocalDate dateOfBirth, String address) {
        usernameService.validateName(firstName, "firstName");
        usernameService.validateName(lastName, "lastName");

        String rawPassword = passwordGenerator.generate();

        Trainee trainee = new Trainee();
        trainee.setFirstName(firstName);
        trainee.setLastName(lastName);
        trainee.setUsername(usernameService.reserveUsername(firstName, lastName));
        trainee.setPassword(passwordEncoder.encode(rawPassword));
        trainee.setActive(true);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);

        Trainee saved = traineeDao.save(trainee);
        saved.setRawPassword(rawPassword);
        log.info("Created trainee username={}", saved.getUsername());
        gymMetrics.incrementTraineeRegistrations();
        return saved;
    }

    //task6
    @Transactional(readOnly = true)
    public Optional<Trainee> selectTraineeByUsername(String username) {
        return traineeDao.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Trainee> selectTrainee(Long id) {
        return traineeDao.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Trainee> selectAllTrainees() {
        return traineeDao.findAll();
    }

    //REST task 5 same lookup as selectTraineeByUsername, but also eager initialization
    //for lazy trainers collection while session is open,
    //thus controller can safely read it after this transactional method has returned
    //(collection is in memory, not not-yet-loaded proxy)
    @Transactional(readOnly = true)
    public Optional<Trainee> selectTraineeProfileByUsername(String username) {
        Optional<Trainee> found = traineeDao.findByUsername(username);
        found.ifPresent(t -> Hibernate.initialize(t.getTrainers()));
        return found;
    }

    //task10
    @Transactional
    public Trainee updateTrainee(Trainee trainee) {
        traineeDao.findById(trainee.getId()).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot update — trainee not found: id=" + trainee.getId()));
        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainee id={}", updated.getId());
        return updated;
    }

    //collection completely untouched, and dirty checking persists just the changed columns.
    //to prevent data wipe of assigned trainers when profile is updated, we mutate profile
    //instead of creating transient and merging
    @Transactional
    public Trainee updateTraineeProfile(String username, String firstName, String lastName,
                                         LocalDate dateOfBirth, String address, boolean isActive) {
        usernameService.validateName(firstName, "firstName");
        usernameService.validateName(lastName, "lastName");
        //fetch the existing entity directly from the Database
        Trainee t = traineeDao.findByUsername(username).orElseThrow(() ->
                new EntityNotFoundException("Trainee not found: " + username));
        //only change this fields
        t.setFirstName(firstName);
        t.setLastName(lastName);
        t.setDateOfBirth(dateOfBirth);
        t.setAddress(address);
        t.setActive(isActive);
        Trainee updated = traineeDao.update(t);
        Hibernate.initialize(updated.getTrainers());
        log.info("Updated trainee profile username={}", username);
        return updated;
    }

    //task11
    @Transactional
    public Trainee activateDeactivate(String username, boolean isActive) {
        Trainee t = traineeDao.findByUsername(username).orElseThrow(() ->
                new EntityNotFoundException("Trainee not found: " + username));
        UserActivationRule.apply(t, isActive);
        Trainee updated = traineeDao.update(t);
        log.info("Trainee username={} isActive set to {}", username, isActive);
        return updated;
    }

    //task12
    //all training are collected for workload service before deletion
    @Transactional
    public void deleteTrainee(Long id) {
        List<Training> trainings = trainingDao.findByTraineeId(id);
        traineeDao.deleteById(id);
        if (!trainings.isEmpty()) {
            eventPublisher.publishEvent(new TrainingsRemovedEvent(this, trainings));
        }
        log.info("Deleted trainee id={} (cascade: {} training(s) removed)", id, trainings.size());
    }

    @Transactional
    public void deleteTraineeByUsername(String username) {
        List<Training> trainings = trainingDao.findByTraineeCriteria(username, null, null, null, null);
        traineeDao.deleteByUsername(username);
        if (!trainings.isEmpty()) {
            eventPublisher.publishEvent(new TrainingsRemovedEvent(this, trainings));
        }
        log.info("Deleted trainee username={} (cascade: {} training(s) removed)", username, trainings.size());
    }

    //task14
    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username,
                                              LocalDate from, LocalDate to,
                                              String trainerName,
                                              TrainingType.TrainingTypeName trainingType) {
        return trainingDao.findByTraineeCriteria(username, from, to, trainerName, trainingType);
    }

    //task15

    @Transactional(readOnly = true)
    public List<Trainer> getNotAssignedActiveTrainers(String traineeUsername) {
        return trainerDao.findActiveNotAssignedToTrainee(traineeUsername);
    }

    //task18

    @Transactional
    public Trainee updateTraineeTrainers(String traineeUsername, List<String> trainerUsernames) {
        List<Trainer> trainers = trainerUsernames.stream()
                .map(u -> trainerDao.findByUsername(u).orElseThrow(() ->
                        new EntityNotFoundException("Trainer not found: username=" + u)))
                .toList();
        Trainee updated = traineeDao.updateTrainers(traineeUsername, trainers);
        log.info("Updated trainer list for trainee username={}: {} trainer(s)",
                traineeUsername, trainers.size());
        return updated;
    }
}
