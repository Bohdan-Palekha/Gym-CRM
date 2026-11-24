package com.gymcrm.controller;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.dto.request.ActiveStatusRequest;
import com.gymcrm.dto.request.TrainerProfileUpdateRequest;
import com.gymcrm.dto.request.TrainerRegistrationRequest;
import com.gymcrm.dto.response.ErrorResponse;
import com.gymcrm.dto.response.RegistrationResponse;
import com.gymcrm.dto.response.TrainerProfileResponse;
import com.gymcrm.dto.response.TrainerProfileUpdateResponse;
import com.gymcrm.dto.response.TrainerTrainingResponse;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainer;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.TrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

//REST task 2,8,9,13,16
@Slf4j
@RestController
@RequestMapping("/api/trainers")
@RequiredArgsConstructor
@Tag(name = "Trainers", description = "Trainer registration, profile and training management")
public class TrainerController {

    private final TrainerService trainerService;
    private final JwtService jwtService;

    @Operation(summary = "Register a new trainer (REST task 2)",
            description = "Public endpoint — no auth required. Username and password are generated "
                    + "on server, and the account is already logged in: a bearer token is returned "
                    + "alongside the credentials (security task 8).",
            security = {})
    @ApiResponse(responseCode = "201", description = "Trainer created",
            content = @Content(schema = @Schema(implementation = RegistrationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Sensitive
    public ResponseEntity<EntityModel<RegistrationResponse>> register(@Valid @RequestBody TrainerRegistrationRequest request) {
        Trainer created = trainerService.createTrainer(request.firstName(), request.lastName(), request.specialization());
        String token = jwtService.generateToken(created.getUsername());
        RegistrationResponse body = new RegistrationResponse(created.getUsername(), created.getRawPassword(), token);
        EntityModel<RegistrationResponse> model = EntityModel.of(body,
                linkTo(methodOn(TrainerController.class).getProfile(created.getUsername())).withSelfRel());
        return ResponseEntity.created(URI.create("/api/trainers/" + created.getUsername())).body(model);
    }

    @Operation(summary = "Get a trainer's profile (REST task 8)")
    @ApiResponse(responseCode = "200", description = "Profile found",
            content = @Content(schema = @Schema(implementation = TrainerProfileResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainer with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<TrainerProfileResponse>> getProfile(
            @Parameter(description = "Trainer username", required = true) @PathVariable String username) {
        Trainer trainer = trainerService.selectTrainerProfileByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found: " + username));
        return ResponseEntity.ok(withProfileLinks(username, TrainerProfileResponse.from(trainer)));
    }

    @Operation(summary = "Update a trainer's profile (REST task 9)",
            description = "Username is unchangabel, taken from path not body, specialization/username are never part of this request.")
    @ApiResponse(responseCode = "200", description = "Profile updated",
            content = @Content(schema = @Schema(implementation = TrainerProfileUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainer with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<TrainerProfileUpdateResponse>> updateProfile(
            @Parameter(description = "Trainer username", required = true) @PathVariable String username,
            @Valid @RequestBody TrainerProfileUpdateRequest request) {
        Trainer updated = trainerService.updateTrainerProfile(
                username, request.firstName(), request.lastName(), request.isActive());
        return ResponseEntity.ok(withProfileLinks(username, TrainerProfileUpdateResponse.from(updated)));
    }

    @Operation(summary = "Activate or deactivate a trainer (REST task 16)",
            description = "Not idempotent (REST note 9) wanted end state is supplied"
                    + "request that matches the current state are not silently accepted, rejected with 409")
    @ApiResponse(responseCode = "200", description = "State updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainer with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trainer is already in the requested active state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/{username}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainer username", required = true) @PathVariable String username,
            @Valid @RequestBody ActiveStatusRequest request) {
        trainerService.activateDeactivate(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "List a trainer's trainings, with optional filters (REST task 13)")
    @ApiResponse(responseCode = "200", description = "Trainings returned",
            content = @Content(schema = @Schema(implementation = TrainerTrainingResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainer with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/{username}/trainings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<TrainerTrainingResponse>> getTrainings(
            @Parameter(description = "Trainer username", required = true) @PathVariable String username,
            @Parameter(description = "Only trainings on/after this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Only trainings on/before this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Filter by trainee name (partial match)")
            @RequestParam(required = false) String traineeName) {
        trainerService.selectTrainerByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainer not found: " + username));
        List<TrainerTrainingResponse> trainings = trainerService
                .getTrainerTrainings(username, periodFrom, periodTo, traineeName)
                .stream().map(TrainerTrainingResponse::from).toList();
        Link self = linkTo(methodOn(TrainerController.class)
                .getTrainings(username, periodFrom, periodTo, traineeName)).withSelfRel();
        Link trainer = linkTo(methodOn(TrainerController.class).getProfile(username)).withRel("trainer");
        return ResponseEntity.ok(CollectionModel.of(trainings, self, trainer));
    }

    //link set shared by get and update profile, they return full profile representation,
    //but by different response DTO, nav options are identical
    private <T> EntityModel<T> withProfileLinks(String username, T body) {
        return EntityModel.of(body,
                linkTo(methodOn(TrainerController.class).getProfile(username)).withSelfRel(),
                linkTo(methodOn(TrainerController.class).updateProfile(username, null)).withRel("edit"),
                linkTo(methodOn(TrainerController.class)
                        .getTrainings(username, null, null, null)).withRel("trainings"));
    }
}
