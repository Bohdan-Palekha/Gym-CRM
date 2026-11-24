package com.gymcrm.dao.impl;

import com.gymcrm.dao.TrainingDao;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TrainingDaoImpl implements TrainingDao {


    private final SessionFactory sessionFactory;

    @Autowired
    public TrainingDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    //write

    //session refrence reattaches a possibly detached training type as a managed proxy so persist can satisfy fk without SELECT
    @Override
    public Training save(Training training) {
        Session session = sessionFactory.getCurrentSession();

        if (training.getTrainingType() != null && training.getTrainingType().getId() != null) {
            TrainingType managed = session.getReference(
                    TrainingType.class, training.getTrainingType().getId());
            training.setTrainingType(managed);
        }

        session.persist(training);
        log.debug("Persisted training name={}", training.getTrainingName());
        return training;
    }

    //read
    @Override
    public Optional<Training> findById(Long id) {
        return Optional.ofNullable(
                sessionFactory.getCurrentSession().get(Training.class, id));
    }

    @Override
    public List<Training> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Training", Training.class)
                .list();
    }

    @Override
    public List<Training> findByTraineeId(Long traineeId) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Training t WHERE t.trainee.id = :id", Training.class)
                .setParameter("id", traineeId)
                .list();
    }

    @Override
    public List<Training> findByTrainerId(Long trainerId) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Training t WHERE t.trainer.id = :id", Training.class)
                .setParameter("id", trainerId)
                .list();
    }

    @Override
    public int deleteByTraineeId(Long traineeId) {
        int n = sessionFactory.getCurrentSession()
                .createMutationQuery("DELETE FROM Training t WHERE t.trainee.id = :id")
                .setParameter("id", traineeId)
                .executeUpdate();
        log.debug("Deleted {} training(s) for trainee id={}", n, traineeId);
        return n;
    }

    @Override
    public List<Training> findByTraineeCriteria(
            String traineeUsername,
            LocalDate from,
            LocalDate to,
            String trainerName,
            TrainingType.TrainingTypeName trainingType) {

        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Training> cq = cb.createQuery(Training.class);
        Root<Training> root = cq.from(Training.class);
        Join<Training, Trainee> traineeJoin = root.join("trainee");
        Join<Training, Trainer> trainerJoin = root.join("trainer");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(traineeJoin.get("username"), traineeUsername));

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), to));
        }
        if (trainerName != null && !trainerName.isBlank()) {
            String pattern = "%" + trainerName.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(trainerJoin.get("firstName")), pattern),
                    cb.like(cb.lower(trainerJoin.get("lastName")), pattern)
            ));
        }
        if (trainingType != null) {
            Join<Training, TrainingType> typeJoin = root.join("trainingType");
            predicates.add(cb.equal(typeJoin.get("trainingTypeName"), trainingType));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        return session.createQuery(cq).list();
    }

    @Override
    public List<Training> findByTrainerCriteria(
            String trainerUsername,
            LocalDate from,
            LocalDate to,
            String traineeName) {

        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Training> cq = cb.createQuery(Training.class);
        Root<Training> root = cq.from(Training.class);

        Join<Training, Trainer> trainerJoin = root.join("trainer");
        Join<Training, Trainee> traineeJoin = root.join("trainee");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(trainerJoin.get("username"), trainerUsername));

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), to));
        }
        if (traineeName != null && !traineeName.isBlank()) {
            String pattern = "%" + traineeName.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(traineeJoin.get("firstName")), pattern),
                    cb.like(cb.lower(traineeJoin.get("lastName")), pattern)
            ));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        return session.createQuery(cq).list();
    }
}
