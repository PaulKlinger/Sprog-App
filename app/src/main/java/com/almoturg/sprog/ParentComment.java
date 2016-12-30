package com.almoturg.sprog;

class ParentComment {
    String content;
    String author;
    String link;
    Poem is_poem;

    ParentComment(String content, String author, String link) {
        this.content = content;
        this.author = author;
        this.link = link;
    }
}
