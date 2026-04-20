package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum DataStore {
    INSTANCE;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    DataStore() {
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 40);
        Room hall1 = new Room("HALL-01", "Main Lecture Hall", 200);
        rooms.put(lib301.getId(), lib301);
        rooms.put(hall1.getId(), hall1);

        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor co2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "HALL-01");
        sensors.put(temp.getId(), temp);
        sensors.put(co2.getId(), co2);
        lib301.getSensorIds().add(temp.getId());
        hall1.getSensorIds().add(co2.getId());

        readings.put(temp.getId(), Collections.synchronizedList(new ArrayList<>()));
        readings.put(co2.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    public Collection<Room> allRooms() { return rooms.values(); }
    public Room getRoom(String id) { return rooms.get(id); }
    public boolean roomExists(String id) { return rooms.containsKey(id); }
    public void putRoom(Room r) { rooms.put(r.getId(), r); }
    public Room removeRoom(String id) { return rooms.remove(id); }

    public Collection<Sensor> allSensors() { return sensors.values(); }
    public Sensor getSensor(String id) { return sensors.get(id); }
    public boolean sensorExists(String id) { return sensors.containsKey(id); }
    public void putSensor(Sensor s) {
        sensors.put(s.getId(), s);
        readings.computeIfAbsent(s.getId(), k -> Collections.synchronizedList(new ArrayList<>()));
    }
    public Sensor removeSensor(String id) {
        readings.remove(id);
        return sensors.remove(id);
    }

    public List<SensorReading> readingsFor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()));
    }
}
