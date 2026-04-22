package com.westminster.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "safety net" mapper (spec Part 5.2).
 *
 * Responsibilities:
 *   1. Intercept any unexpected runtime Throwable (NullPointerException,
 *      IndexOutOfBoundsException, etc.) and return a sanitised HTTP 500
 *      without leaking the stack trace to the client.
 *   2. Re-wrap built-in JAX-RS WebApplicationException instances (405, 406,
 *      415, ...) that do not have a more specific mapper, so every error
 *      response in the API uses the same JSON envelope {status, error, message}.
 *
 * More specific ExceptionMapper providers (NotFoundExceptionMapper,
 * RoomNotEmptyExceptionMapper, etc.) are preferred by JAX-RS dispatch and
 * will fire before this one for their own exception types.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {
        // -- Case 1: Framework-generated WebApplicationException ----------
        // (e.g. 405 Method Not Allowed, 415 Unsupported Media Type, 406 Not
        // Acceptable). These have the right status but an empty body by
        // default — re-wrap them in our uniform JSON envelope.
        if (t instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String reason = reasonPhrase(status);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status",  status);
            body.put("error",   reason);
            body.put("message", (wae.getMessage() == null || wae.getMessage().isBlank())
                                ? reason
                                : wae.getMessage());

            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }

        // -- Case 2: Truly unexpected error -- log server-side, return
        //    a generic 500 with NO stack-trace leakage to the client. ------
        LOG.log(Level.SEVERE, "Unhandled exception — returning generic 500", t);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  500);
        body.put("error",   "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the API administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    /** Map an HTTP status code to its canonical reason phrase. */
    private static String reasonPhrase(int status) {
        Response.Status s = Response.Status.fromStatusCode(status);
        if (s != null) return s.getReasonPhrase();
        // Unmapped status codes (e.g. 422) — provide a sensible fallback.
        return switch (status) {
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            default  -> "Error";
        };
    }
}
