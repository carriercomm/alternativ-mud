/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.alternativmud.logic.time;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author teofil
 */
public class TimeConfig {
    private String [] ntpServers = new String [] {"tempus1.gum.gov.pl", "tempus2.gum.gov.pl"};
    private int timeSynchronizationFrequencyMs = 60*60*1000; // 1 hours
    private int yearCorrection = 75;

    public String[] getNtpServers() {
        return ntpServers;
    }

    public void setNtpServers(String[] ntpServers) {
        this.ntpServers = ntpServers;
    }

    public int getTimeSynchronizationFrequencyMs() {
        return timeSynchronizationFrequencyMs;
    }

    public void setTimeSynchronizationFrequencyMs(int timeSynchronizationFrequencyMs) {
        this.timeSynchronizationFrequencyMs = timeSynchronizationFrequencyMs;
    }

    public int getYearCorrection() {
        return yearCorrection;
    }

    public void setYearCorrection(int yearCorrection) {
        this.yearCorrection = yearCorrection;
    }
}
