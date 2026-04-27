// src/main/java/it/univ/ddmanager/sessionlog/dto/SessionEventRequest.java
package it.univ.ddmanager.sessionlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionEventRequest {

    @NotNull
    private Long sessionId;

    @NotBlank
    private String title;

    private String type;
    private String description;
    private String inGameTime;
    private Boolean isVisibleToPlayers;
}
