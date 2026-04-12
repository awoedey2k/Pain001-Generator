package com.lanre.personl.iso20022.routing.service;

import com.prowidesoftware.swift.model.mx.MxPacs00200112;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
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
     * Generates a pacs.002.001.12 rejection report for a message that couldn't be routed.
     */
    public String generateNoRoutingRuleRejection(String originalMsgId) {
        MxPacs00200112 mx = new MxPacs00200112();
        FIToFIPaymentStatusReportV12 report = new FIToFIPaymentStatusReportV12();

        // 1. Group Header
        GroupHeader101 grpHdr = new GroupHeader101();
        grpHdr.setMsgId("REJ-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            grpHdr.setCreDtTm(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (Exception ignored) {}
        report.setGrpHdr(grpHdr);

        // 2. Original Group Info and Status
        OriginalGroupHeader17 orgnlGrp = new OriginalGroupHeader17();
        orgnlGrp.setOrgnlMsgId(originalMsgId);
        orgnlGrp.setOrgnlMsgNmId("pacs.008.001.10");
        orgnlGrp.setGrpSts("RJCT");

        // 3. Status Reason Information
        StatusReasonInformation12 rsnInf = new StatusReasonInformation12();
        StatusReason6Choice rsn = new StatusReason6Choice();
        rsn.setPrtry("NoRoutingRule");
        rsnInf.setRsn(rsn);
        rsnInf.getAddtlInf().add("The switch could not find a matching market infrastructure for this currency/BIC combination.");
        
        orgnlGrp.addStsRsnInf(rsnInf);
        report.addOrgnlGrpInfAndSts(orgnlGrp);

        mx.setFIToFIPmtStsRpt(report);
        return mx.message();
    }
}
