package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.auth.ResendCodeRequest;
import com.epam.edp.demo.dto.dev.OtpResponse;
import com.epam.edp.demo.service.auth.VerificationTokenService;
import com.epam.edp.demo.service.seed.SeedService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@Slf4j
public class DevController {

    private final SeedService seedService;
    private VerificationTokenService verificationTokenService;

    public DevController(SeedService seedService,
                         VerificationTokenService verificationTokenService) {
        this.seedService = seedService;
        this.verificationTokenService = verificationTokenService;
    }

    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Hidden
    public void seed() {
        log.info("seeding data");
        seedService.seed();
        log.info("data seeded");
    }

    @PostMapping("/code")
    public OtpResponse getOpt(@RequestBody ResendCodeRequest request) {
        log.debug("getting new otp for testing for ({}-{})", request.email(), request.verificationType());
        String code = verificationTokenService.getNewOpt(request.email(), request.verificationType());
        return new OtpResponse(code);
    }
}
