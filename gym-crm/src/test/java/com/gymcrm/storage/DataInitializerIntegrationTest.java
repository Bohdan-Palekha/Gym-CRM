package com.gymcrm.storage;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.model.TrainingType;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(TestHibernateConfig.class)
@Transactional
@DisplayName("DataInitializer integration")
class DataInitializerIntegrationTest {

    @Autowired private SessionFactory sessionFactory;

    @Test
    @DisplayName("all five TrainingTypeName values are seeded on startup")
    void allTrainingTypesSeeded() {
        List<TrainingType> all = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();

        Set<TrainingType.TrainingTypeName> seeded = all.stream()
                .map(TrainingType::getTrainingTypeName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(TrainingType.TrainingTypeName.values()), seeded);
    }

    @Test
    @DisplayName("each TrainingType row has a non-null generated id")
    void eachRowHasGeneratedId() {
        List<TrainingType> all = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();
        assertTrue(all.stream().allMatch(t -> t.getId() != null));
    }

    @Test
    @DisplayName("no duplicate rows exist per TrainingTypeName (idempotent seeding)")
    void noDuplicates() {
        List<TrainingType> all = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();
        assertEquals(TrainingType.TrainingTypeName.values().length, all.size());
    }
}
