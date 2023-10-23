package io.bdrc.ldspdi.ontology.service.core;

public class OntPolicy {

    public String baseUri;
    public String graph;
    public String file;
    public boolean visible;

    public OntPolicy() {
        super();
        this.baseUri = "";
        this.graph = "";
        this.visible = false;
    }

    public OntPolicy(String baseUri, String graph, String fileuri, String file, boolean visible) {
        super();
        this.baseUri = parseBaseUri(baseUri);
        this.graph = graph;
        this.file = file;
        this.visible = visible;
    }

    private String parseBaseUri(String s) {
        if (s.endsWith("/") || s.endsWith("#")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "OntPolicy [baseUri=" + baseUri + ", graph=" + graph + ", file=" + file + ", visible=" + visible + "]";
    }

}
