package com.gymcrm.service;

import com.gymcrm.client.event.TrainingsRemovedEvent;
import com.gymcrm.dao.TraineeDao;
import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.util.PasswordGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraineeService")
class TraineeServiceTest {

    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingDao trainingDao;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private UsernameService usernameService;
    @Mock
    private GymMetrics gymMetrics;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TraineeService service;
    private Trainee sampleTrainee;

    @BeforeEach
    void setUp() {
        service = new TraineeService();
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setTrainingDao(trainingDao);
        service.setPasswordGenerator(passwordGenerator);
        service.setUsernameService(usernameService);
        service.setGymMetrics(gymMetrics);
        service.setPasswordEncoder(passwordEncoder);
        service.setEventPublisher(eventPublisher);
        sampleTrainee = new Trainee();
        sampleTrainee.setId(1L);
        sampleTrainee.setFirstName("Alice");
        sampleTrainee.setLastName("Smith");
        sampleTrainee.setUsername("Alice.Smith");
        sampleTrainee.setPassword("secretPass1");
        sampleTrainee.setActive(true);
        sampleTrainee.setDateOfBirth(LocalDate.of(1995, 3, 14));
        sampleTrainee.setAddress("10 Main St");
    }

    @Nested
    @DisplayName("createTrainee — Task 2")
    class CreateTrainee {

        @Test
        @DisplayName("security task 4 — password is hashed before persisting; plaintext is only available via getRawPassword()")
        void validInput_persists() {
            when(usernameService.reserveUsername("Alice", "Smith")).thenReturn("Alice.Smith");
            when(passwordGenerator.generate()).thenReturn("pass123456");
            when(passwordEncoder.encode("pass123456")).thenReturn("$2a$hashed-pass123456");
            when(traineeDao.save(any(Trainee.class))).thenAnswer(inv -> {
                Trainee t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            Trainee result = service.createTrainee("Alice", "Smith",
                    LocalDate.of(1995, 3, 14), "10 Main St");

            assertEquals("Alice.Smith", result.getUsername());
            assertEquals("$2a$hashed-pass123456", result.getPassword(), "the hash is what gets persisted");
            assertEquals("pass123456", result.getRawPassword(), "the plaintext is still available once, for the response");
            verify(traineeDao).save(argThat(t -> "$2a$hashed-pass123456".equals(t.getPassword())));
            verify(gymMetrics).incrementTraineeRegistrations();
        }

        @Test
        @DisplayName("blank firstName delegates to UsernameService validation")
        void blankFirstName_throws() {
            doThrow(new IllegalArgumentException("firstName must not be blank"))
                    .when(usernameService).validateName(eq(""), eq("firstName"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.createTrainee("", "Smith", null, null));
            verifyNoInteractions(traineeDao);
        }
    }

    @Nested
    @DisplayName("select* — Task 6")
    class Select {

        @Test
        @DisplayName("selectTraineeByUsername delegates and returns Optional")
        void selectByUsername_delegates() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));
            assertTrue(service.selectTraineeByUsername("Alice.Smith").isPresent());
        }

        @Test
        @DisplayName("selectTrainee(Long) delegates to findById")
        void selectById_delegates() {
            when(traineeDao.findById(1L)).thenReturn(Optional.of(sampleTrainee));
            assertTrue(service.selectTrainee(1L).isPresent());
        }

        @Test
        @DisplayName("selectAllTrainees delegates to findAll")
        void selectAll_delegates() {
            when(traineeDao.findAll()).thenReturn(List.of(sampleTrainee));
            assertEquals(1, service.selectAllTrainees().size());
        }
    }

    @Nested
    @DisplayName("updateTrainee — Task 10")
    class UpdateTrainee {

        @Test
        @DisplayName("existing trainee is merged and returned")
        void existing_updates() {
            when(traineeDao.findById(1L)).thenReturn(Optional.of(sampleTrainee));
            when(traineeDao.update(sampleTrainee)).thenReturn(sampleTrainee);
            assertSame(sampleTrainee, service.updateTrainee(sampleTrainee));
        }

        @Test
        @DisplayName("non-existing id throws EntityNotFoundException")
        void nonExistingId_throws() {
            sampleTrainee.setId(99L);
            when(traineeDao.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> service.updateTrainee(sampleTrainee));
        }
    }

    @Nested
    @DisplayName("activateDeactivate — Task 11")
    class ActivateDeactivate {

        @Test
        @DisplayName("sets active flag directly on Trainee (no getUser() layer)")
        void setsActiveFlagDirectly() {
            sampleTrainee.setActive(true);
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));
            when(traineeDao.update(any())).thenReturn(sampleTrainee);

            Trainee result = service.activateDeactivate("Alice.Smith", false);
            assertFalse(result.isActive());
        }

        @Test
        @DisplayName("unknown username throws EntityNotFoundException")
        void unknownUsername_throws() {
            when(traineeDao.findByUsername("nobody")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> service.activateDeactivate("nobody", true));
        }

        @Test
        @DisplayName("requesting the state the trainee is already in throws InvalidStateException, not a silent no-op (RESTtask note 9)")
        void alreadyInRequestedState_throws() {
            sampleTrainee.setActive(true);
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            assertThrows(InvalidStateException.class,
                    () -> service.activateDeactivate("Alice.Smith", true));
            verify(traineeDao, never()).update(any());
        }
    }

    @Nested
    @DisplayName("delete — Task 13")
    class Delete {

        @Test
        @DisplayName("deleteTrainee(Long) delegates to deleteById")
        void deleteById_delegates() {
            when(trainingDao.findByTraineeId(1L)).thenReturn(List.of());
            service.deleteTrainee(1L);
            verify(traineeDao).deleteById(1L);
        }

        @Test
        @DisplayName("deleteTraineeByUsername delegates to deleteByUsername")
        void deleteByUsername_delegates() {
            when(trainingDao.findByTraineeCriteria("Alice.Smith", null, null, null, null))
                    .thenReturn(List.of());
            service.deleteTraineeByUsername("Alice.Smith");
            verify(traineeDao).deleteByUsername("Alice.Smith");
        }

        @Test
        @DisplayName("a trainee with no trainings publishes no workload event")
        void noTrainings_publishesNoEvent() {
            when(trainingDao.findByTraineeId(1L)).thenReturn(List.of());
            service.deleteTrainee(1L);
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("delete — cascade notifies trainer-workload-service")
    class DeleteCascadeNotification {

        @Test
        @DisplayName("deleteTrainee(Long): trainings are fetched BEFORE the delete call, "
                + "then a single TrainingsRemovedEvent carries all of them")
        void byId_fetchesBeforeDeleteThenPublishes() {
            Training t1 = new Training();
            t1.setId(10L);
            Training t2 = new Training();
            t2.setId(11L);
            when(trainingDao.findByTraineeId(1L)).thenReturn(List.of(t1, t2));

            service.deleteTrainee(1L);

            InOrder order = inOrder(trainingDao, traineeDao, eventPublisher);
            order.verify(trainingDao).findByTraineeId(1L);
            order.verify(traineeDao).deleteById(1L);
            order.verify(eventPublisher).publishEvent(any(TrainingsRemovedEvent.class));

            ArgumentCaptor<TrainingsRemovedEvent> captor = ArgumentCaptor.forClass(TrainingsRemovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(List.of(t1, t2), captor.getValue().getTrainings());
        }

        @Test
        @DisplayName("deleteTraineeByUsername: same fetch-then-delete-then-publish ordering, "
                + "reusing findByTraineeCriteria instead of a new DAO method")
        void byUsername_fetchesBeforeDeleteThenPublishes() {
            Training t1 = new Training();
            t1.setId(20L);
            when(trainingDao.findByTraineeCriteria("Alice.Smith", null, null, null, null))
                    .thenReturn(List.of(t1));

            service.deleteTraineeByUsername("Alice.Smith");

            InOrder order = inOrder(trainingDao, traineeDao, eventPublisher);
            order.verify(trainingDao).findByTraineeCriteria("Alice.Smith", null, null, null, null);
            order.verify(traineeDao).deleteByUsername("Alice.Smith");
            order.verify(eventPublisher).publishEvent(any(TrainingsRemovedEvent.class));

            ArgumentCaptor<TrainingsRemovedEvent> captor = ArgumentCaptor.forClass(TrainingsRemovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(List.of(t1), captor.getValue().getTrainings());
        }

        @Test
        @DisplayName("a trainee with no trainings deletes cleanly and publishes no event")
        void byUsername_noTrainings_publishesNoEvent() {
            when(trainingDao.findByTraineeCriteria("Alice.Smith", null, null, null, null))
                    .thenReturn(List.of());
            service.deleteTraineeByUsername("Alice.Smith");
            verify(traineeDao).deleteByUsername("Alice.Smith");
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("getTraineeTrainings — Task 14")
    class GetTraineeTrainings {

        @Test
        @DisplayName("delegates to TrainingDao with all criteria")
        void delegatesWithAllCriteria() {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);
            when(trainingDao.findByTraineeCriteria(
                    "Alice.Smith", from, to, "Bob", TrainingType.TrainingTypeName.YOGA))
                    .thenReturn(List.of());

            service.getTraineeTrainings("Alice.Smith", from, to, "Bob",
                    TrainingType.TrainingTypeName.YOGA);

            verify(trainingDao).findByTraineeCriteria(
                    "Alice.Smith", from, to, "Bob", TrainingType.TrainingTypeName.YOGA);
        }

        @Test
        @DisplayName("null criteria passes through")
        void nullCriteria_passesThrough() {
            when(trainingDao.findByTraineeCriteria("Alice.Smith", null, null, null, null))
                    .thenReturn(List.of());
            service.getTraineeTrainings("Alice.Smith", null, null, null, null);
            verify(trainingDao).findByTraineeCriteria("Alice.Smith", null, null, null, null);
        }
    }

    @Nested
    @DisplayName("getNotAssignedActiveTrainers — Task 17")
    class NotAssigned {

        @Test
        @DisplayName("delegates to TrainerDao")
        void delegates() {
            when(trainerDao.findActiveNotAssignedToTrainee("Alice.Smith"))
                    .thenReturn(List.of(new Trainer()));
            assertEquals(1, service.getNotAssignedActiveTrainers("Alice.Smith").size());
        }
    }

    @Nested
    @DisplayName("updateTraineeTrainers — Task 18")
    class UpdateTrainers {

        @Test
        @DisplayName("resolves usernames and persists trainer list")
        void resolvesAndPersists() {
            Trainer t1 = new Trainer();
            t1.setId(10L);
            Trainer t2 = new Trainer();
            t2.setId(20L);

            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(t1));
            when(trainerDao.findByUsername("Carol.Lee")).thenReturn(Optional.of(t2));
            when(traineeDao.updateTrainers(eq("Alice.Smith"), anyList())).thenReturn(sampleTrainee);

            assertNotNull(service.updateTraineeTrainers("Alice.Smith",
                    List.of("Bob.Jones", "Carol.Lee")));
            verify(traineeDao).updateTrainers(eq("Alice.Smith"),
                    argThat(list -> list.size() == 2));
        }

        @Test
        @DisplayName("unknown trainer username throws EntityNotFoundException")
        void unknownTrainer_throws() {
            when(trainerDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    service.updateTraineeTrainers("Alice.Smith", List.of("ghost")));
        }
    }

    @Nested
    @DisplayName("selectTraineeProfileByUsername — REST 5")
    class SelectProfile {

        @Test
        @DisplayName("present trainee has its (plain, already-initialized) trainers collection returned untouched")
        void presentTrainee_returnsWithTrainers() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            Optional<Trainee> result = service.selectTraineeProfileByUsername("Alice.Smith");

            assertTrue(result.isPresent());
            assertSame(sampleTrainee, result.get());
        }

        @Test
        @DisplayName("unknown username returns empty Optional")
        void unknownUsername_returnsEmpty() {
            when(traineeDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertTrue(service.selectTraineeProfileByUsername("ghost").isEmpty());
        }
    }

    @Nested
    @DisplayName("updateTraineeProfile — REST 6")
    class UpdateProfile {

        @Test
        @DisplayName("valid update mutates the managed entity's scalar fields and persists")
        void validUpdate_mutatesAndPersists() {
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));
            when(traineeDao.update(sampleTrainee)).thenReturn(sampleTrainee);

            Trainee result = service.updateTraineeProfile("Alice.Smith", "Alicia", "Smyth",
                    LocalDate.of(1996, 1, 1), "22 New Street", false);

            assertEquals("Alicia", result.getFirstName());
            assertEquals("Smyth", result.getLastName());
            assertEquals(LocalDate.of(1996, 1, 1), result.getDateOfBirth());
            assertEquals("22 New Street", result.getAddress());
            assertFalse(result.isActive());
            // same instance mutated in place, not a freshly-built detached Trainee handed to
            // merge() — this is what protects the trainers association from being wiped
            assertSame(sampleTrainee, result);
        }

        @Test
        @DisplayName("does not replace the trainers collection reference")
        void doesNotReplaceTrainersCollection() {
            Object originalTrainersRef = sampleTrainee.getTrainers();
            when(traineeDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));
            when(traineeDao.update(sampleTrainee)).thenReturn(sampleTrainee);

            service.updateTraineeProfile("Alice.Smith", "Alice", "Smith", null, null, true);

            assertSame(originalTrainersRef, sampleTrainee.getTrainers());
        }

        @Test
        @DisplayName("blank firstName delegates to UsernameService validation")
        void blankFirstName_throws() {
            doThrow(new IllegalArgumentException("firstName must not be blank"))
                    .when(usernameService).validateName(eq(""), eq("firstName"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateTraineeProfile("Alice.Smith", "", "Smith", null, null, true));
            verifyNoInteractions(traineeDao);
        }

        @Test
        @DisplayName("unknown username throws EntityNotFoundException")
        void unknownUsername_throws() {
            when(traineeDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    service.updateTraineeProfile("ghost", "A", "B", null, null, true));
        }
    }
}
