package io.bdrc.ldspdi.Utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.objects.json.QueryTemplate;
import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.ldspdi.sparql.QueryConstants;
import io.bdrc.ldspdi.sparql.QueryFileParser;

public class DocFileModel {    
    
    ArrayList<String> files;
    public static Logger log=LoggerFactory.getLogger(DocFileModel.class.getName());
    public Set<String> keys;
    HashMap<String,ArrayList<QueryTemplate>> templ;
    
    public DocFileModel() {
        this.files=getQueryTemplates();
        setContentModel();        
    }
    
    public void setContentModel(){
                
        templ=new HashMap<>();        
        
            for(String file:files) {                
                QueryFileParser qfp=new QueryFileParser(file);              
                QueryTemplate qt=qfp.getTemplate();
                String queryScope=qt.getQueryScope();
                   
                if(templ.containsKey(queryScope)) {
                    templ.get(queryScope).add(qt);                    
                }else {
                    ArrayList<QueryTemplate> qtlist=new ArrayList<>();
                    qtlist.add(qt);
                    templ.put(queryScope,qtlist);
                }
            }
            this.keys=templ.keySet();
    }
    
    public ArrayList<QueryTemplate> getTemplates(String key){
        return templ.get(key);
    }
    
    public Set<String> getKeys() {
        return keys;
    }

    public static ArrayList<String> getQueryTemplates() {
        ArrayList<String> files=new ArrayList<>();
        Path dpath = Paths.get(ServiceConfig.getProperty(QueryConstants.QUERY_PATH)+"public");      
        if (Files.isDirectory(dpath)) {        
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dpath)) {
                for (Path path : stream) {
                    String tmp=path.toString();
                    //Filtering arq files
                    if(tmp.endsWith(".arq")) {
                        files.add(tmp.substring(tmp.lastIndexOf("/")+1));
                    }
                }
            } catch (IOException e) {
                log.error("Error while getting query templates", e);
                e.printStackTrace();
            }
        }
        return files;       
    }

}
