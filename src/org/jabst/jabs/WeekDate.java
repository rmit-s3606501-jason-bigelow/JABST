package org.jabst.jabs;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/** WeekDate represents a time and a day of week together.
 *  WeekDate is used to represent employee availability. It stores the time
 *  of day as 
 */
public class WeekDate implements Comparable<WeekDate> {
    /** Monday, Tuesday, ... @see java.time.DayOfWeek */
    DayOfWeek dayOfWeek;
    /** Range: 0 <= timeOfDay <= 86399 */
    private int timeOfDay;
    
    public WeekDate(DayOfWeek dayOfWeek, int timeOfDay) {
        this.dayOfWeek = dayOfWeek;
        this.timeOfDay = timeOfDay;
    }
    
    /** Gets the hour (0-23) that the time of day starts at */
    public int getStartingHour() {
        return timeOfDay / 3600;
    }
    
    /** Sets the time of day to the given value in seconds.
      * Note: you may want to construct a new WeekDate instead
      */
    public boolean setTimeOfDay(int timeOfDay) {
        if (0 <= timeOfDay && timeOfDay <= 86399) {
            this.timeOfDay = timeOfDay;
            return true;
        } else return false;
    }
    
    /** Sets the time of day to the given hour (0-23)
      * Note: you may want to construct a new WeekDate instead
      */
    public boolean setTimeOfDayHour(int hour) {
        if (0 <= hour && hour <= 23) {
            timeOfDay = hour*3600;
            return true;
        } else return false;
    }

    /** Sets the time of day to the given hour (0-23), minute (0-59), second (0-59)
      * Note: you may want to construct a new WeekDate instead
      */
    public boolean setTimeOfDayHMS(int hour, int minute, int second) {
        if (0 <= hour && hour <= 23) {
        	if(0 <= minute && minute < 60) {
        		if(0 <= second && second < 60) {
		            timeOfDay = hour*3600 + minute*60 + second;
		            return true;
        		}
        	}
        }
        return false;
    }
    
    public int getTime() { return timeOfDay; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }

    public int compareTo(WeekDate other) {
    	int day = this.dayOfWeek.compareTo(other.getDayOfWeek());
    	if(day == 0) {
    		return this.getTime() - other.getTime();
    	} else {
    		return day;
    	}
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK));
        sb.append(" ");
        sb.append(String.format("%02d", getStartingHour()));
        sb.append(":");
        sb.append(String.format("%02d", (timeOfDay/60)%60));
        return sb.toString();
    }
}
