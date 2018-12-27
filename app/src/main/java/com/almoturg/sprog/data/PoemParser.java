package com.almoturg.sprog.data;

import android.util.JsonReader;
import android.util.JsonToken;

import com.almoturg.sprog.model.ParentComment;
import com.almoturg.sprog.model.Poem;
import com.almoturg.sprog.model.SprogDbHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


class PoemParser {
    private JsonReader reader;
    private HashMap<String, Poem> mainpoem_links;
    private HashSet<String> read_poems;
    private HashSet<String> favorite_poems;
    private MarkdownConverter markdownConverter;

    PoemParser(InputStream in, SprogDbHelper dbHelper, MarkdownConverter markdownConverter)
            throws IOException {
        this.markdownConverter = markdownConverter;
        read_poems = dbHelper.getReadPoems();
        favorite_poems = dbHelper.getFavoritePoems();
        reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        mainpoem_links = new HashMap<>();
        reader.beginArray();
    }

    List<Poem> getPoems(int n) throws IOException {
        if (reader == null) {
            return null;
        }
        List<Poem> poems = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!reader.hasNext()) {
                reader.close();
                reader = null;
                break;
            }
            poems.add(readPoem());
        }
        return poems;
    }

    private Poem readPoem() throws IOException {
        int gold = -1;
        int silver = -1;
        int platinum = -1;
        int score = -1;
        String content = null;
        double timestamp = -1;
        String post_title = null;
        String post_author = null;
        String post_content = null;
        String post_url = null;
        List<ParentComment> parents = null;
        String link = null;
        Poem main_poem = null;
        boolean read = false;
        boolean favorite = false;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            JsonToken check = reader.peek();
            if (check == JsonToken.NULL) {
                reader.skipValue();
                continue;
            }
            switch (name) {
                case "gold":
                    gold = reader.nextInt();
                    break;
                case "silver":
                    silver = reader.nextInt();
                    break;
                case "platinum":
                    platinum = reader.nextInt();
                    break;
                case "score":
                    score = reader.nextInt();
                    break;
                case "link":
                    link = reader.nextString();
                    main_poem = mainpoem_links.get(link);
                    break;
                case "orig_content":
                    content = reader.nextString().replace("^", "");
                    break;
                case "timestamp":
                    timestamp = reader.nextDouble();
                    break;
                case "submission_title":
                    post_title = reader.nextString();
                    break;
                case "submission_url":
                    post_url = reader.nextString();
                    break;
                case "submission_user":
                    post_author = formatUsername(reader.nextString());
                    break;
                case "orig_submission_content":
                    post_content = reader.nextString().replace("^", "");
                    break;
                case "parents":
                    parents = readParentCommentArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        CharSequence first_line;

        first_line = markdownConverter.convertMarkdown(
                content.trim().split("\n", 2)[0].trim() + "...");


        if (read_poems.contains(link)) {
            read = true;
        }
        if (favorite_poems.contains(link)) {
            favorite = true;
        }

        Poem poem = new Poem(silver, gold, platinum, score, content, first_line, timestamp,
                post_title, post_author, post_content, post_url,
                parents, link, main_poem, read, favorite);

        if (main_poem == null) {
            // add parent comments of this poem which are poems to mainpoem_links
            for (ParentComment p : poem.parents) {
                if (p.author.equalsIgnoreCase("/u/poem_for_your_sprog")) {
                    mainpoem_links.put(p.link, poem);
                }
            }
        } else {
            // if this poem is the parent of another one put it into the corresponding ParentComment
            for (ParentComment p : main_poem.parents) {
                if (p.link.equals(poem.link)) {
                    p.is_poem = poem;
                }
            }
        }
        return poem;
    }

    private List<ParentComment> readParentCommentArray() throws IOException {
        List<ParentComment> parents = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            parents.add(readParentComment());
        }
        reader.endArray();
        return parents;
    }

    private ParentComment readParentComment() throws IOException {
        String content = null;
        String author = null;
        String link = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            JsonToken check = reader.peek();
            if (check == JsonToken.NULL) {
                reader.skipValue();
                continue;
            }
            switch (name) {
                case "author":
                    author = formatUsername(reader.nextString());
                    break;
                case "orig_body":
                    content = reader.nextString().replace("^", "");
                    break;
                case "link":
                    link = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new ParentComment(content, author, link);
    }

    private static String formatUsername(String username) {
        username = username.replace("\\_", "_");
        if (username.equals("(deleted user)")) {
            return username;
        } else {
            return "/u/" + username;
        }
    }
}
