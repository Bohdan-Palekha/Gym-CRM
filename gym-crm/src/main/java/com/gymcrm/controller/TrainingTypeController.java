package com.gymcrm.controller;

import com.gymcrm.dto.response.TrainingTypeResponse;
import com.gymcrm.service.TrainingTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

//REST task 17
//by REST note 14 read-only reference data, now requires auth
@Slf4j
@RestController
@RequestMapping("/api/training-types")
@RequiredArgsConstructor
@Tag(name = "Training Types", description = "Read-only training type reference data")
public class TrainingTypeController {

    private final TrainingTypeService trainingTypeService;

    @Operation(summary = "List all training types (REST task 17)")
    @ApiResponse(responseCode = "200", description = "Training types returned",
            content = @Content(schema = @Schema(implementation = TrainingTypeResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<TrainingTypeResponse>> getAll() {
        List<TrainingTypeResponse> types = trainingTypeService.selectAllTrainingTypes()
                .stream().map(TrainingTypeResponse::from).toList();
        return ResponseEntity.ok(CollectionModel.of(types,
                linkTo(methodOn(TrainingTypeController.class).getAll()).withSelfRel()));
    }
}
