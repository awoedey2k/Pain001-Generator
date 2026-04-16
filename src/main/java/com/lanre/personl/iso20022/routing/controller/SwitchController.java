package com.lanre.personl.iso20022.routing.controller;

import com.lanre.personl.iso20022.logging.LoggingContext;
import com.lanre.personl.iso20022.routing.service.PaymentRouterService;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/switch")
@RequiredArgsConstructor
@Tag(name = "routing", description = "Switch routing and market-infrastructure dispatch endpoints.")
@SecurityRequirement(name = "basicAuth")
public class SwitchController {

    private final PaymentRouterService routerService;

    /**
     * Entry point for the modern Interbank Clearing Switch.
     * Routes pacs.008 messages across different simulating Market Infrastructures.
     */
    @PostMapping(value = "/route", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Route pacs.008 through the configured switch rules",
            description = "Validates the incoming pacs.008, applies ordered routing rules from configuration, wraps the message with a BAH envelope, and returns the routed payload or a pacs.002 rejection."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = {
                    @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "routePacs008",
                                    value = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10\">...</Document>"
                            )),
                    @Content(mediaType = MediaType.TEXT_XML_VALUE,
                            schema = @Schema(type = "string"))
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wrapped BAH payload or pacs.002 rejection",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Unable to process or route the message"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role"),
            @ApiResponse(responseCode = "413", description = "Payload exceeds configured request-size limit")
    })
    public ResponseEntity<String> routePayment(@RequestBody String xmlPayload) {
        MxPacs00800110 parsed = tryParse(xmlPayload);
        String msgId = extractMsgId(parsed);
        String endToEndId = extractEndToEndId(parsed);

        try (LoggingContext.Scope ignored = LoggingContext.withIdentifiers(msgId, endToEndId)) {
            log.info("Switch controller received routing request.");
            try {
                String response = routerService.processAndRoute(xmlPayload);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Switch failed to process payment", e);
                // In a real switch, we'd return a system fault pacs.002
                return ResponseEntity.badRequest().body("<Error>" + e.getMessage() + "</Error>");
            }
        }
    }

    private MxPacs00800110 tryParse(String xmlPayload) {
        try {
            return MxPacs00800110.parse(xmlPayload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractMsgId(MxPacs00800110 parsed) {
        try {
            return parsed.getFIToFICstmrCdtTrf().getGrpHdr().getMsgId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractEndToEndId(MxPacs00800110 parsed) {
        try {
            return parsed.getFIToFICstmrCdtTrf().getCdtTrfTxInf().get(0).getPmtId().getEndToEndId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
