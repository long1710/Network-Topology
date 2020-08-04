/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Allocator that assigns the first available processors (according to
 * order specified when allocator is created).
 */

package simulator.allocator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import simulator.Factory;
import simulator.Job;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class SortedFreeListAllocator extends LinearAllocator {

	public SortedFreeListAllocator(Mesh m, String filename) {
		//takes machine to be allocated and file giving linear order
		//(file format described at head of LinearAllocator.java)
		super(m, filename);
	}

	public static SortedFreeListAllocator Make(ArrayList<String> params){
		Factory.argsAtLeast(1,params);
		Factory.argsAtMost(1,params);

		try{
			Mesh m = (Mesh) Main.getMachine();
			return new SortedFreeListAllocator(m,params.get(1));
		}
		catch(ClassCastException e){
			Main.error("Linear allocators require a Mesh machine");
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
		return com+"Linear Allocator (Sorted Free List)";
	}

	public AllocInfo allocate(Job job) {
		//allocates j if possible
		//returns information on the allocation or null if it wasn't possible
		//(doesn't make allocation; merely returns info on possible allocation)

		if(!canAllocate(job))
			return null;

		TreeSet<MeshLocation> avail = new TreeSet<MeshLocation>(ordering);
		avail.addAll(((Mesh)machine).freeProcessors());

		int num = job.getProcsNeeded();  //number of processors for job

		MeshAllocInfo retVal = new MeshAllocInfo(job);
		Iterator<MeshLocation> it = avail.iterator();
		for(int i=0; i<num; i++)
			retVal.processors[i] = it.next();
		return retVal;
	}
}
