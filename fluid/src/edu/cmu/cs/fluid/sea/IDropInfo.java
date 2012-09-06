package edu.cmu.cs.fluid.sea;

import java.util.Collection;
import java.util.Set;

import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * The interface for the base class for all drops within the sea, intended to
 * allow multiple implementations. The analysis uses the IR drop-sea and the
 * Eclipse client loads snapshots using a IR-free drop-sea.
 */
public interface IDropInfo {

  /**
   * Gets the requested XML attribute or {@code null}. Only used for persisting
   * drops.
   * 
   * @param key
   *          the attribute key.
   * @return the requested XML attribute or {@code null}
   */
  String getAttribute(String key);

  /**
   * Gets the XML element name for this drop.
   * 
   * @return the XML element name for this drop.
   */
  String getXMLElementName();

  /**
   * Gets the IR drop-sea type name, descended from {@link Drop}, even if this
   * is the IR-free drop-sea used for saving and restoring.
   * 
   * @return the IR drop-sea type name, descended from {@link Drop}
   */
  String getTypeName();

  /**
   * Checks if this drop, in the IR drop-sea, is an instance of the passed
   * class. Typically this replaces code like: <i>receiver<i>
   * <tt>instanceof</tt> <i>type</i>.
   * <p>
   * This allows type checks in the IR-free drop-sea used for saving and
   * restoring. In particular this call should be used in {@link DropPredicate}
   * instances.
   * 
   * @param a
   *          type in the IR drop-sea, descended from {@link Drop}.
   * @return {@code true} if this drop, in the IR drop-sea, is an instance of
   *         the passed type, {@code false} otherwise.
   */
  boolean instanceOf(Class<?> type);

  /**
   * Gets this drop's message.
   * 
   * @return the message set for this drop, usually used by the UI.
   */
  String getMessage();

  /**
   * Returns whether this drop is valid or not. A drop being valid indicates
   * that the knowledge represented by the drop is currently still supported
   * within the truth maintenance system.
   * 
   * @return <code>true</code> if the drop is invalid, <code>false</code>
   *         otherwise
   */
  boolean isValid();

  /**
   * Queries if any of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return <code>true</code> if at least one of this drop's deponent drops
   *         matches the specified drop predicate.
   */
  boolean hasMatchingDeponents(DropPredicate p);

  /**
   * Returns the set of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  Set<? extends IDropInfo> getMatchingDeponents(DropPredicate p);

  /**
   * Queries if any of this drop's dependent drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return <code>true</code> if at least one of this drop's dependent drops
   *         matches the specified drop predicate.
   */
  boolean hasMatchingDependents(DropPredicate p);

  /**
   * Returns the set of this drop's dependents drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  Set<? extends IDropInfo> getMatchingDependents(DropPredicate p);

  /**
   * Indicates if this drop wants to be displayed at the top level in the user
   * interface.
   * 
   * @return {@code true} if this drop wants to be displayed at the top level,
   *         {@code false} otherwise.
   */
  boolean requestTopLevel();

  /**
   * Gets the source reference of this drop.
   * 
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  ISrcRef getSrcRef();

  /**
   * Gets the user interface reporting category for this drop.
   * 
   * @return a category, or {@code null} if none is set.
   */
  Category getCategory();

  /**
   * Sets the user interface reporting category for this drop.
   * 
   * @param category
   *          a category to set, or {@code null} to clear the category.
   */
  void setCategory(Category c);

  /**
   * Gets the supporting information about this drop.
   * 
   * @return the set (possibly empty) of supporting information about this drop.
   */
  Collection<ISupportingInformation> getSupportingInformation();

  /**
   * Gets the set of proposed promises for this drop.
   * 
   * @return the, possibly empty but non-null, set of proposed promises for this
   *         drop.
   */
  Collection<? extends IProposedPromiseDropInfo> getProposals();

  /**
   * Places the needed attributes for persistence of this drop on the passed XML
   * output builder.
   * <p>
   * This is used to persist the IR drop-sea so that it can be loaded into the
   * IR-free drop-sea.
   * 
   * @param s
   *          an XML builder.
   */
  void snapshotAttrs(XMLCreator.Builder s);

  /**
   * Computes a hash of the subtree from the fAST node that this drop is related
   * to the fAST branches.
   * 
   * @return a hash.
   */
  Long getTreeHash();

  /**
   * Computes a hash of the location in the fAST node that this drop is related
   * to.
   * 
   * @return a hash.
   */
  Long getContextHash();
}
