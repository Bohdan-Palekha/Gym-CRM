package com.gymcrm.service;

import com.gymcrm.client.event.TrainingAddedEvent;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingService")
class TrainingServiceTest {

    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TrainingService service;
    private Trainee trainee;
    private Trainer trainer;
    private TrainingType yogaType;

    @BeforeEach
    void setUp() {
        service = new TrainingService();
        service.setTrainingDao(trainingDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setEventPublisher(eventPublisher);

        yogaType = new TrainingType(1L, TrainingType.TrainingTypeName.YOGA);

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Smith");
        trainee.setUsername("Alice.Smith");
        trainee.setPassword("p1");
        trainee.setActive(true);

        trainer = new Trainer();
        trainer.setId(2L);
        trainer.setFirstName("Bob");
        trainer.setLastName("Jones");
        trainer.setUsername("Bob.Jones");
        trainer.setPassword("p2");
        trainer.setActive(true);
        trainer.setSpecialization(yogaType);
    }

    @Nested
    @DisplayName("addTraining — Task 16")
    class AddTraining {

        @Test
        @DisplayName("valid input creates and returns training")
        void validInput_creates() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(trainee));
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(trainer));
            when(trainingTypeDao.findByName(TrainingType.TrainingTypeName.YOGA))
                    .thenReturn(Optional.of(yogaType));
            when(trainingDao.save(any())).thenAnswer(inv -> {
                Training t = inv.getArgument(0);
                t.setId(100L);
                return t;
            });

            Training result = service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Morning Flow",
                    LocalDate.of(2024, 5, 10), TrainingType.TrainingTypeName.YOGA, 60);

            assertEquals("Morning Flow", result.getTrainingName());
            assertEquals(60, result.getDurationMinutes());
            assertSame(trainee, result.getTrainee());
            assertSame(trainer, result.getTrainer());
            verify(trainingDao).save(any(Training.class));

            ArgumentCaptor<TrainingAddedEvent> captor = ArgumentCaptor.forClass(TrainingAddedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertSame(result, captor.getValue().getTraining());
        }

        @Test
        @DisplayName("unknown trainee username throws EntityNotFoundException")
        void unknownTrainee_throws() {
            when(traineeDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.addTraining(
                    "ghost", "Bob.Jones", "Session", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 30));
            verifyNoInteractions(trainingDao);
        }

        @Test
        @DisplayName("unknown trainer username throws EntityNotFoundException")
        void unknownTrainer_throws() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(trainee));
            when(trainerDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.addTraining(
                    "Alice.Smith", "ghost", "Session", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 30));
            verifyNoInteractions(trainingDao);
        }

        @Test
        @DisplayName("training type not in DB throws EntityNotFoundException")
        void typeNotFound_throws() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(trainee));
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(trainer));
            when(trainingTypeDao.findByName(TrainingType.TrainingTypeName.ZUMBA))
                    .thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Session", LocalDate.now(),
                    TrainingType.TrainingTypeName.ZUMBA, 30));
        }

        @Test
        @DisplayName("blank trainingName throws IllegalArgumentException")
        void blankName_throws() {
            assertThrows(IllegalArgumentException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "  ", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 30));
        }

        @Test
        @DisplayName("null date throws IllegalArgumentException")
        void nullDate_throws() {
            assertThrows(IllegalArgumentException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Session", null,
                    TrainingType.TrainingTypeName.YOGA, 30));
        }

        @Test
        @DisplayName("zero duration throws IllegalArgumentException")
        void zeroDuration_throws() {
            assertThrows(IllegalArgumentException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Session", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 0));
        }

        @Test
        @DisplayName("null type throws IllegalArgumentException")
        void nullType_throws() {
            assertThrows(IllegalArgumentException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Session", LocalDate.now(), null, 30));
        }
    }

    @Nested
    @DisplayName("addTraining(no type) — REST 14")
    class AddTrainingDerivedType {

        @Test
        @DisplayName("derives training type from the trainer's own specialization")
        void derivesTypeFromTrainerSpecialization() {
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(trainer));
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(trainee));
            when(trainingTypeDao.findByName(TrainingType.TrainingTypeName.YOGA))
                    .thenReturn(Optional.of(yogaType));
            when(trainingDao.save(any())).thenAnswer(inv -> {
                Training t = inv.getArgument(0);
                t.setId(101L);
                return t;
            });

            Training result = service.addTraining(
                    "Alice.Smith", "Bob.Jones", "Morning Flow", LocalDate.of(2026, 8, 1), 60);

            assertEquals(TrainingType.TrainingTypeName.YOGA, result.getTrainingType().getTrainingTypeName());
            assertEquals("Morning Flow", result.getTrainingName());
            assertEquals(60, result.getDurationMinutes());
        }

        @Test
        @DisplayName("unknown trainer username throws EntityNotFoundException before resolving type")
        void unknownTrainer_throws() {
            when(trainerDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.addTraining(
                    "Alice.Smith", "ghost", "Session", LocalDate.now(), 30));
            verifyNoInteractions(trainingDao);
        }
    }

    @Nested
    @DisplayName("createTraining(ids) — legacy backward compat")
    class CreateTrainingById {

        @Test
        @DisplayName("valid ids creates training")
        void valid_creates() {
            when(traineeDao.findById(1L)).thenReturn(Optional.of(trainee));
            when(trainerDao.findById(2L)).thenReturn(Optional.of(trainer));
            when(trainingDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Training result = service.createTraining(
                    1L, 2L, "Evening Flow", yogaType, LocalDate.of(2024, 6, 1), 45);

            assertEquals("Evening Flow", result.getTrainingName());
            verify(trainingDao).save(any(Training.class));
            verify(eventPublisher).publishEvent(any(TrainingAddedEvent.class));
        }

        @Test
        @DisplayName("unknown trainee id throws EntityNotFoundException")
        void unknownTrainee_throws() {
            when(traineeDao.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    service.createTraining(99L, 2L, "Flow", yogaType, LocalDate.now(), 30));
        }
    }

    @Nested
    @DisplayName("workload notification event")
    class WorkloadNotificationEvent {

        @Test
        @DisplayName("no event is published when validation fails before any save")
        void validationFailure_publishesNothing() {
            assertThrows(IllegalArgumentException.class, () -> service.addTraining(
                    "Alice.Smith", "Bob.Jones", "  ", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 30));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("no event is published when the trainer/trainee lookup fails before any save")
        void notFound_publishesNothing() {
            when(traineeDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.addTraining(
                    "ghost", "Bob.Jones", "Session", LocalDate.now(),
                    TrainingType.TrainingTypeName.YOGA, 30));
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("read operations")
    class ReadOperations {

        @Test
        @DisplayName("selectAllTrainings delegates to dao.findAll()")
        void selectAll_delegates() {
            when(trainingDao.findAll()).thenReturn(List.of());
            assertTrue(service.selectAllTrainings().isEmpty());
            verify(trainingDao).findAll();
        }

        @Test
        @DisplayName("selectTrainingsByTrainee delegates with correct id")
        void byTrainee_delegates() {
            when(trainingDao.findByTraineeId(1L)).thenReturn(List.of());
            service.selectTrainingsByTrainee(1L);
            verify(trainingDao).findByTraineeId(1L);
        }

        @Test
        @DisplayName("selectTrainingsByTrainer delegates with correct id")
        void byTrainer_delegates() {
            when(trainingDao.findByTrainerId(2L)).thenReturn(List.of());
            service.selectTrainingsByTrainer(2L);
            verify(trainingDao).findByTrainerId(2L);
        }

        @Test
        @DisplayName("selectTraining returns Optional from dao.findById()")
        void selectTraining_returnsOptional() {
            Training training = new Training();
            when(trainingDao.findById(100L)).thenReturn(Optional.of(training));
            assertEquals(Optional.of(training), service.selectTraining(100L));
        }
    }
}
