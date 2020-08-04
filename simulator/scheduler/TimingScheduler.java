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
import simulator.allocator.Allocator;

public abstract class TimingScheduler extends Scheduler {
    
    //Called when a job timer expires
    public abstract void timerExpires(Machine mach, Allocator alloc,
				      Scheduler sched,
				      PriorityQueue<Event> events,
				      Statistics stats, Job j, long time); 
}
