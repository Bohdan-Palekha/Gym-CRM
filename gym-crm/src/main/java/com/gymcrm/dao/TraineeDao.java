package com.gymcrm.dao;

import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {

    Trainee save(Trainee trainee);

    Optional<Trainee> findById(Long id);

    Optional<Trainee> findByUsername(String username);

    List<Trainee> findAll();

    Trainee update(Trainee trainee);

    void deleteById(Long id);

    //cascade is handled by jpa orphanRemoval
    void deleteByUsername(String username);

    Trainee updateTrainers(String traineeUsername, List<Trainer> newTrainers);
}
