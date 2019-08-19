package edu.asu.diging.citesphere.web.forms;

import javax.validation.constraints.NotEmpty;

public class CitationConceptForm {
    
    @NotEmpty(message="Name cannot be empty.")
    private String name;
    private String description;
    private String uri;
    private String type;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    
    
}