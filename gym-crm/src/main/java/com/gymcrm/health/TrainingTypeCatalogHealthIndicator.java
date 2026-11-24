package com.gymcrm.health;

import com.gymcrm.model.TrainingType;
import com.gymcrm.service.TrainingTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//health check for business
//verifies that every training type is present in training refrence
//since table backs trainer reg, missing catalog would break flow and database would still show as healthy
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingTypeCatalogHealthIndicator implements HealthIndicator {

    private final TrainingTypeService trainingTypeService;

    @Override
    public Health health() {
        try {
            List<TrainingType> types = trainingTypeService.selectAllTrainingTypes();
            Set<TrainingType.TrainingTypeName> present = types.stream()
                    .map(TrainingType::getTrainingTypeName)
                    .collect(Collectors.toSet());
            List<TrainingType.TrainingTypeName> missing = Arrays.stream(TrainingType.TrainingTypeName.values())
                    .filter(name -> !present.contains(name))
                    .toList();

            if (!missing.isEmpty()) {
                log.warn("Training type catalog is incomplete, missing={}", missing);
                return Health.down()
                        .withDetail("expectedCount", TrainingType.TrainingTypeName.values().length)
                        .withDetail("actualCount", types.size())
                        .withDetail("missing", missing)
                        .build();
            }
            return Health.up()
                    .withDetail("count", types.size())
                    .build();
        } catch (Exception ex) {
            log.error("Training type catalog health check failed", ex);
            return Health.down(ex).build();
        }
    }
}
