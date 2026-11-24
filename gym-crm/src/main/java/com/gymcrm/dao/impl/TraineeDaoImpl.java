package com.gymcrm.dao.impl;

import com.gymcrm.dao.TraineeDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TraineeDaoImpl implements TraineeDao {


    private final SessionFactory sessionFactory;

    @Autowired
    public TraineeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    //write

    @Override
    public Trainee save(Trainee trainee) {
        sessionFactory.getCurrentSession().persist(trainee);
        log.debug("Persisted trainee username={}", trainee.getUsername());
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        Trainee merged = (Trainee) sessionFactory.getCurrentSession().merge(trainee);
        log.debug("Updated trainee id={}", merged.getId());
        return merged;
    }

    @Override
    public void deleteById(Long id) {
        Trainee t = findById(id).orElseThrow(() ->
                new EntityNotFoundException("Trainee not found for delete: id=" + id));
        sessionFactory.getCurrentSession().remove(t);
        log.debug("Deleted trainee id={}", id);
    }

    @Override
    public void deleteByUsername(String username) {
        Trainee t = findByUsername(username).orElseThrow(() ->
                new EntityNotFoundException("Trainee not found for delete: username=" + username));
        sessionFactory.getCurrentSession().remove(t);
        log.debug("Deleted trainee username={}", username);
    }

    @Override
    public Trainee updateTrainers(String traineeUsername, List<Trainer> newTrainers) {
        Trainee t = findByUsername(traineeUsername).orElseThrow(() ->
                new EntityNotFoundException("Trainee not found: username=" + traineeUsername));
        t.setTrainers(new HashSet<>(newTrainers));
        Trainee merged = (Trainee) sessionFactory.getCurrentSession().merge(t);
        log.debug("Updated trainer list for trainee username={}, count={}",
                traineeUsername, newTrainers.size());
        return merged;
    }

    //read

    @Override
    public Optional<Trainee> findById(Long id) {
        return Optional.ofNullable(
                sessionFactory.getCurrentSession().get(Trainee.class, id));
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        if (username == null) return Optional.empty();
        //hibernate auto-joins users & trainee
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "FROM Trainee t WHERE t.username = :username",
                        Trainee.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    @Override
    public List<Trainee> findAll() {
        //joined inheritance issues the join automatically
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Trainee", Trainee.class)
                .list();
    }
}
