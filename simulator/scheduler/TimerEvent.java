/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package simulator.scheduler;

import java.util.PriorityQueue;
import simulator.Event;
import simulator.Job;
import simulator.Machine;
import simulator.Statistics;
import simulator.allocator.AllocInfo;
import simulator.allocator.Allocator;

public class TimerEvent extends Event {
    
    private Job job;		
    private AllocInfo allocInfo;
    
    public TimerEvent(long time, Job job, AllocInfo allocInfo) {
	super(time);
	this.job = job;
        this.allocInfo = allocInfo;
    } 
    
    public AllocInfo getAllocInfo() {
        return this.allocInfo;
    }
    
    public long getJobNum() {
        return this.job.getJobNum();
    }
  
    public int type() {
	return Event.TIMER;
    }
    
    public void happen(Machine mach, Allocator alloc, Scheduler sched, 
            PriorityQueue<Event> events, Statistics stats) {
        
        ((TimingScheduler)sched).timerExpires(mach, alloc, sched, events, stats, job, time);
        
	happenHelper(mach, alloc, sched, events, stats);	
    }
}
