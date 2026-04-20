package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.RoomNotEmptyException;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final DataStore store = DataStore.INSTANCE;

    @GET
    public Collection<Room> listAll() {
        return store.allRooms();
    }

    @GET
    @Path("/{roomId}")
    public Room getOne(@PathParam("roomId") String roomId) {
        Room r = store.getRoom(roomId);
        if (r == null) throw new NotFoundException("Room '" + roomId + "' not found");
        return r;
    }

    @POST
    public Response create(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        store.putRoom(room);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(room.getId()).build())
                       .entity(room)
                       .build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response delete(@PathParam("roomId") String roomId) {
        Room r = store.getRoom(roomId);
        if (r == null) throw new NotFoundException("Room '" + roomId + "' not found");

        long linked = store.allSensors().stream()
                .filter(s -> roomId.equals(s.getRoomId()))
                .count();
        int totalSensors = Math.max(r.getSensorIds().size(), (int) linked);
        if (totalSensors > 0) {
            throw new RoomNotEmptyException(roomId, totalSensors);
        }

        store.removeRoom(roomId);
        return Response.noContent().build();
    }
}
