package com.lanre.personl.iso20022.api.error;

public class DuplicateRequestException extends RuntimeException {

    private final String endToEndId;
    private final String messageFamily;

    public DuplicateRequestException(String endToEndId, String messageFamily) {
        super("Duplicate request detected for endToEndId=" + endToEndId + ", messageFamily=" + messageFamily);
        this.endToEndId = endToEndId;
        this.messageFamily = messageFamily;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getMessageFamily() {
        return messageFamily;
    }
}
