package be.cohabitapi.cohabitapi.API;

import jakarta.ws.rs.core.Response;

import java.util.Collections;

// This API is made just to be a sort of library of methods that we use in at least two files
public class APIResponse {

    public APIResponse() {
    }

    // Builds an error response
    public static Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(Collections.singletonMap(
                        "message", message
                ))
                .build();
    }
}
