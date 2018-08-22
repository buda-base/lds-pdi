package io.bdrc.auth.rdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

public class ModelUpdate extends TimerTask{
    
    long time;

    @Override
    public void run() {
        long time=1;
        Long obj=Long.valueOf(RdfAuthModel.getUpdated());
        if(obj!=null) {
            time=(long)obj;
        }
        //System.out.println("TIME >>"+time);
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(ServiceConfig.getProperty("authUpdatePath"));
        //HttpGet get=new HttpGet("http://localhost:8080/authmodel/updated");
        long lastUpdate=1;
        try {
            HttpResponse resp=client.execute(get);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            resp.getEntity().writeTo(baos);
            lastUpdate = Long.parseLong(baos.toString());
            //System.out.println("UPDATED >>"+lastUpdate);
            if(lastUpdate > time) {
                //System.out.println("<< UPDATE NEEDED >>");
                //do update
                RdfAuthModel.update(lastUpdate);
            }
            else {
                //test purpose only
                //System.out.println("<< Model is up to date >>");
            }
        } catch (IOException | RestException e) {            
            e.printStackTrace();
        }
    }

}
