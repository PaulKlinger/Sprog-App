package com.almoturg.sprog.model;

import com.annimon.stream.Stream;

import java.util.Arrays;
import java.util.List;

public class PoemStatistics {

    public long num;
    public long total_words;
    public long total_score;
    public double avg_score;
    public double med_score;
    public double total_gold;

    public long total_timmy;
    public long total_timmy_fucking_died;

    public PoemStatistics(List<Poem> poems){
        num = poems.size();

        total_words = Stream.of(poems).map(p -> p.content.split("\\s+"))
                .flatMap(s -> Stream.of(s)).count();
        total_score = Stream.of(poems).mapToLong(p -> p.score).sum();
        avg_score = ((double) total_score) / num;
        med_score = median(Stream.of(poems).mapToDouble(p -> p.score).toArray());
        total_gold = Stream.of(poems).mapToLong(p -> p.gold).sum();
        total_timmy = Stream.of(poems).filter(p -> p.content.toLowerCase()
                .contains("timmy")).count();
        total_timmy_fucking_died = Stream.of(poems).filter(p -> p.content.toLowerCase()
                .contains("timmy fucking died")).count();
    }

    private static double median(double[] m) {
        if (m.length == 0) {
            return 0;
        }
        Arrays.sort(m);
        int middle = m.length/2;
        if (m.length%2 == 1) {
            return m[middle];
        } else {
            return (m[middle-1] + m[middle]) / 2.0;
        }
    }
}
