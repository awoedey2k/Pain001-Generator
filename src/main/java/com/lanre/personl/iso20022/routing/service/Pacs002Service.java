package com.lanre.personl.iso20022.routing.service;

import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.prowidesoftware.swift.model.mx.MxPacs00200112;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

/**
 * ISO 20022 Status Reporting Engine (pacs.002).
 * <p>
 * This service generates automated Status Report messages (pacs.002.001.12) to
 * communicate rejections or updates within the interbank network.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * Triggered automatically when the {@link PaymentRouterService} cannot find a
 * matching Market Infrastructure for a processed settlement.
 */
@Slf4j
@Service
public class Pacs002Service {

    /**
     * Generates a pacs.002.001.12 validation status report for an inbound pacs.008.
     */
    public String generateValidationStatusReport(String originalMsgId, ValidationReport report) {
        MxPacs00200112 mx = new MxPacs00200112();
        FIToFIPaymentStatusReportV12 statusReport = new FIToFIPaymentStatusReportV12();

        statusReport.setGrpHdr(buildGroupHeader("STS-"));
        statusReport.addOrgnlGrpInfAndSts(
                buildOriginalGroupStatus(
                        originalMsgId != null ? originalMsgId : "UNKNOWN-PACS-ID",
                        report.isValid() ? "ACCP" : "RJCT",
                        report.isValid() ? List.of() : report.getErrorMessages(),
                        report.getFailedStage(),
                        "VALIDATION_FAILED"
                )
        );

        mx.setFIToFIPmtStsRpt(statusReport);
        return mx.message();
    }

    /**
     * Generates a pacs.002.001.12 rejection report for a message that couldn't be routed.
     */
    public String generateNoRoutingRuleRejection(String originalMsgId) {
        MxPacs00200112 mx = new MxPacs00200112();
        FIToFIPaymentStatusReportV12 report = new FIToFIPaymentStatusReportV12();

        report.setGrpHdr(buildGroupHeader("REJ-"));
        report.addOrgnlGrpInfAndSts(
                buildOriginalGroupStatus(
                        originalMsgId,
                        "RJCT",
                        List.of("The switch could not find a matching market infrastructure for this currency/BIC combination."),
                        null,
                        "NoRoutingRule"
                )
        );

        mx.setFIToFIPmtStsRpt(report);
        return mx.message();
    }

    private GroupHeader101 buildGroupHeader(String prefix) {
        GroupHeader101 grpHdr = new GroupHeader101();
        grpHdr.setMsgId(prefix + UUID.randomUUID().toString().substring(0, 8));
        grpHdr.setCreDtTm(nowAsXmlGregorianCalendar());
        return grpHdr;
    }

    private OriginalGroupHeader17 buildOriginalGroupStatus(
            String originalMsgId,
            String status,
            List<String> messages,
            String stage,
            String reasonCode
    ) {
        OriginalGroupHeader17 orgnlGrp = new OriginalGroupHeader17();
        orgnlGrp.setOrgnlMsgId(originalMsgId);
        orgnlGrp.setOrgnlMsgNmId("pacs.008.001.10");
        orgnlGrp.setGrpSts(status);

        for (String message : messages) {
            StatusReasonInformation12 rsnInf = new StatusReasonInformation12();
            StatusReason6Choice rsn = new StatusReason6Choice();
            rsn.setPrtry(reasonCode);
            rsnInf.setRsn(rsn);
            rsnInf.getAddtlInf().add(stage != null ? stage + ": " + message : message);
            orgnlGrp.addStsRsnInf(rsnInf);
        }

        return orgnlGrp;
    }

    private XMLGregorianCalendar nowAsXmlGregorianCalendar() {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pacs.002 timestamp", e);
        }
    }
}
