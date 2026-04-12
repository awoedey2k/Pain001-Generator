package com.lanre.personl.iso20022.pain001.service;

import com.lanre.personl.iso20022.pain001.model.PaymentRequest;
import com.prowidesoftware.swift.model.mx.MxPain00100111;
import com.prowidesoftware.swift.model.mx.dic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * Service that converts a flat {@link PaymentRequest} into a valid
 * ISO 20022 pain.001.001.11 (Customer Credit Transfer Initiation) XML document.
 *
 * <h3>ISO 20022 Message Hierarchy</h3>
 * <pre>
 * Document
 * └─ CstmrCdtTrfInitn  (CustomerCreditTransferInitiationV11)
 *    ├─ GrpHdr           (GroupHeader95)         ← message-level metadata
 *    │   ├─ MsgId         unique message ID
 *    │   ├─ CreDtTm       creation timestamp
 *    │   ├─ NbOfTxs       number of transactions
 *    │   ├─ CtrlSum       control sum (total amount)
 *    │   └─ InitgPty      initiating party
 *    │
 *    └─ PmtInf           (PaymentInstruction40)  ← debit-side grouping
 *        ├─ PmtInfId      payment information ID
 *        ├─ PmtMtd        payment method ("TRF")
 *        ├─ NbOfTxs       transactions in this block
 *        ├─ CtrlSum       control sum for this block
 *        ├─ ReqdExctnDt   requested execution date
 *        ├─ Dbtr          debtor party name
 *        ├─ DbtrAcct      debtor IBAN
 *        ├─ DbtrAgt       debtor agent (BIC)
 *        │
 *        └─ CdtTrfTxInf  (CreditTransferTransaction54)  ← credit-side detail
 *            ├─ PmtId      payment identification
 *            │   ├─ InstrId    instruction ID
 *            │   └─ EndToEndId end-to-end ID
 *            ├─ Amt         instructed amount + currency
 *            ├─ CdtrAgt     creditor agent (BIC)
 *            ├─ Cdtr        creditor party name
 *            ├─ CdtrAcct    creditor IBAN
 *            └─ RmtInf      remittance information
 * </pre>
 *
 * <p>This class uses the <b>Prowide ISO 20022</b> open-source library
 * ({@code com.prowidesoftware:pw-iso20022}) which provides typed Java
 * classes generated from the official ISO 20022 XSD schemas.</p>
 */
@Slf4j
@Service
public class Pain001GeneratorService {

    /**
     * Generates a pretty-printed pain.001.001.11 XML string from the given
     * {@link PaymentRequest}.
     *
     * @param request the flat payment request data
     * @return a valid, pretty-printed XML string conforming to pain.001.001.11
     */
    public String generatePain001Xml(PaymentRequest request) {
        log.info("Generating pain.001.001.11 XML for debtor={}, creditor={}, amount={} {}",
                request.getDebtorName(), request.getCreditorName(),
                request.getAmount(), request.getCurrency());

        // ── 1. Create the top-level MX message container ────────────────
        MxPain00100111 mxMessage = new MxPain00100111();
        CustomerCreditTransferInitiationV11 initiation = new CustomerCreditTransferInitiationV11();

        // ── 2. Build GroupHeader (message-level metadata) ───────────────
        GroupHeader95 groupHeader = buildGroupHeader(request);
        initiation.setGrpHdr(groupHeader);

        // ── 3. Build PaymentInformation (debit-side grouping) ───────────
        PaymentInstruction40 paymentInstruction = buildPaymentInstruction(request);
        initiation.getPmtInf().add(paymentInstruction);

        // ── 4. Wire everything together ─────────────────────────────────
        mxMessage.setCstmrCdtTrfInitn(initiation);

        // ── 5. Serialize to pretty-printed XML ──────────────────────────
        String xml = mxMessage.message();

        log.debug("Generated pain.001.001.11 XML:\n{}", xml);
        return xml;
    }

    // =====================================================================
    //  Private Builder Methods
    // =====================================================================

    /**
     * Builds the GroupHeader95 block.
     *
     * <p>The GroupHeader is mandatory and appears exactly once per message.
     * It carries metadata that applies to the entire message:
     * <ul>
     *   <li><b>MsgId</b> – A unique identifier for this message (UUID-based).</li>
     *   <li><b>CreDtTm</b> – The date-time the message was created (ISO 8601).</li>
     *   <li><b>NbOfTxs</b> – Total number of individual transactions.</li>
     *   <li><b>CtrlSum</b> – Sum of all transaction amounts (for reconciliation).</li>
     *   <li><b>InitgPty</b> – The party that initiated the payment file.</li>
     * </ul>
     */
    private GroupHeader95 buildGroupHeader(PaymentRequest request) {
        GroupHeader95 grpHdr = new GroupHeader95();

        // MsgId: Unique message identification – using a time-prefixed UUID
        String msgId = "MSGID-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(ZonedDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        grpHdr.setMsgId(msgId);
        log.debug("GroupHeader.MsgId = {}", msgId);

        // CreDtTm: Creation date-time in XMLGregorianCalendar format
        grpHdr.setCreDtTm(nowAsXmlGregorianCalendar());
        log.debug("GroupHeader.CreDtTm = {}", grpHdr.getCreDtTm());

        // NbOfTxs: Number of individual transactions (we have exactly 1)
        grpHdr.setNbOfTxs("1");

        // CtrlSum: Control sum – total of all instructed amounts
        grpHdr.setCtrlSum(request.getAmount());

        // InitgPty: Initiating party – the debtor in our simple use case
        PartyIdentification135 initiatingParty = new PartyIdentification135();
        initiatingParty.setNm(request.getDebtorName());
        grpHdr.setInitgPty(initiatingParty);

        return grpHdr;
    }

    /**
     * Builds the PaymentInstruction40 block.
     *
     * <p>PaymentInformation groups one or more credit-transfer transactions
     * that share the same debit-side characteristics (same debtor, same
     * debtor account, same execution date, etc.).
     *
     * <p>In this implementation, each PaymentRequest maps to exactly one
     * PaymentInformation block containing one CreditTransferTransactionInformation.
     */
    private PaymentInstruction40 buildPaymentInstruction(PaymentRequest request) {
        PaymentInstruction40 pmtInf = new PaymentInstruction40();

        // PmtInfId: Unique ID for this payment information block
        pmtInf.setPmtInfId("PMTINF-" + UUID.randomUUID().toString().substring(0, 12));

        // PmtMtd: Payment method – "TRF" for Credit Transfer
        pmtInf.setPmtMtd(PaymentMethod3Code.TRF);

        // NbOfTxs: Transactions within this payment information block
        pmtInf.setNbOfTxs("1");

        // CtrlSum: Control sum for this block
        pmtInf.setCtrlSum(request.getAmount());

        // ReqdExctnDt: Requested execution date (today)
        DateAndDateTime2Choice execDate = new DateAndDateTime2Choice();
        execDate.setDt(todayAsXmlGregorianCalendar());
        pmtInf.setReqdExctnDt(execDate);

        // ── Debtor Information ──────────────────────────────────────────
        // Dbtr: Debtor party (the payer)
        PartyIdentification135 debtor = new PartyIdentification135();
        debtor.setNm(request.getDebtorName());
        pmtInf.setDbtr(debtor);

        // DbtrAcct: Debtor account (IBAN)
        CashAccount40 debtorAccount = new CashAccount40();
        AccountIdentification4Choice debtorAcctId = new AccountIdentification4Choice();
        debtorAcctId.setIBAN(request.getDebtorIban());
        debtorAccount.setId(debtorAcctId);
        pmtInf.setDbtrAcct(debtorAccount);

        // DbtrAgt: Debtor's bank (financial institution identified by BIC)
        BranchAndFinancialInstitutionIdentification6 debtorAgent =
                new BranchAndFinancialInstitutionIdentification6();
        FinancialInstitutionIdentification18 debtorFinInst =
                new FinancialInstitutionIdentification18();
        debtorFinInst.setBICFI(request.getDebtorBic());
        debtorAgent.setFinInstnId(debtorFinInst);
        pmtInf.setDbtrAgt(debtorAgent);

        // ── Credit Transfer Transaction ─────────────────────────────────
        CreditTransferTransaction54 cdtTrfTxInf = buildCreditTransferTransaction(request);
        pmtInf.getCdtTrfTxInf().add(cdtTrfTxInf);

        return pmtInf;
    }

    /**
     * Builds the CreditTransferTransaction54 block.
     *
     * <p>This is the credit-side detail for an individual payment within a
     * PaymentInformation block. It contains:
     * <ul>
     *   <li>Payment identification (InstrId, EndToEndId)</li>
     *   <li>Instructed amount and currency</li>
     *   <li>Creditor agent (BIC)</li>
     *   <li>Creditor name and account (IBAN)</li>
     *   <li>Remittance information</li>
     * </ul>
     */
    private CreditTransferTransaction54 buildCreditTransferTransaction(PaymentRequest request) {
        CreditTransferTransaction54 txInfo = new CreditTransferTransaction54();

        // ── Payment Identification ──────────────────────────────────────
        PaymentIdentification6 pmtId = new PaymentIdentification6();
        // InstrId: Instruction identification (unique within the message)
        pmtId.setInstrId("INSTR-" + UUID.randomUUID().toString().substring(0, 8));
        // EndToEndId: End-to-end identification (travels with the payment)
        pmtId.setEndToEndId(request.getEndToEndId() != null
                ? request.getEndToEndId()
                : "E2E-" + UUID.randomUUID().toString().substring(0, 12));
        txInfo.setPmtId(pmtId);

        // ── Amount ──────────────────────────────────────────────────────
        AmountType4Choice amount = new AmountType4Choice();
        ActiveOrHistoricCurrencyAndAmount instdAmt = new ActiveOrHistoricCurrencyAndAmount();
        instdAmt.setValue(request.getAmount());
        instdAmt.setCcy(request.getCurrency());
        amount.setInstdAmt(instdAmt);
        txInfo.setAmt(amount);

        // ── Creditor Agent (beneficiary's bank) ─────────────────────────
        if (request.getCreditorBic() != null) {
            BranchAndFinancialInstitutionIdentification6 creditorAgent =
                    new BranchAndFinancialInstitutionIdentification6();
            FinancialInstitutionIdentification18 creditorFinInst =
                    new FinancialInstitutionIdentification18();
            creditorFinInst.setBICFI(request.getCreditorBic());
            creditorAgent.setFinInstnId(creditorFinInst);
            txInfo.setCdtrAgt(creditorAgent);
        }

        // ── Creditor (beneficiary party) ────────────────────────────────
        PartyIdentification135 creditor = new PartyIdentification135();
        creditor.setNm(request.getCreditorName());
        txInfo.setCdtr(creditor);

        // ── Creditor Account (IBAN) ─────────────────────────────────────
        CashAccount40 creditorAccount = new CashAccount40();
        AccountIdentification4Choice creditorAcctId = new AccountIdentification4Choice();
        creditorAcctId.setIBAN(request.getCreditorIban());
        creditorAccount.setId(creditorAcctId);
        txInfo.setCdtrAcct(creditorAccount);

        // ── Remittance Information ──────────────────────────────────────
        if (request.getRemittanceInfo() != null) {
            RemittanceInformation21 rmtInf = new RemittanceInformation21();
            rmtInf.getUstrd().add(request.getRemittanceInfo());
            txInfo.setRmtInf(rmtInf);
        }

        return txInfo;
    }

    // =====================================================================
    //  Date/Time Utilities
    // =====================================================================

    /**
     * Creates an {@link XMLGregorianCalendar} representing the current instant.
     */
    private XMLGregorianCalendar nowAsXmlGregorianCalendar() {
        try {
            GregorianCalendar cal = GregorianCalendar.from(ZonedDateTime.now());
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XMLGregorianCalendar for CreDtTm", e);
        }
    }

    /**
     * Creates an {@link XMLGregorianCalendar} representing today's date (date-only, no time).
     */
    private XMLGregorianCalendar todayAsXmlGregorianCalendar() {
        try {
            LocalDate today = LocalDate.now();
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED,
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XMLGregorianCalendar for ReqdExctnDt", e);
        }
    }
}
