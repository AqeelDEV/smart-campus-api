package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root "Discovery" endpoint — HATEOAS entry point for the Smart Campus API.
 * Returns API metadata (version, contact, documentation) together with a map
 * of primary resource collections. Each resource entry includes:
 *   - "href":    absolute URL
 *   - "methods": HTTP verbs supported on the collection
 *   - "rel":     semantic relation name
 * This way a client that starts at /api/v1 can discover every top-level
 * capability without hard-coding URI shapes.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discovery(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        // --- Contact block (administrative details) ----------------------
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Operations");
        contact.put("team", "University of Westminster, School of CSE");
        contact.put("email", "campus-ops@westminster.ac.uk");

        // --- Resource map (HATEOAS links) --------------------------------
        Map<String, Object> rooms = new LinkedHashMap<>();
        rooms.put("rel",     "rooms");
        rooms.put("href",    base + "/rooms");
        rooms.put("methods", List.of("GET", "POST"));
        rooms.put("item",    base + "/rooms/{roomId}");

        Map<String, Object> sensors = new LinkedHashMap<>();
        sensors.put("rel",     "sensors");
        sensors.put("href",    base + "/sensors");
        sensors.put("methods", List.of("GET", "POST"));
        sensors.put("query",   Map.of("type", "Filter by sensor type, e.g. ?type=CO2"));
        sensors.put("item",    base + "/sensors/{sensorId}");
        sensors.put("nested",  Map.of(
                "readings", base + "/sensors/{sensorId}/readings"));

        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("rooms",   rooms);
        resources.put("sensors", sensors);

        // --- Self link ---------------------------------------------------
        Map<String, Object> self = new LinkedHashMap<>();
        self.put("rel",     "self");
        self.put("href",    base);
        self.put("methods", List.of("GET"));

        // --- Envelope ----------------------------------------------------
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api",           "Smart Campus Sensor & Room Management API");
        body.put("version",       "1.0.0");
        body.put("apiVersion",    "v1");
        body.put("description",   "RESTful service for managing rooms, sensors and historical sensor readings across a university smart-campus deployment.");
        body.put("documentation", "https://github.com/AqeelDev/smart-campus-api#readme");
        body.put("contact",       contact);
        body.put("_links",        Map.of("self", self));
        body.put("resources",     resources);
        return body;
    }
}
