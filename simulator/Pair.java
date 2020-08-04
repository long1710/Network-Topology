/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Class to hold pair of objects.  Used to return multiple things at once.
 */

package simulator;

public class Pair<A,B> {
    private A first;
    private B second;

    public Pair(A a,B b){
	first=a;
	second=b;
    }

    public A getFirst() {  return first; }
    public B getSecond() {  return second; }
    public void setFirst(A a) {  first=a; }
    public void setSecond(B b) {  second=b; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
		Pair<A,B> other = (Pair<A,B>) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }
}
