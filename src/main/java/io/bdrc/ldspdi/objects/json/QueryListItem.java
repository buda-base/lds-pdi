package io.bdrc.ldspdi.objects.json;

public class QueryListItem {
    
    public String name;
    public String descLink;        
       
    public QueryListItem(String name, String descLink) {
        super();
        this.name = name.substring(0,name.lastIndexOf("."));
        this.descLink = descLink;        
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescLink() {
        return descLink;
    }
    public void setDescLink(String descLink) {
        this.descLink = descLink;
    }

}
