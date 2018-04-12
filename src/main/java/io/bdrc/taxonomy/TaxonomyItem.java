package io.bdrc.taxonomy;

public class TaxonomyItem {
    
    String uri;
    String label;
    
    public TaxonomyItem(String uri, String label) {
        super();
        this.uri = uri;
        this.label = label;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "TaxonomyItem [uri=" + uri + ", label=" + label + "]";
    }
    
    
}
