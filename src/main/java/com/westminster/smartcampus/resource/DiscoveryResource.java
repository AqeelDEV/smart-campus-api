package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discovery(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base + "/rooms");
        resources.put("sensors", base + "/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api", "Smart Campus Sensor & Room Management API");
        body.put("version", "1.0.0");
        body.put("contact", "campus-ops@westminster.ac.uk");
        body.put("resources", resources);
        return body;
    }
}
