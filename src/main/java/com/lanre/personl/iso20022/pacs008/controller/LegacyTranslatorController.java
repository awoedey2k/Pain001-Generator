package com.lanre.personl.iso20022.pacs008.controller;

import com.lanre.personl.iso20022.pacs008.service.LegacyTranslatorService;
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
@RequestMapping("/api/v1/translator")
@RequiredArgsConstructor
@Tag(name = "translation", description = "Legacy SWIFT FIN to ISO 20022 translation endpoints.")
@SecurityRequirement(name = "basicAuth")
public class LegacyTranslatorController {

    private final LegacyTranslatorService translatorService;

    @PostMapping(value = "/mt103", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Translate MT103 text into pacs.008 XML",
            description = "Accepts a simplified MT103 payload and returns the translated pacs.008 ISO 20022 message."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(
                            name = "mt103",
                            value = ":20:REF12345\n:32A:260416EUR1500,\n:50K:/123456789\nACME CORPORATION\n:59:/GB29NWBK60161331926819\nWIDGET SUPPLIES LTD"
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Translated pacs.008 XML",
                    content = @Content(mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Unable to parse or translate MT103 payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Caller lacks WRITER or ADMIN role")
    })
    public ResponseEntity<String> translateMT103(@RequestBody String mt103Payload) {
        log.info("Received request to translate MT103 payload.");
        try {
            MxPacs00800110 pacs008 = translatorService.translateMT103ToPacs008(mt103Payload);
            return ResponseEntity.ok(pacs008.message());
        } catch (Exception e) {
            log.error("Failed to translate MT103 message", e);
            return ResponseEntity.badRequest()
                    .body("<Error>Failed to parse or translate MT103 snippet: " + e.getMessage() + "</Error>");
        }
    }
}
