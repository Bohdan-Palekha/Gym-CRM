package com.gymcrm.service;

import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.exception.EntityNotFoundException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TrainerService {

    private TrainerDao trainerDao;
    private TrainingDao trainingDao;
    private TrainingTypeDao trainingTypeDao;
    private PasswordGenerator passwordGenerator;
    private UsernameService usernameService;
    private GymMetrics gymMetrics;

    @Autowired
    public void setTrainerDao(TrainerDao d) {
        this.trainerDao = d;
    }

    @Autowired
    public void setTrainingDao(TrainingDao d) {
        this.trainingDao = d;
    }

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao d) {
        this.trainingTypeDao = d;
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

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    //task1
    @Transactional
    public Trainer createTrainer(String firstName, String lastName,
                                 TrainingType.TrainingTypeName specialization) {
        usernameService.validateName(firstName, "firstName");
        usernameService.validateName(lastName, "lastName");

        if (specialization == null) {
            throw new IllegalArgumentException("specialization must not be null");
        }

        TrainingType type = trainingTypeDao.findByName(specialization).orElseThrow(() ->
                new EntityNotFoundException("TrainingType not found: " + specialization));

        String rawPassword = passwordGenerator.generate();

        Trainer trainer = new Trainer();
        trainer.setFirstName(firstName);
        trainer.setLastName(lastName);
        trainer.setUsername(usernameService.reserveUsername(firstName, lastName));
        trainer.setPassword(passwordEncoder.encode(rawPassword));
        trainer.setActive(true);
        trainer.setSpecialization(type);

        Trainer saved = trainerDao.save(trainer);
        saved.setRawPassword(rawPassword);
        log.info("Created trainer username={}", saved.getUsername());
        gymMetrics.incrementTrainerRegistrations();
        return saved;
    }

    //task5

    @Transactional(readOnly = true)
    public Optional<Trainer> selectTrainerByUsername(String username) {
        return trainerDao.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Trainer> selectTrainer(Long id) {
        return trainerDao.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Trainer> selectAllTrainers() {
        return trainerDao.findAll();
    }

    //task8
    //same idea as in trainee
    @Transactional(readOnly = true)
    public Optional<Trainer> selectTrainerProfileByUsername(String username) {
        Optional<Trainer> found = trainerDao.findByUsername(username);
        found.ifPresent(t -> Hibernate.initialize(t.getTrainees()));
        return found;
    }

    //task9
    @Transactional
    public Trainer updateTrainer(Trainer trainer) {
        trainerDao.findById(trainer.getId()).orElseThrow(() ->
                new EntityNotFoundException(
                        "Cannot update — trainer not found: id=" + trainer.getId()));
        Trainer updated = trainerDao.update(trainer);
        log.info("Updated trainer id={}", updated.getId());
        return updated;
    }

    //task9
    //same idea as in trainee service updateProfile
    @Transactional
    public Trainer updateTrainerProfile(String username, String firstName, String lastName,
                                         boolean isActive) {
        usernameService.validateName(firstName, "firstName");
        usernameService.validateName(lastName, "lastName");
        Trainer t = trainerDao.findByUsername(username).orElseThrow(() ->
                new EntityNotFoundException("Trainer not found: " + username));
        t.setFirstName(firstName);
        t.setLastName(lastName);
        t.setActive(isActive);
        Trainer updated = trainerDao.update(t);
        Hibernate.initialize(updated.getTrainees());
        log.info("Updated trainer profile username={}", username);
        return updated;
    }

    //task10

    @Transactional
    public Trainer activateDeactivate(String username, boolean isActive) {
        Trainer t = trainerDao.findByUsername(username).orElseThrow(() ->
                new EntityNotFoundException("Trainer not found: " + username));
        UserActivationRule.apply(t, isActive);
        Trainer updated = trainerDao.update(t);
        log.info("Trainer username={} isActive set to {}", username, isActive);
        return updated;
    }

    //task15

    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username,
                                              LocalDate from, LocalDate to,
                                              String traineeName) {
        return trainingDao.findByTrainerCriteria(username, from, to, traineeName);
    }
}
