package com.gymcrm.service;

import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeServiceTest {

    @Mock
    private TrainingTypeDao trainingTypeDao;

    private TrainingTypeService trainingTypeService;

    @BeforeEach
    void setUp() {
        trainingTypeService = new TrainingTypeService();
        trainingTypeService.setTrainingTypeDao(trainingTypeDao);
    }

    @Test
    @DisplayName("REST task 17 - returns all training types from the DAO unchanged")
    void selectAllTrainingTypesDelegatesToDao() {
        List<TrainingType> types = List.of(
                new TrainingType(1L, TrainingType.TrainingTypeName.FITNESS),
                new TrainingType(2L, TrainingType.TrainingTypeName.YOGA));
        when(trainingTypeDao.findAll()).thenReturn(types);

        List<TrainingType> result = trainingTypeService.selectAllTrainingTypes();

        assertEquals(types, result);
    }
}
