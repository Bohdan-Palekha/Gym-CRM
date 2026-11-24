package com.gymcrm.service;

import com.gymcrm.dao.TrainerDao;
import com.gymcrm.dao.TrainingDao;
import com.gymcrm.dao.TrainingTypeDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.util.PasswordGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerService")
class TrainerServiceTest {

    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private UsernameService usernameService;
    @Mock
    private GymMetrics gymMetrics;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TrainerService service;
    private Trainer sampleTrainer;
    private TrainingType yogaType;

    @BeforeEach
    void setUp() {
        service = new TrainerService();
        service.setTrainerDao(trainerDao);
        service.setTrainingDao(trainingDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setPasswordGenerator(passwordGenerator);
        service.setUsernameService(usernameService);
        service.setGymMetrics(gymMetrics);
        service.setPasswordEncoder(passwordEncoder);
        yogaType = new TrainingType(1L, TrainingType.TrainingTypeName.YOGA);
        sampleTrainer = new Trainer();
        sampleTrainer.setId(2L);
        sampleTrainer.setFirstName("Bob");
        sampleTrainer.setLastName("Jones");
        sampleTrainer.setUsername("Bob.Jones");
        sampleTrainer.setPassword("trainerPass1");
        sampleTrainer.setActive(true);
        sampleTrainer.setSpecialization(yogaType);
    }

    @Nested
    @DisplayName("createTrainer — Task 1")
    class CreateTrainer {

        @Test
        @DisplayName("security task 4 — password is hashed before persisting; plaintext is only available via getRawPassword()")
        void validInput_persists() {
            when(usernameService.reserveUsername("Bob", "Jones")).thenReturn("Bob.Jones");
            when(passwordGenerator.generate()).thenReturn("gen123456");
            when(passwordEncoder.encode("gen123456")).thenReturn("$2a$hashed-gen123456");
            when(trainingTypeDao.findByName(TrainingType.TrainingTypeName.YOGA))
                    .thenReturn(Optional.of(yogaType));
            when(trainerDao.save(any())).thenAnswer(inv -> {
                Trainer t = inv.getArgument(0);
                t.setId(2L);
                return t;
            });

            Trainer result = service.createTrainer("Bob", "Jones",
                    TrainingType.TrainingTypeName.YOGA);

            assertEquals("Bob.Jones", result.getUsername());
            assertEquals(TrainingType.TrainingTypeName.YOGA,
                    result.getSpecialization().getTrainingTypeName());
            assertEquals("$2a$hashed-gen123456", result.getPassword(), "the hash is what gets persisted");
            assertEquals("gen123456", result.getRawPassword(), "the plaintext is still available once, for the response");
            verify(trainerDao).save(argThat(t -> "$2a$hashed-gen123456".equals(t.getPassword())));
            verify(gymMetrics).incrementTrainerRegistrations();
        }

        @Test
        @DisplayName("null specialization throws IllegalArgumentException")
        void nullSpecialization_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.createTrainer("Bob", "Jones", null));
            verifyNoInteractions(trainerDao);
        }

        @Test
        @DisplayName("specialization not in DB throws EntityNotFoundException")
        void specializationNotFound_throws() {
            when(trainingTypeDao.findByName(TrainingType.TrainingTypeName.FITNESS))
                    .thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    service.createTrainer("Bob", "Jones", TrainingType.TrainingTypeName.FITNESS));
        }
    }

    @Nested
    @DisplayName("select* — Task 5")
    class Select {

        @Test
        @DisplayName("selectTrainerByUsername returns matching trainer")
        void selectByUsername_returnsTrainer() {
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));
            assertTrue(service.selectTrainerByUsername("Bob.Jones").isPresent());
        }

        @Test
        @DisplayName("selectTrainer(Long) delegates to findById")
        void selectById_delegates() {
            when(trainerDao.findById(2L)).thenReturn(Optional.of(sampleTrainer));
            assertTrue(service.selectTrainer(2L).isPresent());
        }

        @Test
        @DisplayName("selectAllTrainers delegates to findAll")
        void selectAll_delegates() {
            when(trainerDao.findAll()).thenReturn(List.of(sampleTrainer));
            assertEquals(1, service.selectAllTrainers().size());
        }
    }

    @Nested
    @DisplayName("updateTrainer — Task 9")
    class UpdateTrainer {

        @Test
        @DisplayName("existing trainer is merged and returned")
        void existing_updates() {
            when(trainerDao.findById(2L)).thenReturn(Optional.of(sampleTrainer));
            when(trainerDao.update(sampleTrainer)).thenReturn(sampleTrainer);
            assertSame(sampleTrainer, service.updateTrainer(sampleTrainer));
        }

        @Test
        @DisplayName("non-existing id throws EntityNotFoundException")
        void nonExistingId_throws() {
            sampleTrainer.setId(99L);
            when(trainerDao.findById(99L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> service.updateTrainer(sampleTrainer));
        }
    }

    @Nested
    @DisplayName("activateDeactivate — Task 12")
    class ActivateDeactivate {

        @Test
        @DisplayName("sets active flag directly on Trainer (no getUser() layer)")
        void setsActiveFlagDirectly() {
            sampleTrainer.setActive(true);
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));
            when(trainerDao.update(any())).thenReturn(sampleTrainer);
            assertFalse(service.activateDeactivate("Bob.Jones", false).isActive());
        }

        @Test
        @DisplayName("unknown username throws EntityNotFoundException")
        void unknownUsername_throws() {
            when(trainerDao.findByUsername("nobody")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> service.activateDeactivate("nobody", true));
        }

        @Test
        @DisplayName("requesting the state the trainer is already in throws InvalidStateException, not a silent no-op (RESTtask note 9)")
        void alreadyInRequestedState_throws() {
            sampleTrainer.setActive(true);
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));

            assertThrows(InvalidStateException.class,
                    () -> service.activateDeactivate("Bob.Jones", true));
            verify(trainerDao, never()).update(any());
        }
    }

    @Nested
    @DisplayName("getTrainerTrainings — Task 15")
    class GetTrainerTrainings {

        @Test
        @DisplayName("delegates to TrainingDao with all criteria")
        void delegatesWithAllCriteria() {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 6, 30);
            when(trainingDao.findByTrainerCriteria("Bob.Jones", from, to, "Alice"))
                    .thenReturn(List.of());
            service.getTrainerTrainings("Bob.Jones", from, to, "Alice");
            verify(trainingDao).findByTrainerCriteria("Bob.Jones", from, to, "Alice");
        }

        @Test
        @DisplayName("null criteria passes through")
        void nullCriteria_passesThrough() {
            when(trainingDao.findByTrainerCriteria("Bob.Jones", null, null, null))
                    .thenReturn(List.of());
            service.getTrainerTrainings("Bob.Jones", null, null, null);
        }
    }

    @Nested
    @DisplayName("selectTrainerProfileByUsername — REST 8")
    class SelectProfile {

        @Test
        @DisplayName("present trainer is returned")
        void presentTrainer_returned() {
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));
            assertSame(sampleTrainer, service.selectTrainerProfileByUsername("Bob.Jones").orElseThrow());
        }

        @Test
        @DisplayName("unknown username returns empty Optional")
        void unknownUsername_returnsEmpty() {
            when(trainerDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertTrue(service.selectTrainerProfileByUsername("ghost").isEmpty());
        }
    }

    @Nested
    @DisplayName("updateTrainerProfile — REST 9")
    class UpdateProfile {

        @Test
        @DisplayName("valid update mutates scalar fields but never touches specialization")
        void validUpdate_neverTouchesSpecialization() {
            when(trainerDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));
            when(trainerDao.update(sampleTrainer)).thenReturn(sampleTrainer);

            Trainer result = service.updateTrainerProfile("Bob.Jones", "Robert", "Jones", false);

            assertEquals("Robert", result.getFirstName());
            assertFalse(result.isActive());
            assertEquals(TrainingType.TrainingTypeName.YOGA,
                    result.getSpecialization().getTrainingTypeName());
            assertSame(yogaType, result.getSpecialization());
        }

        @Test
        @DisplayName("blank lastName delegates to UsernameService validation")
        void blankLastName_throws() {
            doNothing().when(usernameService).validateName("Bob", "firstName");
            doThrow(new IllegalArgumentException("lastName must not be blank"))
                    .when(usernameService).validateName(eq(""), eq("lastName"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateTrainerProfile("Bob.Jones", "Bob", "", true));
            verifyNoInteractions(trainerDao);
        }

        @Test
        @DisplayName("unknown username throws EntityNotFoundException")
        void unknownUsername_throws() {
            when(trainerDao.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () ->
                    service.updateTrainerProfile("ghost", "A", "B", true));
        }
    }
}
