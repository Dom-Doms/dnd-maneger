// src/main/java/it/univ/ddmanager/world/dto/WorldRequest.java
package it.univ.ddmanager.world.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorldRequest {

    @NotBlank
    private String name;

    private String description;

    private Boolean isPublic;
}
