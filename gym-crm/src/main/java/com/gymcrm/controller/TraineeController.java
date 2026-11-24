package com.gymcrm.controller;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.dto.request.ActiveStatusRequest;
import com.gymcrm.dto.request.TraineeProfileUpdateRequest;
import com.gymcrm.dto.request.TraineeRegistrationRequest;
import com.gymcrm.dto.request.TrainerListUpdateRequest;
import com.gymcrm.dto.response.ErrorResponse;
import com.gymcrm.dto.response.RegistrationResponse;
import com.gymcrm.dto.response.TraineeProfileResponse;
import com.gymcrm.dto.response.TraineeProfileUpdateResponse;
import com.gymcrm.dto.response.TraineeTrainingResponse;
import com.gymcrm.dto.response.TrainerSummary;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.TrainingType;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.TraineeService;
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

@Slf4j
@RestController
@RequestMapping("/api/trainees")
@RequiredArgsConstructor
@Tag(name = "Trainees", description = "Trainee registration, profile and training management")
public class TraineeController {

    private final TraineeService traineeService;
    private final JwtService jwtService;

    @Operation(summary = "Register a new trainee (REST task 1)",
            description = "Public endpoint — no auth required. Username and password are generated "
                    + "on server, and the account is already logged in: a bearer token is returned "
                    + "alongside the credentials (security task 8).",
            security = {})
    @ApiResponse(responseCode = "201", description = "Trainee created",
            content = @Content(schema = @Schema(implementation = RegistrationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Sensitive
    public ResponseEntity<EntityModel<RegistrationResponse>> register(@Valid @RequestBody TraineeRegistrationRequest request) {
        Trainee created = traineeService.createTrainee(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.address());
        String token = jwtService.generateToken(created.getUsername());
        RegistrationResponse body = new RegistrationResponse(created.getUsername(), created.getRawPassword(), token);
        EntityModel<RegistrationResponse> model = EntityModel.of(body,
                linkTo(methodOn(TraineeController.class).getProfile(created.getUsername())).withSelfRel());
        return ResponseEntity.created(URI.create("/api/trainees/" + created.getUsername())).body(model);
    }

    @Operation(summary = "Get a trainee's profile (REST task 5)")
    @ApiResponse(responseCode = "200", description = "Profile found",
            content = @Content(schema = @Schema(implementation = TraineeProfileResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<TraineeProfileResponse>> getProfile(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username) {
        Trainee trainee = traineeService.selectTraineeProfileByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found: " + username));
        return ResponseEntity.ok(withProfileLinks(username, TraineeProfileResponse.from(trainee)));
    }

    @Operation(summary = "Update a trainee's profile (REST task 6)",
            description = " Username is unchangabel, taken from path not body, specialization/username are never part of this request.")
    @ApiResponse(responseCode = "200", description = "Profile updated",
            content = @Content(schema = @Schema(implementation = TraineeProfileUpdateResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{username}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<TraineeProfileUpdateResponse>> updateProfile(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username,
            @Valid @RequestBody TraineeProfileUpdateRequest request) {
        Trainee updated = traineeService.updateTraineeProfile(
                username, request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address(), request.isActive());
        return ResponseEntity.ok(withProfileLinks(username, TraineeProfileUpdateResponse.from(updated)));
    }

    @Operation(summary = "Delete a trainee's profile (REST task 7)",
            description = "Hard delete; cascades to the trainee's trainings (REST note 10).")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username) {
        traineeService.deleteTraineeByUsername(username);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Activate or deactivate a trainee (REST task 15)",
            description = "Not idempotent (REST note 9) wanted end state is supplied"
                    + "request that matches the current state are not silently accepted, rejected with 409")
    @ApiResponse(responseCode = "200", description = "State updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trainee is already in the requested active state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/{username}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setActive(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username,
            @Valid @RequestBody ActiveStatusRequest request) {
        traineeService.activateDeactivate(username, request.isActive());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "List a trainee's trainings, with optional filters (REST task 12)")
    @ApiResponse(responseCode = "200", description = "Trainings returned",
            content = @Content(schema = @Schema(implementation = TraineeTrainingResponse.class)))
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/{username}/trainings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<TraineeTrainingResponse>> getTrainings(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username,
            @Parameter(description = "Only trainings on/after this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Only trainings on/before this date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Filter by trainer name (partial match)")
            @RequestParam(required = false) String trainerName,
            @Parameter(description = "Filter by training type")
            @RequestParam(required = false) TrainingType.TrainingTypeName trainingType) {
        traineeService.selectTraineeByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found: " + username));
        List<TraineeTrainingResponse> trainings = traineeService
                .getTraineeTrainings(username, periodFrom, periodTo, trainerName, trainingType)
                .stream().map(TraineeTrainingResponse::from).toList();
        Link self = linkTo(methodOn(TraineeController.class)
                .getTrainings(username, periodFrom, periodTo, trainerName, trainingType)).withSelfRel();
        Link trainee = linkTo(methodOn(TraineeController.class).getProfile(username)).withRel("trainee");
        return ResponseEntity.ok(CollectionModel.of(trainings, self, trainee));
    }

    @Operation(summary = "List active trainers not yet assigned to a trainee (REST task 10)")
    @ApiResponse(responseCode = "200", description = "Trainers returned",
            content = @Content(schema = @Schema(implementation = TrainerSummary.class)))
    @ApiResponse(responseCode = "404", description = "No trainee with that username",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(value = "/{username}/unassigned-trainers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<TrainerSummary>> getUnassignedTrainers(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username) {
        traineeService.selectTraineeByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Trainee not found: " + username));
        List<TrainerSummary> trainers = traineeService.getNotAssignedActiveTrainers(username)
                .stream().map(TrainerSummary::from).toList();
        Link self = linkTo(methodOn(TraineeController.class).getUnassignedTrainers(username)).withSelfRel();
        Link trainee = linkTo(methodOn(TraineeController.class).getProfile(username)).withRel("trainee");
        return ResponseEntity.ok(CollectionModel.of(trainers, self, trainee));
    }

    @Operation(summary = "Replace a trainee's trainer list (REST ask 11)",
            description = "Full replacement, not a merge, exactly one trainer.")
    @ApiResponse(responseCode = "200", description = "Trainer list updated",
            content = @Content(schema = @Schema(implementation = TrainerSummary.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Trainee, or one of the trainer usernames, not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/{username}/trainers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<TrainerSummary>> updateTrainerList(
            @Parameter(description = "Trainee username", required = true) @PathVariable String username,
            @Valid @RequestBody TrainerListUpdateRequest request) {
        Trainee updated = traineeService.updateTraineeTrainers(username, request.trainerUsernames());
        List<TrainerSummary> trainers = updated.getTrainers().stream().map(TrainerSummary::from).toList();
        Link self = linkTo(methodOn(TraineeController.class).updateTrainerList(username, null)).withSelfRel();
        Link trainee = linkTo(methodOn(TraineeController.class).getProfile(username)).withRel("trainee");
        return ResponseEntity.ok(CollectionModel.of(trainers, self, trainee));
    }

    //link set shared by get and update profile, they return full profile representation,
    //but by different response DTO, nav options are identical
     private <T> EntityModel<T> withProfileLinks(String username, T body) {
        return EntityModel.of(body,
                linkTo(methodOn(TraineeController.class).getProfile(username)).withSelfRel(),
                linkTo(methodOn(TraineeController.class).updateProfile(username, null)).withRel("edit"),
                linkTo(methodOn(TraineeController.class).delete(username)).withRel("delete"),
                linkTo(methodOn(TraineeController.class)
                        .getTrainings(username, null, null, null, null)).withRel("trainings"),
                linkTo(methodOn(TraineeController.class).getUnassignedTrainers(username)).withRel("unassigned-trainers"),
                linkTo(methodOn(TraineeController.class).updateTrainerList(username, null)).withRel("trainers"));
    }
}
