package com.gymcrm.workload.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadResponse;
import com.gymcrm.workload.exception.GlobalExceptionHandler;
import com.gymcrm.workload.exception.TrainerWorkloadNotFoundException;
import com.gymcrm.workload.service.TrainerWorkloadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainerWorkloadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("TrainerWorkloadController")
class TrainerWorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TrainerWorkloadService trainerWorkloadService;

    @Test
    @DisplayName("GET .../years/{year}/months/{month} — returns the monthly summary")
    void getMonthlySummary_returnsSummary() throws Exception {
        when(trainerWorkloadService.getMonthlySummary("bob.trainer", 2026, 8))
                .thenReturn(new MonthlyTrainingHoursResponse("bob.trainer", "Bob", "Jones", true, 2026, 8, 90));

        mockMvc.perform(get("/api/trainer-workloads/bob.trainer/years/2026/months/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(90))
                .andExpect(jsonPath("$.trainerUsername").value("bob.trainer"));
    }

    @Test
    @DisplayName("GET .../years/{year}/months/{month} — unknown trainer returns 404")
    void getMonthlySummary_unknownTrainer_returns404() throws Exception {
        when(trainerWorkloadService.getMonthlySummary("ghost", 2026, 8))
                .thenThrow(new TrainerWorkloadNotFoundException("No workload recorded for trainer: ghost"));

        mockMvc.perform(get("/api/trainer-workloads/ghost/years/2026/months/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No workload recorded for trainer: ghost"));
    }

    @Test
    @DisplayName("GET .../years/{year}/months/{month} — month out of range returns 400")
    void getMonthlySummary_monthOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/trainer-workloads/bob.trainer/years/2026/months/13"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trainerWorkloadService);
    }

    @Test
    @DisplayName("GET /{username} — returns the full nested Years -> Months summary")
    void getFullSummary_returnsSummary() throws Exception {
        when(trainerWorkloadService.getFullSummary("bob.trainer"))
                .thenReturn(new TrainerWorkloadResponse("bob.trainer", "Bob", "Jones", true, List.of()));

        mockMvc.perform(get("/api/trainer-workloads/bob.trainer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerFirstName").value("Bob"));
    }

    @Test
    @DisplayName("GET /{username} — unknown trainer returns 404")
    void getFullSummary_unknownTrainer_returns404() throws Exception {
        when(trainerWorkloadService.getFullSummary("ghost"))
                .thenThrow(new TrainerWorkloadNotFoundException("No workload recorded for trainer: ghost"));

        mockMvc.perform(get("/api/trainer-workloads/ghost"))
                .andExpect(status().isNotFound());
    }
}
