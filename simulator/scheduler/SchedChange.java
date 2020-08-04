/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Represents a schedule change in StatefulScheduler (job starting or ending)
 * Comparable based on time of change
 */

package simulator.scheduler;

import simulator.Job;
import simulator.Main;
import simulator.Utility;

public abstract class SchedChange implements Comparable<SchedChange>{

	protected long time;  //when change occurs
	public Job job;       //job either started or stopped

	public SchedChange(long time, Job job) {
		this.time = time;
		this.job = job;
	}

	public int compareTo(SchedChange other) {
		if(this.equals(other))
			return 0;

		int diff = Utility.getSign(time - other.time);
		if(diff != 0)
			return diff;

		//must be at same time

		//special case for exactly one being zero length
		boolean run1 = (this.job.getActualTime()==0);
		boolean run2 = (other.job.getActualTime()==0);
		if(run1 ^ run2){
			if((run1 && (other instanceof EstJobStart))
					||(run2 && (this instanceof EstJobEnd)))
				return -1;
			else
				return 1;
		}

		//special case for both being zero length
		if(run1 && run2){

			//if start and finish to the same job, start goes first
			if(this.job.equals(other.job)){
				if(this instanceof EstJobStart
						&& other instanceof EstJobEnd)
					return -1;
				else {
					boolean mustBe = ((other instanceof EstJobStart) &&
							(this instanceof EstJobEnd));
					if(!mustBe)  //must hold since already handled equals
						Main.error("failed assertion in SchedChange");
					return 1;
				}
			}

			//otherwise order jobs sequentially by number
			return Utility.getSign(this.job.getJobNum() -
					other.job.getJobNum());
		}

		//Neither job has zero length.
		//Ends come before starts so that processors 
		//are freed before being reallocated.
		if(this instanceof EstJobEnd
				&& other instanceof EstJobStart)
			return -1;
		else if(this instanceof EstJobStart
				&& other instanceof EstJobEnd)
			return 1;

		//must be same time and same type of change
		//tiebreak by job number
		return Utility.getSign(job.getJobNum() - other.job.getJobNum());
	}

	public boolean equals(Object other) {
		if(!(other instanceof SchedChange))
			return false;
		return this.equals((SchedChange) other);
	}

	public boolean equals(SchedChange other) {
		return ((this.time == other.time)
				&& this.job.equals(other.job));
	}

	public long getTime() {  //get when the change happens
		return time;
	}

	abstract public int freeProcChange(); 
	//returns # by which #free procs changes
}

