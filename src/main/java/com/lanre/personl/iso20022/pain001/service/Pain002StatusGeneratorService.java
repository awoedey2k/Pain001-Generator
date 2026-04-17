package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.model.ValidationReport;
import com.prowidesoftware.swift.model.mx.MxPain00200110;
import com.prowidesoftware.swift.model.mx.dic.*;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.UUID;

@Service
public class Pain002StatusGeneratorService {

    /**
     * Generates a pain.002.001.10 (Customer Payment Status Report) message natively
     * acting as the Acknowledgement / Reject payload for a processed pain.001
     * input.
     * 
     * @param originalMsgId The GroupHeader MsgId of the incoming pain.001 (if
     *                      available)
     * @param report        The validation report from the Gatekeeper engine.
     * @return ISO 20022 Compliant pain.002.001.10 XML string
     */
    public String generateStatusReport(String originalMsgId, ValidationReport report) {

        MxPain00200110 mx = new MxPain00200110();
        CustomerPaymentStatusReportV10 statusReport = new CustomerPaymentStatusReportV10();

        // 1. Setup response GroupHeader
        GroupHeader86 grpHdr = new GroupHeader86();
        grpHdr.setMsgId("STAT-" + UUID.randomUUID().toString().substring(0, 8));
        grpHdr.setCreDtTm(nowAsXmlGregorianCalendar());

        // Reflect initiating party
        PartyIdentification135 initgPty = new PartyIdentification135();
        initgPty.setNm("PAYMENT-SWITCH-GATEKEEPER");
        grpHdr.setInitgPty(initgPty);
        statusReport.setGrpHdr(grpHdr);

        // 2. Setup Original Group Information And Status (OgnlGrpInfAndSts)
        OriginalGroupHeader17 origGrpInf = new OriginalGroupHeader17();
        origGrpInf.setOrgnlMsgId(originalMsgId != null ? originalMsgId : "UNKNOWN");
        origGrpInf.setOrgnlMsgNmId("pain.001.001.11");

        // Set status ACCP (Accepted) or RJCT (Rejected)
        String txnStatus = report.isValid() ? "ACCP" : "RJCT";
        origGrpInf.setGrpSts(txnStatus);

        // 3. Attach validation reasons if rejected
        if (!report.isValid()) {
            for (String errorStr : report.getErrorMessages()) {
                StatusReasonInformation12 statusReason = new StatusReasonInformation12();

                // Set external or proprietary reason code
                StatusReason6Choice reasonCode = new StatusReason6Choice();
                reasonCode.setPrtry("VALIDATION_FAILED");
                statusReason.setRsn(reasonCode);

                // Map the full system error inside additional info
                statusReason.getAddtlInf().add(report.getFailedStage() + ": " + errorStr);

                origGrpInf.getStsRsnInf().add(statusReason);
            }
        }

        statusReport.setOrgnlGrpInfAndSts(origGrpInf);
        mx.setCstmrPmtStsRpt(statusReport);

        return mx.message();
    }

    public String generateDuplicateStatusReport(String originalMsgId, String endToEndId) {
        MxPain00200110 mx = new MxPain00200110();
        CustomerPaymentStatusReportV10 statusReport = new CustomerPaymentStatusReportV10();

        GroupHeader86 grpHdr = new GroupHeader86();
        grpHdr.setMsgId("STAT-" + UUID.randomUUID().toString().substring(0, 8));
        grpHdr.setCreDtTm(nowAsXmlGregorianCalendar());
        PartyIdentification135 initgPty = new PartyIdentification135();
        initgPty.setNm("PAYMENT-SWITCH-GATEKEEPER");
        grpHdr.setInitgPty(initgPty);
        statusReport.setGrpHdr(grpHdr);

        OriginalGroupHeader17 origGrpInf = new OriginalGroupHeader17();
        origGrpInf.setOrgnlMsgId(originalMsgId != null ? originalMsgId : "UNKNOWN");
        origGrpInf.setOrgnlMsgNmId("pain.001.001.11");
        origGrpInf.setGrpSts("RJCT");

        StatusReasonInformation12 statusReason = new StatusReasonInformation12();
        StatusReason6Choice reasonCode = new StatusReason6Choice();
        reasonCode.setPrtry("DUPL");
        statusReason.setRsn(reasonCode);
        statusReason.getAddtlInf().add("Duplicate request detected for EndToEndId=" + (endToEndId != null ? endToEndId : "UNKNOWN"));
        origGrpInf.getStsRsnInf().add(statusReason);

        statusReport.setOrgnlGrpInfAndSts(origGrpInf);
        mx.setCstmrPmtStsRpt(statusReport);
        return mx.message();
    }

    private XMLGregorianCalendar nowAsXmlGregorianCalendar() {
        try {
            GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (Exception e) {
            throw new RuntimeException("Validation Engine failed parsing time", e);
        }
    }
}
