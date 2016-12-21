package com.almoturg.sprog;

/**
 * Created by Paul on 2016-12-18.
 */
class ParentComment {
    public String content;
    public String author;
    public int gold;
    public int score;
    public double timestamp;
    public String link;
    public Poem is_poem;

    public ParentComment(int gold, int score, String content, String author, double timestamp,
                         String link) {
        this.content = content;
        this.author = author;
        this.gold = gold;
        this.score = score;
        this.timestamp = timestamp;
        this.link = link;
    }
}
