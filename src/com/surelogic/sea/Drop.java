package com.surelogic.sea;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

/**
 * The abstract base class for all drops within the sea, intended to be
 * subclassed and extended. This class forms the basis for a truth maintenance
 * system by managing dependent and deponent drops. Each instance represents a
 * <i>drop</i> of information within a <i>sea</i> of knowledge.
 * <p>
 * Dependent drops are specified by clients though the public interface and
 * deponent drops are automatically tracked.
 * 
 * @see Sea
 */
public abstract class Drop {

	/**
	 * Logger for this class
	 */
	protected static final Logger LOG = SLLogger.getLogger("Drop");

	/**
	 * Constructs a drop within the default sea.
	 */
	public Drop() {
		// use the default sea
		this(Sea.getDefault());
	}

	/**
	 * Constructs a drop within the specified sea.
	 * 
	 * @param sea
	 *            the sea to create the drop within.
	 */
	private Drop(Sea sea) {
		mySea = sea;
		mySea.notify(this, DropEvent.Created);
	}

	/**
	 * Gets the sea that this drop is part of.
	 * 
	 * @return the sea this drop exists within.
	 */
	public final Sea getSea() {
		return mySea;
	}

	/**
	 * Gets this drop's message.
	 * 
	 * @return the message set for this drop, usually used by the UI.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets this drop's message.
	 * 
	 * @param message
	 *            the message to set for the UI about this drop.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Sets this drop's message using a call to
	 * {@link MessageFormat#format(String, Object[])}.
	 * <P>
	 * An example is
	 * 
	 * <pre>
	 * Drop d;
	 * ...
	 * d.setMessage(&quot;lock {0} (pre-scrubbed, not complete)&quot;, lock);
	 * </pre>
	 * 
	 * @param message
	 *            the message to set for the UI about this drop.
	 */
	public void setMessage(String message, Object... args) {
		this.message = (args.length == 0) ? message : MessageFormat.format(
				message, args);
	}

	/**
	 * Adds a dependent drop to this drop, meaning that the truth of the added
	 * dependent drop relies upon the truth of this drop. This method makes this
	 * drop a deponent for the dependent drop.
	 * 
	 * @param dependent
	 *            the drop this drop is a deponent for.
	 */
	final public void addDependent(Drop dependent) {
		if (!valid || dependent == null || !dependent.isValid()) {
			return;
		}
		if (dependents.add(dependent)) {
			dependent.addDeponent(this);
		}
	}

	/**
	 * Adds a group of dependent drops to this drop, meaning that the truth of
	 * all the added dependent drops relies upon the truth of this drop. This
	 * method makes this drop a deponent for each of the dependent drops.
	 * 
	 * @param dependents
	 *            the array of drops this drop is a deponent for.
	 */
	final public void addDependents(Drop[] dependents) {
		if (!valid || dependents == null) {
			return;
		}
		for (int i = 0; i < dependents.length; i++) {
			addDependent(dependents[i]);
		}
	}

	/**
	 * Adds a group of dependent drops to this drop, meaning that the truth of
	 * all the added dependent drops relies upon the truth of this drop. This
	 * method makes this drop a deponent for each of the dependent drops.
	 * 
	 * @param dependents
	 *            the collection of drops this drop is a deponent for.
	 */
	final public void addDependents(Collection<? extends Drop> dependents) {
		if (!valid || dependents == null) {
			return;
		}
		for (Drop drop : dependents) {
			addDependent(drop);
		}
	}

	/**
	 * Adds this drop as a dependent to each member of a group of deponent
	 * drops, meaning that the truth of this drop relies upon the truth of each
	 * of the deponent drops. This method makes this drop a dependent for each
	 * of the deponent drops.
	 * 
	 * @param dependonts
	 *            the collection of drops this drop is a dependent of.
	 */
	final public void addDeponents(Collection<? extends Drop> deponents) {
		if (!valid || dependents == null) {
			return;
		}
		for (Drop drop : deponents) {
			drop.addDependent(this);
		}
	}

	/**
	 * Returns all immediate dependent drops of this drop.
	 * 
	 * @return the set of dependent drops
	 */
	final public Set<Drop> getDependents() {
		return new HashSet<Drop>(dependents);
	}

	/**
	 * Returns all immediate deponent drops of this drop.
	 * 
	 * @return the set of deponent drops
	 */
	final public Set<Drop> getDeponents() {
		return new HashSet<Drop>(deponents);
	}

	/**
	 * Queries if any of this drop's dependent drops matches the given drop
	 * predicate.
	 * 
	 * @param pred
	 *            the drop predicate to use.
	 * @return <code>true</code> if at least one of this drop's dependent
	 *         drops matches the specified drop predicate.
	 */
	final public boolean hasMatchingDependents(DropPredicate pred) {
		return Sea.hasMatchingDrops(pred, dependents);
	}

	/**
	 * Queries if any of this drop's deponent drops matches the given drop
	 * predicate.
	 * 
	 * @param pred
	 *            the drop predicate to use.
	 * @return <code>true</code> if at least one of this drop's deponent drops
	 *         matches the specified drop predicate.
	 */
	final public boolean hasMatchingDeponents(DropPredicate pred) {
		return Sea.hasMatchingDrops(pred, deponents);
	}

	/**
	 * Indicates if this drop has any deponent drops.
	 * 
	 * @return <code>true</code> if this drop has one or more deponent drops,
	 *         <code>false</code> otherwise.
	 */
	final public boolean hasDeponents() {
		return !deponents.isEmpty();
	}

	/**
	 * Indicates if this drop has any dependent drops.
	 * 
	 * @return <code>true</code> if this drop has one or more dependent drops,
	 *         <code>false</code> otherwise.
	 */
	final public boolean hasDependents() {
		return !dependents.isEmpty();
	}

	/**
	 * Adds the deponent drops to the set passed in
	 */
	final public void addDeponentsTo(Set<Drop> drops) {
		drops.addAll(deponents);
	}

	/**
	 * Adds the dependents to the set passed in
	 */
	final public void addDependentsTo(Set<Drop> drops) {
		drops.addAll(dependents);
	}

	/**
	 * Adds the matching deponent drops to the set passed in
	 */
	final public void addMatchingDeponentsTo(Set<Drop> drops, DropPredicate pred) {
		Sea.addMatchingDropsFrom(deponents, pred, drops);
	}

	/**
	 * Adds the matching dependents to the set passed in
	 */
	final public void addMatchingDependentsTo(Set<Drop> drops,
			DropPredicate pred) {
		Sea.addMatchingDropsFrom(dependents, pred, drops);
	}

	/**
	 * Invalidates, makes false, the information that this drop represents.
	 */
	final public void invalidate() {
		if (!valid) {
			return;
		}
		// System.out.println("Invalidating "+getMessage());

		valid = false;
		// inform deponent drops
		for (Iterator<Drop> i = deponents.iterator(); i.hasNext();) {
			Drop deponent = i.next();
			deponent.removeDependent(this);
		}
		deponents.clear(); // consistent state
		// inform dependents
		for (Iterator<Drop> i = dependents.iterator(); i.hasNext();) {
			Drop dependent = i.next();
			dependent.removeDeponent(this);
		}
		dependents.clear(); // consistent state
		mySea.notify(this, DropEvent.Invalidated);
	}

	/**
	 * Returns whether this drop is valid or not. A drop being valid indicates
	 * that the knowledge represented by the drop is currently still supported
	 * within the truth maintenance system.
	 * 
	 * @return <code>true</code> if the drop is invalid, <code>false</code>
	 *         otherwise
	 */
	final public boolean isValid() {
		return valid;
	}

	/**
	 * Invoked when a dependent drop is invalidated. This method has no effect,
	 * however, a subclass may override to change this behavior. This method's
	 * behavior is consistent with truth maintenance system use, as the truth of
	 * this drop should not depend upon the truth of any dependent drop.
	 */
	protected void dependentInvalidAction() {
		// do nothing
	}

	/**
	 * Invoked when a deponent drop is invalidated causing this drop to be
	 * invalidated as well. A subclass may override to change this behavior.
	 * This method's behavior is consistent with truth maintenance system use,
	 * because the truth of this drop should depend upon the truth of any
	 * deponent drop.
	 * 
	 * @param invalidDeponent
	 *            the deponent that became invalid.
	 */
	protected void deponentInvalidAction(Drop invalidDeponent) {
		invalidate();
	}

	/**
	 * Removes the specified drop from the list of this drop's dependent drops
	 * adding it into this drop's past dependent drops Causes
	 * {@link #dependentInvalidAction()}to be invoked.
	 * 
	 * @param dependent
	 *            the dependent drop to remove
	 */
	private void removeDependent(Drop dependent) {
		if (!valid || dependent == null) {
			return;
		}
		if (dependents.remove(dependent)) {
			mySea.notify( this, DropEvent.DependentInvalidated);
			dependentInvalidAction();
		}
	}

	/**
	 * Adds the specified drop to the list of this drop's deponent drops. This
	 * method is private because deponent drops are internally managed, i.e.,
	 * client code may only add dependent drops directly.
	 * 
	 * @param deponent
	 *            the deponent drop to add
	 */
	private void addDeponent(Drop deponent) {
		if (!valid || deponent == null) {
			return;
		}
		deponents.add(deponent);
	}

	/**
	 * Removes the specified drop from the list of this drop's deponent drops
	 * adding it into this drop's past deponent drops. Causes
	 * {@link #deponentInvalidAction(Drop)}to be invoked.
	 * 
	 * @param deponent
	 *            the deponent drop to remove
	 */
	private void removeDeponent(Drop deponent) {
		if (!valid || deponent == null) {
			return;
		}
		if (deponents.remove(deponent)) {
			mySea.notify(this, DropEvent.DeponentInvalidated);
			deponentInvalidAction(deponent);
		}
	}

	/**
	 * Notes if the drop has been invalidated by a call to {@link #invalidate()}.
	 */
	private boolean valid = true;

	/**
	 * A mutable text message about this drop, usually used by the UI.
	 */
	private String message = "(EMPTY)";

	/**
	 * The set of drops whose truth depends upon this drop.
	 * <p>
	 * <b>Dependent</b>: (definition) Contingent on another. Subordinate.
	 * Relying on or requiring the aid of another for support.
	 */
	final private Set<Drop> dependents = new HashSet<Drop>();

	/**
	 * The set of drops upon whose truth this drop depends upon.
	 * <p>
	 * <b>Deponent</b>: (definition) One who testifies under oath, especially
	 * in writing.
	 */
	final private Set<Drop> deponents = new HashSet<Drop>();

	/**
	 * A link to the {@link Sea} object this drop exists within.
	 */
	final private Sea mySea;
}