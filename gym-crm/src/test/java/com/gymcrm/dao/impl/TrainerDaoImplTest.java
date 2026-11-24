package com.gymcrm.dao.impl;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("TrainerDaoImpl")
class TrainerDaoImplTest {

    @Autowired
    private TrainerDao trainerDao;
    @Autowired
    private TraineeDao traineeDao;
    @Autowired
    private SessionFactory sessionFactory;

    private TrainingType yogaType;
    private TrainingType fitnessType;

    @BeforeEach
    void loadReferenceData() {
        yogaType = fetchType(TrainingType.TrainingTypeName.YOGA);
        fitnessType = fetchType(TrainingType.TrainingTypeName.FITNESS);
    }

    private TrainingType fetchType(TrainingType.TrainingTypeName name) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType t WHERE t.trainingTypeName = :n", TrainingType.class)
                .setParameter("n", name).uniqueResult();
    }

    private Trainer newTrainer(String firstName, String lastName, String username,
                               TrainingType type, boolean active) {
        Trainer tr = new Trainer();
        tr.setFirstName(firstName);
        tr.setLastName(lastName);
        tr.setUsername(username);
        tr.setPassword("p");
        tr.setActive(active);
        tr.setSpecialization(type);
        return tr;
    }

    private Trainee newTrainee(String username) {
        Trainee te = new Trainee();
        te.setFirstName("T");
        te.setLastName("E");
        te.setUsername(username);
        te.setPassword("p");
        te.setActive(true);
        return te;
    }

    @Test
    @DisplayName("save: inserts into users + trainer tables; id assigned")
    void save_persistsBothTables() {
        Trainer saved = trainerDao.save(newTrainer("John", "Doe", "John.Doe", yogaType, true));

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), saved.getUserId()); // JOINED: id = user_id
        assertEquals(TrainingType.TrainingTypeName.YOGA,
                saved.getSpecialization().getTrainingTypeName());
    }

    @Test
    @DisplayName("findByUsername: inherited username column is queried directly")
    void findByUsername_returnsTrainer() {
        trainerDao.save(newTrainer("Jane", "Doe", "Jane.Doe", fitnessType, true));

        Optional<Trainer> found = trainerDao.findByUsername("Jane.Doe");

        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
        assertEquals("Jane.Doe", found.get().getUsername());
    }

    @Test
    @DisplayName("findByUsername: unknown username returns empty")
    void findByUsername_unknown_empty() {
        assertTrue(trainerDao.findByUsername("no.one").isEmpty());
    }

    @Test
    @DisplayName("findAll: returns all saved trainers")
    void findAll_returnsAll() {
        trainerDao.save(newTrainer("A", "1", "A.1", yogaType, true));
        trainerDao.save(newTrainer("B", "2", "B.2", fitnessType, true));
        assertTrue(trainerDao.findAll().size() >= 2);
    }

    @Test
    @DisplayName("update: persists changes to both users and trainer tables")
    void update_persistsChanges() {
        Trainer saved = trainerDao.save(newTrainer("Old", "Name", "Old.Name", yogaType, true));
        saved.setFirstName("Updated");
        Trainer updated = trainerDao.update(saved);
        assertEquals("Updated", updated.getFirstName());
    }


    @Test
    @DisplayName("Task 17: excludes trainers already assigned to the trainee")
    void findActiveNotAssigned_excludesAssigned() {
        Trainer t1 = trainerDao.save(newTrainer("Ana", "T", "Ana.T", yogaType, true));
        Trainer t2 = trainerDao.save(newTrainer("Ben", "U", "Ben.U", fitnessType, true));
        Trainee trainee = traineeDao.save(newTrainee("trainee.one"));
        traineeDao.updateTrainers(trainee.getUsername(), List.of(t1));

        List<Trainer> result = trainerDao.findActiveNotAssignedToTrainee(trainee.getUsername());

        assertFalse(result.contains(t1));
        assertTrue(result.contains(t2));
    }

    @Test
    @DisplayName("Task 17: excludes inactive trainers (tr.active = true in HQL)")
    void findActiveNotAssigned_excludesInactive() {
        Trainer active = trainerDao.save(newTrainer("Cara", "V", "Cara.V", yogaType, true));
        Trainer inactive = trainerDao.save(newTrainer("Dan", "W", "Dan.W", fitnessType, false));
        Trainee trainee = traineeDao.save(newTrainee("trainee.two"));

        List<Trainer> result = trainerDao.findActiveNotAssignedToTrainee(trainee.getUsername());

        assertTrue(result.contains(active));
        assertFalse(result.contains(inactive));
    }

    @Test
    @DisplayName("Task 17: trainee with no assignments gets all active trainers")
    void findActiveNotAssigned_noAssignments_returnsAllActive() {
        trainerDao.save(newTrainer("Eli", "X", "Eli.X", yogaType, true));
        trainerDao.save(newTrainer("Faye", "Y", "Faye.Y", fitnessType, true));
        Trainee trainee = traineeDao.save(newTrainee("trainee.three"));

        assertTrue(trainerDao.findActiveNotAssignedToTrainee(trainee.getUsername()).size() >= 2);
    }
}
