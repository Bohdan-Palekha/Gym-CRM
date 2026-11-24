package com.gymcrm.dao.impl;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.model.TrainingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("TrainingTypeDaoImpl")
class TrainingTypeDaoImplTest {

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns exactly five seeded training types")
        void returnsFiveTypes() {
            List<TrainingType> all = trainingTypeDao.findAll();
            assertEquals(TrainingType.TrainingTypeName.values().length, all.size());
        }

        @Test
        @DisplayName("every returned row has a non-null id")
        void everyRowHasId() {
            trainingTypeDao.findAll().forEach(t -> assertNotNull(t.getId()));
        }

        @Test
        @DisplayName("all enum values are present")
        void allEnumValuesPresent() {
            List<TrainingType> all = trainingTypeDao.findAll();
            for (TrainingType.TrainingTypeName name : TrainingType.TrainingTypeName.values()) {
                assertTrue(
                        all.stream().anyMatch(t -> t.getTrainingTypeName() == name),
                        "Missing TrainingTypeName: " + name);
            }
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByName {

        @Test
        @DisplayName("YOGA row is found and id is non-null")
        void yoga_isFound() {
            Optional<TrainingType> result = trainingTypeDao.findByName(
                    TrainingType.TrainingTypeName.YOGA);
            assertTrue(result.isPresent());
            assertNotNull(result.get().getId());
            assertEquals(TrainingType.TrainingTypeName.YOGA,
                    result.get().getTrainingTypeName());
        }

        @Test
        @DisplayName("FITNESS row is found")
        void fitness_isFound() {
            assertTrue(trainingTypeDao.findByName(
                    TrainingType.TrainingTypeName.FITNESS).isPresent());
        }

        @Test
        @DisplayName("ZUMBA row is found")
        void zumba_isFound() {
            assertTrue(trainingTypeDao.findByName(
                    TrainingType.TrainingTypeName.ZUMBA).isPresent());
        }

        @Test
        @DisplayName("STRETCHING row is found")
        void stretching_isFound() {
            assertTrue(trainingTypeDao.findByName(
                    TrainingType.TrainingTypeName.STRETCHING).isPresent());
        }

        @Test
        @DisplayName("RESISTANCE row is found")
        void resistance_isFound() {
            assertTrue(trainingTypeDao.findByName(
                    TrainingType.TrainingTypeName.RESISTANCE).isPresent());
        }

        @Test
        @DisplayName("null argument returns empty Optional without exception")
        void nullName_returnsEmpty() {
            Optional<TrainingType> result = trainingTypeDao.findByName(null);
            assertTrue(result.isEmpty());
        }
    }
}
