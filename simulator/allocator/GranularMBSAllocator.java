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

public class GranularMBSAllocator extends MBSAllocator {

	public GranularMBSAllocator(Mesh m, int x, int y, int z) {
		super(m, x, y, z);
		this.DEBUG = false;
	}

    public String getSetupInfo(boolean comment){
    	String com;
    	if(comment) com="# ";
    	else com="";
    	return com+"Multiple Buddy Strategy (MBS) Allocator using Granular divisions";
    }
	
	public static GranularMBSAllocator Make(ArrayList<String> params){
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(0,params);

		Machine mach = Main.getMachine();
		if(mach instanceof Mesh){
			Mesh m = (Mesh) mach;
			return new GranularMBSAllocator(m,m.getXDim(),m.getYDim(),m.getZDim());
		} else {
			Main.error("Granular MBS Allocator requires a mesh machine");
		}
		return null;
	}

	public void initialize(MeshLocation dim, MeshLocation off){
		//add all the 1x1x1's so the set of blocks
		int rank = createRank(1);
		MeshLocation sizeOneDim = new MeshLocation(1,1,1);
		for (int i=0;i<dim.x;i++){
			for(int j=0;j<dim.y;j++){
				for(int k=0;k<dim.z;k++){
					this.FBR.get(rank).add(new Block(new MeshLocation(i,j,k),sizeOneDim));
				}
			}
		}
		
		//iterate over all the ranks
		while(mergeAll());
		
		System.out.println("");
	}
	
	/**
	 * The new mergeAll, with start with the given rank, and scan all the ranks below it (descending).
	 * mergeAll with make 3 passes for each rank
	 */
	public boolean mergeAll(){
		//default return
		boolean retVal = false;
		
		//workaround to delete during iteration
		TreeSet<Block> toRemove = new TreeSet<Block>();
		
		//we will be scanning 3 times, for each dimension
		for(int d=0;d<3;d++){		
			//scan through and try to merge everything
			for(int i=(this.ordering.size()-1);i>=0;i--){
				TreeSet<Block> blocks = this.FBR.get(i);
				Iterator<Block> it = blocks.iterator();

				while(blocks.size() > 0 && it.hasNext()){
					//get the first block
					Block first = it.next();

					//make sure we aren't trying to remove it
					if(toRemove.contains(first)){
						it.remove();
						toRemove.remove(first);
					} else {
						//figure out the second block
						Block second = nextBlock(d,first);

						if(blocks.contains(second)){
							//get the real second block
							second = FBRGet(second);

							//make our new block 
							Block newBlock = mergeBlocks(first,second);
				
							//preserve hierarchy
							newBlock.addChild(first);
							newBlock.addChild(second);
							first.parent = newBlock;
							second.parent = newBlock;

							//add new block
							int newRank = createRank(newBlock.size());
							this.FBR.get(newRank).add(newBlock);

							//get rid of the old blocks
							it.remove();
							toRemove.add(second);

							//change return value since we created a block
							retVal = true;
						} //ends nested if
					} //ends removal if
				} //ends while iteration
			} //ends for through ranks
		} //ends for all dimensions
		return retVal;
	}

	/**
	 * returns the next block, by looking in x,y,z directions in the order depending on a given d
	 */
	public Block nextBlock(int d, Block first){
		if(d < 0 || d > 2)
			throw new IndexOutOfBoundsException();
		if(d == 0)
			return lookX(first);
		if(d == 1)
			return lookY(first);
		if(d == 2)
			return lookZ(first);
		return null;
	}
		
	/**
	 * Return a block that is the given blocks partner in the x direction.
	 */
	public Block lookX(Block b){
		return new Block(new MeshLocation(b.location.x+b.dimension.x,b.location.y,b.location.z),b.dimension);
	}
	
	public Block lookY(Block b){
		return new Block(new MeshLocation(b.location.x,b.location.y+b.dimension.y,b.location.z),b.dimension);
	}
	
	public Block lookZ(Block b){
		return new Block(new MeshLocation(b.location.x,b.location.y,b.location.z+b.dimension.z),b.dimension);
	}
	
	/**
	 * Attempts to perform a get operation on a set. Returns null if the block is not found
	 * This method is needed because Block.equals() is not really equals, but really similarEnough()
	 * We need to get the block from the FBR because it comes with the parent/children hierarchy.
	 */
	public Block FBRGet(Block needle){
		Block retVal;

		//Figure out where too look
		//TODO: error checking?
		TreeSet<Block> haystack = this.FBR.get(this.ordering.indexOf(needle.size()));

		if(!haystack.contains(needle)){
			return null;
		}
			
		//Locate it, and return
		Iterator<Block> it = haystack.iterator();
		if(!it.hasNext())
			return null;
		retVal = it.next();
		while(!needle.equals(retVal)){
			if(!it.hasNext())
				return null;
			retVal = it.next();
		}
		
		return retVal;
	}
	
	public Block mergeBlocks(Block first, Block second){
		if(first.equals(second) || !first.dimension.equals(second.dimension)){
			throw new UnsupportedOperationException();
		}
		//do some setup
		MeshLocation dimension = new MeshLocation(0,0,0);
		MeshLocation location = second.location;
		if (first.location.compareTo(second.location) < 0)
			location = first.location;

		//determine whether we need to change the x dimension
		if (first.location.x == second.location.x){
			dimension.x = first.dimension.x;
		} else {
			dimension.x = first.dimension.x+second.dimension.x;
		}
		
		//determine whether we need to change the y dimension
		if (first.location.y == second.location.y){
			dimension.y = first.dimension.y;
		} else {
			dimension.y = first.dimension.y+second.dimension.y;
		}
		
		//determine whether we need to change the z dimension
		if (first.location.z == second.location.z){
			dimension.z = first.dimension.z;
		} else {
			dimension.z = first.dimension.z+second.dimension.z;
		}
	
		Block toReturn = new Block(location,dimension);
		toReturn.addChild(first);
		toReturn.addChild(second);
		return toReturn;
	}
}