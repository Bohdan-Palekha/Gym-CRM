package com.gymcrm.controller;

import com.gymcrm.model.TrainingType;
import com.gymcrm.service.TrainingTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainingTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrainingTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainingTypeService trainingTypeService;

    @Test
    @DisplayName("returns all training types, HATEOAS-wrapped (REST task 17)")
    void returnsAllTrainingTypes() throws Exception {
        when(trainingTypeService.selectAllTrainingTypes()).thenReturn(List.of(
                new TrainingType(1L, TrainingType.TrainingTypeName.FITNESS),
                new TrainingType(2L, TrainingType.TrainingTypeName.YOGA)));

        mockMvc.perform(get("/api/training-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.trainingTypes[0].trainingType").value("FITNESS"))
                .andExpect(jsonPath("$._embedded.trainingTypes[0].trainingTypeId").value(1))
                .andExpect(jsonPath("$._embedded.trainingTypes[1].trainingType").value("YOGA"))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/training-types")));
    }

    @Test
    @DisplayName("empty list still returns 200 with a self link")
    void emptyListReturns200() throws Exception {
        when(trainingTypeService.selectAllTrainingTypes()).thenReturn(List.of());

        mockMvc.perform(get("/api/training-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href", containsString("/api/training-types")));
    }
}
