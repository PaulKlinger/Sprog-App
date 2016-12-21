package com.almoturg.sprog;

import java.util.List;

/**
 * Created by Paul on 2016-12-18.
 */
class Poem {
    public int gold;
    public int score;
    public CharSequence content;
    public CharSequence first_line;
    public double timestamp;
    public String post_title;
    public String post_author;
    public String post_content;
    public List<ParentComment> parents;
    public String link;
    public Poem main_poem;

    public Poem(int gold, int score, CharSequence content, double timestamp,
                String post_title, String post_author, String post_content,
                List<ParentComment> parents, String link, Poem main_poem) {
        this.content = content;
        this.gold = gold;
        this.score = score;
        this.timestamp = timestamp;
        this.post_title = post_title;
        this.post_author = post_author;
        this.post_content = post_content;
        this.parents = parents;
        this.link = link;
        this.main_poem = main_poem;
    }
}
