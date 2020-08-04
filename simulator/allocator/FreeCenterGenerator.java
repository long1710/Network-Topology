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
import java.util.List;
import simulator.Mesh;
import simulator.MeshLocation;

public class FreeCenterGenerator extends CenterGenerator {
	//generated list is all free locations

	public FreeCenterGenerator(Mesh m) {
		super(m);
	}

	public List<MeshLocation> getCenters(ArrayList<MeshLocation> available) {
		//returns List containing contents of available
		List<MeshLocation> retVal = (List<MeshLocation>)
				new ArrayList<MeshLocation>();
		for(MeshLocation loc : available)
			retVal.add(loc);
		return retVal;
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"FreeCenterGenerator";
	}
}

