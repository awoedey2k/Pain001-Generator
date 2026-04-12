package com.lanre.personl.iso20022.pain001.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Flat POJO representing a simple payment request.
 *
 * <p>This object captures the minimum data needed to construct a valid
 * pain.001.001.11 (Customer Credit Transfer Initiation) message.
 *
 * <h3>Mapping to ISO 20022 Hierarchy</h3>
 * <pre>
 * PaymentRequest field       →  ISO 20022 Component / Element
 * ─────────────────────────────────────────────────────────────
 * (auto-generated)           →  GrpHdr / MsgId
 * (auto-generated)           →  GrpHdr / CreDtTm
 * (auto-generated)           →  GrpHdr / NbOfTxs
 * amount + currency          →  GrpHdr / CtrlSum  (mirrored from transaction)
 *
 * debtorName                 →  PmtInf / Dbtr / Nm
 * debtorIban                 →  PmtInf / DbtrAcct / Id / IBAN
 * debtorBic                  →  PmtInf / DbtrAgt / FinInstnId / BICFI
 *
 * amount                     →  PmtInf / CdtTrfTxInf / Amt / InstdAmt
 * currency                   →  PmtInf / CdtTrfTxInf / Amt / InstdAmt[@Ccy]
 * creditorName               →  PmtInf / CdtTrfTxInf / Cdtr / Nm
 * creditorIban               →  PmtInf / CdtTrfTxInf / CdtrAcct / Id / IBAN
 * creditorBic                →  PmtInf / CdtTrfTxInf / CdtrAgt / FinInstnId / BICFI
 * remittanceInfo             →  PmtInf / CdtTrfTxInf / RmtInf / Ustrd
 * endToEndId                 →  PmtInf / CdtTrfTxInf / PmtId / EndToEndId
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    // ── Debtor (Payer) ──────────────────────────────────────────────────
    /** Full legal name of the debtor (payer). Maps to Dbtr/Nm */
    private String debtorName;

    /** IBAN of the debtor's account. Maps to DbtrAcct/Id/IBAN */
    private String debtorIban;

    /** BIC/SWIFT code of the debtor's bank. Maps to DbtrAgt/FinInstnId/BICFI */
    private String debtorBic;

    // ── Creditor (Payee) ────────────────────────────────────────────────
    /** Full legal name of the creditor (beneficiary). Maps to Cdtr/Nm */
    private String creditorName;

    /** IBAN of the creditor's account. Maps to CdtrAcct/Id/IBAN */
    private String creditorIban;

    /** BIC/SWIFT code of the creditor's bank. Maps to CdtrAgt/FinInstnId/BICFI */
    private String creditorBic;

    // ── Transaction Details ─────────────────────────────────────────────
    /** Transfer amount (e.g., 1500.00). Maps to CdtTrfTxInf/Amt/InstdAmt */
    private BigDecimal amount;

    /** ISO 4217 currency code (e.g., "EUR", "USD"). Attribute on InstdAmt */
    private String currency;

    /** Unique end-to-end identifier for this transaction. Maps to PmtId/EndToEndId */
    private String endToEndId;

    /** Free-text remittance information (e.g., invoice number). Maps to RmtInf/Ustrd */
    private String remittanceInfo;
}
