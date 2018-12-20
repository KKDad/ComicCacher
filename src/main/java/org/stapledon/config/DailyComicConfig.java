package org.stapledon.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class DailyComicConfig
{
    public String name;
    public Date startDate;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}
