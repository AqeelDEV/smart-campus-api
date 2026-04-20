package com.westminster.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    private final String linkedField;
    private final String linkedValue;

    public LinkedResourceNotFoundException(String linkedField, String linkedValue) {
        super("Referenced " + linkedField + " '" + linkedValue + "' does not exist.");
        this.linkedField = linkedField;
        this.linkedValue = linkedValue;
    }

    public String getLinkedField() { return linkedField; }
    public String getLinkedValue() { return linkedValue; }
}
