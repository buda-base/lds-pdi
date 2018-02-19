package io.bdrc.restapi.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


@Provider
public class RestExceptionMapper implements ExceptionMapper<RestException> {

    @Override
    public Response toResponse(RestException exception) 
    {
        return Response.status(exception.status)
                .entity(new ErrorMessage(exception))
                //.header("Access-Control-Allow-Origin", "*")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
}

