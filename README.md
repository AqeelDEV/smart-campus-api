# Smart Campus Sensor & Room Management API

A JAX-RS RESTful service built for the 5COSC022W *Client-Server Architectures* coursework.
It models the core data of a university "Smart Campus" — **Rooms**, **Sensors** deployed inside those rooms, and the **historical readings** each sensor emits.

---

## Overview

- **Tech stack:** Java 17, JAX-RS 2.x (`javax.ws.rs`), Jersey 2.39.1, embedded Grizzly HTTP server, Jackson for JSON.
- **Storage:** in-memory only (`ConcurrentHashMap` + `ArrayList`) — no database.
- **Base URL:** `http://localhost:8080/api/v1/`
- **Versioning:** declared via `@ApplicationPath("/api/v1")` on `JaxRsApplication`.

### Resource hierarchy

```
/api/v1                                  -> Discovery (HATEOAS entrypoint)
/api/v1/rooms                            -> Room collection
/api/v1/rooms/{roomId}                   -> single Room
/api/v1/sensors                          -> Sensor collection  (?type=CO2 filter)
/api/v1/sensors/{sensorId}               -> single Sensor
/api/v1/sensors/{sensorId}/readings      -> Readings sub-resource (history + append)
```

### HTTP contract

| Outcome                                          | Status | Body                              |
|--------------------------------------------------|--------|-----------------------------------|
| Successful read                                  | 200    | JSON resource                     |
| Successful create                                | 201    | JSON resource + `Location` header |
| Successful delete                                | 204    | empty                             |
| Missing resource                                 | 404    | JSON error                        |
| `Content-Type` not `application/json` on POST    | 415    | empty (JAX-RS default)            |
| Referenced `roomId` doesn't exist                | 422    | JSON error                        |
| Delete room that still has sensors               | 409    | JSON error                        |
| POST reading to a `MAINTENANCE` / `OFFLINE` sensor | 403  | JSON error                        |
| Unexpected server error                          | 500    | sanitised JSON (no stack trace)   |

Every error response uses the same shape: `{"status", "error", "message", ...context}`.

---

## Build & run

### Prerequisites
- JDK 17 (or later)
- Apache Maven 3.8+ (NetBeans ships with a compatible one under `…\Apache NetBeans\java\maven\bin\`)
- Port `8080` free

### From the terminal

```bash
git clone <your-repo-url>
cd smart-campus-api
mvn clean package
mvn exec:java
```

The console prints:

```
Smart Campus API started.
Discovery: http://localhost:8080/api/v1/
Press Ctrl+C to stop.
```

Stop the server with **Ctrl+C**.

### From NetBeans

1. **File → Open Project...** → select the `smart-campus-api` folder.
2. Wait for indexing / dependency download to finish.
3. **Right-click the project → Run** (NetBeans invokes `exec:java` using the bundled Maven).
4. Stop with the red square in the Output window.

---

## Seeded demo data

On startup `DataStore.INSTANCE` seeds the in-memory maps so the API is demo-ready immediately:

| Rooms     | Sensors   | Status | Room     |
|-----------|-----------|--------|----------|
| `LIB-301` (Library Quiet Study, cap 40) | `TEMP-001` (Temperature, 21.5) | ACTIVE | `LIB-301` |
| `HALL-01` (Main Lecture Hall, cap 200)  | `CO2-001`  (CO2, 412.0)        | ACTIVE | `HALL-01` |

---

## Sample `curl` commands

All commands assume the server is running at `http://localhost:8080`.

### 1. Discovery (HATEOAS)
```bash
curl -i http://localhost:8080/api/v1/
```

### 2. Create a room (201 + Location)
```bash
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"id":"LAB-101","name":"Physics Lab","capacity":30}' \
  http://localhost:8080/api/v1/rooms
```

### 3. Register a sensor tied to that room
```bash
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"id":"OCC-001","type":"Occupancy","status":"ACTIVE","roomId":"LAB-101"}' \
  http://localhost:8080/api/v1/sensors
```

### 4. Register a sensor with a bad roomId (**422 Unprocessable Entity**)
```bash
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"type":"CO2","status":"ACTIVE","roomId":"NOPE"}' \
  http://localhost:8080/api/v1/sensors
```

### 5. Filter sensors by type
```bash
curl -i "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### 6. Append a reading (bumps the parent sensor's `currentValue`)
```bash
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"value":22.5}' \
  http://localhost:8080/api/v1/sensors/TEMP-001/readings
curl -s http://localhost:8080/api/v1/sensors/TEMP-001
```

### 7. Try to delete a room that still has sensors (**409 Conflict**)
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 8. Wrong content type (**415 Unsupported Media Type** — JAX-RS default)
```bash
curl -i -X POST -H "Content-Type: text/plain" --data "hi" \
  http://localhost:8080/api/v1/sensors
```

---

## Report — answers to the specification questions

*(Each task's mark is split 50% code / 30% video / 20% written answer — the answers below are worth that 20%.)*

### Part 1.1 — JAX-RS resource lifecycle & concurrency

By default, JAX-RS treats a root resource class as **per-request**: a fresh instance is constructed for every incoming HTTP request, so instance fields cannot be assumed to survive across requests. Any state that must persist (the `rooms`, `sensors`, and `readings` maps in this API) therefore has to live *outside* the resource — here it lives in the `DataStore` enum singleton, which exists for the lifetime of the JVM. Because Grizzly dispatches requests on a worker-thread pool, that singleton is accessed concurrently; I used `ConcurrentHashMap` for the top-level collections and `Collections.synchronizedList` for the per-sensor reading history so that map reads, map writes, and list appends are all safe without coarse-grained `synchronized` blocks. If I had used a plain `HashMap`, a concurrent `put` during a re-hash could lose entries or throw `ConcurrentModificationException`; if I had tried to cache data on resource fields I would effectively be throwing it away every request.

### Part 1.2 — Why HATEOAS matters

The `/api/v1` discovery endpoint returns not only metadata but a `resources` map whose values are **absolute URIs** to the top-level collections (`rooms`, `sensors`). This is the HATEOAS idea: the server, not the client, is the source of truth for *what actions are possible and where*. A client that started at `/api/v1` and followed links never needs to hard-code a URL pattern; if the API later moves `sensors` to a new path or publishes a new resource, the client picks it up automatically. Compared to static documentation, hypermedia (1) couples the client to URI *roles* instead of URI *shapes*, (2) lets the server progressively enable/disable capabilities at runtime, and (3) makes the API discoverable from the root, which is how browsers have scaled the human web.

### Part 2.1 — IDs vs full objects in list responses

`GET /rooms` currently returns full `Room` objects. Returning only IDs would shrink each response dramatically and save bandwidth when the caller just needs to page through identifiers (e.g. an autocomplete). The cost is that every client that actually needs `name` or `capacity` must make a follow-up `GET /rooms/{id}` for every single entry — the classic "N+1 request" problem, which is usually slower overall than one larger payload. The pragmatic middle ground is a thin summary DTO (`id`, `name`, `capacity`) on the collection endpoint and the full object on the detail endpoint; since the coursework's `Room` already is small, returning the full objects is a reasonable default.

### Part 2.2 — Is DELETE idempotent here?

Yes. Idempotency is a statement about *resulting state*, not the response code. First `DELETE /rooms/LAB-101` returns **204** and removes the room; a second identical call returns **404** (the room is already gone), but the state of the server is unchanged — it is still "no LAB-101". Because the end state is the same no matter how many times the call is made, the operation satisfies RFC 7231's definition of idempotent. Note this is distinct from "safe": DELETE does mutate state, so it is idempotent but not safe.

### Part 3.1 — `@Consumes(APPLICATION_JSON)` mismatch

`@Consumes` is used during **content negotiation**, before the resource method is invoked. When a client sends `Content-Type: text/plain` to an endpoint that only declares `@Consumes(MediaType.APPLICATION_JSON)`, Jersey's request matcher cannot find a method able to accept that body, so the framework short-circuits and returns **415 Unsupported Media Type**. The method body never runs, which is exactly what we want — `MessageBodyReader<Sensor>` for JSON would never have been able to parse a plain-text payload, and letting it try would surface an internal parsing error to the client instead of a well-defined protocol-level response.

### Part 3.2 — `@QueryParam` vs path for filtering

A query parameter (`/sensors?type=CO2`) expresses *"the same collection, narrowed by a criterion"*. A path segment (`/sensors/type/CO2`) would assert that `CO2` is a distinct sub-resource, which it isn't — it's the same collection filtered differently. Query parameters also compose cleanly (`?type=CO2&status=ACTIVE&limit=50`) while path segments force a combinatorial explosion of routes. Caching behaviour is also clearer: intermediaries treat `?type=CO2` as a variant of the parent resource rather than as a separate entity, which matches the real semantics.

### Part 4.1 — Benefits of the sub-resource locator pattern

`SensorResource.readings(...)` is a **sub-resource locator**: it has no HTTP verb annotation, it only resolves the path prefix and returns a `SensorReadingResource` instance. That handoff gives us two concrete benefits. First, separation of concerns — `SensorResource` does not need to know anything about reading history, and `SensorReadingResource` only knows about one concrete sensor (it receives it in its constructor, so every method inside has trivial access to `parentSensor`). Second, composability — when tomorrow we add filtering or aggregation on readings, the new `@GET` / `@POST` methods simply live in `SensorReadingResource` instead of bloating `SensorResource` with `sensors/{id}/readings/...` endpoints. The alternative — a single monolithic controller handling every path level — scales linearly with complexity and becomes unreadable quickly.

### Part 5.2 — Why 422, not 404

A **404** means "the URL you asked for does not name anything in my universe." That is not what happened when the client POSTs `{"roomId":"NOPE"}` to `/sensors`: the URL `/sensors` is valid, the method is valid, and the JSON parses correctly — the body just references a room that does not exist. **422 Unprocessable Entity** was introduced by WebDAV and reused by modern APIs for exactly this scenario: syntactically valid, semantically invalid. Returning 404 would confuse the client into thinking `/sensors` itself is gone, which would break retry logic or monitoring that watches for missing endpoints. 422 keeps the diagnosis inside the payload, where the field and value pinpoint the exact problem.

### Part 5.4 — Cybersecurity risk of leaked stack traces

`GenericExceptionMapper` logs the full `Throwable` to the server but returns a bland JSON body to the client. Returning the raw trace instead would hand attackers a reconnaissance goldmine: the exact Jersey version (useful for CVE matching), the JDK build, internal package names that reveal project layout, file paths that hint at the deployment environment, and sometimes snippets of query text or sanitized SQL. An attacker with that information can target known vulnerabilities, phrase social-engineering attacks more convincingly, or probe more efficiently for misconfigurations. The OWASP "Improper Error Handling" category exists because of exactly this failure mode.

### Part 5.5 — Why filters for cross-cutting concerns

`LoggingFilter` is one class that logs every request and every response for the entire API. If the same observability were expressed as `Logger.info(...)` calls inside each resource method, you would need to remember to add them to every new endpoint, the log format would drift across contributors, and refactoring would touch dozens of files to change one field. Filters make logging **pluggable** — annotate with `@Provider`, register once, and every request/response flows through it, including error responses produced by exception mappers. The same mechanism scales to authentication, request-id propagation, metrics, and CORS, which is why JAX-RS ships filters as the supported extension point for cross-cutting behaviour.

---

## Project layout

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/westminster/smartcampus/
    ├── Main.java                       # boots Grizzly on :8080
    ├── config/
    │   └── JaxRsApplication.java       # @ApplicationPath("/api/v1")
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java              # enum singleton, thread-safe maps
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── SensorRoomResource.java
    │   ├── SensorResource.java         # contains the readings() sub-resource locator
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   └── SensorUnavailableException.java
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java                # 409
    │   ├── LinkedResourceNotFoundExceptionMapper.java      # 422
    │   ├── SensorUnavailableExceptionMapper.java           # 403
    │   ├── NotFoundExceptionMapper.java                    # 404 (clean JSON)
    │   └── GenericExceptionMapper.java                     # 500 safety net
    └── filter/
        └── LoggingFilter.java                              # request + response
```
