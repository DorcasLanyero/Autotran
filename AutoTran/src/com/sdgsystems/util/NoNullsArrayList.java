package com.sdgsystems.util;

import java.util.ArrayList;

public class NoNullsArrayList<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
     * Adds the specified object at the end of this {@code ArrayList} unless it's null.
     *
     * @param object
     *            the object to add.
     * @return always true
     */
    @Override 
    public boolean add(E object) {
    	if(object != null) {
    		return super.add(object);
    	}
    	else {
    		return false;
    	}
    }
    
    @Override 
    public void add(int index, E object) {
    	if(object != null) {
    		super.add(index, object);
    	}
    }
}
