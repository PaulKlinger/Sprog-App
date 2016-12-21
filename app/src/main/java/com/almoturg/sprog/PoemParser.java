package com.almoturg.sprog;

import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Paul on 2016-12-18.
 */

public class PoemParser {
    private JsonReader reader;
    private HashMap<String, Poem> mainpoem_links;

    public PoemParser(InputStream in) throws IOException {
        this.reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        mainpoem_links = new HashMap<>();
        reader.beginArray();
    }

    public List<Poem> getPoems(int n) throws IOException {
        if (reader == null){
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
        int score = -1;
        String content = null;
        double timestamp = -1;
        String post_title = null;
        String post_author = null;
        String post_content = null;
        List<ParentComment> parents = null;
        String link = null;
        Poem mainpoem = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();

            switch (name) {
                case "gold":
                    gold = reader.nextInt();
                    break;
                case "score":
                    score = reader.nextInt();
                    break;
                case "link":
                    link = reader.nextString();
                    mainpoem = mainpoem_links.get(link);
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
                case "submission_user":
                    post_author = "/u/" + reader.nextString().replace("\\_", "_");
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
        Poem poem = new Poem(gold, score, content, timestamp,
                post_title, post_author, post_content,
                parents, link, mainpoem);
        if (mainpoem == null) {
            // add parent comments of this poem which are poems to mainpoem_links
            for (ParentComment p : poem.parents) {
                if (p.author.equalsIgnoreCase("/u/poem_for_your_sprog")) {
                    mainpoem_links.put(p.link, poem);
                }
            }
        } else {
            // if this poem is the parent of another one put it into the corresponding ParentComment
            for (ParentComment p : mainpoem.parents) {
                if (p.link.equals(poem.link)) {
                    p.is_poem = poem;
                }
            }
        }
        return poem;
    }

    private List<ParentComment> readParentCommentArray() throws IOException {
        List<ParentComment> parents = new ArrayList<ParentComment>();

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
        int gold = -1;
        int score = -1;
        double timestamp = -1;
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
                    author = "/u/" + reader.nextString().replace("\\_", "_");
                    break;
                case "orig_body":
                    content = reader.nextString().replace("^", "");
                    break;
                case "gold":
                    gold = reader.nextInt();
                    break;
                case "score":
                    score = reader.nextInt();
                    break;
                case "timestamp":
                    timestamp = reader.nextDouble();
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
        return new ParentComment(gold, score, content, author, timestamp, link);
    }
}
