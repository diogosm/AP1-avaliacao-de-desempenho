/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.Iterator;
import java.util.LinkedList;

import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;


/**
 * This report counts the number of contacts each hour
 *
 * @author Frans Ekman
 */
public class Report1 extends Report implements ConnectionListener {

	private LinkedList<Integer> contactCounts;
	private int currentHourCount;
	private int currentHour;

	public Report1() {
		init();
	}

	@Override
	public void init() {
		super.init();
		contactCounts = new LinkedList<Integer>();
	}

	public void hostsConnected(DTNHost host1, DTNHost host2) {
		int time = SimClock.getIntTime() / 3600;
		while (Math.floor(time) > currentHour) {
			contactCounts.add(new Integer(currentHourCount));
			currentHourCount = 0;
			currentHour++;
		}

		currentHourCount++;
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// Do nothing
	}

	public void done() {
       
		Iterator<Integer> iterator = contactCounts.iterator();
        int hour = 0;
       
		while (iterator.hasNext()) {
            Integer count = (Integer)iterator.next();
                   
            if (hour%24 < 10) {
                write("0" + hour%24 + ":00 - " + "0" + hour%24 + ":59" + "\t" + count);    
            }else{
			    write(hour%24 + ":00 - " + hour%24 + ":59" + "\t" + count);
            }
            hour++;
		}
		super.done();
	}

}
