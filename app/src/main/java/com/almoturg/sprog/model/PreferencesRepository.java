package com.almoturg.sprog.model;

public interface PreferencesRepository {
    boolean getNotifyNew();
    void setNotifyNew(boolean newValue);

    boolean getLongPressLink();
    void setLongPressLink(boolean newValue);

    int getDisplayedNotificationDialog();
    void setDisplayedNotificationDialog(int newValue);

    boolean getMarkRead();
    void setMarkRead(boolean newValue);

    long getLastUpdateTime();
    void setLastUpdateTime(long newValue);

    long getLastFullUpdateTime();
    void setLastFullUpdateTime(long newValue);

    long getLastPoemTime();
    void setLastPoemTime(long newValue);

    boolean getUpdateNext();
    void setUpdateNext(boolean newValue);

    long getLastFCMTimestamp();
    void setLastFCMTimestamp(long newValue);
}
