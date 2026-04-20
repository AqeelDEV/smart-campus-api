package com.westminster.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) {
        LOG.info(String.format("REQ  %s %s", req.getMethod(), req.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        LOG.info(String.format("RES  %d  %s %s",
                res.getStatus(), req.getMethod(), req.getUriInfo().getRequestUri()));
    }
}
