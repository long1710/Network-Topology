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
import simulator.HasSetupInfo;
import simulator.Mesh;
import simulator.MeshLocation;

public abstract class CenterGenerator implements HasSetupInfo {
    //a way to generate possible center points
    
    Mesh machine;  //the machine we're generating for
    
    protected CenterGenerator(Mesh m) {
	machine = m;
    }
    
    abstract public List<MeshLocation> getCenters(ArrayList<MeshLocation> available);
    //returns centers to try given the current free processors
}

