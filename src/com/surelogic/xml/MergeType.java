package com.surelogic.xml;

public enum MergeType {
	/**
	 * Merge dirty elements to fluid 
	 */
	MERGE, 
	/**
	 * Update changes to client
	 */
	UPDATE;
	
	public static MergeType get(boolean toClient) {
		return toClient ? UPDATE : MERGE;
	}
}
