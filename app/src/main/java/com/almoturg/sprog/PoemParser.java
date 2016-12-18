package com.almoturg.sprog;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul on 2016-12-18.
 */

public class PoemParser {
    static public List<Poem> readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readPoemsArray(reader);
        } finally {
            reader.close();
        }
    }

    static private List<Poem> readPoemsArray(JsonReader reader) throws IOException {
        List<Poem> Poems = new ArrayList<Poem>();

        reader.beginArray();
        while (reader.hasNext()) {
            Poems.add(readPoem(reader));
        }
        reader.endArray();
        return Poems;
    }

    static private Poem readPoem(JsonReader reader) throws IOException {
        int gold = -1;
        int score = -1;
        String content = null;
        double timestamp = -1;
        String post_title = null;
        String post_author = null;
        String post_content = null;
        List<ParentComment> parents = null;
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
                    post_author = reader.nextString().replace("\\_", "_");
                    break;
                case "orig_submission_content":
                    post_content = reader.nextString().replace("^", "");
                    break;
                case "parents":
                    parents = readParentCommentArray(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Poem(gold, score, content, timestamp,
                post_title, post_author, post_content,
                parents);
    }

    static private List<ParentComment> readParentCommentArray(JsonReader reader) throws IOException {
        List<ParentComment> parents = new ArrayList<ParentComment>();

        reader.beginArray();
        while (reader.hasNext()) {
            parents.add(readParentComment(reader));
        }
        reader.endArray();
        return parents;
    }

    static private ParentComment readParentComment(JsonReader reader) throws IOException {
        String content = null;
        String author = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "author":
                    author = reader.nextString().replace("\\_", "_");;
                    break;
                case "orig_body":
                    content = reader.nextString().replace("^", "");
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new ParentComment(content, author);
    }
}
