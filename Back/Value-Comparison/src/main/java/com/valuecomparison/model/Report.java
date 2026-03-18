package com.valuecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reports")
public class Report {
@Id
    private String id;
    private String searchedTerm;
    private String contentMarkdown;
    private LocalDateTime creationDate;

    public Report(){}

    public Report(String searchedTerm, String contentMarkdown){
        this.searchedTerm = searchedTerm;
        this.contentMarkdown = contentMarkdown;
        this.creationDate = LocalDateTime.now();
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getSearchedTerm() {
        return searchedTerm;
    }

    public void setSearchedTerm(String searchedTerm){
        this.searchedTerm = searchedTerm;
    }

    public String getContentMarkdown(){
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public LocalDateTime getCreationDate(){
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

}
