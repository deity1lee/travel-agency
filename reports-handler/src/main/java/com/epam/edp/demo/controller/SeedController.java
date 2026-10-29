package com.epam.edp.demo.controller;

import com.epam.edp.demo.service.SeedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/seed")
@Slf4j
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService){
        this.seedService = seedService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seedReports(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam int agentCount,
                            @RequestParam int tourCount) {
        log.info("Request to seed report data with dates {}, {}", startDate, endDate);
        seedService.seedReports(startDate, endDate, agentCount, tourCount);
        log.info("Seeding completed :)");
    }

}
