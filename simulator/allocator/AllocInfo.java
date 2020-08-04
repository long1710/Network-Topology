/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/** Representation of an allocation.
 * Created by Allocator and returned to Scheduler, which uses it to update Machine.
 * Subclasses used for different types of machines.
 */

package simulator.allocator;

import simulator.Job;
import simulator.Machine;

public class AllocInfo {
	public Job job;

	public AllocInfo(Job job) {
		this.job = job;
	}

	public String getProcList(Machine m){
		return "";
	}
}
