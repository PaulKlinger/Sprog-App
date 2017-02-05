package com.almoturg.sprog.model;

import com.almoturg.sprog.util.SprogDbHelper;

import java.util.List;


public class Poem {
    public int gold;
    public int score;
    public String content;
    public CharSequence first_line;
    public double timestamp;
    public String post_title;
    public String post_author;
    public String post_content;
    public List<ParentComment> parents;
    public String link;
    public Poem main_poem;
    public boolean read;
    public boolean favorite;

    public Poem(int gold, int score, String content, CharSequence first_line, double timestamp,
                String post_title, String post_author, String post_content,
                List<ParentComment> parents, String link, Poem main_poem,
                boolean is_read, boolean is_favorite) {
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

        this.first_line = first_line;
        this.read = is_read;
        this.favorite = is_favorite;
    }

    public void toggleFavorite(SprogDbHelper db){
        this.favorite = !this.favorite;
        if (this.favorite) {
            db.addFavoritePoem(this.link);
        } else {
            db.removeFavoritePoem(this.link);
        }

    }
}
