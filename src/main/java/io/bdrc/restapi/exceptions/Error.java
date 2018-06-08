package io.bdrc.restapi.exceptions;

public class Error {
    
    public int code;
    public String msg;
    
    public Error() {
        super();
        this.code = -1;
        this.msg = "";
    }
    
    public Error(int code) {
        super();
        this.code = code;
        switch (code) {
            case 5001:
                msg="${ctx}";
            case 5002:
                msg="URI Parsing error ${ctx}";
            case 5003:
                msg="Missing Parameter ${ctx}";
            case 5004:
                msg="Missing Resource ${ctx}";
            case 5005:
                msg="Unknown ${ctx}";
            case 5010:
                msg="No graph was found for ${ctx}";
            case 5011:
                msg="Parsing error ${ctx}";
            case 5012:
                msg="Sparql processing error ${ctx}";
            case 5020:
                msg="No resource found for ${ctx}";
            case 5030:
                msg="Json processing error ${ctx}";
            default:
                msg="${ctx}"; 
        }
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
    
    public  static int GENERIC_ERR=5001;
    public  static int URI_SYNTAX_ERR=5002;
    public  static int MISSING_PARAM_ERR=5003;
    public  static int MISSING_RES_ERR=5004;
    public  static int UNKNOWN_ERR=5005;
    
    public  static int NO_GRAPH_ERR=5010;
    public  static int PARSE_ERR=5011;
    public  static int SPARQL_ERR=5012;
    
    public  static int ONT_URI_ERR=5020;
    
    public  static int JSON_ERR=5030;
}
