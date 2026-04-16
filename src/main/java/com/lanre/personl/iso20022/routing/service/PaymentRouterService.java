package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.pacs008.service.Pacs008Service;
import com.lanre.personl.iso20022.routing.entity.PaymentRoutingAudit;
import com.lanre.personl.iso20022.routing.repository.PaymentRoutingRepository;
import com.lanre.personl.iso20022.routing.strategy.MarketInfrastructureAdapter;
import com.prowidesoftware.swift.model.mx.BusinessAppHdrV03;
import com.prowidesoftware.swift.model.mx.MxPacs00800110;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Intelligent Payment Router (The Switch).
 * <p>
 * This service acts as the central hub for interbank message routing. It takes
 * validated pacs.008 messages, selects the appropriate Market Infrastructure (MI)
 * via the Strategy Pattern, wraps the payload in a Business Application Header (BAH),
 * and persists the routing decision for audit purposes.
 * </p>
 *
 * <p><b>Routing Rules (Prioritized):</b></p>
 * <ol>
 *   <li><b>High-Value Override</b>: Priority BICs are routed to specialized settlement queues.</li>
 *   <li><b>Currency Mapping</b>: Payments are routed based on ISO 4217 Match (e.g., EUR to SEPA).</li>
 *   <li><b>Fallback</b>: Unroutable payments trigger an automated pacs.002 rejection.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRouterService {

    private static final String BAH_NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:head.001.001.03";

    private final Pacs008Service pacs008Service;
    private final Pacs002Service pacs002Service;
    private final PaymentRoutingRepository routingRepository;
    private final List<MarketInfrastructureAdapter> adapters;

    /**
     * Entry point for the Switch. Validates, wraps with BAH, routes, and audits.
     */
    @Transactional
    public String processAndRoute(String rawPacs008) {
        log.info("Switch receiving new pacs.008 message for routing.");

        // 1. Validation & Parsing
        MxPacs00800110 mxMessage = pacs008Service.validateAndParse(rawPacs008);
        FIToFICustomerCreditTransferV10 pacs = mxMessage.getFIToFICstmrCdtTrf();
        
        String msgId = pacs.getGrpHdr().getMsgId();
        String currency = pacs.getGrpHdr().getTtlIntrBkSttlmAmt().getCcy();
        
        // Extract Receiver BIC (Creditor Agent)
        String receiverBic = extractReceiverBic(pacs);

        // 2. Select Strategy (Rule C: High-Value > Rule A/B: Currency)
        Optional<MarketInfrastructureAdapter> selectedAdapter = findBestAdapter(currency, receiverBic);

        if (selectedAdapter.isPresent()) {
            MarketInfrastructureAdapter adapter = selectedAdapter.get();
            log.info("Routing logic chose adapter: {}", adapter.getName());
            
            // 3. Wrap with BAH
            String bahXml = generateBah(msgId, receiverBic);
            String wrappedMessage = wrapMessage(bahXml, rawPacs008);
            
            // 4. Execute Route (Simulation)
            adapter.route(mxMessage);
            
            // 5. Audit Persistence
            saveAudit(msgId, currency, receiverBic, adapter.getName(), true, "Routed successfully");
            
            return wrappedMessage;
        } else {
            log.warn("No routing rule found for {} @ {}", currency, receiverBic);
            saveAudit(msgId, currency, receiverBic, "NONE", false, "No matching Market Infrastructure found");
            
            // Generate pacs.002 Rejection
            return pacs002Service.generateNoRoutingRuleRejection(msgId);
        }
    }

    private String extractReceiverBic(FIToFICustomerCreditTransferV10 pacs) {
        try {
            // Heuristically grabbing from InstdAgt in GrpHdr or CdtrAgt in TxInfo
            if (pacs.getGrpHdr().getInstdAgt() != null) {
                return pacs.getGrpHdr().getInstdAgt().getFinInstnId().getBICFI();
            }
            return pacs.getCdtTrfTxInf().get(0).getCdtrAgt().getFinInstnId().getBICFI();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Optional<MarketInfrastructureAdapter> findBestAdapter(String currency, String bic) {
        // Find HighValue first
        Optional<MarketInfrastructureAdapter> highValue = adapters.stream()
            .filter(a -> a.getName().contains("HIGH-VALUE") && a.supports(currency, bic))
            .findFirst();
            
        if (highValue.isPresent()) return highValue;

        // Fallback to currency matching
        return adapters.stream()
            .filter(a -> a.supports(currency, bic))
            .findFirst();
    }

    private String generateBah(String msgId, String receiverBic) {
        BusinessAppHdrV03 bah = new BusinessAppHdrV03();
        bah.setBizMsgIdr("BAH-" + UUID.randomUUID().toString().substring(0, 8));
        bah.setMsgDefIdr("pacs.008.001.10");
        
        try {
            bah.setCreDt(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (Exception e) {
            log.error("Failed to set BAH creation date", e);
        }
        
        // Sender (The Switch)
        Party44Choice fr = new Party44Choice();
        fr.setFIId(createAgent("THESWITCHXXX"));
        bah.setFr(fr);

        // Receiver
        Party44Choice to = new Party44Choice();
        to.setFIId(createAgent(receiverBic));
        bah.setTo(to);
        
        return bah.xml();
    }

    private BranchAndFinancialInstitutionIdentification6 createAgent(String bic) {
        BranchAndFinancialInstitutionIdentification6 agent = new BranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 finId = new FinancialInstitutionIdentification18();
        finId.setBICFI(bic);
        agent.setFinInstnId(finId);
        return agent;
    }

    private String wrapMessage(String bahXml, String payloadXml) {
        // Keep the outer wrapper aligned with the BusinessAppHdrV03 schema version.
        return "<AppHdrAndMsg xmlns=\"" + BAH_NAMESPACE + "\">\n" +
                bahXml + "\n" +
                payloadXml + "\n" +
                "</AppHdrAndMsg>";
    }

    private void saveAudit(String msgId, String ccy, String bic, String dest, boolean success, String reason) {
        PaymentRoutingAudit audit = PaymentRoutingAudit.builder()
            .msgId(msgId)
            .currency(ccy)
            .receiverBic(bic)
            .destinationRoute(dest)
            .highValue(dest.contains("HIGH-VALUE"))
            .processedAt(LocalDateTime.now())
            .decisionReason(reason)
            .build();
        routingRepository.save(audit);
    }
}
