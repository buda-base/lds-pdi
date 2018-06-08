package io.bdrc.restapi.exceptions;

public class Error {
    
    public int code;
    public String msg;
    
    public Error(int code, String msg) {
        super();
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    } 
    
    public Error setContext(String ctx) {
        msg=msg.replace("${ctx}", ctx);
        return this;
    } 
    
    public static Error URISyntaxERR=new Error(5002,"${ctx} : URISyntaxException");
    public static Error MISSING_PARAM_ERR=new Error(5003,"Parameters are missing : ${ctx}");
    
    public static Error NO_GRAPH_ERR=new Error(5010,"No graph was found for : ${ctx}");
    
    public static Error ONT_URI_ERR=new Error(5020,"There is no resource matching the following URI: ${ctx}");
    
    public static Error JSON_ERR=new Error(5030,"JsonProcessingException ${ctx}");
}
