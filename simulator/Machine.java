/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

package simulator;

import simulator.allocator.AllocInfo;

public abstract class Machine implements HasSetupInfo {

	protected int numAvail;          //number of available processors
	protected int numProcs;


	public int numFreeProcessors() {
		return numAvail;
	}

	public int numProcs(){
		return numProcs;
	}

	abstract public void reset();

	abstract public void allocate(AllocInfo allocInfo);

	abstract public void deallocate(AllocInfo allocInfo);
}
