package com.crusader.tinkoffnews.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by crusader on 1/1/17.
 * Class Article represents any article getted by Tinkoff API
 */

public class Article implements Serializable {

    private Integer id;
    private String text;
    private Date publicationDate;
    private String fullText;

    //Don't need those:
    //private String name;
    //private Integer bankInfoTypeId;

    public Article(Integer id,  String text, long publicationDate){
        this.id = id;
        this.text = text;
        this.publicationDate = new Date(publicationDate);
    }

    public boolean isDetailed(){
        if(fullText == null){
            return false;
        } else {
            return true;
        }
    }
    public Integer getId() {
        return this.id;
    }

    public String getText(){
        return this.text;
    }

    public Date getPublicationDate(){
        return this.publicationDate;
    }

    public void setFullText(String fullText){
        this.fullText = fullText;
    }

    public String getFullText(){
        return this.fullText;
    }

    public int compareTo(Article article){
        return article.getPublicationDate().compareTo(this.getPublicationDate());
    };

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Article))
            return false;
        //Check news only by ID, cos it's unique
        if(((Article) obj).getId().equals(this.getId()))
            return true;
        return false;
    }
}
