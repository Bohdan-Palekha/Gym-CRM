package com.gymcrm.dao.impl;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.gymcrm.TestHibernateConfig;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class
})
@Transactional
class TraineeDaoDbUnitIntegrationTest {

    @Autowired
    private TraineeDao traineeDao;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    @DisplayName("findAll() returns the trainees seeded directly into the database via DBUnit")
    @DatabaseSetup("/dbunit/trainees-seed.xml")
    void findAll_returnsSeededTrainees() {
        List<Trainee> all = traineeDao.findAll();

        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(t -> "priya.kapoor".equals(t.getUsername())
                && "Priya".equals(t.getFirstName())
                && "4 Cedar Court".equals(t.getAddress())
                && t.isActive()));
        assertTrue(all.stream().anyMatch(t -> "noah.fischer".equals(t.getUsername())
                && !t.isActive()
                && t.getAddress() == null));
    }

    @Test
    @DisplayName("findByUsername() reads a single seeded trainee with its optional fields intact")
    @DatabaseSetup("/dbunit/trainees-seed.xml")
    void findByUsername_returnsMatchingTrainee() {
        Optional<Trainee> found = traineeDao.findByUsername("priya.kapoor");

        assertTrue(found.isPresent());
        assertEquals("Kapoor", found.get().getLastName());
        assertEquals(LocalDate.of(1997, 3, 22), found.get().getDateOfBirth());
    }

    @Test
    @DisplayName("findByUsername() for a username not present in the dataset is empty")
    @DatabaseSetup("/dbunit/trainees-seed.xml")
    void findByUsername_unknownUsername_isEmpty() {
        assertTrue(traineeDao.findByUsername("does.not.exist").isEmpty());
    }

    @Test
    @DisplayName("deleteByUsername() leaves the database exactly as expected by the assertion dataset")
    @DatabaseSetup("/dbunit/trainees-seed.xml")
    @ExpectedDatabase(value = "/dbunit/trainees-after-delete.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void deleteByUsername_leavesExpectedState() {
        traineeDao.deleteByUsername("noah.fischer");
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    @DisplayName("deleteByUsername() on an unseeded username throws and leaves the seed untouched")
    @DatabaseSetup("/dbunit/trainees-seed.xml")
    @ExpectedDatabase(value = "/dbunit/trainees-seed.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void deleteByUsername_unknownUsername_throwsAndLeavesSeedIntact() {
        assertThrows(EntityNotFoundException.class, () -> traineeDao.deleteByUsername("ghost.user"));
    }
}
