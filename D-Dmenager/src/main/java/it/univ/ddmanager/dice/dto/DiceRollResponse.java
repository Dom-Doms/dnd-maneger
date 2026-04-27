package it.univ.ddmanager.dice.dto;

import java.time.Instant;

public record DiceRollResponse(
        int value,
        int sides,
        Instant rolledAt
) {}

