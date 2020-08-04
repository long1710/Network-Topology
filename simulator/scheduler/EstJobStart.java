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
import simulator.Pair;

public class EstJobStart extends SchedChange { //change where job starts
	private EstJobEnd partner;  //corresponding end change

	public EstJobStart(long time, Job job, EstJobEnd partner) {
		super(time, job);
		this.partner = partner;		
	}

	public EstJobEnd getPartner() {
		return partner;
	}

	public int freeProcChange() {
		return -job.getProcsNeeded();
	}

	public Pair<EstJobStart,EstJobEnd> copy(){
		EstJobEnd ne = new EstJobEnd(partner.time,job);
		EstJobStart ns = new EstJobStart(time,job,ne);
		return new Pair<EstJobStart,EstJobEnd>(ns,ne);
	}

	public String toString() {
		return time + ": start " + job;
	}

	public boolean equals(Object other) {
		return ((other instanceof EstJobStart)
				//&& ((EstJobStart) other).job.equals(this.job)
				&& super.equals(other));
	}

	public boolean equals(SchedChange other) {
		return ((other instanceof EstJobStart) &&
				super.equals(other));
	}
	
	public int hashCode() {
		return (int)time + 17*job.hashCode() + 1231;
	}
}


