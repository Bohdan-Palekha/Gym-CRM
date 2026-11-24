package com.gymcrm.storage;

import com.gymcrm.model.TrainingType;
import jakarta.annotation.PostConstruct;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//data initializer for training type
//rows that already exist are skipped
//raw Hibernate session is used PostConstruct fires before the transaction proxy is active
@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public DataInitializer(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @PostConstruct
    public void seedTrainingTypes() {
        log.info("DataInitializer: seeding TrainingType reference data...");
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            for (TrainingType.TrainingTypeName name : TrainingType.TrainingTypeName.values()) {
                Long count = session
                        .createQuery(
                            "SELECT COUNT(t) FROM TrainingType t WHERE t.trainingTypeName = :name",
                            Long.class)
                        .setParameter("name", name)
                        .uniqueResult();

                if (count == 0) {
                    TrainingType tt = new TrainingType();
                    tt.setTrainingTypeName(name);
                    session.persist(tt);
                    log.debug("Inserted TrainingType: {}", name);
                } else {
                    log.debug("TrainingType already exists, skipping: {}", name);
                }
            }

            tx.commit();
            log.info("DataInitializer: TrainingType seeding complete");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            log.error("DataInitializer: failed to seed TrainingType data", e);
            throw new RuntimeException("DataInitializer failed", e);
        }
    }
}
