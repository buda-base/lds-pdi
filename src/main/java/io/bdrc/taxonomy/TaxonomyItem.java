package io.bdrc.taxonomy;

import java.util.ArrayList;

import io.bdrc.ldspdi.results.Field;
import io.bdrc.ldspdi.results.LiteralStringField;

public class TaxonomyItem {
    
    public int count;
    public ArrayList<Field> prefLabel;
    
    
    public TaxonomyItem(int count, ArrayList<Field> prefLabel) {
        super();
        this.count = count;
        this.prefLabel = prefLabel;
    }
    
    public TaxonomyItem(int count, LiteralStringField lsf) {
        super();
        this.count = count;
        this.prefLabel = new ArrayList<>();
        prefLabel.add(lsf);
    }
    
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public ArrayList<Field> getData() {
        return prefLabel;
    }
    public void setData(ArrayList<Field> prefLabel) {
        this.prefLabel = prefLabel;
    }

    @Override
    public String toString() {
        return "TaxNode [count=" + count + ", prefLabel=" + prefLabel + "]";
    }

}
