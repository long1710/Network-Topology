/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Version of machine that doesn't keep track of processor locations
 */

package simulator;
 
import java.util.ArrayList;

import simulator.allocator.AllocInfo;

public class SimpleMachine extends Machine {

	private static final boolean debug = false;   //whether to include debug printouts
	public int counter = 0;
	public int counter_out = 0;
	public SimpleMachine(int procs) {
		//takes number of processors
		numProcs = procs;
		reset();
	}

	//Factory creation method
	public static SimpleMachine Make(ArrayList<String> params) {
		Factory.argsAtMost(1,params);
		Factory.argsAtLeast(1,params);

		return new SimpleMachine(Integer.parseInt(params.get(1)));
	}

	public void reset() {
		numAvail = numProcs;
	}

	public static String getParamHelp() {
		return "[<num procs>]";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"SimpleMachine with "+numProcs+" processors";
	}

	public void allocate(AllocInfo allocInfo) {
		//allocate processors
		int num = allocInfo.job.getProcsNeeded();  //number of processors
		if(counter < 10){

			counter++;
		}
		if(debug)
			System.err.println("allocate(" + allocInfo.job + "); " +
					(numAvail - num) + " processors free");

		if(num > numAvail)
			Main.error("Attempt to allocate " + num
					+ " processors when only " + numAvail 
					+ " are available");

		numAvail = numAvail - num;
	}

	public void deallocate(AllocInfo allocInfo) {
		//deallocate processors

		int num = allocInfo.job.getProcsNeeded();  //number of processors
		if(counter_out < 10){
			counter_out++;
		}
		if(debug)
			System.err.println("deallocate(" + allocInfo.job + "); " +
					(numAvail + num) + " processors free");

		if(num > (numProcs - numAvail))
			Main.error("Attempt to deallocate " + num
					+ " processors when only " + (numProcs-numAvail)
					+ " are busy");

		numAvail += num;
	}
}
