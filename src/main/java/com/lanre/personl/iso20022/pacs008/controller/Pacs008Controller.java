package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/pacs008")
@RequiredArgsConstructor
@Tag(name = "pacs.008", description = "Financial institution credit transfer generation endpoints.")
@SecurityRequirement(name = "basicAuth")
public class Pacs008Controller {

    private final Pacs008Service pacs008Service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Generate pacs.008.001.10 XML",
            description = "Accepts a flat payment request and returns an interbank credit transfer XML document."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "pacs.008 XML generated",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role")
    })
    public ResponseEntity<String> generatePacs008(@Valid @RequestBody PaymentRequest request) {
        try (LoggingContext.Scope ignored = LoggingContext.withEndToEndId(request.getEndToEndId())) {
            log.info("Received request to generate pacs.008 XML for: {} -> {}", request.getDebtorName(), request.getCreditorName());
            try {
                String xml = pacs008Service.generatePacs008Xml(request);
                return ResponseEntity.ok(xml);
            } catch (Exception e) {
                log.error("Failed to generate pacs.008 message", e);
                return ResponseEntity.internalServerError().body("<Error>" + e.getMessage() + "</Error>");
            }
        }
    }
}
