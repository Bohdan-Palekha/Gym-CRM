package com.gymcrm.facade;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.service.AuthenticationService;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

//delegation layer for bussiness logic in services
@Component
public class GymFacade implements GymCrmFacade {

    @Autowired
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final AuthenticationService authenticationService;

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService,
                     AuthenticationService authenticationService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
    }

    // trainee orig
    @Override
    public Trainee createTrainee(String firstName, String lastName,
                                 LocalDate dateOfBirth, String address) {
        return traineeService.createTrainee(firstName, lastName, dateOfBirth, address);
    }

    @Override
    public Trainee updateTrainee(Trainee trainee) {
        return traineeService.updateTrainee(trainee);
    }

    @Override
    public void deleteTrainee(Long id) {
        traineeService.deleteTrainee(id);
    }

    @Override
    public Optional<Trainee> selectTrainee(Long id) {
        return traineeService.selectTrainee(id);
    }

    @Override
    public Optional<Trainee> selectTraineeByUsername(String username) {
        return traineeService.selectTraineeByUsername(username);
    }

    @Override
    public List<Trainee> selectAllTrainees() {
        return traineeService.selectAllTrainees();
    }

    //trainee new

    @Sensitive
    @Override
    public void authenticateTrainee(String username, String password) {
        authenticationService.authenticate(username, password);
    }

    @Sensitive
    @Override
    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        authenticationService.changePassword(username, oldPassword, newPassword);
    }

    @Override
    public Trainee activateDeactivateTrainee(String username, boolean isActive) {
        return traineeService.activateDeactivate(username, isActive);
    }

    @Override
    public void deleteTraineeByUsername(String username) {
        traineeService.deleteTraineeByUsername(username);
    }

    @Override
    public List<Training> getTraineeTrainings(String username, LocalDate from, LocalDate to,
                                              String trainerName,
                                              TrainingType.TrainingTypeName trainingType) {
        return traineeService.getTraineeTrainings(username, from, to, trainerName, trainingType);
    }

    @Override
    public List<Trainer> getNotAssignedActiveTrainers(String traineeUsername) {
        return traineeService.getNotAssignedActiveTrainers(traineeUsername);
    }

    @Override
    public Trainee updateTraineeTrainers(String traineeUsername, List<String> trainerUsernames) {
        return traineeService.updateTraineeTrainers(traineeUsername, trainerUsernames);
    }

    //trainer orig

    @Override
    public Trainer createTrainer(String firstName, String lastName,
                                 TrainingType.TrainingTypeName specialization) {
        return trainerService.createTrainer(firstName, lastName, specialization);
    }

    @Override
    public Trainer updateTrainer(Trainer trainer) {
        return trainerService.updateTrainer(trainer);
    }

    @Override
    public Optional<Trainer> selectTrainer(Long id) {
        return trainerService.selectTrainer(id);
    }

    @Override
    public Optional<Trainer> selectTrainerByUsername(String username) {
        return trainerService.selectTrainerByUsername(username);
    }

    @Override
    public List<Trainer> selectAllTrainers() {
        return trainerService.selectAllTrainers();
    }

    //trainer new
    @Sensitive
    @Override
    public void authenticateTrainer(String username, String password) {
        authenticationService.authenticate(username, password);
    }

    @Sensitive
    @Override
    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        authenticationService.changePassword(username, oldPassword, newPassword);
    }

    @Override
    public Trainer activateDeactivateTrainer(String username, boolean isActive) {
        return trainerService.activateDeactivate(username, isActive);
    }

    @Override
    public List<Training> getTrainerTrainings(String username, LocalDate from, LocalDate to,
                                              String traineeName) {
        return trainerService.getTrainerTrainings(username, from, to, traineeName);
    }

    //training orig
    @Override
    public Training createTraining(Long traineeId, Long trainerId,
                                   String trainingName, TrainingType trainingType,
                                   LocalDate trainingDate, int durationMinutes) {
        return trainingService.createTraining(
                traineeId, trainerId, trainingName,
                trainingType, trainingDate, durationMinutes);
    }

    @Override
    public Optional<Training> selectTraining(Long id) {
        return trainingService.selectTraining(id);
    }

    @Override
    public List<Training> selectAllTrainings() {
        return trainingService.selectAllTrainings();
    }

    @Override
    public List<Training> selectTrainingsByTrainee(Long traineeId) {
        return trainingService.selectTrainingsByTrainee(traineeId);
    }

    @Override
    public List<Training> selectTrainingsByTrainer(Long trainerId) {
        return trainingService.selectTrainingsByTrainer(trainerId);
    }

    //training new

    @Override
    public Training addTraining(String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate,
                                TrainingType.TrainingTypeName trainingType, int durationMinutes) {
        return trainingService.addTraining(
                traineeUsername, trainerUsername, trainingName,
                trainingDate, trainingType, durationMinutes);
    }
}
