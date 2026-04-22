package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor parentSensor;
    private final DataStore store = DataStore.INSTANCE;

    public SensorReadingResource(Sensor parentSensor) {
        this.parentSensor = parentSensor;
    }

    @GET
    public List<SensorReading> history() {
        return store.readingsFor(parentSensor.getId());
    }

    @POST
    public Response append(SensorReading incoming, @Context UriInfo uriInfo) {
        // Spec Part 5.3: a sensor marked MAINTENANCE is physically disconnected
        // and must not accept new readings. Map to HTTP 403 Forbidden.
        if ("MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            throw new SensorUnavailableException(parentSensor.getId(), parentSensor.getStatus());
        }

        SensorReading reading = new SensorReading(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                incoming.getValue()
        );

        List<SensorReading> history = store.readingsFor(parentSensor.getId());
        history.add(reading);
        parentSensor.setCurrentValue(reading.getValue());

        return Response.created(uriInfo.getAbsolutePathBuilder().path(reading.getId()).build())
                       .entity(reading)
                       .build();
    }
}
