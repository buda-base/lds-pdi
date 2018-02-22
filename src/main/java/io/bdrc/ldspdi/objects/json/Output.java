package io.bdrc.ldspdi.objects.json;

public class Output {
    
    public String name;
    public String type;
    public String description;
    
    public Output(String name,String type, String description) {
        super();
        this.name = name;
        this.type = type;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    

}
