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

import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class MeshAllocInfo extends AllocInfo {

    public MeshLocation[] processors;

    public MeshAllocInfo(Job j) {
	super(j);
	processors = new MeshLocation[j.getProcsNeeded()];
    }

    public String toString() {
	//returns list of processors as coordinates
	
	String retVal = "[";
	for(MeshLocation ml : processors) 
	    retVal += ml;
	return retVal + "]";
    }

    public String getProcList(Machine m){
    	//returns list of processors as single integers

    	String ret="";
    	try{
    		Mesh mesh = (Mesh) m;
    		for(MeshLocation ml : processors){
    			ret += ml.x + mesh.getXDim()*ml.y + mesh.getXDim()*mesh.getYDim()*ml.z+",";
    		}
    		if(ret.length()>0)
    			ret=ret.substring(0,ret.length()-1);
    	} catch(ClassCastException e){
    		Main.error("MeshAllocInfo requires Mesh machine");
    	}
    	return ret;	
    }
}
