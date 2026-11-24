package com.gymcrm.dao;

import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingDao {

    Training save(Training training);

    Optional<Training> findById(Long id);

    List<Training> findAll();

    List<Training> findByTraineeId(Long traineeId);

    List<Training> findByTrainerId(Long trainerId);

    int deleteByTraineeId(Long traineeId);

    List<Training> findByTraineeCriteria(
            String traineeUsername,
            LocalDate from,
            LocalDate to,
            String trainerName,
            TrainingType.TrainingTypeName trainingType);

    List<Training> findByTrainerCriteria(
            String trainerUsername,
            LocalDate from,
            LocalDate to,
            String traineeName);
}
