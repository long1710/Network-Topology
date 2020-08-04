/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Interface that contains a single abstract method called getSetupInfo.
 * Classes that have many different configurations (such as Machines, 
 * Schedulers, and Allocators) should implement this interface and
 * any non-abstract class derived from those classes should implement the 
 * getSetupInfo and have it print information about the parameters and 
 * setup info for the object.
*/

package simulator;

public interface HasSetupInfo{
    public abstract String getSetupInfo(boolean comment);
};
