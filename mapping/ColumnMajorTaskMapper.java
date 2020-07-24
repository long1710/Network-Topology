/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description

package mapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import simulator.MeshLocation;

public class ColumnMajorTaskMapper extends TaskMapper {

	public Map<Integer, MeshLocation> mesh_map(MeshLocation[] chosen_procs, JobDimension dim) {
		Map<Integer, MeshLocation> retMap = new HashMap<Integer, MeshLocation>();

		this.orderJobDimensions(chosen_procs, dim);  //rotate the job
		
		Arrays.sort(chosen_procs);

		int i = 0;
		for (MeshLocation m: chosen_procs) {
			retMap.put(i, m);
			if(i >= ((dim.x * dim.y * dim.z) - 1))
				break;
			i++;
		}

		return retMap;
	}

}
