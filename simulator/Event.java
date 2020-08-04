/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents an event for the event queue.  Superclass for specific kinds of events.
 */

package simulator;

import java.util.PriorityQueue;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;
import simulator.scheduler.Scheduler;

public abstract class Event implements Comparable<Event> {

	protected long time;         //when the event occurs

	private int eventNum;      //the event's unique number

	private static int nextEventNum = 0;  //number to assign next Event

	public Event(long Time) {
		time = Time;
		eventNum = nextEventNum;
		nextEventNum++;
	}

	public long getTime() {
		return time;
	}

	public int compareTo(Event other) {
		if(time == other.time) {
			if(type() == other.type())
				return (eventNum - other.eventNum);
			return (type() - other.type());
		}
		return (int)(time-other.time);
	}

	abstract public int type();  //returns code indicating type of event:
	public static final int DEPARTURE = 0;
	public static final int ARRIVAL = 1;
	public static final int TIMER = 2;

	abstract public void happen(Machine mach, Allocator alloc, Scheduler sched, 
			PriorityQueue<Event> events, Statistics stats);

	protected void happenHelper(Machine mach, Allocator alloc, Scheduler sched, 
			PriorityQueue<Event> events, Statistics stats) {
		//the commmon part of different subclass's happen methods;
		//tries to start jobs if appropriate
		if(handleArrivalsSeparately || (events.size() == 0) ||
				(events.peek().getTime() != time)) {
			AllocInfo allocInfo;
			do {
				allocInfo = sched.tryToStart(alloc, time, mach, events, stats);
			} while(allocInfo != null);
		}
	}

	//whether we should try to start jobs after each event:
	//(if false, only tries once all events at same time have occurred)
	protected static final boolean handleArrivalsSeparately = false;

	public Job getJob() {
		return null;
	}
}
