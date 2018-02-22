package io.bdrc.ldspdi.objects.json;

public abstract class Param {
    
    String type;
    String name;
    
    public Param(String type, String name) {
        super();
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }    
}
