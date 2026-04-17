package com.lanre.personl.iso20022.api.error;

import com.lanre.personl.iso20022.lifecycle.repository.IsoMessageAuditRepository;
import com.lanre.personl.iso20022.pain001.service.Pain002StatusGeneratorService;
import com.lanre.personl.iso20022.routing.service.Pacs002Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.Optional;

@RestControllerAdvice
@SuppressWarnings("null")
public class GlobalIso20022ExceptionHandler {

    private final Pain002StatusGeneratorService pain002StatusGeneratorService;
    private final Pacs002Service pacs002Service;
    private final IsoMessageAuditRepository isoMessageAuditRepository;

    public GlobalIso20022ExceptionHandler(
            Pain002StatusGeneratorService pain002StatusGeneratorService,
            Pacs002Service pacs002Service,
            IsoMessageAuditRepository isoMessageAuditRepository
    ) {
        this.pain002StatusGeneratorService = pain002StatusGeneratorService;
        this.pacs002Service = pacs002Service;
        this.isoMessageAuditRepository = isoMessageAuditRepository;
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<String> handleDuplicateRequest(DuplicateRequestException exception) {
        String endToEndId = exception.getEndToEndId();
        String messageFamily = exception.getMessageFamily();

        if (Objects.equals(messageFamily, "pain.001")) {
            String originalMsgId = isoMessageAuditRepository
                    .findLatestMessageIdByWorkflowEndToEndIdAndMessageType(endToEndId, "pain.001")
                    .orElse("UNKNOWN");
            String xml = pain002StatusGeneratorService.generateDuplicateStatusReport(originalMsgId, endToEndId);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }

        if (Objects.equals(messageFamily, "pacs.008")) {
            String originalMsgId = isoMessageAuditRepository
                    .findLatestMessageIdByWorkflowEndToEndIdAndMessageType(endToEndId, "pacs.008")
                    .orElse("UNKNOWN-PACS-ID");
            String xml = pacs002Service.generateDuplicateRejection(originalMsgId, endToEndId);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"DUPLICATE_REQUEST\",\"endToEndId\":\"" + sanitize(endToEndId) + "\"}");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String message = Optional.ofNullable(exception.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse(exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"DATA_INTEGRITY_VIOLATION\",\"message\":\"" + sanitize(message) + "\"}");
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
