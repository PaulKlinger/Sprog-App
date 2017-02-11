package com.almoturg.sprog.model;

import android.util.Log;

import com.annimon.stream.Stream;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

public class PoemStatistics {
    private List<Poem> poems;

    public long num;
    public long total_words;
    public double avg_words;

    public long total_score;
    public double avg_score;
    public double med_score;

    public long total_gold;

    public long total_timmy;
    public long total_timmy_fucking_died;

    public PoemStatistics(List<Poem> poems){
        this.poems = poems;

        num = poems.size();

        total_words = Stream.of(poems).map(p -> p.content.split("\\s+"))
                .flatMap(s -> Stream.of(s)).count();
        avg_words = ((double) total_words) / num;

        total_score = Stream.of(poems).mapToLong(p -> p.score).sum();
        avg_score = ((double) total_score) / num;
        med_score = median(Stream.of(poems).mapToDouble(p -> p.score).toArray());

        total_gold = Stream.of(poems).mapToLong(p -> p.gold).sum();
        total_timmy = Stream.of(poems).filter(p -> p.content.toLowerCase()
                .contains("timmy")).count();
        total_timmy_fucking_died = Stream.of(poems).filter(p -> p.content.toLowerCase()
                .contains("timmy fucking died")).count();

    }

    public LinkedHashMap<Integer, Integer> getMonthNPoems() {
        Calendar date = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.setTimeInMillis(getFirstTimestamp());
        Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        LinkedHashMap<Integer, Integer> monthNPoems = new LinkedHashMap<>();

        do {
            monthNPoems.put(totalMonths(date), 0);
            date.add(Calendar.MONTH, 1);
        } while (date.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                date.get(Calendar.MONTH) <= now.get(Calendar.MONTH));

        int pkey;
        for (Poem p : poems) {
            date.setTimeInMillis(p.timestamp_long);
            pkey = totalMonths(date);
            monthNPoems.put(pkey, monthNPoems.get(pkey) + 1);
        }
        return monthNPoems;
    }

    private static int totalMonths(Calendar cal) {
        return cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH);
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

    private long getFirstTimestamp(){
        return (long) (Stream.of(poems).mapToDouble(p -> p.timestamp).min().orElse(-1) * 1000);
    }
}
