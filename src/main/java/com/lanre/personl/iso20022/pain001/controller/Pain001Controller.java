package com.lanre.personl.iso20022.pain001.controller;

import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.lanre.personl.iso20022.pain001.service.Pain001GeneratorService;
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
    public ResponseEntity<String> generatePain001(@RequestBody PaymentRequest request) {
        log.info("Received pain.001.001.11 generation request for debtor={} → creditor={}",
                request.getDebtorName(), request.getCreditorName());

        String xml = generatorService.generatePain001Xml(request);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
