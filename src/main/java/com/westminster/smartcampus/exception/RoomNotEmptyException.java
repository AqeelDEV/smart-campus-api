package com.westminster.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {
    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' still has " + sensorCount + " active sensor(s) and cannot be deleted.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId() { return roomId; }
    public int getSensorCount() { return sensorCount; }
}
