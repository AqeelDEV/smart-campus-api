package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.INSTANCE;

    @GET
    public List<Sensor> list(@QueryParam("type") String type) {
        if (type == null || type.isBlank()) {
            return store.allSensors().stream().collect(Collectors.toList());
        }
        return store.allSensors().stream()
                .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{sensorId}")
    public Sensor getOne(@PathParam("sensorId") String sensorId) {
        Sensor s = store.getSensor(sensorId);
        if (s == null) throw new NotFoundException("Sensor '" + sensorId + "' not found");
        return s;
    }

    @POST
    public Response create(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor.getRoomId() == null || !store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.putSensor(sensor);
        Room parent = store.getRoom(sensor.getRoomId());
        if (!parent.getSensorIds().contains(sensor.getId())) {
            parent.getSensorIds().add(sensor.getId());
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build())
                       .entity(sensor)
                       .build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor s = store.getSensor(sensorId);
        if (s == null) throw new NotFoundException("Sensor '" + sensorId + "' not found");
        return new SensorReadingResource(s);
    }

    @DELETE
    @Path("/{sensorId}")
    public Response delete(@PathParam("sensorId") String sensorId) {
        Sensor s = store.getSensor(sensorId);
        if (s == null) throw new NotFoundException("Sensor '" + sensorId + "' not found");

        Room parent = store.getRoom(s.getRoomId());
        if (parent != null) {
            parent.getSensorIds().remove(sensorId);
        }
        store.removeSensor(sensorId);
        return Response.noContent().build();
    }
}
