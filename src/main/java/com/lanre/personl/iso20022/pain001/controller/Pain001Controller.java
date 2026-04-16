package com.lanre.personl.iso20022.pain001.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.pain001.service.Pain001GeneratorService;
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

/**
 * REST controller that exposes the pain.001.001.11 generation capability
 * as an HTTP endpoint. Accepts a JSON {@link PaymentRequest} and returns
 * the generated ISO 20022 XML.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pain001")
@RequiredArgsConstructor
@Tag(name = "pain.001", description = "Customer credit transfer initiation generation endpoints.")
@SecurityRequirement(name = "basicAuth")
public class Pain001Controller {

    private final Pain001GeneratorService generatorService;

    /**
     * Generates a pain.001.001.11 XML document from a JSON PaymentRequest.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "debtorName": "Acme Corporation",
     *   "debtorIban": "DE89370400440532013000",
     *   "debtorBic": "COBADEFFXXX",
     *   "creditorName": "Widget Supplies Ltd",
     *   "creditorIban": "GB29NWBK60161331926819",
     *   "creditorBic": "NWBKGB2L",
     *   "amount": 1500.00,
     *   "currency": "EUR",
     *   "endToEndId": "INV-2026-00042",
     *   "remittanceInfo": "Invoice 2026-00042 payment"
     * }
     * }</pre>
     *
     * @param request the payment request data
     * @return the generated XML with Content-Type application/xml
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    @Operation(
            summary = "Generate pain.001.001.11 XML",
            description = "Accepts a flat payment request and returns a standards-compliant customer credit transfer initiation XML document."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "pain.001 XML generated",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role")
    })
    public ResponseEntity<String> generatePain001(@Valid @RequestBody PaymentRequest request) {
        try (LoggingContext.Scope ignored = LoggingContext.withEndToEndId(request.getEndToEndId())) {
            log.info("Received pain.001.001.11 generation request for debtor={} → creditor={}",
                    request.getDebtorName(), request.getCreditorName());

            String xml = generatorService.generatePain001Xml(request);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(MediaType.APPLICATION_XML_VALUE))
                    .body(xml);
        }
    }
}
