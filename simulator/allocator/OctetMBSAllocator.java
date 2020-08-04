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
import java.util.Iterator;
import java.util.TreeSet;
import simulator.Factory;
import simulator.Machine;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class OctetMBSAllocator extends MBSAllocator {

    public OctetMBSAllocator(Mesh m, int x, int y, int z){
    	//we don't do anything special in construction
    	super(m,x,y,z);
    	this.DEBUG = false;
    }

    public String getSetupInfo(boolean comment){
    	String com;
    	if(comment) com="# ";
    	else com="";
    		return com+"Multiple Buddy Strategy (MBS) Allocator using Octet divisions";
    }

	public static OctetMBSAllocator Make(ArrayList<String> params){
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(0,params);

		Machine mach = Main.getMachine();
		if(mach instanceof Mesh){
			Mesh m = (Mesh) mach;
			return new OctetMBSAllocator(m,m.getXDim(),m.getYDim(),m.getZDim());
		} else {
			Main.error("Octet MBS Allocator requires a mesh machine");
		}
		return null;
	}
    
    public void initialize(MeshLocation dim, MeshLocation off){
		//System.out.println("Initializing a "+dim.x+"x"+dim.y+"x"+dim.z+" region at "+off);

		//figure out the largest cube possible
		int constraintSide = Math.min(Math.min(dim.x, dim.y), dim.z);
		int maxSize = (int) Math.floor(Math.log(constraintSide)/Math.log(2));
		int sideLen = (int) Math.pow(2,maxSize);
		
		//Start creating our cube
		MeshLocation blockDim = new MeshLocation(sideLen,sideLen,sideLen);
		int blockSize = blockDim.x*blockDim.y*blockDim.z;
		
		//see if we have already made one of these size blocks
		int rank = this.ordering.indexOf(blockSize);
		if(rank < 0){
			rank = createRank(blockSize);
		}
		
		//add this block to the FBR
		Block block = new Block(new MeshLocation(off.x,off.y,off.z),new MeshLocation(blockDim.x,blockDim.y,blockDim.z)); //Do I need to make a new dim object?
		this.FBR.get(rank).add(block);
		if (block.size() > 1)
			createChildren(block);
		
		//initialize the 3 remaining regions
		if(dim.x - sideLen > 0)
			initialize(new MeshLocation(dim.x-sideLen,sideLen,sideLen), new MeshLocation(off.x+sideLen,off.y,off.z));
		if(dim.y - sideLen > 0)
			initialize(new MeshLocation(dim.x,dim.y-sideLen,sideLen), new MeshLocation(off.x,off.y+sideLen,off.z));
		if(dim.z - sideLen > 0)
			initialize(new MeshLocation(dim.x,dim.y,dim.z-sideLen), new MeshLocation(off.x,off.y,off.z+sideLen));
    }

    public Iterator<Block> splitBlock(Block b){
    	//create a set to iterate over
    	TreeSet<Block> children = new TreeSet<Block>();

    	//determine the size (blocks should be cubes, thus dimension.x=dimension.y=dimension.z)
    	int size = (int) (Math.log(b.dimension.x)/Math.log(2));
    	
    	//we want one size smaller, but they need to be
		if(size-1 >= 0){
			//determine new sideLen
			int sideLen = (int) Math.pow(2,size-1);
			MeshLocation dim = new MeshLocation(sideLen,sideLen,sideLen);

			//Bottom layer of the cube
			children.add(new Block(new MeshLocation(b.location.x, b.location.y, b.location.z),dim,b));
			children.add(new Block(new MeshLocation(b.location.x, b.location.y+sideLen, b.location.z),dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen, b.location.y+sideLen, b.location.z), dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen, b.location.y, b.location.z), dim,b));
			//Top layer of the cube
			children.add(new Block(new MeshLocation(b.location.x, b.location.y, b.location.z+sideLen),dim,b));
			children.add(new Block(new MeshLocation(b.location.x, b.location.y+sideLen, b.location.z+sideLen),dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen, b.location.y+sideLen, b.location.z+sideLen), dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen, b.location.y, b.location.z+sideLen), dim,b));
		}

		if (DEBUG) System.out.println("Made blocks for splitBlock("+b+")");

		return children.iterator();
    }
}