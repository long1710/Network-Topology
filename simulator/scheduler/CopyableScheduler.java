/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Subtype of Scheduler with method to copy scheduler's internal state.
 * Used when computing fair start times.
 */

package simulator.scheduler;

public interface  CopyableScheduler {
	public Scheduler copy(); 
}
