package io.bdrc.ldspdi.ontology.service.core;

public class OntPolicy {

    public String baseUri;
    public String graph;
    public String fileUri;
    public boolean visible;

    public OntPolicy() {
        super();
        this.baseUri = "";
        this.graph = "";
        this.fileUri = "";
        this.visible = false;
    }

    public OntPolicy(String baseUri, String graph, String fileuri, boolean visible) {
        super();
        this.baseUri = baseUri;
        this.graph = graph;
        this.fileUri = fileuri;
        this.visible = visible;
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

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    @Override
    public String toString() {
        return "OntPolicy [baseUri=" + baseUri + ", graph=" + graph + ", visible=" + visible + "]";
    }

}
