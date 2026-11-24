package com.gymcrm.dao.impl;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("TrainingDaoImpl")
class TrainingDaoImplTest {

    @Autowired
    private TrainingDao trainingDao;
    @Autowired
    private TraineeDao traineeDao;
    @Autowired
    private TrainerDao trainerDao;
    @Autowired
    private SessionFactory sessionFactory;

    private Trainee trainee;
    private Trainer trainer;
    private TrainingType yogaType;
    private TrainingType fitnessType;

    @BeforeEach
    void setUp() {
        yogaType = fetchType(TrainingType.TrainingTypeName.YOGA);
        fitnessType = fetchType(TrainingType.TrainingTypeName.FITNESS);
        trainee = new Trainee();
        trainee.setFirstName("Alice");
        trainee.setLastName("Smith");
        trainee.setUsername("Alice.Smith.T");
        trainee.setPassword("p");
        trainee.setActive(true);
        traineeDao.save(trainee);
        trainer = new Trainer();
        trainer.setFirstName("Bob");
        trainer.setLastName("Jones");
        trainer.setUsername("Bob.Jones.T");
        trainer.setPassword("p");
        trainer.setActive(true);
        trainer.setSpecialization(yogaType);
        trainerDao.save(trainer);
    }

    private TrainingType fetchType(TrainingType.TrainingTypeName n) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType t WHERE t.trainingTypeName = :n", TrainingType.class)
                .setParameter("n", n).uniqueResult();
    }

    private Training saveTraining(String name, LocalDate date, int duration, TrainingType type) {
        Training t = new Training();
        t.setTrainee(trainee);
        t.setTrainer(trainer);
        t.setTrainingName(name);
        t.setTrainingDate(date);
        t.setTrainingType(type);
        t.setDurationMinutes(duration);
        return trainingDao.save(t);
    }

    @Test
    @DisplayName("save: assigns id; FK columns reference JOINED-inheritance PKs")
    void save_persistsTraining() {
        Training saved = saveTraining("Morning Flow", LocalDate.of(2024, 3, 1), 60, yogaType);
        assertNotNull(saved.getId());
        assertEquals("Morning Flow", saved.getTrainingName());
        assertEquals(trainee.getId(), saved.getTraineeId());
        assertEquals(trainer.getId(), saved.getTrainerId());
    }

    @Test
    @DisplayName("Task 14: no filters returns all trainee trainings")
    void findByTraineeCriteria_noFilters_returnsAll() {
        saveTraining("S1", LocalDate.of(2024, 1, 10), 30, yogaType);
        saveTraining("S2", LocalDate.of(2024, 2, 15), 45, fitnessType);
        List<Training> result = trainingDao.findByTraineeCriteria(
                "Alice.Smith.T", null, null, null, null);
        assertTrue(result.size() >= 2);
    }

    @Test
    @DisplayName("Task 14: date range filter restricts results")
    void findByTraineeCriteria_dateRange_filters() {
        saveTraining("InRange", LocalDate.of(2024, 3, 15), 60, yogaType);
        saveTraining("OutRange", LocalDate.of(2024, 5, 1), 30, yogaType);

        List<Training> result = trainingDao.findByTraineeCriteria(
                "Alice.Smith.T",
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1),
                null, null);

        assertTrue(result.stream().anyMatch(t -> t.getTrainingName().equals("InRange")));
        assertTrue(result.stream().noneMatch(t -> t.getTrainingName().equals("OutRange")));
    }

    @Test
    @DisplayName("Task 14: type filter restricts results")
    void findByTraineeCriteria_typeFilter_restricts() {
        saveTraining("Yoga1", LocalDate.of(2024, 3, 1), 60, yogaType);
        saveTraining("Fitness1", LocalDate.of(2024, 3, 2), 45, fitnessType);

        List<Training> result = trainingDao.findByTraineeCriteria(
                "Alice.Smith.T", null, null, null, TrainingType.TrainingTypeName.YOGA);

        assertTrue(result.stream().allMatch(t ->
                t.getTrainingType().getTrainingTypeName() == TrainingType.TrainingTypeName.YOGA));
    }

    @Test
    @DisplayName("Task 14: trainer name filter uses inherited firstName/lastName on Trainer join")
    void findByTraineeCriteria_trainerNameFilter() {
        saveTraining("ByTrainer", LocalDate.of(2024, 4, 1), 30, yogaType);

        List<Training> result = trainingDao.findByTraineeCriteria(
                "Alice.Smith.T", null, null, "bob", null); // trainer firstName = "Bob"

        assertTrue(result.stream().anyMatch(t -> t.getTrainingName().equals("ByTrainer")));
    }

    @Test
    @DisplayName("Task 15: no filters returns all trainer trainings")
    void findByTrainerCriteria_noFilters_returnsAll() {
        saveTraining("T1", LocalDate.of(2024, 1, 5), 30, yogaType);
        saveTraining("T2", LocalDate.of(2024, 2, 5), 60, fitnessType);

        List<Training> result = trainingDao.findByTrainerCriteria(
                "Bob.Jones.T", null, null, null);
        assertTrue(result.size() >= 2);
    }

    @Test
    @DisplayName("Task 15: from-date filter works")
    void findByTrainerCriteria_fromFilter_works() {
        saveTraining("Early", LocalDate.of(2024, 1, 1), 30, yogaType);
        saveTraining("Recent", LocalDate.of(2024, 6, 1), 45, yogaType);

        List<Training> result = trainingDao.findByTrainerCriteria(
                "Bob.Jones.T", LocalDate.of(2024, 5, 1), null, null);

        assertTrue(result.stream().anyMatch(t -> t.getTrainingName().equals("Recent")));
        assertTrue(result.stream().noneMatch(t -> t.getTrainingName().equals("Early")));
    }

    @Test
    @DisplayName("Task 15: trainee name filter uses inherited firstName/lastName on Trainee join")
    void findByTrainerCriteria_traineeNameFilter() {
        saveTraining("ByTrainee", LocalDate.of(2024, 4, 1), 30, yogaType);

        List<Training> result = trainingDao.findByTrainerCriteria(
                "Bob.Jones.T", null, null, "alice");

        assertTrue(result.stream().anyMatch(t -> t.getTrainingName().equals("ByTrainee")));
    }

    @Test
    @DisplayName("deleteByTraineeId: removes all trainings for the trainee")
    void deleteByTraineeId_removesAll() {
        saveTraining("Del1", LocalDate.of(2024, 3, 1), 30, yogaType);
        saveTraining("Del2", LocalDate.of(2024, 4, 1), 45, yogaType);

        int deleted = trainingDao.deleteByTraineeId(trainee.getId());

        assertTrue(deleted >= 2);
        assertTrue(trainingDao.findByTraineeId(trainee.getId()).isEmpty());
    }
}
