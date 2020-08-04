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

import java.util.TreeSet;
import simulator.Job;

public class MBSMeshAllocInfo extends MeshAllocInfo {

    public TreeSet<Block> blocks;

    public MBSMeshAllocInfo(Job j){
	super(j);
	//Keep track of the blocks allocated
	this.blocks = new TreeSet<Block>();
    }

    public String toString(){
	String retVal = this.job.toString()+"\n  ";
	for(Block block : blocks){
	    retVal = retVal + block;
	}
	return retVal;
    }
}