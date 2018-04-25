package io.bdrc.taxonomy;

public class TaxNode {
    
    public int level;
    public String data;
    
    
    public TaxNode(int level, String data) {
        super();
        this.level = level;
        this.data = data;
    }
    
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "TaxNode [level=" + level + ", data=" + data + "]";
    }
    
    
    

}
