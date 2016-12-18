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
    public String post_content;
    public List<ParentComment> parents;

    public Poem(int gold, int score, String content, double timestamp,
                String post_title, String post_author, String post_content,
                List<ParentComment> parents) {
        this.content = content;
        this.gold = gold;
        this.score = score;
        this.timestamp = timestamp;
        this.post_title = post_title;
        this.post_author = post_author;
        this.post_content = post_content;
        this.parents = parents;
    }
}
