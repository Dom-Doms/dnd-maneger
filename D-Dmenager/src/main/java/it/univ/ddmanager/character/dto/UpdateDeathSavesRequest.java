package it.univ.ddmanager.character.dto;

import lombok.Data;

@Data
public class UpdateDeathSavesRequest {

    private int successes;
    private int failures;
}
