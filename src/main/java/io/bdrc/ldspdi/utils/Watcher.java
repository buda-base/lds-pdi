package io.bdrc.ldspdi.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.ldspdi.service.ServiceConfig;

public class Watcher implements Runnable {

    public final static Logger log = LoggerFactory.getLogger(Watcher.class);

    long time;
    String query, template;

    public Watcher(long time, String query, String template) {
        this.time = time;
        this.query = query;
        this.template = template;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public void run() {

        if (time > Long.parseLong(ServiceConfig.getProperty("watcherTimeLimit"))) {
            log.info("WATCHER DETECTED A SLOW RUNNING TEMPLATE {} in {} ms", template, time);
            ObjectMapper mapper = new ObjectMapper();
            try {
                String s = mapper.writeValueAsString(this);
                String dir = System.getProperty("ldspdi.configpath") + "watcher/";
                Helpers.createDirIfNotExists(dir);
                BufferedWriter writer = new BufferedWriter(new FileWriter(dir + template + "_" + System.currentTimeMillis() + ".json"));
                writer.write(s);
                writer.close();
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        Watcher w = new Watcher(800, "Test", "template");
        w.run();
    }

}
