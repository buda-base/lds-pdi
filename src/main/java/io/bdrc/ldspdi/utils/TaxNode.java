package io.bdrc.ldspdi.utils;

import java.util.ArrayList;
import java.util.List;

public class TaxNode implements Comparable<TaxNode> {

    private String uri = null;
    private List<TaxNode> children = new ArrayList<>();
    private TaxNode parent = null;

    public TaxNode(final String data) {
        this.uri = data;
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
