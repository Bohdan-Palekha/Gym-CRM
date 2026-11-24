package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.dto.request.ActiveStatusRequest;
import com.gymcrm.dto.request.TraineeProfileUpdateRequest;
import com.gymcrm.dto.request.TraineeRegistrationRequest;
import com.gymcrm.dto.request.TrainerListUpdateRequest;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.TraineeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TraineeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TraineeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TraineeService traineeService;

    @MockBean
    private JwtService jwtService;

    private Trainer trainer;
    private Trainee trainee;

    @BeforeEach
    void setUp() {
        TrainingType yoga = new TrainingType(1L, TrainingType.TrainingTypeName.YOGA);
        trainer = new Trainer();
        trainer.setId(10L);
        trainer.setFirstName("Marcus");
        trainer.setLastName("Lee");
        trainer.setUsername("marcus.lee");
        trainer.setPassword("pw");
        trainer.setActive(true);
        trainer.setSpecialization(yoga);

        trainee = new Trainee();
        trainee.setId(1L);
        trainee.setFirstName("Alice");
        trainee.setLastName("Nguyen");
        trainee.setUsername("alice.nguyen");
        trainee.setPassword("pw");
        trainee.setActive(true);
        trainee.setDateOfBirth(LocalDate.of(1998, 5, 12));
        trainee.setAddress("12 River Road");
        Set<Trainer> trainers = new HashSet<>();
        trainers.add(trainer);
        trainee.setTrainers(trainers);
    }

    @Nested
    @DisplayName("REST task 1 - Trainee Registration")
    class Registration {

        @Test
        @DisplayName("valid request returns 201 with generated credentials and a bearer token")
        void registersTrainee() throws Exception {
            Trainee created = new Trainee();
            created.setUsername("bob.stone");
            created.setPassword("$2a$hashed"); //persistent hash
            created.setRawPassword("gEn3r4ted"); // one-time plaintext for response
            when(traineeService.createTrainee(eq("Bob"), eq("Stone"), any(), any())).thenReturn(created);
            when(jwtService.generateToken("bob.stone")).thenReturn("mocked.jwt.token");

            TraineeRegistrationRequest request = new TraineeRegistrationRequest("Bob", "Stone", null, null);

            mockMvc.perform(post("/api/trainees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/trainees/bob.stone"))
                    .andExpect(jsonPath("$.username").value("bob.stone"))
                    .andExpect(jsonPath("$.password").value("gEn3r4ted"))
                    .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainees/bob.stone")));
        }

        @Test
        @DisplayName("blank first name returns 400")
        void rejectsBlankFirstName() throws Exception {
            TraineeRegistrationRequest request = new TraineeRegistrationRequest(" ", "Stone", null, null);

            mockMvc.perform(post("/api/trainees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));

            verifyNoInteractions(traineeService);
        }

        @Test
        @DisplayName("future date of birth returns 400")
        void rejectsFutureDateOfBirth() throws Exception {
            TraineeRegistrationRequest request =
                    new TraineeRegistrationRequest("Bob", "Stone", LocalDate.now().plusDays(1), null);

            mockMvc.perform(post("/api/trainees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("REST task 5 - Get Trainee Profile")
    class GetProfile {

        @Test
        @DisplayName("existing username returns 200 with profile, no username field")
        void returnsProfile() throws Exception {
            when(traineeService.selectTraineeProfileByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));

            mockMvc.perform(get("/api/trainees/alice.nguyen"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").doesNotExist())
                    .andExpect(jsonPath("$.firstName").value("Alice"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.trainers[0].username").value("marcus.lee"))
                    .andExpect(jsonPath("$.trainers[0].specialization").value("YOGA"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainees/alice.nguyen")))
                    .andExpect(jsonPath("$._links.edit.href", containsString("/api/trainees/alice.nguyen")))
                    .andExpect(jsonPath("$._links.delete.href", containsString("/api/trainees/alice.nguyen")))
                    .andExpect(jsonPath("$._links.trainings.href", containsString("/api/trainees/alice.nguyen/trainings")))
                    .andExpect(jsonPath("$._links.trainers.href", containsString("/api/trainees/alice.nguyen/trainers")))
                    .andExpect(jsonPath("$._links['unassigned-trainers'].href",
                            containsString("/api/trainees/alice.nguyen/unassigned-trainers")));
        }

        @Test
        @DisplayName("unknown username returns 404")
        void unknownUsernameReturns404() throws Exception {
            when(traineeService.selectTraineeProfileByUsername("nobody")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/trainees/nobody"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("REST task 6 - Update Trainee Profile")
    class UpdateProfile {

        @Test
        @DisplayName("valid update returns 200 with username included")
        void updatesProfile() throws Exception {
            when(traineeService.updateTraineeProfile(eq("alice.nguyen"), eq("Alice"), eq("N."),
                    any(), any(), eq(false))).thenReturn(trainee);

            TraineeProfileUpdateRequest request =
                    new TraineeProfileUpdateRequest("Alice", "N.", null, null, false);

            mockMvc.perform(put("/api/trainees/alice.nguyen")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice.nguyen"));
        }

        @Test
        @DisplayName("missing isActive returns 400")
        void missingIsActiveReturns400() throws Exception {
            String json = """
                    {"firstName":"Alice","lastName":"Nguyen"}
                    """;

            mockMvc.perform(put("/api/trainees/alice.nguyen")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("unknown username returns 404")
        void unknownUsernameReturns404() throws Exception {
            when(traineeService.updateTraineeProfile(eq("nobody"), any(), any(), any(), any(), anyBoolean()))
                    .thenThrow(new EntityNotFoundException("Trainee not found: nobody"));

            TraineeProfileUpdateRequest request = new TraineeProfileUpdateRequest("A", "B", null, null, true);

            mockMvc.perform(put("/api/trainees/nobody")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("REST task 7 - Delete Trainee Profile")
    class Delete {

        @Test
        @DisplayName("existing username returns 200")
        void deletesTrainee() throws Exception {
            mockMvc.perform(delete("/api/trainees/alice.nguyen"))
                    .andExpect(status().isOk());
            verify(traineeService).deleteTraineeByUsername("alice.nguyen");
        }

        @Test
        @DisplayName("unknown username returns 404")
        void unknownUsernameReturns404() throws Exception {
            doThrow(new EntityNotFoundException("Trainee not found: nobody"))
                    .when(traineeService).deleteTraineeByUsername("nobody");

            mockMvc.perform(delete("/api/trainees/nobody"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("REST task 15 - Activate/De-activate Trainee")
    class ActivateDeactivate {

        @Test
        @DisplayName("valid request returns 200")
        void activatesTrainee() throws Exception {
            ActiveStatusRequest request = new ActiveStatusRequest(false);

            mockMvc.perform(patch("/api/trainees/alice.nguyen/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            verify(traineeService).activateDeactivate("alice.nguyen", false);
        }

        @Test
        @DisplayName("missing isActive returns 400")
        void missingIsActiveReturns400() throws Exception {
            mockMvc.perform(patch("/api/trainees/alice.nguyen/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("already-in-that-state returns 409, not a silent 200 (RESTtask note 9: not idempotent)")
        void alreadyInStateReturns409() throws Exception {
            doThrow(new InvalidStateException("User 'alice.nguyen' is already active."))
                    .when(traineeService).activateDeactivate("alice.nguyen", true);

            ActiveStatusRequest request = new ActiveStatusRequest(true);

            mockMvc.perform(patch("/api/trainees/alice.nguyen/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("HTTP semantics: unsupported method/content-type must not surface as a 500")
    class HttpSemantics {

        @Test
        @DisplayName("wrong HTTP method on a mapped path returns 405, not 500")
        void wrongMethodReturns405() throws Exception {
            mockMvc.perform(get("/api/trainees"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("unsupported content type on registration returns 415, not 500")
        void unsupportedMediaTypeReturns415() throws Exception {
            mockMvc.perform(post("/api/trainees")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("not json"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("a path matching no mapping at all returns 404, not 500")
        void unmappedPathReturns404() throws Exception {
            mockMvc.perform(delete("/api/trainings/1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("REST task 12 - Get Trainee Trainings List")
    class Trainings {

        @Test
        @DisplayName("returns trainings mapped to trainer full name")
        void returnsTrainings() throws Exception {
            when(traineeService.selectTraineeByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));

            Training training = new Training();
            training.setTrainingName("Morning Yoga");
            training.setTrainingDate(LocalDate.of(2026, 8, 1));
            training.setDurationMinutes(60);
            training.setTrainingType(trainer.getSpecialization());
            training.setTrainer(trainer);
            training.setTrainee(trainee);

            when(traineeService.getTraineeTrainings(eq("alice.nguyen"), any(), any(), any(), any()))
                    .thenReturn(List.of(training));

            mockMvc.perform(get("/api/trainees/alice.nguyen/trainings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.trainings[0].trainingName").value("Morning Yoga"))
                    .andExpect(jsonPath("$._embedded.trainings[0].trainerName").value("Marcus Lee"))
                    .andExpect(jsonPath("$._embedded.trainings[0].trainingType").value("YOGA"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainees/alice.nguyen/trainings")))
                    .andExpect(jsonPath("$._links.trainee.href", containsString("/api/trainees/alice.nguyen")));
        }

        @Test
        @DisplayName("unknown trainee returns 404 without querying trainings")
        void unknownTraineeReturns404() throws Exception {
            when(traineeService.selectTraineeByUsername("nobody")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/trainees/nobody/trainings"))
                    .andExpect(status().isNotFound());

            verify(traineeService, never()).getTraineeTrainings(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("invalid trainingType query param returns 400")
        void invalidTrainingTypeReturns400() throws Exception {
            when(traineeService.selectTraineeByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));

            mockMvc.perform(get("/api/trainees/alice.nguyen/trainings").param("trainingType", "NOT_A_TYPE"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("REST task 10 - Get not-assigned active trainers")
    class UnassignedTrainers {

        @Test
        @DisplayName("returns trainer summaries")
        void returnsUnassignedTrainers() throws Exception {
            when(traineeService.selectTraineeByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));
            when(traineeService.getNotAssignedActiveTrainers("alice.nguyen")).thenReturn(List.of(trainer));

            mockMvc.perform(get("/api/trainees/alice.nguyen/unassigned-trainers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.trainers[0].username").value("marcus.lee"))
                    .andExpect(jsonPath("$._embedded.trainers[0].specialization").value("YOGA"));
        }

        @Test
        @DisplayName("unknown trainee returns 404")
        void unknownTraineeReturns404() throws Exception {
            when(traineeService.selectTraineeByUsername("nobody")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/trainees/nobody/unassigned-trainers"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("REST task 11 - Update Trainee's Trainer List")
    class UpdateTrainerList {

        @Test
        @DisplayName("valid list returns 200 with updated trainers")
        void updatesTrainerList() throws Exception {
            when(traineeService.updateTraineeTrainers(eq("alice.nguyen"), anyList())).thenReturn(trainee);

            TrainerListUpdateRequest request = new TrainerListUpdateRequest(List.of("marcus.lee"));

            mockMvc.perform(put("/api/trainees/alice.nguyen/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.trainers[0].username").value("marcus.lee"));
        }

        @Test
        @DisplayName("empty trainer list returns 400")
        void emptyListReturns400() throws Exception {
            TrainerListUpdateRequest request = new TrainerListUpdateRequest(List.of());

            mockMvc.perform(put("/api/trainees/alice.nguyen/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(traineeService);
        }

        @Test
        @DisplayName("unknown trainer username returns 404")
        void unknownTrainerReturns404() throws Exception {
            when(traineeService.updateTraineeTrainers(eq("alice.nguyen"), anyList()))
                    .thenThrow(new EntityNotFoundException("Trainer not found: ghost"));

            TrainerListUpdateRequest request = new TrainerListUpdateRequest(List.of("ghost"));

            mockMvc.perform(put("/api/trainees/alice.nguyen/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
