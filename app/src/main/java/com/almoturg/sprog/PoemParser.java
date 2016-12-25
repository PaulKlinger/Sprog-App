package com.almoturg.sprog;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.uncod.android.bypass.Bypass;


class PoemParser {
    private JsonReader reader;
    private HashMap<String, Poem> mainpoem_links;
    private Bypass bypass;

    PoemParser(InputStream in, Context context) throws IOException {
        synchronized (SprogApplication.bypassLock){
            bypass = new Bypass(context, new Bypass.Options());
        }
        this.reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
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
        int score = -1;
        String content = null;
        double timestamp = -1;
        String post_title = null;
        String post_author = null;
        String post_content = null;
        List<ParentComment> parents = null;
        String link = null;
        Poem main_poem = null;

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

        CharSequence first_line;

        synchronized (SprogApplication.bypassLock){
            first_line = bypass.markdownToSpannable(content.trim().split("\n", 2)[0].trim() + "...");
        }

        Poem poem = new Poem(gold, score, content, first_line, timestamp,
                post_title, post_author, post_content,
                parents, link, main_poem);
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
                    author = "/u/" + reader.nextString().replace("\\_", "_");
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
}
