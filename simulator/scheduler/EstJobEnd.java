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

import simulator.Job;

public class EstJobEnd extends SchedChange { //change where job ends
	
	public EstJobEnd(long time, Job job) {
		super(time, job);
	}

	public int freeProcChange() {
		return job.getProcsNeeded();
	}

	public String toString() {
		return time + ": end " + job;
	}

	public boolean equals(Object other){
		return ((other instanceof EstJobEnd)
				&& super.equals(other));
	}

	public boolean equals(SchedChange other) {
		return ((other instanceof EstJobEnd) &&
				super.equals(other));
	}

	public int hashCode() {
		return (int)time + 17*job.hashCode() + 1023;
	}
}


