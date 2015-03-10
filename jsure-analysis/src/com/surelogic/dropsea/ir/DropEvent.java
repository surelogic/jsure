package com.surelogic.dropsea.ir;

/**
 * Describes events that occur during a drop's lifetime.
 */
public enum DropEvent {

	/**
	 * The drop has been created. Until it is invalidated, the the knowledge the
	 * drop represents is considered true.
	 */
	Created,
	/**
	 * The drop has been invalidated. The knowledge it represents has is
	 * obsolete or has become false.
	 */
	Invalidated,
	/**
	 * A dependent of the drop has been invalidated.
	 */
	DependentInvalidated,

	/**
	 * A deponent of the drop has been invalidated.
	 */
	DeponentInvalidated
}
