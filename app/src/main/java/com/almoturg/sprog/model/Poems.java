package com.almoturg.sprog.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Poems {
    public static List<Poem> poems = new ArrayList<>();
    public static List<Poem> filtered_poems = new ArrayList<>();
    private static HashSet<String> poem_links = new HashSet<>();

    public static void sort(String sort_order) {
        if (sort_order.equals("Date")) {
            Collections.sort(poems, (p1, p2) -> (int) (p2.timestamp - p1.timestamp));
        } else if (sort_order.equals("Score")) {
            Collections.sort(poems, (p1, p2) -> (p2.score - p1.score));
        } else if (sort_order.equals("Awards")) {
            Collections.sort(poems, (p1, p2) -> (p2.totalAwards() - p1.totalAwards()));
        } else if (sort_order.equals("Random")) {
            Collections.shuffle(poems);
        }
    }

    private static boolean filter_matches(Poem p, String search_string, boolean only_favorites, boolean only_unread,
                                          boolean only_long, boolean only_short) {
        return (search_string.isEmpty() || p.content.toLowerCase().contains(search_string)) &&
                (!only_favorites || p.favorite) &&
                (!only_unread || !p.read) &&
                (!only_long || p.content.length() >= 550) &&
                (!only_short || p.content.length() <= 200);
    }

    public static void filter(String search_string, boolean only_favorites, boolean only_unread,
                              boolean only_long, boolean only_short) {
        filtered_poems = new ArrayList<>();
        for (Poem p : poems) {
            if (filter_matches(p, search_string, only_favorites,
                    only_unread, only_long, only_short)) {
                filtered_poems.add(p);
            }
        }
    }

    public static void add(List<Poem> new_poems,
                           String search_string, boolean only_favorites, boolean only_unread,
                           boolean only_long, boolean only_short) {
        for (Poem p : new_poems) {
            // Might not be necessary with the check for minimum timestamp
            // in PoemsFileParser. But probably still a good idea?
            if (!poem_links.contains(p.link)) {
                poem_links.add(p.link);
                poems.add(p);
                if (filter_matches(p, search_string, only_favorites,
                        only_unread, only_long, only_short)) {
                    filtered_poems.add(p);
                }
            }
        }
    }

    public static void clear() {
        poems.clear();
        filtered_poems.clear();
        poem_links.clear();
    }

}
