# ISO 20022 `pain.001` Generator

A Spring Boot utility that seamlessly generates a valid ISO 20022 `pain.001.001.11` (Customer Credit Transfer Initiation) XML message based on a flat, simple Java/JSON input.

Built completely around the robust **Prowide ISO 20022 open-source library**, this tool handles all the complex hierarchical mappings—like transforming a flat representation into the standards-compliant `GroupHeader`, `PaymentInformation`, and `CreditTransferTransactionInformation` structures.

## 🚀 Key Features

- **Java 17+ & Spring Boot 3.x Ready**: Built using the latest standards and specifically patched for JDK versions that removed the legacy `javax.xml.bind` packages.
- **Prowide ISO 20022 Integration**: Leverages the official schemas provided by SWIFT and standardized by Prowide to ensure 100% adherence to standard XML paths and attribute properties.
- **REST Interface Built-In**: Includes a RESTful endpoint to supply payment context payload as JSON while receiving perfectly formatted and indented XML strings.
- **Automatic Fields**: Automatically handles `MsgId`, `CreDtTm`, `NbOfTxs`, `CtrlSum`, and auto-generates End-to-End identification components safely.

## 📦 Requirements

- **Java 17** or higher
- **Maven** 3.8+

## 🏁 How to Run

1. **Clone the repository** (if you haven't already):

   ```bash
   git clone https://github.com/awoedey2k/Pain001-Generator.git
   cd "Pain001-Generator"
   ```

2. **Clean & Build**:

   ```bash
   mvn clean install
   ```

3. **Start the Application**:

   ```bash
   mvn spring-boot:run
   ```

   > The application launches on default port `8080`.

## 🛠️ Usage Example

You can generate the XML on the fly by hitting the provided endpoint utilizing `cURL` or Postman.

**Endpoint**: `POST http://localhost:8080/api/v1/pain001`
**Header**: `Content-Type: application/json`

**Sample Request Payload:**

```json
{
  "debtorName": "Acme Corporation",
  "debtorIban": "DE89370400440532013000",
  "debtorBic": "COBADEFFXXX",
  "creditorName": "Widget Supplies Ltd",
  "creditorIban": "GB29NWBK60161331926819",
  "creditorBic": "NWBKGB2L",
  "amount": 1500.00,
  "currency": "EUR",
  "endToEndId": "INV-2026-00042",
  "remittanceInfo": "Invoice 2026-00042 payment"
}
```

**Expected XML Response (`application/xml`):**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<Doc:Document xmlns:Doc="urn:iso:std:iso:20022:tech:xsd:pain.001.001.11">
    <Doc:CstmrCdtTrfInitn>
        <Doc:GrpHdr>
            <Doc:MsgId>MSGID-20260412123300-4fe65c60</Doc:MsgId>
            <Doc:CreDtTm>2026-04-12T12:33:00</Doc:CreDtTm>
            <Doc:NbOfTxs>1</Doc:NbOfTxs>
            <Doc:CtrlSum>1500.00</Doc:CtrlSum>
            <Doc:InitgPty>
                <Doc:Nm>Acme Corporation</Doc:Nm>
            </Doc:InitgPty>
        </Doc:GrpHdr>
        <Doc:PmtInf>
            <Doc:PmtInfId>PMTINF-4fe65c60-b12</Doc:PmtInfId>
            <Doc:PmtMtd>TRF</Doc:PmtMtd>
            <Doc:NbOfTxs>1</Doc:NbOfTxs>
            <Doc:CtrlSum>1500.00</Doc:CtrlSum>
            <Doc:ReqdExctnDt>
                <Doc:Dt>2026-04-12</Doc:Dt>
            </Doc:ReqdExctnDt>
            <Doc:Dbtr>
                <Doc:Nm>Acme Corporation</Doc:Nm>
            </Doc:Dbtr>
            <Doc:DbtrAcct>
                <Doc:Id>
                    <Doc:IBAN>DE89370400440532013000</Doc:IBAN>
                </Doc:Id>
            </Doc:DbtrAcct>
            <Doc:DbtrAgt>
                <Doc:FinInstnId>
                    <Doc:BICFI>COBADEFFXXX</Doc:BICFI>
                </Doc:FinInstnId>
            </Doc:DbtrAgt>
            <Doc:CdtTrfTxInf>
                <Doc:PmtId>
                    <Doc:InstrId>INSTR-abf05f10</Doc:InstrId>
                    <Doc:EndToEndId>INV-2026-00042</Doc:EndToEndId>
                </Doc:PmtId>
                <Doc:Amt>
                    <Doc:InstdAmt Ccy="EUR">1500.00</Doc:InstdAmt>
                </Doc:Amt>
                <Doc:CdtrAgt>
                    <Doc:FinInstnId>
                        <Doc:BICFI>NWBKGB2L</Doc:BICFI>
                    </Doc:FinInstnId>
                </Doc:CdtrAgt>
                <Doc:Cdtr>
                    <Doc:Nm>Widget Supplies Ltd</Doc:Nm>
                </Doc:Cdtr>
                <Doc:CdtrAcct>
                    <Doc:Id>
                        <Doc:IBAN>GB29NWBK60161331926819</Doc:IBAN>
                    </Doc:Id>
                </Doc:CdtrAcct>
                <Doc:RmtInf>
                    <Doc:Ustrd>Invoice 2026-00042 payment</Doc:Ustrd>
                </Doc:RmtInf>
            </Doc:CdtTrfTxInf>
        </Doc:PmtInf>
    </Doc:CstmrCdtTrfInitn>
</Doc:Document>
```

## 🛡️ Gatekeeper Validation Engine

This project also includes a robust, multi-stage validation pipeline for incoming `pain.001` messages.

**Endpoint**: `POST http://localhost:8080/api/v1/validate/pain001`
**Header**: `Content-Type: application/xml` OR `text/xml`

The Gatekeeper enforces:

1. **Stage 1 (Technical)**: Strict validation against the official SWIFT/ISO `pain.001.001.11.xsd` tracking syntax and XML namespace integrity.
2. **Stage 2 (Semantic)**: Deep inspection validating elements against canonical ISO tables (e.g., ISO 4217 Currency Codes).
3. **Stage 3 (Business Constraints)**: Execution Date limits blocking weekend processing or retroactive dates.

**Responses (`application/xml`)**: Native ISO 20022 Status!

- **Valid Message**: Returns a `pain.002.001.10` Customer Payment Status Report mapped purely to **ACCP** (Accepted).
- **Invalid Message**: If parsing bounces in any layer, natively returns a `pain.002` tracking the exact error layer via `RJCT` (Rejected) `<StsRsnInf>` reason codes.

## 🌐 Legacy MT103 Translator & Pacs.008 Expansion

The system has been expanded natively processing ISO 20022 Interbank (`pacs.008.001.10`) structures alongside Legacy SWIFT `MT103` string conversions, bridging interoperability gaps.

### The Legacy Translator Mapping
The legacy translator automatically decodes a raw FIN string and bridges it identically into the `pacs.008` object hierarchy utilizing native Prowide extraction algorithms.

**Endpoint**: `POST http://localhost:8080/api/v1/translator/mt103`
**Header**: `Content-Type: text/plain`

**Sample Request Payload (MT103 Text):**
```text
{1:F01BANKDEFMAXXX2039063581}{2:O1031609160904BANKDEFXAXXX89549829458949811609N}{4:
:20:O-0T21516
:32A:210412USD100000,
:50K:/123456789
JOHN DOE
123 MAIN STREET
NEW YORK, NY
US
:59:/987654321
JANE DOE
456 OAK AVENUE
LONDON
UK
-}
```

**Expected Translation Mapping (`pacs.008`):**
The generated elements perfectly encapsulate "Data Overflow" safeguards utilizing the versatile 7-iteration `<AdrLine>` natively permitted by ISO.

| Legacy MT103 Field | ISO 20022 `pacs.008` Node | Translation Rule |
| :--- | :--- | :--- |
| `:32A:` Date, Currency, Amount | `<TtlIntrBkSttlmAmt Ccy="...">` <br> `<IntrBkSttlmDt>` | Safely parses the unstructured Date into rigid XML patterns seamlessly while extracting numeric values. |
| `:50K:` Ordering Customer | `<Dbtr>` <br> `<Nm>` & `<PstlAdr>` | Line 1 maps to `<Nm>`. Remaining Lines 2-4 map identically as raw strings safely descending into iteration sequences inside `<AdrLine>` to negate truncation. |
| `:59:` Beneficiary Customer | `<Cdtr>` & `<CdtrAcct>` | Evaluates prefix boundaries dynamically, resolving exact `<Id><Othr>` paths alongside name assignments. |

### Pacs.008 Gatekeeper
It validates the parsed Interbank payload identically against `pacs.008.001.10.xsd` definitions whilst evaluating business limitations natively enforcing standard BIC rules (enforcing exactly 8 or 11 character constraints securely against `InstgAgt` and `InstdAgt` `BICFI` records).

**Endpoint**: `POST http://localhost:8080/api/v1/validate/pacs008`
**Header**: `Content-Type: application/xml` OR `text/xml`


### Pacs.008 Generation (Interbank Settlement)
Similar to `pain.001`, you can generate an Interbank Settlement message directly from a JSON payload.

**Endpoint**: `POST http://localhost:8080/api/v1/pacs008`
**Header**: `Content-Type: application/json`

| Request Field | pacs.008 Mapping |
| :--- | :--- |
| `amount` / `currency` | `<IntrBkSttlmAmt>` & `<InstdAmt>` |
| `debtorName` / `debtorIban` / `debtorBic` | `<Dbtr>`, `<DbtrAcct>`, `<DbtrAgt>` |
| `creditorName` / `creditorIban` / `creditorBic` | `<Cdtr>`, `<CdtrAcct>`, `<CdtrAgt>` |
| `endToEndId` | `<PmtId>/<EndToEndId>` |

## 🕹️ Intelligent Payment Router (The Switch)

The system is now a functional **ISO 20022 Switch**, capable of routing `pacs.008` messages to specialized clearing adapters with BAH wrapping and real-time auditing.

**Endpoint**: `POST http://localhost:8080/api/v1/switch/route`  
**Header**: `Content-Type: application/xml`

### Switch Features:
- **BAH Wrapping**: Automatically wraps payloads with the `head.001.001.03` Business Application Header for standard interbank transmission.
- **Intelligent Routing Logic**: Uses the **Strategy Pattern** to select destinations:
    - **Rule A (SEPA)**: Routes `EUR` payments to a mock SEPA Instant service.
    - **Rule B (Fedwire)**: Routes `USD` payments to a Fedwire mock service.
    - **Rule C (Priority)**: Prioritizes BICs on the "High-Value" list (e.g., `CITIUS33XXX`) regardless of currency.
- **Fail-Safe Rejections**: If no routing rule matches (e.g., `GBP`), it generates a native **`pacs.002.001.12`** rejection XML.
- **JPA Audit Trail**: Every decision is persisted in the `PAYMENT_ROUTING_AUDIT` table for compliance tracking.

## 🧠 How the Code Works

The magic primarily takes place in `Pain001GeneratorService.java`. It converts a basic POJO class mapped from your inbound JSON into the heavily nested model entities supported by the Prowide open-source framework (`SRU2023` package release).

| PaymentRequest Variable | Generated Element |
| :--- | :--- |
| `amount` + `currency` | `<CtrlSum>` (GroupLevel) & `<InstdAmt Ccy="...">` (CreditLevel) |
| `debtorName` / `debtorIban` / `debtorBic` | `<PmtInf>/<Dbtr>`, `<DbtrAcct>`, `<DbtrAgt>` |
| `creditorName` / `creditorIban` / `creditorBic` | `<CdtTrfTxInf>/<Cdtr>`, `<CdtrAcct>`, `<CdtrAgt>` |
| `remittanceInfo` | `<CdtTrfTxInf>/<RmtInf>/<Ustrd>` |
| `endToEndId` | `<CdtTrfTxInf>/<PmtId>/<EndToEndId>` |

## ✅ Running Tests

```bash
mvn test
```

The project utilizes `JUnit 5` to analyze XML containment, testing the creation of tags mapping specifically across the required root node elements (`<Doc:Document>`, `<Doc:CstmrCdtTrfInitn>`) down to deep value validation routines safely resolving nested structural data points.
