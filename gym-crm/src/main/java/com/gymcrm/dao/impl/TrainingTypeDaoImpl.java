package com.gymcrm.dao.impl;

import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.model.TrainingType;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeDaoImpl implements TrainingTypeDao {

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainingTypeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<TrainingType> findByName(TrainingType.TrainingTypeName name) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "FROM TrainingType t WHERE t.trainingTypeName = :name",
                        TrainingType.class)
                .setParameter("name", name)
                .uniqueResultOptional();
    }

    @Override
    public List<TrainingType> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();
    }
}
