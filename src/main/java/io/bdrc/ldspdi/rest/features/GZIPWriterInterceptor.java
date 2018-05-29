package io.bdrc.ldspdi.rest.features;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GZIPWriterInterceptor implements WriterInterceptor{

    public final static Logger log=LoggerFactory.getLogger(GZIPWriterInterceptor.class.getName());
    private HttpHeaders httpHeaders;

    
    public GZIPWriterInterceptor(@Context @NotNull HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
    
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        MultivaluedMap<String,String> requestHeaders =  httpHeaders.getRequestHeaders();
        List<String> acceptEncoding = requestHeaders.get(HttpHeaders.ACCEPT_ENCODING);
        boolean process=false;
        if(acceptEncoding !=null) {                        
            for(String st:acceptEncoding) {                  
                if(st.contains("gzip")) {process=true;}
            }
        }
        MultivaluedMap<String,Object> headers = context.getHeaders();              
        List<Object> l=headers.get("Vary");        
        if(process) {            
            headers.add("Content-Encoding", "gzip");
            if(l!=null) {
                headers.putSingle("Vary", "Accept, Accept-Encoding");
            }else {
                headers.putSingle("Vary", "Accept-Encoding");
            }
            final OutputStream outputStream = context.getOutputStream();
            context.setOutputStream(new GZIPOutputStream(outputStream));
            context.proceed();
        }else {            
            context.proceed();
        }
    }

}
