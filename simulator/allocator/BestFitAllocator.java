/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 *  Allocator that uses the best-fit linear allocation strategy
 */

package simulator.allocator;
/**
 * (according to the order specified when the allocator is created).
 * Uses the smallest interval of free processors that is big enough.  If
 * none are big enough, chooses the one that minimizes the span
 * (maximum distance along linear order between assigned processors).
 */

import java.util.ArrayList;
import simulator.Factory;
import simulator.Job;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class BestFitAllocator extends LinearAllocator {

	public BestFitAllocator(Mesh m, String filename) {
		//takes machine to be allocated and file giving linear order
		//(file format described at head of LinearAllocator.java)
		super(m, filename);
	}

	public static BestFitAllocator Make(ArrayList<String> params) {
		Factory.argsAtLeast(1,params);
		Factory.argsAtMost(1,params);

		try {
			Mesh m = (Mesh) Main.getMachine();
			return new BestFitAllocator(m,params.get(1));
		}
		catch(ClassCastException e){
			Main.error("Linear allocators require a Mesh machine");
		}
		catch (IndexOutOfBoundsException z) {
			System.err.println("ERROR (IOOB):  " + z);
			System.exit(0);
		}
		return null;
	}

	public static String getParamHelp(){
		return "[<file>]\n\tfile: Path to file giving the curve";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Linear Allocator (Best Fit)";
	}

	public AllocInfo allocate(Job job) {
		//allocates job if possible
		//returns information on the allocation or null if it wasn't possible
		//(doesn't make allocation; merely returns info on possible allocation)

		if(!canAllocate(job))   //check if we have enough free processors
			return null;

		ArrayList<ArrayList<MeshLocation>> intervals = getIntervals();

		int num = job.getProcsNeeded();  //number of processors for job

		int bestInterval = -1;  //index of best interval found so far
		//(-1 = none)
		int bestSize = Integer.MAX_VALUE;  //its size

		//look for smallest sufficiently-large interval
		for(int i=0; i<intervals.size(); i++) {
			int size = intervals.get(i).size();
			if((size >= num) && (size < bestSize)) {
				bestInterval = i;
				bestSize = size;
			}
		}

		if(bestInterval == -1) {
			//no single interval is big enough; minimize the span
			return minSpanAllocate(job);
		} else {
			MeshAllocInfo retVal = new MeshAllocInfo(job);
			for(int j=0; j<num; j++)
				retVal.processors[j] = intervals.get(bestInterval).get(j);
			return retVal;
		}
	}
}
