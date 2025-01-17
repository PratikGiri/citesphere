package edu.asu.diging.citesphere.core.authority.impl;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the data for VIAF authority record
 * @author Maulik Limbadiya
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ViafRecordData {

    private String nameType;
    private MainHeadings mainHeadings;
    @JsonProperty("Document")
    private Map<String, Object> document;
    
    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }
    
    public MainHeadings getMainHeadings() {
        return mainHeadings;
    }

    public void setMainHeadings(MainHeadings mainHeadings) {
        this.mainHeadings = mainHeadings;
    }
    
    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> document) {
        this.document = document;
    }

    static public class MainHeadings {
        
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<Data> data;

        public List<Data> getData() {
            return data;
        }

        public void setData(List<Data> data) {
            this.data = data;
        }
    }
    
    static public class Data {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }       
    }
}
