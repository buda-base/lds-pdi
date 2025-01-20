package io.bdrc.ldspdi.exceptions;

public class LdsError {

	public int code;
	public String msg;

	public LdsError() {
		super();
		this.code = -1;
		this.msg = "";
	}

	public LdsError(int code) {
		super();
		this.code = code;
		switch (code) {
		case 5001:
			msg = "${ctx}";
		case 5002:
			msg = "URI Parsing error ${ctx}";
		case 5003:
			msg = "Missing Parameter ${ctx}";
		case 5004:
			msg = "Missing Resource ${ctx}";
		case 5005:
			msg = "Unknown ${ctx}";
		case 5006:
			msg = "Asynchrounous query issue : ${ctx}";
		case 5010:
			msg = "No graph was found for ${ctx}";
		case 5011:
			msg = "Parsing error ${ctx}";
		case 5012:
			msg = "Sparql processing error ${ctx}";
		case 5020:
			msg = "No resource found for ${ctx}";
		case 5030:
			msg = "Json processing error ${ctx}";
		default:
			msg = "${ctx}";
		}
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public LdsError setMsg(final String msgArg) {
		msg = msgArg;
		return this;
	}

	public LdsError setContext(final String ctx) {
		msg = msg.replace("${ctx}", ctx);
		return this;
	}

	public LdsError setContext(final String ctx, final Exception ex) {
		msg = msg.replace("${ctx}", ctx) + System.lineSeparator() + "Exception : " + ex.getMessage();
		return this;
	}

	public final static int GENERIC_ERR = 5001;
	public final static int URI_SYNTAX_ERR = 5002;
	public final static int MISSING_PARAM_ERR = 5003;
	public final static int MISSING_RES_ERR = 5004;
	public final static int UNKNOWN_ERR = 5005;
	public final static int ASYNC_ERR = 5006;
	public final static int NO_ACCEPT_ERR = 5007;

	public final static int NO_GRAPH_ERR = 5010;
	public final static int PARSE_ERR = 5011;
	public final static int SPARQL_ERR = 5012;
	public final static int WRONG_MT_ERR = 5013;
	
	public final static int ONT_URI_ERR = 5020;

	public final static int JSON_ERR = 5030;
}
