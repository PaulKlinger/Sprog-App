package com.almoturg.sprog;

import java.io.Serializable;

/**
 * Created by Paul on 2016-12-18.
 */
class ParentComment implements Serializable {
    public String content;
    public String author;

    public ParentComment(String content, String author) {
        this.content = content;
        this.author = author;
    }
}
