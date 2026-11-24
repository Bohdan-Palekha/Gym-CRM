package com.gymcrm.dto.response;

import com.gymcrm.model.Trainee;
import io.swagger.v3.oas.annotations.media.Schema;

//single entry in trainee list per REST task 8,9
@Schema(description = "Trainee summary, as shown inside a Trainees List")
public record TraineeSummary(

        @Schema(description = "Trainee username", example = "alice.nguyen")
        String username,

        @Schema(description = "Trainee first name", example = "Alice")
        String firstName,

        @Schema(description = "Trainee last name", example = "Nguyen")
        String lastName
) {
    public static TraineeSummary from(Trainee trainee) {
        return new TraineeSummary(trainee.getUsername(), trainee.getFirstName(), trainee.getLastName());
    }
}
