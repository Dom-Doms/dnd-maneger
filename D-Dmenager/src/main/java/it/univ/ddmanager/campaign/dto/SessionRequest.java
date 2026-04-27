// src/main/java/it/univ/ddmanager/campaign/dto/SessionRequest.java
package it.univ.ddmanager.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {

    @NotBlank
    private String title;

    @NotNull
    private Integer sessionNumber;

    private LocalDate sessionDate;

    private String notes;
}