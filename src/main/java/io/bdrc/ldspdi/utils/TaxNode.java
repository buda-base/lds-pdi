package io.bdrc.ldspdi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.SKOS;

public class TaxNode implements Comparable<TaxNode> {

    private String uri = null;
    private List<TaxNode> children = new ArrayList<>();
    private TaxNode parent = null;
    private LinkedList<TaxNode> pathFromRoot = null;
    private List<Map<String,String>> labels = null;
    
    public LinkedList<TaxNode> getPathFromRoot() {
        if (this.pathFromRoot != null)
            return this.pathFromRoot;
        this.pathFromRoot = new LinkedList<>();
        TaxNode node = this;
        this.pathFromRoot.addFirst(node);
        while (node != null) {
            node = node.getParent();
            if (node != null)
                this.pathFromRoot.addFirst(node);
        }
        return this.pathFromRoot;
    }
    
    public List<Map<String,String>> getLabels() {
        return this.labels;
    }
    
    private static final List<Map<String,String>> initLabels(final Model m, final String uri) {
        final List<Map<String,String>> labels = new ArrayList<>();
        final ExtendedIterator<Triple> labelIt = m.getGraph().find(NodeFactory.createURI(uri), SKOS.prefLabel.asNode(), Node.ANY);
        while (labelIt.hasNext()) {
            LiteralLabel l = labelIt.next().getObject().getLiteral();
            final Map<String,String> label = new HashMap<>();
            labels.add(label);
            label.put("@language", l.language());
            label.put("@value", (String) l.getValue());
        }
        return labels;
    }

    public TaxNode(final String uri, Model m) {
        this.uri = uri;
        this.labels = initLabels(m, uri);
    }

    public TaxNode addChild(final TaxNode child) {
        child.setParent(this);
        this.children.add(child);
        return child;
    }

    public void addChildren(final List<TaxNode> children) {
        children.forEach(each -> each.setParent(this));
        this.children.addAll(children);
    }

    public List<TaxNode> getChildren() {
        return children;
    }

    public String getUri() {
        return uri;
    }

    public void setData(final String data) {
        this.uri = data;
    }

    private void setParent(final TaxNode parent) {
        this.parent = parent;
    }

    public TaxNode getParent() {
        return parent;
    }

    public boolean hasChild(final TaxNode node) {
        return children.contains(node);
    }

    @Override
    public int compareTo(TaxNode arg0) {
        if (uri == null)
            return -1;
        return uri.compareTo(arg0.getUri());
    }

}
