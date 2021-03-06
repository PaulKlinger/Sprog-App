package com.almoturg.sprog.model;

import com.almoturg.sprog.util.Util;

import java.util.List;


public class Poem {
    public int silver;
    public int gold;
    public int platinum;
    public int score;
    public String content;
    public CharSequence first_line;
    public double timestamp;
    public long timestamp_long;
    public String post_title;
    public String post_author;
    public String post_content;
    public String post_url;
    public List<ParentComment> parents;
    public String link;
    public Poem main_poem;
    public boolean read;
    public boolean favorite;

    public Poem(int silver, int gold, int platinum, int score, String content,
                CharSequence first_line, double timestamp,
                String post_title, String post_author, String post_content, String post_url,
                List<ParentComment> parents, String link, Poem main_poem,
                boolean is_read, boolean is_favorite) {
        this.content = content;
        this.silver = silver;
        this.gold = gold;
        this.platinum = platinum;
        this.score = score;
        this.timestamp = timestamp;
        this.timestamp_long = (long) timestamp * 1000;
        this.post_title = post_title;
        this.post_author = post_author;
        this.post_content = post_content;
        this.post_url = post_url;
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

    public int totalAwards(){
        return 3 * this.platinum + 2 * this.gold + this.silver;
    }

    public String getId() {
        return Util.last(this.link.split("/"));
    }
}
