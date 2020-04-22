package io.bdrc.ldspdi.ontology.service.core;

public class OntPolicy {

    public String baseUri;
    public String graph;
    public String fileUri;
    public String file;
    public boolean visible;

    public OntPolicy() {
        super();
        this.baseUri = "";
        this.graph = "";
        this.fileUri = "";
        this.visible = false;
    }

    public OntPolicy(String baseUri, String graph, String fileuri, String file, boolean visible) {
        super();
        this.baseUri = parseBaseUri(baseUri);
        this.graph = graph;
        this.fileUri = fileuri;
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

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "OntPolicy [baseUri=" + baseUri + ", graph=" + graph + ", fileUri=" + fileUri + ", file=" + file + ", visible=" + visible + "]";
    }

}
