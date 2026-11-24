package com.gymcrm.dao.impl;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.UserDao;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import com.gymcrm.model.User;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("UserDaoImpl")
class UserDaoImplTest {

    @Autowired
    private UserDao userDao;
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

    private Trainee newTrainee(String username) {
        Trainee t = new Trainee();
        t.setFirstName("Alice");
        t.setLastName("Brown");
        t.setUsername(username);
        t.setPassword("testPass01");
        t.setActive(true);
        t.setDateOfBirth(LocalDate.of(1990, 1, 1));
        t.setAddress("Test Street");
        return t;
    }

    private Trainer newTrainer(String username) {
        Trainer tr = new Trainer();
        tr.setFirstName("Bob");
        tr.setLastName("Green");
        tr.setUsername(username);
        tr.setPassword("testPass02");
        tr.setActive(true);
        tr.setSpecialization(yogaType);
        return tr;
    }

    @Test
    @DisplayName("findByUsername: resolves a trainee's username to the concrete Trainee subtype")
    void findByUsername_resolvesConcreteTraineeSubtype() {
        traineeDao.save(newTrainee("Alice.Brown"));

        Optional<User> found = userDao.findByUsername("Alice.Brown");

        assertTrue(found.isPresent());
        assertInstanceOf(Trainee.class, found.get());
        assertEquals("Alice", found.get().getFirstName());
        assertEquals("testPass01", found.get().getPassword());
    }

    @Test
    @DisplayName("findByUsername: resolves a trainer's username to the concrete Trainer subtype")
    void findByUsername_resolvesConcreteTrainerSubtype() {
        trainerDao.save(newTrainer("Bob.Green"));

        Optional<User> found = userDao.findByUsername("Bob.Green");

        assertTrue(found.isPresent());
        assertInstanceOf(Trainer.class, found.get());
        assertEquals("Bob", found.get().getFirstName());
    }

    @Test
    @DisplayName("findByUsername: unknown username returns empty, regardless of role")
    void findByUsername_unknown_empty() {
        assertTrue(userDao.findByUsername("nobody.here").isEmpty());
    }

    @Test
    @DisplayName("findByUsername: null username returns empty rather than throwing")
    void findByUsername_null_empty() {
        assertTrue(userDao.findByUsername(null).isEmpty());
    }

    @Test
    @DisplayName("update: a password change on a trainee-backed User persists to the trainee")
    void update_traineeBackedUser_persistsPasswordChange() {
        Trainee saved = traineeDao.save(newTrainee("Carol.King"));
        User user = userDao.findByUsername("Carol.King").orElseThrow();

        user.setPassword("newPass999");
        userDao.update(user);

        Trainee reloaded = traineeDao.findById(saved.getId()).orElseThrow();
        assertEquals("newPass999", reloaded.getPassword());
    }

    @Test
    @DisplayName("update: a password change on a trainer-backed User persists to the trainer")
    void update_trainerBackedUser_persistsPasswordChange() {
        Trainer saved = trainerDao.save(newTrainer("Dave.Adams"));
        User user = userDao.findByUsername("Dave.Adams").orElseThrow();

        user.setPassword("anotherNewPass");
        userDao.update(user);

        Trainer reloaded = trainerDao.findById(saved.getId()).orElseThrow();
        assertEquals("anotherNewPass", reloaded.getPassword());
    }
}
