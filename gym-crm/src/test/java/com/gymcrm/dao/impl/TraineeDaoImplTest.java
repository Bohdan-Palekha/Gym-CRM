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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("TraineeDaoImpl")
class TraineeDaoImplTest {

    @Autowired
    private TraineeDao traineeDao;
    @Autowired
    private TrainerDao trainerDao;
    @Autowired
    private SessionFactory sessionFactory;

    private TrainingType yogaType;

    @BeforeEach
    void loadReferenceData() {
        yogaType = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType t WHERE t.trainingTypeName = :n", TrainingType.class)
                .setParameter("n", TrainingType.TrainingTypeName.YOGA)
                .uniqueResult();
    }


    private Trainee newTrainee(String firstName, String lastName, String username) {
        Trainee t = new Trainee();
        t.setFirstName(firstName);
        t.setLastName(lastName);
        t.setUsername(username);
        t.setPassword("testPass01");
        t.setActive(true);
        t.setDateOfBirth(LocalDate.of(1990, 1, 1));
        t.setAddress("Test Street");
        return t;
    }

    private Trainer newTrainer(String username) {
        Trainer tr = new Trainer();
        tr.setFirstName("T");
        tr.setLastName("R");
        tr.setUsername(username);
        tr.setPassword("p");
        tr.setActive(true);
        tr.setSpecialization(yogaType);
        return tr;
    }

    @Test
    @DisplayName("save: persists into both users and trainee tables; id assigned")
    void save_persistsBothTables() {
        Trainee saved = traineeDao.save(newTrainee("Alice", "Brown", "Alice.Brown"));

        assertNotNull(saved.getId());
        // getUserId() now returns getId() — same value (JOINED inheritance)
        assertEquals(saved.getId(), saved.getUserId());
        assertEquals("Alice.Brown", saved.getUsername());
        assertEquals("Alice", saved.getFirstName());
    }

    @Test
    @DisplayName("findById: loads inherited User fields alongside Trainee fields")
    void findById_loadsInheritedFields() {
        Trainee saved = traineeDao.save(newTrainee("Bob", "Green", "Bob.Green"));

        Optional<Trainee> found = traineeDao.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Bob.Green", found.get().getUsername());
        assertEquals("Bob", found.get().getFirstName());
        assertTrue(found.get().isActive());
    }

    @Test
    @DisplayName("findById: returns empty Optional for non-existing id")
    void findById_nonExisting_empty() {
        assertTrue(traineeDao.findById(999_999L).isEmpty());
    }

    @Test
    @DisplayName("findByUsername: queries inherited username column")
    void findByUsername_returnsTrainee() {
        traineeDao.save(newTrainee("Carol", "King", "Carol.King"));

        Optional<Trainee> found = traineeDao.findByUsername("Carol.King");

        assertTrue(found.isPresent());
        assertEquals("Carol", found.get().getFirstName());
    }

    @Test
    @DisplayName("findByUsername: unknown username returns empty")
    void findByUsername_unknown_empty() {
        assertTrue(traineeDao.findByUsername("nobody.here").isEmpty());
    }

    @Test
    @DisplayName("findAll: returns all persisted trainees with inherited fields")
    void findAll_returnsAll() {
        traineeDao.save(newTrainee("Dave", "A", "Dave.A"));
        traineeDao.save(newTrainee("Eve", "B", "Eve.B"));

        List<Trainee> all = traineeDao.findAll();
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().allMatch(t -> t.getFirstName() != null));
    }

    @Test
    @DisplayName("update: persists changes to both users and trainee tables")
    void update_persistsChanges() {
        Trainee saved = traineeDao.save(newTrainee("Frank", "Z", "Frank.Z"));
        saved.setAddress("New Address 99");
        saved.setFirstName("Franklin");     // inherited User field

        traineeDao.update(saved);

        Trainee reloaded = traineeDao.findById(saved.getId()).orElseThrow();
        assertEquals("New Address 99", reloaded.getAddress());
        assertEquals("Franklin", reloaded.getFirstName());
    }

    @Test
    @DisplayName("deleteById: removes rows from both users and trainee tables")
    void deleteById_removesBothRows() {
        Trainee saved = traineeDao.save(newTrainee("Grace", "H", "Grace.H"));
        traineeDao.deleteById(saved.getId());
        assertTrue(traineeDao.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("deleteByUsername: removes rows from both tables")
    void deleteByUsername_removesBothRows() {
        traineeDao.save(newTrainee("Hank", "I", "Hank.I"));
        traineeDao.deleteByUsername("Hank.I");
        assertTrue(traineeDao.findByUsername("Hank.I").isEmpty());
    }

    @Test
    @DisplayName("updateTrainers: replaces trainer set in trainee_trainer join table")
    void updateTrainers_replacesSet() {
        Trainee trainee = traineeDao.save(newTrainee("Ivy", "J", "Ivy.J"));
        Trainer trainer = trainerDao.save(newTrainer("Trainer.One"));

        Trainee updated = traineeDao.updateTrainers("Ivy.J", List.of(trainer));

        assertEquals(1, updated.getTrainers().size());
        assertTrue(updated.getTrainers().contains(trainer));
    }

    @Test
    @DisplayName("updateTrainers: empty list clears join table for trainee")
    void updateTrainers_emptyList_clearsJoinTable() {
        traineeDao.save(newTrainee("Jack", "K", "Jack.K"));
        assertTrue(traineeDao.updateTrainers("Jack.K", List.of()).getTrainers().isEmpty());
    }
}
