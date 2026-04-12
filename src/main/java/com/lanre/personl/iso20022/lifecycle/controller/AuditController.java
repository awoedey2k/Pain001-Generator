package com.lanre.personl.iso20022.lifecycle.controller;

import com.lanre.personl.iso20022.lifecycle.entity.PaymentWorkflow;
import com.lanre.personl.iso20022.lifecycle.repository.PaymentWorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lifecycle")
@RequiredArgsConstructor
public class AuditController {

    private final PaymentWorkflowRepository workflowRepository;

    @GetMapping("/{endToEndId}")
    public ResponseEntity<PaymentWorkflow> getWorkflow(@PathVariable String endToEndId) {
        return workflowRepository.findByEndToEndId(endToEndId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public List<PaymentWorkflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }
}
