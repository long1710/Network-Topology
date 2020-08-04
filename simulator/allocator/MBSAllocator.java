/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * By default the MBSAllocator provides a layered 2D mesh approach to
 * the Multi Buddy Strategy
 * A Note on Extending:  The only thing you need to do is override the initialize method,
 * create complete blocks, and make sure the "root" blocks are in the FBR.
 */

package simulator.allocator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import simulator.Factory;
import simulator.Job;
import simulator.Machine;
import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public class MBSAllocator extends Allocator {

	protected ArrayList<TreeSet<Block>> FBR;
	protected ArrayList<Integer> ordering;

	//We know it must be a mesh, so make it one so we can access the goods.
	protected Mesh meshMachine;

	protected boolean DEBUG = false;

	public MBSAllocator(Mesh m, int x, int y, int z){
		this.meshMachine = m; //make us happy
		this.machine = m;     //make Allocator happy

		this.FBR = new ArrayList<TreeSet<Block>>();
		this.ordering = new ArrayList<Integer>();

		//create the starting blocks
		initialize(new MeshLocation(x,y,z),new MeshLocation(0,0,0));
		if (DEBUG) printFBR("Post Initialize:");
	}

	public static MBSAllocator Make(ArrayList<String> params){
		Factory.argsAtLeast(0,params);
		Factory.argsAtMost(0,params);

		Machine mach = Main.getMachine();
		if(mach instanceof Mesh){
			Mesh m = (Mesh) mach;
			return new MBSAllocator(m,m.getXDim(),m.getYDim(),m.getZDim());
		} else {
			Main.error("MBS Allocator requires a mesh machine");
		}
		return null;
	}

	public String getSetupInfo(boolean comment){
		String com;
		if(comment) com="# ";
		else com="";
		return com+"Multiple Buddy Strategy (MBS) Allocator";
	}

	public static String getParamHelp(){
		return "";
	}

	/**
	 * Initialize will fill in the FBR with z number of blocks (1 for
	 * each layer) that fit into the given x,y dimensions.  It is
	 * assumed that those dimensions are non-zero.
	 */
	public void initialize(MeshLocation dim, MeshLocation off){
		if (DEBUG) System.out.println("Initializing a "+dim.x+"x"+dim.y+"x"+dim.z+" region at "+off);

		//Figure out the largest possible block possible
		int maxSize = (int) Math.floor(Math.log(Math.min(dim.x,dim.y))/Math.log(2));
		int sideLen = (int) Math.pow(2,maxSize);
		//create a flat square
		MeshLocation blockDim = new MeshLocation(sideLen,sideLen,1);
		int size = blockDim.x*blockDim.y*blockDim.z;

		//see if we have already made one of these size blocks
		int rank = this.ordering.indexOf(size);
		if(rank < 0){
			rank = createRank(size);
		}

		//add block to the set at the given rank, determined by lookup
		for(int i=0; i<dim.z; i++){
			Block block = new Block(new MeshLocation(off.x,off.y,i),new MeshLocation(blockDim.x,blockDim.y,blockDim.z)); //Do I need to make a new dim object?
			this.FBR.get(rank).add(block);
			createChildren(block);

			//update the rank (createChildren may have added new ranks to ordering and FBR)
			rank = this.ordering.indexOf(size);
		}

		//initialize the two remaining rectangles of the region
		if(dim.x - sideLen > 0)
			initialize(new MeshLocation(dim.x - sideLen,dim.y,dim.z),new MeshLocation(off.x+sideLen,off.y,1));
		if(dim.y - sideLen > 0)
			initialize(new MeshLocation(sideLen,dim.y-sideLen,dim.z),new MeshLocation(off.x,off.y+sideLen,1));
	}

	/**
	 * Creates a rank in both the FBR, and in the ordering.
	 * If a rank already exists, it does not create a new rank,
	 * it just returns the one already there
	 */
	public int createRank(int size){
		if (this.ordering.contains(size))
			return this.ordering.indexOf(size);

		//scan through to the right spot
		int i = 0;
		for (i=0;i<this.ordering.size() && this.ordering.get(i) < size; i++);

		//add this block size into the ordering
		this.ordering.add(i,size);

		//make our corresponding TreeSet
		this.FBR.add(i, new TreeSet<Block>());

		if (DEBUG) System.out.println("Added a rank "+i+" for size "+size);

		return i;
	}

	/**
	 *  Essentially this will reinitialize a block, except add the
	 *  children to the b.children, then recurse
	 */
	protected void createChildren(Block b){
		Iterator<Block> children = splitBlock(b);
		Block next;

		if (DEBUG) System.out.print("Creating children for "+b+" :: ");

		while (children.hasNext()){
			next = children.next();

			if (DEBUG) System.out.print(next+" ");

			b.addChild(next);

			//make sure the proper rank exists, in both ordering and FBR
			int size = next.dimension.x*next.dimension.y*next.dimension.z;
			createRank(size);

			if(next.size() > 1)
				createChildren(next);
		}
		if (DEBUG) System.out.println("");
	}

	public Iterator<Block> splitBlock (Block b) {
		//create the set to iterate over
		TreeSet<Block> children = new TreeSet<Block>();

		//determine the size (blocks should be cubes, thus dimension.x=dimension.y)
		int size = (int) (Math.log(b.dimension.x)/Math.log(2));
		//we want one size smaller, but they need to be
		if(size-1 >= 0){
			int sideLen = (int) Math.pow(2,size-1);
			MeshLocation dim = new MeshLocation(sideLen,sideLen,1 /*sideLen*/);

			children.add(new Block(new MeshLocation(b.location.x,
					b.location.y,
					b.location.z),
					dim,b));
			children.add(new Block(new MeshLocation(b.location.x,
					b.location.y+sideLen,
					b.location.z),
					dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen,
					b.location.y+sideLen,
					b.location.z),
					dim,b));
			children.add(new Block(new MeshLocation(b.location.x+sideLen,
					b.location.y,
					b.location.z),
					dim,b));
		}
		if (DEBUG) System.out.println("Made blocks for splitBlock("+b+")");
		return children.iterator();
	}

	public MBSMeshAllocInfo allocate(Job job){
		if (DEBUG) System.out.println("Allocating "+job);

		MBSMeshAllocInfo retVal = new MBSMeshAllocInfo(job);
		int allocated = 0;

		//a map of dimensions to numbers
		TreeMap<Integer,Integer> RBR = factorRequest(job);

		while(allocated < job.getProcsNeeded()){
			//Start trying allocate the largest blocks
			Integer currentRank = RBR.lastKey();

			//see if there is a key like that in FBR.
			if(this.FBR.get(currentRank).size() > 0){  //TODO: try/catch for error?
				//Move the block from FBR to retVal
				Block newBlock = this.FBR.get(currentRank).first();
				retVal.blocks.add(newBlock);
				this.FBR.get(currentRank).remove(newBlock);

				//add all the processors to retVal, and make progress
				//in the loop
				Iterator<MeshLocation> it = newBlock.processors();
				for (int i=allocated;it.hasNext();i++){
					retVal.processors[i] = it.next();
					allocated++;
				}

				//also be sure to remove the allocated block from the RBR
				if (RBR.get(currentRank)-1 > 0){
					RBR.put(currentRank,RBR.get(currentRank)-1);
				} else {
					RBR.remove(currentRank);
				}
			} else {
				//See if there is a larger one we can split up
				if(!splitLarger(currentRank)){
					//since we were unable to split a larger block, make request smaller
					splitRequest(RBR,currentRank);

					//if there are non left to request at the current rank, clean up the RBR
					if(RBR.get(currentRank) <= 0){
						RBR.remove(currentRank);

						//make sure we look at the next lower rank
						//currentRank = currentRank - 1;
					}

				}
				if (DEBUG) printFBR("After all splitting");
			}
		}
		return retVal;
	}

	/**
	 * Calculates the RBR, which is a map of ranks to number of blocks at that rank
	 */
	public TreeMap<Integer,Integer> factorRequest(Job j){
		TreeMap<Integer,Integer> retVal = new TreeMap<Integer,Integer>();
		int procs = 0;


		while (procs < j.getProcsNeeded()){
			//begin our search
			Iterator<Integer> sizes = this.ordering.iterator();

			//look for the largest size block that fits the procs needed
			int size = -1;
			int prevSize = -1;
			while(sizes.hasNext()){
				prevSize = size;
				size = sizes.next();
				if(size > (j.getProcsNeeded() - procs)){
					//cancel the progress made with this run through the loop
					size = prevSize;
					break;
				}
			}
			//make sure something was done
			if(prevSize == -1 || size == -1){
				//catch the special case where we only have one size
				if(this.ordering.size() == 1){
					size = this.ordering.get(0);
				} else {
					throw new Error("while loop never ran");
				}
			}

			//get the rank
			int rank = this.ordering.indexOf(size);
			if(!retVal.containsKey(rank)){
				retVal.put(rank,0);
			}

			//increment that value of the map
			retVal.put(rank,retVal.get(rank)+1);

			//make progress in the larger while loop
			procs += this.ordering.get(rank);
		}

		if (DEBUG){
			System.out.println("Factored request: ");
			printRBR(retVal);
		}
		return retVal;
	}

	/**
	 * Breaks up a request for a block with a given rank into smaller request if able.
	 */
	public void splitRequest(TreeMap<Integer,Integer> RBR, Integer rank){
		if (!RBR.containsKey(rank))
			throw new IndexOutOfBoundsException();
		if (rank <= 0)
			throw new UnsupportedOperationException("Cannot split a request of size 0");
		if (RBR.get(rank) == 0){
			//throw new UnsupportedOperationException("Cannot split a block of size 0");
			return;
		}

		//decrement the current rank
		RBR.put(rank,RBR.get(rank)-1);

		//get the number of blocks we need from the previous rank
		int count = (int) this.ordering.get(rank)/this.ordering.get(rank-1);

		//increment the previous rank, and if it doesn't exists create it
		if(RBR.containsKey(rank-1)) {
			RBR.put(rank-1,RBR.get(rank-1)+count);
		} else {
			RBR.put(rank-1,count);
		}

		if (DEBUG){
			System.out.println("Split a request up");
			printRBR(RBR);
		}
	}

	/**
	 * Determines whether a split up of a possible larger block was
	 * successful.  It begins looking at one larger than rank.
	 */
	public boolean splitLarger(Integer rank){
		if (DEBUG) System.out.println("Splitting a block at rank "+rank);

		//make sure that we can search in rank+1
		//FBR has same size as ordering
		if (rank+1 >= this.FBR.size())
			return false;

		//pass off the work
		if(this.FBR.get(rank+1).size() == 0){
			//recurse! if necessary
			if(!splitLarger(rank+1))
				return false;
		}

		//split a block since by this point in the method we have guaranteed its existence
		Block toSplit = this.FBR.get(rank+1).first();
		Iterator<Block> spawn = toSplit.getChildren();

		//add children to the FBR
		while (spawn.hasNext()){
			Block currentBlock = spawn.next();
			this.FBR.get(rank).add(currentBlock);
		}

		//remove toSplit from the FBR
		this.FBR.get(rank+1).remove(toSplit);

		return true;
	}

	public void deallocate(AllocInfo alloc){
		if (DEBUG) System.out.println("Deallocating job with "+alloc.job.getProcsNeeded()+" procs");
		//check to make sure it is a MBSMeshAllocInfo...                        
		if (!(alloc instanceof MBSMeshAllocInfo)){
			throw new IllegalArgumentException();
		} else {
			unallocate( (MBSMeshAllocInfo) alloc);
		}
	}

	public void unallocate(MBSMeshAllocInfo info){
		//add all blocks back into the FBR
		for(Block b : info.blocks){
			int rank = this.ordering.indexOf(b.size());
			this.FBR.get(rank).add(b);
		}
		//for each block see if its parent is all free
		for(Block b : info.blocks){
			mergeBlock(b.parent);
		}
	}

	public void mergeBlock(Block p){
		if (!(p instanceof Block))
			return;
		//make sure p isn't in FBR
		int rank = this.ordering.indexOf(p.size());
		if(this.FBR.get(rank).contains(p))
			return;

		//see if children are in the FBR
		for(Block child : p.children){
			rank = this.ordering.indexOf(child.size());
			if(!this.FBR.get(rank).contains(child))
				return;
		}
		//by this point in the code they all are
		for(Block child : p.children){
			rank = this.ordering.indexOf(child.size());
			this.FBR.get(rank).remove(child);
		}
		rank = this.ordering.indexOf(p.size());
		this.FBR.get(rank).add(p);
		//recurse!
		mergeBlock(p.parent);
	}

	public void printRBR(TreeMap<Integer,Integer> RBR){
		Set<Integer> keys = RBR.keySet();
		for(Integer key : keys){
			System.out.println("Rank "+key+" has "+RBR.get(key)+" requested blocks");
		}
	}

	public void printFBR(String msg){
		System.out.println(msg);
		if(this.ordering.size() != this.FBR.size())
			throw new Error("Ordering vs FBR size mismatch");
		for (int i=0;i<this.ordering.size();i++){
			System.out.println("Rank: "+i+" for size "+this.ordering.get(i));
			Iterator<Block> it = this.FBR.get(i).iterator();
			while (it.hasNext()){
				System.out.println("  "+it.next());
			}
		}
	}

	public String stringFBR(){
		String retVal = "";
		if(this.ordering.size() != this.FBR.size())
			throw new Error("Ordering vs FBR size mismatch");
		for (int i=0;i<this.ordering.size();i++){
			retVal = retVal+("Rank: "+i+" for size "+this.ordering.get(i)+"\n");
			Iterator<Block> it = this.FBR.get(i).iterator();
			while (it.hasNext()){
				retVal = retVal+("  "+it.next()+"\n");
			}
		}
		return retVal;
	}
}