package com.lanre.personl.iso20022.lifecycle.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import com.lanre.personl.iso20022.lifecycle.repository.PaymentWorkflowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/lifecycle")
@RequiredArgsConstructor
@Tag(name = "lifecycle", description = "Read-only payment lifecycle and audit views.")
@SecurityRequirement(name = "basicAuth")
public class AuditController {

    private final PaymentWorkflowRepository workflowRepository;

    @GetMapping("/{endToEndId}")
    @Operation(
            summary = "Get workflow by EndToEndId",
            description = "Returns the correlated lifecycle record and audit trail for a single payment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle record found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentWorkflow.class))),
            @ApiResponse(responseCode = "404", description = "No workflow found for the supplied EndToEndId"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks AUDITOR or ADMIN role")
    })
    public ResponseEntity<PaymentWorkflow> getWorkflow(@PathVariable String endToEndId) {
        try (LoggingContext.Scope ignored = LoggingContext.withEndToEndId(endToEndId)) {
            log.info("Fetching lifecycle workflow by EndToEndId.");
            return workflowRepository.findByEndToEndId(endToEndId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @GetMapping("/all")
    @Operation(
            summary = "List all workflows",
            description = "Returns all tracked lifecycle records with their audit logs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle records returned",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PaymentWorkflow.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks AUDITOR or ADMIN role")
    })
    public List<PaymentWorkflow> getAllWorkflows() {
        log.info("Fetching all lifecycle workflows.");
        return workflowRepository.findAll();
    }
}
