package com.westminster.smartcampus;

import com.westminster.smartcampus.config.JaxRsApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplicationClass(JaxRsApplication.class)
                .packages("com.westminster.smartcampus.resource",
                          "com.westminster.smartcampus.mapper",
                          "com.westminster.smartcampus.filter")
                .register(JacksonFeature.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = startServer();
        System.out.println("Smart Campus API started.");
        System.out.println("Discovery: " + BASE_URI);
        System.out.println("Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        Thread.currentThread().join();
    }
}
