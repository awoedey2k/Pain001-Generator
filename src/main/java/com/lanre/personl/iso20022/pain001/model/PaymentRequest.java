package com.lanre.personl.iso20022.pain001.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Flat request model used to generate pain.001 and pacs.008 messages.")
public class PaymentRequest {

    // ── Debtor (Payer) ──────────────────────────────────────────────────
    /** Full legal name of the debtor (payer). Maps to Dbtr/Nm */
    @NotBlank
    @Schema(example = "Acme Corporation", description = "Full legal name of the debtor or payer.")
    private String debtorName;

    /** IBAN of the debtor's account. Maps to DbtrAcct/Id/IBAN */
    @NotBlank
    @Schema(example = "DE89370400440532013000", description = "Debtor account IBAN.")
    private String debtorIban;

    /** BIC/SWIFT code of the debtor's bank. Maps to DbtrAgt/FinInstnId/BICFI */
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{8}([A-Z0-9]{3})?$", message = "debtorBic must be an 8 or 11 character BIC.")
    @Schema(example = "COBADEFFXXX", description = "Debtor bank BIC or SWIFT code.")
    private String debtorBic;

    // ── Creditor (Payee) ────────────────────────────────────────────────
    /** Full legal name of the creditor (beneficiary). Maps to Cdtr/Nm */
    @NotBlank
    @Schema(example = "Widget Supplies Ltd", description = "Full legal name of the creditor or beneficiary.")
    private String creditorName;

    /** IBAN of the creditor's account. Maps to CdtrAcct/Id/IBAN */
    @NotBlank
    @Schema(example = "GB29NWBK60161331926819", description = "Creditor account IBAN.")
    private String creditorIban;

    /** BIC/SWIFT code of the creditor's bank. Maps to CdtrAgt/FinInstnId/BICFI */
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{8}([A-Z0-9]{3})?$", message = "creditorBic must be an 8 or 11 character BIC.")
    @Schema(example = "NWBKGB2L", description = "Creditor bank BIC or SWIFT code.")
    private String creditorBic;

    // ── Transaction Details ─────────────────────────────────────────────
    /** Transfer amount (e.g., 1500.00). Maps to CdtTrfTxInf/Amt/InstdAmt */
    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than zero.")
    @Schema(example = "1500.00", description = "Transfer amount.")
    private BigDecimal amount;

    /** ISO 4217 currency code (e.g., "EUR", "USD"). Attribute on InstdAmt */
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a three-letter ISO 4217 code.")
    @Schema(example = "EUR", description = "Three-letter ISO 4217 currency code.")
    private String currency;

    /** Unique end-to-end identifier for this transaction. Maps to PmtId/EndToEndId */
    @NotBlank
    @Schema(example = "INV-2026-00042", description = "Unique end-to-end transaction reference.")
    private String endToEndId;

    /** Free-text remittance information (e.g., invoice number). Maps to RmtInf/Ustrd */
    @Schema(example = "Invoice 2026-00042 payment", description = "Optional unstructured remittance text.")
    private String remittanceInfo;
}
