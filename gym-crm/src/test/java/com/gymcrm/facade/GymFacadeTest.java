package com.gymcrm.facade;

import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.service.AuthenticationService;
import com.gymcrm.service.TraineeService;
import com.gymcrm.service.TrainerService;
import com.gymcrm.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GymFacade")
class GymFacadeTest {

    @Mock private TraineeService traineeService;
    @Mock private TrainerService trainerService;
    @Mock private TrainingService trainingService;
    @Mock private AuthenticationService authenticationService;

    private GymCrmFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(traineeService, trainerService, trainingService, authenticationService);
    }

    @Test
    @DisplayName("GymFacade implements GymCrmFacade interface")
    void facade_implementsGymCrmFacade() {
        assertInstanceOf(GymCrmFacade.class, facade);
    }

    @Nested
    @DisplayName("Trainee operations")
    class TraineeOps {

        @Test
        @DisplayName("createTrainee delegates to TraineeService")
        void createTrainee_delegates() {
            Trainee expected = new Trainee(); expected.setId(1L);
            LocalDate dob = LocalDate.of(1990, 1, 1);
            when(traineeService.createTrainee("Alice", "Brown", dob, "Oak Ave"))
                    .thenReturn(expected);
            assertEquals(expected, facade.createTrainee("Alice", "Brown", dob, "Oak Ave"));
            verify(traineeService).createTrainee("Alice", "Brown", dob, "Oak Ave");
        }

        @Test
        @DisplayName("selectTraineeByUsername delegates to TraineeService")
        void selectByUsername_delegates() {
            Trainee t = new Trainee();
            when(traineeService.selectTraineeByUsername("Alice.Brown"))
                    .thenReturn(Optional.of(t));
            assertTrue(facade.selectTraineeByUsername("Alice.Brown").isPresent());
        }

        @Test
        @DisplayName("selectTraineeByUsername propagates empty Optional")
        void selectByUsername_empty() {
            when(traineeService.selectTraineeByUsername("No.Such"))
                    .thenReturn(Optional.empty());
            assertTrue(facade.selectTraineeByUsername("No.Such").isEmpty());
        }

        @Test
        @DisplayName("updateTrainee delegates to TraineeService")
        void updateTrainee_delegates() {
            Trainee t = new Trainee(); t.setId(1L);
            when(traineeService.updateTrainee(t)).thenReturn(t);
            assertEquals(t, facade.updateTrainee(t));
        }

        @Test
        @DisplayName("deleteTrainee delegates to TraineeService")
        void deleteTrainee_delegates() {
            facade.deleteTrainee(1L);
            verify(traineeService).deleteTrainee(1L);
        }

        @Test
        @DisplayName("selectAllTrainees delegates to TraineeService")
        void selectAll_delegates() {
            when(traineeService.selectAllTrainees())
                    .thenReturn(List.of(new Trainee(), new Trainee()));
            assertEquals(2, facade.selectAllTrainees().size());
        }
    }

    @Nested
    @DisplayName("Trainer operations")
    class TrainerOps {

        @Test
        @DisplayName("createTrainer passes TrainingTypeName enum to TrainerService")
        void createTrainer_passesEnum() {
            Trainer expected = new Trainer(); expected.setId(1L);
            when(trainerService.createTrainer("Jane", "Doe",
                    TrainingType.TrainingTypeName.YOGA)).thenReturn(expected);

            Trainer result = facade.createTrainer("Jane", "Doe",
                    TrainingType.TrainingTypeName.YOGA);
            assertEquals(expected, result);
            verify(trainerService).createTrainer("Jane", "Doe",
                    TrainingType.TrainingTypeName.YOGA);
        }

        @Test
        @DisplayName("selectTrainerByUsername delegates to TrainerService")
        void selectByUsername_delegates() {
            Trainer t = new Trainer();
            when(trainerService.selectTrainerByUsername("Jane.Doe"))
                    .thenReturn(Optional.of(t));
            assertTrue(facade.selectTrainerByUsername("Jane.Doe").isPresent());
        }

        @Test
        @DisplayName("selectTrainerByUsername propagates empty Optional")
        void selectByUsername_empty() {
            when(trainerService.selectTrainerByUsername("No.Such"))
                    .thenReturn(Optional.empty());
            assertTrue(facade.selectTrainerByUsername("No.Such").isEmpty());
        }

        @Test
        @DisplayName("updateTrainer delegates to TrainerService")
        void updateTrainer_delegates() {
            Trainer t = new Trainer(); t.setId(2L);
            when(trainerService.updateTrainer(t)).thenReturn(t);
            assertEquals(t, facade.updateTrainer(t));
        }

        @Test
        @DisplayName("selectAllTrainers delegates to TrainerService")
        void selectAll_delegates() {
            when(trainerService.selectAllTrainers())
                    .thenReturn(List.of(new Trainer()));
            assertEquals(1, facade.selectAllTrainers().size());
        }
    }

    @Nested
    @DisplayName("Training operations")
    class TrainingOps {

        @Test
        @DisplayName("createTraining delegates with all arguments")
        void createTraining_delegates() {
            Training expected = new Training(); expected.setId(1L);
            TrainingType type = new TrainingType(null, TrainingType.TrainingTypeName.YOGA);
            LocalDate    date = LocalDate.of(2024, 3, 15);
            when(trainingService.createTraining(1L, 2L, "Yoga", type, date, 60))
                    .thenReturn(expected);
            assertEquals(expected,
                    facade.createTraining(1L, 2L, "Yoga", type, date, 60));
        }

        @Test
        @DisplayName("selectTrainingsByTrainee delegates to TrainingService")
        void byTrainee_delegates() {
            when(trainingService.selectTrainingsByTrainee(1L))
                    .thenReturn(List.of(new Training()));
            assertEquals(1, facade.selectTrainingsByTrainee(1L).size());
        }

        @Test
        @DisplayName("selectTrainingsByTrainer delegates to TrainingService")
        void byTrainer_delegates() {
            when(trainingService.selectTrainingsByTrainer(2L))
                    .thenReturn(List.of(new Training(), new Training()));
            assertEquals(2, facade.selectTrainingsByTrainer(2L).size());
        }
    }

    @Nested
    @DisplayName("Auth operations")
    class AuthOps {

        @Test
        @DisplayName("authenticateTrainee delegates to AuthenticationService")
        void authenticateTrainee_delegates() {
            facade.authenticateTrainee("alice.nguyen", "pw");
            verify(authenticationService).authenticate("alice.nguyen", "pw");
        }

        @Test
        @DisplayName("changeTraineePassword delegates to AuthenticationService")
        void changeTraineePassword_delegates() {
            facade.changeTraineePassword("alice.nguyen", "old", "new");
            verify(authenticationService).changePassword("alice.nguyen", "old", "new");
        }

        @Test
        @DisplayName("authenticateTrainer delegates to AuthenticationService")
        void authenticateTrainer_delegates() {
            facade.authenticateTrainer("bob.jones", "pw");
            verify(authenticationService).authenticate("bob.jones", "pw");
        }

        @Test
        @DisplayName("changeTrainerPassword delegates to AuthenticationService")
        void changeTrainerPassword_delegates() {
            facade.changeTrainerPassword("bob.jones", "old", "new");
            verify(authenticationService).changePassword("bob.jones", "old", "new");
        }
    }
}
