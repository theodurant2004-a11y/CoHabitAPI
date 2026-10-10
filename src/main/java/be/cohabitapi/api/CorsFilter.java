package be.cohabitapi.cohabitapi.API;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    // Frontend origins allowed to call the API
    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>(Arrays.asList(
            "http://127.0.0.1:5501",
            "http://localhost:5501"
    ));

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // Which website is calling the API? (sent automatically by the browser)
        String origin = request.getHeaderString("Origin");

        // Unknown or missing origin → add nothing, the browser will block it
        if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
            return;
        }

        // Allow this exact origin
        response.getHeaders().add("Access-Control-Allow-Origin", origin);
        // Allow the browser to send/receive cookies
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        // Headers the frontend is allowed to send
        response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Accept");
        // HTTP methods the frontend is allowed to use
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        // The answer depends on the origin → tells caches
        response.getHeaders().add("Vary", "Origin");
    }
}