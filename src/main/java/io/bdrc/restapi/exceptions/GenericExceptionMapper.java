package io.bdrc.restapi.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;



// for both Jersey exceptions and unchecked exceptions

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    
    public Response toResponse(Throwable ex) {
            ErrorMessage errorMessage = new ErrorMessage();
            if (ex instanceof WebApplicationException) {
                errorMessage.status = ((WebApplicationException)ex).getResponse().getStatus();
                errorMessage.developerMessage = null;
            } else {
                StringWriter errorStackTrace = new StringWriter();
                ex.printStackTrace(new PrintWriter(errorStackTrace));
                errorMessage.status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
                errorMessage.developerMessage = errorStackTrace.toString();
            }
            errorMessage.code = RestException.GENERIC_APP_ERROR_CODE;
            errorMessage.message = ex.getMessage();
            errorMessage.link = null;
                    
            return Response.status(errorMessage.status)
                    .entity(errorMessage)
                    .header("Access-Control-Allow-Origin", "*")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }