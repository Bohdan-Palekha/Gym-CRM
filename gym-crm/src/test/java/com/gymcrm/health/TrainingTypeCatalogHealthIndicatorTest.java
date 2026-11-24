package com.gymcrm.health;

import com.gymcrm.model.TrainingType;
import com.gymcrm.service.TrainingTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingTypeCatalogHealthIndicator")
class TrainingTypeCatalogHealthIndicatorTest {

    @Mock
    private TrainingTypeService trainingTypeService;

    private TrainingTypeCatalogHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new TrainingTypeCatalogHealthIndicator(trainingTypeService);
    }

    private static TrainingType typeOf(TrainingType.TrainingTypeName name) {
        return new TrainingType(1L, name);
    }

    @Test
    @DisplayName("reports UP when every TrainingTypeName is present")
    void allPresent_reportsUp() {
        List<TrainingType> all = List.of(
                typeOf(TrainingType.TrainingTypeName.FITNESS),
                typeOf(TrainingType.TrainingTypeName.YOGA),
                typeOf(TrainingType.TrainingTypeName.ZUMBA),
                typeOf(TrainingType.TrainingTypeName.STRETCHING),
                typeOf(TrainingType.TrainingTypeName.RESISTANCE));
        when(trainingTypeService.selectAllTrainingTypes()).thenReturn(all);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(5, health.getDetails().get("count"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("reports DOWN and lists the missing types when the catalog is incomplete")
    void someMissing_reportsDown() {
        when(trainingTypeService.selectAllTrainingTypes())
                .thenReturn(List.of(typeOf(TrainingType.TrainingTypeName.YOGA)));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        List<TrainingType.TrainingTypeName> missing =
                (List<TrainingType.TrainingTypeName>) health.getDetails().get("missing");
        assertEquals(4, missing.size());
        assertFalse(missing.contains(TrainingType.TrainingTypeName.YOGA));
    }

    @Test
    @DisplayName("reports DOWN when the catalog is completely empty")
    void empty_reportsDown() {
        when(trainingTypeService.selectAllTrainingTypes()).thenReturn(List.of());

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(0, health.getDetails().get("actualCount"));
    }

    @Test
    @DisplayName("reports DOWN with error detail when the lookup throws")
    void lookupThrows_reportsDown() {
        when(trainingTypeService.selectAllTrainingTypes()).thenThrow(new RuntimeException("db down"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }
}
