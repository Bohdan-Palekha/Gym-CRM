package com.gymcrm.dao.impl;

import com.gymcrm.dao.TrainerDao;
import com.gymcrm.model.Trainer;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TrainerDaoImpl implements TrainerDao {

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainerDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    //write

    @Override
    public Trainer save(Trainer trainer) {
        sessionFactory.getCurrentSession().persist(trainer);
        log.debug("Persisted trainer username={}", trainer.getUsername());
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        Trainer merged = (Trainer) sessionFactory.getCurrentSession().merge(trainer);
        log.debug("Updated trainer id={}", merged.getId());
        return merged;
    }

    //read

    @Override
    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable(
                sessionFactory.getCurrentSession().get(Trainer.class, id));
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "FROM Trainer t WHERE t.username = :username",
                        Trainer.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    @Override
    public List<Trainer> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Trainer", Trainer.class)
                .list();
    }

    //active trainers not yet in the given trainee's trainer list
    @Override
    public List<Trainer> findActiveNotAssignedToTrainee(String traineeUsername) {
        String hql = """
                SELECT tr FROM Trainer tr
                WHERE tr.active = true
                  AND tr NOT IN (
                      SELECT t FROM Trainee te JOIN te.trainers t
                      WHERE te.username = :username
                  )
                """;
        return sessionFactory.getCurrentSession()
                .createQuery(hql, Trainer.class)
                .setParameter("username", traineeUsername)
                .list();
    }
}
