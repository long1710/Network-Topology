/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package simulator.allocator;

import java.util.ArrayList;
import java.util.Random;
import simulator.Factory;
import simulator.Job;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class RandomAllocator extends Allocator {

	private Random randSrc;

	public RandomAllocator(Mesh mesh) {
		machine = mesh;
		randSrc = new Random();
	}

	public static RandomAllocator Make(ArrayList<String> params){
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(0,params);

		try{
			Mesh m = (Mesh) Main.getMachine();
			return new RandomAllocator(m);
		}
		catch(ClassCastException e){
			Main.error("Random allocator requires a Mesh machine");
		}
		return null;

	}

	public static String getParamHelp(){
		return "";
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Random Allocator";
	}

	public AllocInfo allocate(Job job) {
		//allocates job if possible
		//returns information on the allocation or null if it wasn't possible
		//(doesn't make allocation; merely returns info on possible allocation)

		if(!canAllocate(job))
			return null;

		MeshAllocInfo retVal = new MeshAllocInfo(job);

		//figure out which processors to use
		int numProcs = job.getProcsNeeded();
		ArrayList<MeshLocation> available = ((Mesh)machine).freeProcessors();
		for(int i=0; i<numProcs; i++) {
			int num = randSrc.nextInt(available.size());
			retVal.processors[i] = available.remove(num);
		}
		return retVal;
	}
}
