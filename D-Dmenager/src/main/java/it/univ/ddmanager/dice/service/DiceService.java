package it.univ.ddmanager.dice.service;

import it.univ.ddmanager.dice.dto.DiceRollResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class DiceService {

    private final SecureRandom random = new SecureRandom();

    public DiceRollResponse roll(int sides) {
        int value = random.nextInt(sides) + 1; // 1..sides
        return new DiceRollResponse(value, sides, Instant.now());
    }

    public DiceRollResponse rollD4() {
        return roll(4);
    }

    public DiceRollResponse rollD6() {
        return roll(6);
    }

    public DiceRollResponse rollD8() {
        return roll(8);
    }

    public DiceRollResponse rollD10() {
        return roll(10);
    }

    public DiceRollResponse rollD12() {
        return roll(12);
    }

    public DiceRollResponse rollD20() {
        return roll(20);
    }

}
