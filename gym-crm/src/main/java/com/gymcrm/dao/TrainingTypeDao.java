package com.gymcrm.dao;

import com.gymcrm.model.TrainingType;

import java.util.List;
import java.util.Optional;

//read only
public interface TrainingTypeDao {

    Optional<TrainingType> findByName(TrainingType.TrainingTypeName name);

    List<TrainingType> findAll();
}
