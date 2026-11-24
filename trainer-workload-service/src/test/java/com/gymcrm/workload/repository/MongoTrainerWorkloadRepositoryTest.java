package com.gymcrm.workload.repository;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MongoTrainerWorkloadRepository")
class MongoTrainerWorkloadRepositoryTest {

    @Mock
    private MongoOperations mongoOperations;

    private MongoTrainerWorkloadRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MongoTrainerWorkloadRepository(mongoOperations);
    }

    @Test
    @DisplayName("findByUsername looks the document up by its _id (=username)")
    void findByUsername_delegatesToFindById() {
        TrainerWorkloadDocument document =
                new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, List.of(), 0L);
        when(mongoOperations.findById("bob.trainer", TrainerWorkloadDocument.class)).thenReturn(document);

        Optional<TrainerWorkloadDocument> result = repository.findByUsername("bob.trainer");

        assertTrue(result.isPresent());
        assertSame(document, result.get());
    }

    @Test
    @DisplayName("findByUsername returns empty when no document exists for that username")
    void findByUsername_notFound_returnsEmpty() {
        when(mongoOperations.findById("ghost", TrainerWorkloadDocument.class)).thenReturn(null);

        assertTrue(repository.findByUsername("ghost").isEmpty());
    }

    @Test
    @DisplayName("save delegates to the underlying upsert-by-id save")
    void save_delegates() {
        TrainerWorkloadDocument toSave =
                new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, List.of(), null);
        TrainerWorkloadDocument saved =
                new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, List.of(), 0L);
        when(mongoOperations.save(toSave)).thenReturn(saved);

        TrainerWorkloadDocument result = repository.save(toSave);

        assertSame(saved, result);
    }

    @Test
    @DisplayName("findByFirstNameAndLastName queries by both fields")
    void findByFirstNameAndLastName_queriesBothFields() {
        TrainerWorkloadDocument document =
                new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, List.of(), 0L);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(mongoOperations.find(queryCaptor.capture(), eq(TrainerWorkloadDocument.class)))
                .thenReturn(List.of(document));

        List<TrainerWorkloadDocument> result = repository.findByFirstNameAndLastName("Bob", "Jones");

        assertEquals(List.of(document), result);
        Document queryObject = queryCaptor.getValue().getQueryObject();
        assertEquals("Bob", queryObject.get("firstName"));
        assertEquals("Jones", queryObject.get("lastName"));
    }
}
