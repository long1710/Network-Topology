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

import java.util.Iterator;
import java.util.TreeSet;
import simulator.MeshLocation;

public class Block implements Comparable<Block> {
	public TreeSet<Block> children;
	public Block parent;
	public MeshLocation dimension;
	public MeshLocation location;

	public Block (MeshLocation l, MeshLocation d) {
		this.dimension = d;
		this.location = l;
		this.children = new TreeSet<Block>();
		this.parent = null;
	}
	public Block (MeshLocation l, MeshLocation d, Block p) {
		this.dimension = d;
		this.location = l;
		this.children = new TreeSet<Block>();
		this.parent = p;
	}

	/**
	 * Returns a Iterator over all the processors in this block
	 */
	public Iterator<MeshLocation> processors(){
		TreeSet<MeshLocation> processors = new TreeSet<MeshLocation>();
		for (int i=0;i<this.dimension.x;i++){
			for(int j=0;j<this.dimension.y;j++){
				for(int k=0;k<this.dimension.z;k++){
					processors.add(new MeshLocation(this.location.x+i,
													this.location.y+j,
													this.location.z+k));
				}
			}
		}
		return processors.iterator();
	}

	/**
	 * Calculate the number of processors in this block
	 */
	public int size(){
		return this.dimension.x*this.dimension.y*this.dimension.z;
	}

	public Iterator<Block> getChildren(){
		return this.children.iterator();
	}
	
	public void addChild(Block b){
		this.children.add(b);
	}
	
	/**
	 * Below are standard class methods: equals, toString()
	 */

	public int compareTo(Block other){
		if(this.size() == other.size()){
			if(this.location.equals(other.location)){
				return this.dimension.compareTo(other.dimension);
			}
			return this.location.compareTo(other.location);
		}
		return this.size() - other.size();
	}

	public boolean equals (Object o){
		if (o instanceof Block)
			return equals((Block) o);
		return false;
	}

	/**
	 * This equals method is not truly equals.  It is more of similar enough to fool their mother.
	 * The reason for this is the fact that most collections of blocks are TreeSets.  To test for contains
	 * it would be almost unreasonably difficult to have to construct a tree of parent/children when
	 * checking location and dimension is enough.
	 */
	public boolean equals (Block b){
		return this.dimension.equals(b.dimension)
		&& this.location.equals(b.location);
	}

	public String toString(){
		return "Block["+this.dimension.x+"x"+this.dimension.y+"x"+this.dimension.z+"]@"+this.location;
	}
}