/*
 * Copyright (c) 2014, Knox College
 * All rights reserved.
 *
 * This file is part of the PReMAS software package. For license information, see the LICENSE file
 * in the top level directory of the distribution.
 */

/**
 * A TaskMapper is a vehicle for taking an allocation and specifying
 * an assignment a mesh coordinate (numbered in row-major order from
 * the bottom left) in a job to a MeshLocation of a processor
 * in a given allocation.
 * A TaskMapper object is also the primary repository for tools for 
 * analyzing a mapping.
 */

package mapping;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import simulator.Main;
import simulator.Mesh;
import simulator.MeshLocation;

public abstract class TaskMapper {
    
    /** 
     * The mesh_map method takes a collection of processors (as AllocInfo in future
     * implementations) and the dimensions of a job to be assigned and
     * returns a mapping from a numbered processor (in the job, numbered in 
     * column-major order) to a physical location in the machine mesh
     */
    abstract public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim);

    /**
     * The score method takes a mapping given by mesh_map and a dimension of a job
     * and returns the total communication distance that job will take for each
     * neighbor to communicate. When calling, use the actual job dimension
     * as entered into the machine. Score assumes a column-major order.
     */
    public int score(Map<Integer, MeshLocation> map, JobDimension dim) {
    	int size = dim.totProcs();
    	int retVal = 0;
    	
    	for (int i=0; i<size; i++) {
    		MeshLocation curr = map.get(i);
    		int[] neighbors = this.neighbors(i, dim);
    		for (int l: neighbors) {
    			if (l>-1)
    				retVal+=curr.L1DistanceTo(map.get(l));
    		}
    	}

    	return retVal;
    }

    /**
     * @param job dimensions
     * @return number of edges in the job
     */
    public int numEdges(JobDimension dim) {
    	return (((dim.z - 1) * dim.x * dim.y) +
				((dim.x - 1) * dim.y * dim.z) +
				((dim.y - 1) * dim.x * dim.z));
    }
    
    //The avgDist() method takes a mapping given by mesh_map and
    //the dimensions of a job and returns the average L1 distance
    //between each pair of adjacent (in the job, not the mapping) job nodes
    public double avgDist(Map<Integer, MeshLocation> map, JobDimension dim) {
    	int size = dim.totProcs();
    	int totalDist = 0;   //total distance between communicating nodes

    	for (int i=0; i<size; i++) {
    		MeshLocation curr = map.get(i);
    		int[] neighbors = this.neighbors(i, dim);
    		for (int l: neighbors) {
    			if (l>-1)
    				totalDist+=curr.L1DistanceTo(map.get(l));
    		}
    	}

    	double average = ((double) totalDist / numEdges(dim)) / 2;  //divided by 2 so we don't double count edges
    	return average;
    }

    //returns total distance that i communicates to it neighbors
    //TODO: rename
    //TODO: look at changing the return type; who calls this?
	public double avgDistLocal(Map<Integer, MeshLocation> map, JobDimension dim, int i) {
		int retVal = 0;
		
		MeshLocation curr = map.get(i);
		int[] neighbors = this.neighbors(i, dim);
		for (int l : neighbors) {
			if (l > -1)
				retVal += curr.L1DistanceTo(map.get(l));
		}
		
		return retVal;
	}
	
	//returns total distance that i and j communicate to their neighbors
    //TODO: rename
	//TODO: probably refactor to use the above
	public long avgDistLocal(Map<Integer, MeshLocation> map, JobDimension dim, int i, int j) {
		long retVal = 0;
		
		MeshLocation curr = map.get(i);
		int[] neighbors = this.neighbors(i, dim);
		for (int l : neighbors) {
			if (l > -1)
				retVal += curr.L1DistanceTo(map.get(l));
		}
		curr = map.get(j);
		neighbors = this.neighbors(j, dim);
		for (int l : neighbors) {
			if (l > -1)
				retVal += curr.L1DistanceTo(map.get(l));
		}
		
		return retVal;
	}

	//takes mapping, job dimensions, and average (as computed by avgDist) and
	//returns variance in edge lengths:
	//  ave(sum (val-avg)^2)
	public double variance(Map<Integer, MeshLocation> map, JobDimension dim, double ave) {
		int size = dim.totProcs();
		double sum = 0;

		for (int i=0; i<size; i++) {
			MeshLocation curr = map.get(i);
			int[] neighbors = this.neighbors(i, dim);
			for (int l: neighbors) {
				if (l>-1) {
					double dev = curr.L1DistanceTo(map.get(l)) - ave;
					sum += (dev * dev);
				}
			}
		}

		double average = ((double) sum / numEdges(dim)) / 2;  //divided by 2 so we don't double count edges
		return average;
	}
	
    //returns the greatest distance between two communicating processors
	public int longestDist(Map<Integer, MeshLocation> map, JobDimension dim) {
		int size = dim.totProcs();
		int retVal = 0;

		for (int i=0; i<size; i++) {
			MeshLocation curr = map.get(i);
			int[] neighbors = this.neighbors(i, dim);
			for (int l: neighbors) {
				if (l>-1)
					if(retVal < curr.L1DistanceTo(map.get(l)))
						retVal = curr.L1DistanceTo(map.get(l));
			}
		}

		return retVal;
	}

	/**
	 * @param map
	 * @param dim
	 * @return Highest number of pairs communicating across an edge in job
	 * @throws IllegalArgumentException
	 */
	public double highestUsage(Map<Integer, MeshLocation> map, JobDimension dim) throws IllegalArgumentException {
		Set<MeshEdge> edges = this.getEdgeSet(map, dim);

		double mostUsage = 0.0;  //highest usage found so far
		for(MeshEdge e : edges) {
			double usage = e.usage(map, this, dim);
			if(usage > mostUsage)
				mostUsage = usage;
		}
		return mostUsage;
	}

	public double avgUsage(Map<Integer, MeshLocation> map, JobDimension dim) throws IllegalArgumentException {
		Set<MeshEdge> edges = this.getEdgeSet(map, dim);

		double totalUsage = 0.0;
		for(MeshEdge e : edges) {
			totalUsage += e.usage(map, this, dim);
		}
		return totalUsage / edges.size();
	} 

	public double medianUsage(Map<Integer, MeshLocation> map, JobDimension dim) throws IllegalArgumentException {
		Set<MeshEdge> edges = this.getEdgeSet(map, dim);

		double[] usages = new double[edges.size()];
		//calculate the usage for each node, and track which one
		//has the highest usage (congestion)
		int i = 0;
		for(MeshEdge e : edges) {
			usages[i] = e.usage(map, this, dim);
			i++;
		}
		if(usages.length % 2 == 1)
			return usages[(usages.length / 2) + 1];
		else
			return (usages[usages.length / 2] + usages[(usages.length / 2) + 1]) / 2;
	}

	public double[] allUsageStats(Map<Integer, MeshLocation> map, JobDimension dim) throws IllegalArgumentException {
		Set<MeshEdge> edges = this.getEdgeSet(map, dim);

		double[] usages = new double[edges.size()];
		double mostUsage = 0.0;
		double totalUsage = 0.0;
		int i = 0;
		for(MeshEdge e : edges) {
			usages[i] = e.usage(map, this, dim);
			totalUsage += usages[i];
			if(usages[i] > mostUsage) {
				mostUsage = usages[i];
			}
			i++;
		}
		double[] retVal = new double[3];
		retVal[0] = mostUsage;
		retVal[1] = totalUsage / edges.size();
		if(usages.length % 2 == 1)
			retVal[2] = usages[(usages.length / 2) + 1];
		else
			retVal[2] = (usages[usages.length / 2] + usages[(usages.length / 2) + 1]) / 2;
		return retVal;
	}

	/**
	 * @param map
	 * @param dim
	 * @return set of edges used by the job with the given mapping
	 * @throws IllegalArgumentException
	 */
	public Set<MeshEdge> getEdgeSet(Map<Integer, MeshLocation> map, JobDimension dim) throws IllegalArgumentException {
		MeshLocation mL = null;
		Mesh mesh;
		Iterator<MeshLocation> iter = map.values().iterator();  //TODO: why do we need a loop?
		while(iter.hasNext()) {
			mL = iter.next();
			if(mL.getMesh() == null)
				throw new IllegalArgumentException();
			mesh = mL.getMesh();
		}
		mesh = mL.getMesh();
		Set<MeshEdge> edges = new HashSet<MeshEdge>();
		//loop for generating list of the edges
		//only the edges oriented upwards and to the right (in the job) to avoid double counting
		for(int k = 0; k < mesh.getZDim(); k++) {
			for(int j = 0; j < mesh.getYDim(); j++) {
				for(int i = 0; i < mesh.getXDim(); i++) {
					if(mesh.getIsFree(i, j, k)) {
						if(map.containsValue(new MeshLocation(i, j, k))) {
							for(Map.Entry<Integer, MeshLocation> e : map.entrySet()) {
								if(e.getValue().equals(new MeshLocation(i, j, k))) {
									if(e.getValue().x < (mesh.getXDim() - 1))
										edges.add(new MeshEdge(e.getValue(), new MeshLocation(e.getValue().x + 1, e.getValue().y, e.getValue().z)));
									if(e.getValue().y < (mesh.getYDim() - 1))
										edges.add(new MeshEdge(e.getValue(), new MeshLocation(e.getValue().x, e.getValue().y + 1, e.getValue().z)));
									if(e.getValue().z < (mesh.getZDim() - 1))
										edges.add(new MeshEdge(e.getValue(), new MeshLocation(e.getValue().x, e.getValue().y, e.getValue().z + 1)));
								}
							}
						}
					}
				}
			}
		}
		return edges;
	}

    /**
     * Takes a MeshLocation (understood as an element of a job) and the
     * dimensions of that job and returns the column-major order number
     * of that element.
     */
	public int num(MeshLocation loc, JobDimension dim) {
		return loc.y + (dim.y*loc.x) + (dim.y*dim.x*loc.z);
	}

    /**
     * Takes the number of a job element and the dimensions of the job
     * and returns the location of that element if a column-major order
     * is assumed.
     */
    public MeshLocation loc(int elNum, JobDimension dim) {
    	int prod = dim.x * dim.y;
    	int x = (elNum % prod) / dim.y;
    	int y = elNum % dim.y;
    	int z = elNum / prod;

    	return new MeshLocation(x, y, z);
    }

    /**
     * The method returns an array of neighbors to a given numbered processor.
     * If the coordinates (in the job, not the machine) of this processor
     * is (x,y,z), then the order that neighbors are returned is as follows:
     * (x,y-1,z), (x,y+1,z), (x-1,y,z), (x+1,y,z), (x,y,z-1), (x,y,z+1) or
     * down, up, left, right, under, over.
     * When a node does not have a particular neighbor, its place is filled with -1.
     */
    public int[] neighbors(int n, JobDimension dim) {
    	int[] retArr = new int[6];
    	for(int i=0; i<6; i++)
    		retArr[i] = -1;

    	MeshLocation curr = this.loc(n, dim);
    	if (curr.y > 0)
    		retArr[0] = n-1;
    	if (curr.y < dim.y-1)
    		retArr[1] = n+1;
    	if (curr.x > 0)
    		retArr[2] = n-dim.y;
    	if (curr.x < dim.x-1)
    		retArr[3] = n+dim.y;
    	if (curr.z > 0)
    		retArr[4] = n-dim.x*dim.y;
    	if (curr.z < dim.z-1)
    		retArr[5] = n+dim.x*dim.y;
    	return retArr;
    }
    
    //variables set in orderJobDimensions about extent of used processors
    protected int meshMaxX;     //highest and lowest values in X dimension
    protected int meshMinX;
    protected int meshMaxY;     //in Y dimension
    protected int meshMinY;
    protected int meshMaxZ;		//in Z dimension
    protected int meshMinZ;
    protected int meshX;  //size of coordinate range in each dimension
    protected int meshY;
    protected int meshZ;
    
	/**
	* Orders the job dimensions by size so that they correspond with the mesh's dimensions.
	* (Has effect of rotating the job.)
	* 
	* Also sets protected variables mesh{Min,Max}{X,Y,Z}
	*/
    public void orderJobDimensions(MeshLocation[] chosen_procs, JobDimension dim) {
    	meshMaxX = 0;					//highest and lowest values in X dimension
    	meshMinX = Integer.MAX_VALUE;
    	meshMaxY = 0;					//in Y dimension
    	meshMinY = Integer.MAX_VALUE;
    	meshMaxZ = 0;					//in Z dimension
    	meshMinZ = Integer.MAX_VALUE;
    	if(chosen_procs.length != dim.x * dim.y * dim.z)
    		Main.ierror("Allocation and job size don't match in orderJobDimensions");
    	for (MeshLocation m: chosen_procs) {
    		if(m == null)
    			Main.ierror("Array provided as argument to orderJobDimensions must not contain null entries.");
    		if(m.x > meshMaxX)
    			meshMaxX = m.x;
    		if(m.x < meshMinX)
    			meshMinX = m.x;
    		if(m.y > meshMaxY)
    			meshMaxY = m.y;
    		if(m.y < meshMinY)
    			meshMinY = m.y;
    		if(m.z > meshMaxZ)
    			meshMaxZ = m.z;
    		if(m.z < meshMinZ)
    			meshMinZ = m.z;
    	}

    	meshX = meshMaxX - meshMinX + 1;  //size of coordinate range in each dimension
    	meshY = meshMaxY - meshMinY + 1;
    	meshZ = meshMaxZ - meshMinZ + 1;

    	if((meshX - meshY) * (dim.x - dim.y) < 0) {  	//correct relationship between X & Y
    		int temp = dim.x;
    		dim.x = dim.y;
    		dim.y = temp;
    	} 
    	if((meshX - meshZ) * (dim.x - dim.z) < 0) {	    //correct relationship between X & Z
    		int temp = dim.x;
    		dim.x = dim.z;
    		dim.z = temp;
    	}
    	if((meshY - meshZ) * (dim.y - dim.z) < 0) {     //correct relationship between Y & Z
    		int temp = dim.y;
    		dim.y = dim.z;
    		dim.z = temp;
    	}
    }
}
