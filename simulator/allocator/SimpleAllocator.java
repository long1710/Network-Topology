/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

package simulator.allocator;

import java.util.ArrayList;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.SimpleMachine;

public class SimpleAllocator extends Allocator {

	public SimpleAllocator(SimpleMachine m) {
		machine = m;
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Simple Allocator";
	}


	public static SimpleAllocator Make(ArrayList<String> params) {
		Factory.argsAtMost(0,params);
		Machine m = Main.getMachine();
		try{
			SimpleMachine sm = (SimpleMachine) m;

			return new SimpleAllocator(sm);
		} catch(ClassCastException e) {
			Main.error("You cannot use SimpleAllocator with anything but SimpleMachine");
		}
		return null;
	}

	public static String getParamHelp() {
		return "";
	}

	public AllocInfo allocate(Job j) {
		//allocates j if possible
		//returns information on the allocation or null if it wasn't possible
		//(doesn't make allocation; merely returns info on possible allocation)

		if(canAllocate(j)) {
			return new AllocInfo(j);
		}
		return null;
	}
}
