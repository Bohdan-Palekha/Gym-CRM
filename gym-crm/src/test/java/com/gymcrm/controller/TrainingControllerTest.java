package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.dto.request.AddTrainingRequest;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.service.TrainingService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingService trainingService;

    @Nested
    @DisplayName("REST task 14 - Add Training")
    class AddTraining {

        @Test
        @DisplayName("valid request returns 200 (per spec, not 201)")
        void addsTraining() throws Exception {
            AddTrainingRequest request = new AddTrainingRequest(
                    "alice.nguyen", "marcus.lee", "Morning Yoga", LocalDate.of(2026, 8, 1), 60);

            mockMvc.perform(post("/api/trainings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(trainingService).addTraining(
                    "alice.nguyen", "marcus.lee", "Morning Yoga", LocalDate.of(2026, 8, 1), 60);
        }

        @Test
        @DisplayName("no Training Type field is accepted - it is derived server-side")
        void trainingTypeFieldIsIgnoredIfSent() throws Exception {
            String json = """
                    {"traineeUsername":"alice.nguyen","trainerUsername":"marcus.lee",
                     "trainingName":"Morning Yoga","trainingDate":"2026-08-01",
                     "trainingDuration":60,"trainingType":"ZUMBA"}
                    """;

            mockMvc.perform(post("/api/trainings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            verify(trainingService).addTraining(
                    "alice.nguyen", "marcus.lee", "Morning Yoga", LocalDate.of(2026, 8, 1), 60);
        }

        @Test
        @DisplayName("non-positive duration returns 400")
        void nonPositiveDurationReturns400() throws Exception {
            AddTrainingRequest request = new AddTrainingRequest(
                    "alice.nguyen", "marcus.lee", "Morning Yoga", LocalDate.of(2026, 8, 1), 0);

            mockMvc.perform(post("/api/trainings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(trainingService);
        }

        @Test
        @DisplayName("unknown trainee or trainer returns 404")
        void unknownParticipantReturns404() throws Exception {
            when(trainingService.addTraining(anyString(), anyString(), anyString(), any(), anyInt()))
                    .thenThrow(new EntityNotFoundException("Trainer not found: ghost"));

            AddTrainingRequest request = new AddTrainingRequest(
                    "alice.nguyen", "ghost", "Morning Yoga", LocalDate.of(2026, 8, 1), 60);

            mockMvc.perform(post("/api/trainings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
