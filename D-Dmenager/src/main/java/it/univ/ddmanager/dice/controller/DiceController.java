package it.univ.ddmanager.dice.controller;

import it.univ.ddmanager.dice.dto.DiceRollResponse;
import it.univ.ddmanager.dice.service.DiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dice")
public class DiceController {

    private final DiceService diceService;

    public DiceController(DiceService diceService) {
        this.diceService = diceService;
    }
    @GetMapping("/d4")
    public DiceRollResponse rollD4() {
        return diceService.rollD4();
    }
    @GetMapping("/d6")
    public DiceRollResponse rollD6() {
        return diceService.rollD6();
    }
    @GetMapping("/d8")
    public DiceRollResponse rollD8() {
        return diceService.rollD8();
    }
    @GetMapping("/d10")
    public DiceRollResponse rollD10() {
        return diceService.rollD10();
    }
    @GetMapping("/d12")
    public DiceRollResponse rollD12() {
        return diceService.rollD12();
    }
    @GetMapping("/d20")
    public DiceRollResponse rollD20() {
        return diceService.rollD20();
    }
}

