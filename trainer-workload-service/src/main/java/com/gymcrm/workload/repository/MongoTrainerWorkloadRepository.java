package com.gymcrm.workload.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//wrapper MongoOperations rather than a generic MongoRepository
//persistence is search/update by username, name lookup, depends on the MongoOperations abstraction not MongoTemplate
//easily mockable for the service layer unit tests
//spring auto configured MongoTemplate bean satisfies it directly
@Repository
@RequiredArgsConstructor
public class MongoTrainerWorkloadRepository implements TrainerWorkloadRepository {

    private final MongoOperations mongoOperations;

    @Override
    public Optional<TrainerWorkloadDocument> findByUsername(String username) {
        return Optional.ofNullable(mongoOperations.findById(username, TrainerWorkloadDocument.class));
    }

    @Override
    public TrainerWorkloadDocument save(TrainerWorkloadDocument document) {
        //document upsert by _id (=username)
        //@Version on the document makes this optimistic-locked, throws OptimisticLockingFailureException on conflict
        return mongoOperations.save(document);
    }

    @Override
    public List<TrainerWorkloadDocument> findByFirstNameAndLastName(String firstName, String lastName) {
        Query query = Query.query(Criteria.where("firstName").is(firstName).and("lastName").is(lastName));
        return mongoOperations.find(query, TrainerWorkloadDocument.class);
    }
}
