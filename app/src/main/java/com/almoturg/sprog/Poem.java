package com.almoturg.sprog;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Paul on 2016-12-18.
 */
class Poem implements Serializable {
    public int gold;
    public int score;
    public String content;
    public double timestamp;
    public String post_title;
    public String post_author;
    public CharSequence post_content;
    public List<ParentComment> parents;
    public String link;
    public Poem main_poem;

    public Poem(int gold, int score, String content, double timestamp,
                String post_title, String post_author, CharSequence post_content,
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
