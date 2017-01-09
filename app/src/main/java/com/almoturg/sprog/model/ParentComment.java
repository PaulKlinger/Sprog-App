package com.almoturg.sprog.model;

public class ParentComment {
    public String content;
    public String author;
    public String link;
    public Poem is_poem;

    public ParentComment(String content, String author, String link) {
        this.content = content;
        this.author = author;
        this.link = link;
    }
}
