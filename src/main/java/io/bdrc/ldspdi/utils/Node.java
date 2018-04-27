package io.bdrc.ldspdi.utils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("hiding")
public class Node<String> {
    
    private String data = null;
 
    private List<Node<String>> children = new ArrayList<>();    
 
    private Node<String> parent = null;
 
    public Node(String data) {
        this.data = data;
    }
 
    public Node<String> addChild(Node<String> child) {
        child.setParent(this);
        this.children.add(child);        
        return child;
    }
 
    public void addChildren(List<Node<String>> children) {
        children.forEach(each -> each.setParent(this));
        this.children.addAll(children);
    }
 
    public List<Node<String>> getChildren() {
        return children;
    }
 
    public String getData() {
        return data;
    }
 
    public void setData(String data) {
        this.data = data;
    }
 
    private void setParent(Node<String> parent) {
        this.parent = parent;
    }
 
    public Node<String> getParent() {
        return parent;
    }
    
    public boolean exist(Node<String> node) {
        return children.contains(node);
    }
 
}
