package com.almoturg.sprog;

import java.util.List;


class Poem {
    int gold;
    int score;
    CharSequence content;
    CharSequence first_line;
    double timestamp;
    String post_title;
    String post_author;
    String post_content;
    List<ParentComment> parents;
    String link;
    Poem main_poem;

    Poem(int gold, int score, CharSequence content, CharSequence first_line, double timestamp,
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

        this.first_line = first_line;
    }
}
