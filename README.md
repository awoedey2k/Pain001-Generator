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
