package com.almoturg.sprog.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Poems {
    public static List<Poem> poems = new ArrayList<>();
    public static List<Poem> filtered_poems = new ArrayList<>();

    public static void sort(String sort_order) {
        if (sort_order.equals("Date")) {
            Collections.sort(poems, (p1, p2) -> (int) (p2.timestamp - p1.timestamp));
        } else if (sort_order.equals("Score")) {
            Collections.sort(poems, (p1, p2) -> (p2.score - p1.score));
        } else if (sort_order.equals("Gold")) {
            Collections.sort(poems, (p1, p2) -> (p2.gold - p1.gold));
        }
    }

    public static void filter(String search_string, boolean only_favorites) {
        filtered_poems = new ArrayList<>();
        for (Poem p : poems) {
            String content = p.content.toLowerCase();
            if (content.contains(search_string) &&
                    (!only_favorites || p.favorite)) {
                filtered_poems.add(p);
            }
        }
    }

    public static void add(List<Poem> new_poems) {
        poems.addAll(new_poems);
        filtered_poems.addAll(new_poems);
    }

}
