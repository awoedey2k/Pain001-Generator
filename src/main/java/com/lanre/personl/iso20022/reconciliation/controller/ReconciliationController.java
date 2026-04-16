package com.lanre.personl.iso20022.reconciliation.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.reconciliation.service.Camt053Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Bank-to-Customer Statement Reconciliation.
 * Allows systems to upload bank statement reports (camt.053) to reconcile
 * existing payment instructions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
@Tag(name = "reconciliation", description = "Statement ingestion and payment reconciliation endpoints.")
@SecurityRequirement(name = "basicAuth")
public class ReconciliationController {

    private final Camt053Service camt053Service;

    /**
     * Accepts a raw camt.053 XML statement and triggers the reconciliation logic.
     * This will scan the statement for EndToEndIds matching open 'SETTLING' payments.
     *
     * @param xml the raw ISO 20022 camt.053.001.10 XML payload
     * @return a message confirming the processing start
     */
    @PostMapping("/statement")
    @Operation(
            summary = "Upload camt.053 statement XML for reconciliation",
            description = "Processes an inbound bank statement and updates lifecycle records when EndToEndId matches are found."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_XML_VALUE,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(
                            name = "camt053Xml",
                            value = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:camt.053.001.10\">...</Document>"
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statement accepted and processed",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role"),
            @ApiResponse(responseCode = "413", description = "Payload exceeds configured request-size limit"),
            @ApiResponse(responseCode = "500", description = "Statement processing failed")
    })
    public ResponseEntity<String> reconcileStatement(@RequestBody String xml) {
        try (LoggingContext.Scope ignored = LoggingContext.withMsgId(extractStatementMsgId(xml))) {
            log.info("Received statement upload for reconciliation.");
            try {
                camt053Service.processStatement(xml);
                return ResponseEntity.ok("Statement processed successfully. Reconciliation transitions logged.");
            } catch (Exception e) {
                log.error("Failed to process reconciliation statement", e);
                return ResponseEntity.internalServerError().body("Failed to process statement: " + e.getMessage());
            }
        }
    }

    private String extractStatementMsgId(String xml) {
        String startTag = "<MsgId>";
        String endTag = "</MsgId>";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return xml.substring(start + startTag.length(), end).trim();
        }
        return null;
    }
}
