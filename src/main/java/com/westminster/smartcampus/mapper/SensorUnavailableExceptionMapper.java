package com.westminster.smartcampus.mapper;

import com.westminster.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("sensorId", ex.getSensorId());
        body.put("sensorStatus", ex.getStatus());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
