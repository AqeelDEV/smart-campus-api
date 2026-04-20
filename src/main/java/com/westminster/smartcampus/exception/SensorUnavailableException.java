package com.westminster.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {
    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is in state '" + status + "' and cannot accept readings.");
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() { return sensorId; }
    public String getStatus() { return status; }
}
