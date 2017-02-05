package com.almoturg.sprog;

import com.almoturg.sprog.data.UpdateHelpers;

import org.junit.Test;

import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class UpdateHelpersTest {
    @Test
    public void isUpdateTimeTest(){
        GregorianCalendar now;
        long last_update;
        long last_fcm;

        now = new GregorianCalendar(2017, 1, 15, 14, 45);
        last_update = new GregorianCalendar(2017, 1, 13, 13, 25).getTimeInMillis();
        last_fcm = new GregorianCalendar(2017, 1, 14, 13, 17).getTimeInMillis();
        assertEquals(true, UpdateHelpers.isUpdateTime(now, last_update, last_fcm));

        now = new GregorianCalendar(2017, 1, 15, 14, 45);
        last_update = new GregorianCalendar(2017, 1, 15, 13, 34).getTimeInMillis();
        last_fcm = new GregorianCalendar(2017, 1, 15, 13, 17).getTimeInMillis();
        assertEquals(false, UpdateHelpers.isUpdateTime(now, last_update, last_fcm));

        now = new GregorianCalendar(2017, 1, 15, 14, 45);
        last_update = -1;
        last_fcm = -1;
        assertEquals(true, UpdateHelpers.isUpdateTime(now, last_update, last_fcm));

        now = new GregorianCalendar(2017, 1, 15, 3, 15);
        last_update = new GregorianCalendar(2017, 1, 14, 17, 25).getTimeInMillis();
        last_fcm = new GregorianCalendar(2017, 1, 14, 13, 17).getTimeInMillis();
        assertEquals(true, UpdateHelpers.isUpdateTime(now, last_update, last_fcm));
    }

}
