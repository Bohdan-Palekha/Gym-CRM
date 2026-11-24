package com.gymcrm.service;

import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.model.TrainingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//training types are constant read-only list seeded once by DataInitializer
@Slf4j
@Service
public class TrainingTypeService {

    private TrainingTypeDao trainingTypeDao;

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao d) {
        this.trainingTypeDao = d;
    }

    @Transactional(readOnly = true)
    public List<TrainingType> selectAllTrainingTypes() {
        return trainingTypeDao.findAll();
    }
}
