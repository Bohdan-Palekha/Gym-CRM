package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.dto.request.ActiveStatusRequest;
import com.gymcrm.dto.request.TrainerProfileUpdateRequest;
import com.gymcrm.dto.request.TrainerRegistrationRequest;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.TrainerService;
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

@WebMvcTest(TrainerController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainerService trainerService;

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

        Set<Trainee> trainees = new HashSet<>();
        trainees.add(trainee);
        trainer.setTrainees(trainees);
    }

    @Nested
    @DisplayName("REST task 2 - Trainer Registration")
    class Registration {

        @Test
        @DisplayName("valid request returns 201 with generated credentials and a bearer token")
        void registersTrainer() throws Exception {
            Trainer created = new Trainer();
            created.setUsername("carol.davis");
            created.setPassword("$2a$hashed");
            created.setRawPassword("gEn3r4ted");
            when(trainerService.createTrainer(eq("Carol"), eq("Davis"), eq(TrainingType.TrainingTypeName.ZUMBA)))
                    .thenReturn(created);
            when(jwtService.generateToken("carol.davis")).thenReturn("mocked.jwt.token");

            TrainerRegistrationRequest request =
                    new TrainerRegistrationRequest("Carol", "Davis", TrainingType.TrainingTypeName.ZUMBA);

            mockMvc.perform(post("/api/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/trainers/carol.davis"))
                    .andExpect(jsonPath("$.username").value("carol.davis"))
                    .andExpect(jsonPath("$.password").value("gEn3r4ted"))
                    .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainers/carol.davis")));
        }

        @Test
        @DisplayName("missing specialization returns 400")
        void missingSpecializationReturns400() throws Exception {
            String json = """
                    {"firstName":"Carol","lastName":"Davis"}
                    """;

            mockMvc.perform(post("/api/trainers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("specialization"));

            verifyNoInteractions(trainerService);
        }
    }

    @Nested
    @DisplayName("REST task 8 - Get Trainer Profile")
    class GetProfile {

        @Test
        @DisplayName("existing username returns 200 with profile, no username field")
        void returnsProfile() throws Exception {
            when(trainerService.selectTrainerProfileByUsername("marcus.lee")).thenReturn(Optional.of(trainer));

            mockMvc.perform(get("/api/trainers/marcus.lee"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").doesNotExist())
                    .andExpect(jsonPath("$.firstName").value("Marcus"))
                    .andExpect(jsonPath("$.specialization").value("YOGA"))
                    .andExpect(jsonPath("$.trainees[0].username").value("alice.nguyen"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainers/marcus.lee")))
                    .andExpect(jsonPath("$._links.edit.href", containsString("/api/trainers/marcus.lee")))
                    .andExpect(jsonPath("$._links.trainings.href", containsString("/api/trainers/marcus.lee/trainings")));
        }

        @Test
        @DisplayName("unknown username returns 404")
        void unknownUsernameReturns404() throws Exception {
            when(trainerService.selectTrainerProfileByUsername("nobody")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/trainers/nobody"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("REST task 9 - Update Trainer Profile")
    class UpdateProfile {

        @Test
        @DisplayName("valid update returns 200 with username included, specialization untouched")
        void updatesProfile() throws Exception {
            when(trainerService.updateTrainerProfile("marcus.lee", "Marcus", "Lee", false)).thenReturn(trainer);

            TrainerProfileUpdateRequest request = new TrainerProfileUpdateRequest("Marcus", "Lee", false);

            mockMvc.perform(put("/api/trainers/marcus.lee")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("marcus.lee"))
                    .andExpect(jsonPath("$.specialization").value("YOGA"));

            verify(trainerService).updateTrainerProfile("marcus.lee", "Marcus", "Lee", false);
        }

        @Test
        @DisplayName("a specialization field in the body is silently ignored, not passed to the service")
        void specializationInBodyIsIgnored() throws Exception {
            when(trainerService.updateTrainerProfile(anyString(), anyString(), anyString(), anyBoolean()))
                    .thenReturn(trainer);

            String json = """
                    {"firstName":"Marcus","lastName":"Lee","isActive":true,"specialization":"ZUMBA"}
                    """;

            mockMvc.perform(put("/api/trainers/marcus.lee")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
            verify(trainerService).updateTrainerProfile("marcus.lee", "Marcus", "Lee", true);
        }
    }

    @Nested
    @DisplayName("REST task 16 - Activate/De-activate Trainer")
    class ActivateDeactivate {

        @Test
        @DisplayName("valid request returns 200")
        void activatesTrainer() throws Exception {
            ActiveStatusRequest request = new ActiveStatusRequest(false);

            mockMvc.perform(patch("/api/trainers/marcus.lee/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            verify(trainerService).activateDeactivate("marcus.lee", false);
        }

        @Test
        @DisplayName("unknown username returns 404")
        void unknownUsernameReturns404() throws Exception {
            doThrow(new EntityNotFoundException("Trainer not found: nobody"))
                    .when(trainerService).activateDeactivate(eq("nobody"), anyBoolean());

            ActiveStatusRequest request = new ActiveStatusRequest(true);

            mockMvc.perform(patch("/api/trainers/nobody/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("already-in-that-state returns 409, not a silent 200 (REST task note 9: not idempotent)")
        void alreadyInStateReturns409() throws Exception {
            doThrow(new InvalidStateException("User 'marcus.lee' is already inactive."))
                    .when(trainerService).activateDeactivate("marcus.lee", false);

            ActiveStatusRequest request = new ActiveStatusRequest(false);

            mockMvc.perform(patch("/api/trainers/marcus.lee/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("REST task 13 - Get Trainer Trainings List")
    class Trainings {

        @Test
        @DisplayName("returns trainings mapped to trainee full name")
        void returnsTrainings() throws Exception {
            when(trainerService.selectTrainerByUsername("marcus.lee")).thenReturn(Optional.of(trainer));

            Training training = new Training();
            training.setTrainingName("Morning Yoga");
            training.setTrainingDate(LocalDate.of(2026, 8, 1));
            training.setDurationMinutes(60);
            training.setTrainingType(trainer.getSpecialization());
            training.setTrainer(trainer);
            training.setTrainee(trainee);

            when(trainerService.getTrainerTrainings(eq("marcus.lee"), any(), any(), any()))
                    .thenReturn(List.of(training));

            mockMvc.perform(get("/api/trainers/marcus.lee/trainings")
                            .param("periodFrom", "2026-01-01")
                            .param("periodTo", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.trainings[0].trainingName").value("Morning Yoga"))
                    .andExpect(jsonPath("$._embedded.trainings[0].traineeName").value("Alice Nguyen"))
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/trainers/marcus.lee/trainings")))
                    .andExpect(jsonPath("$._links.trainer.href", containsString("/api/trainers/marcus.lee")));
        }

        @Test
        @DisplayName("unknown trainer returns 404 without querying trainings")
        void unknownTrainerReturns404() throws Exception {
            when(trainerService.selectTrainerByUsername("nobody")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/trainers/nobody/trainings"))
                    .andExpect(status().isNotFound());

            verify(trainerService, never()).getTrainerTrainings(any(), any(), any(), any());
        }
    }
}
