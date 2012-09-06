package edu.cmu.cs.fluid.sea;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.i18n.AnalysisResultMessage;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entities;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.promise.TextFile;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.tree.Operator;

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
public abstract class Drop implements IDropInfo {
  public static final String debug = "";// "Lock field \"this.f_lock\" is less";
  public static final String DEPONENT = "deponent";
  public static final String DEPENDENT = "dependent";
  public static final String MESSAGE = "message";
  public static final String MESSAGE_ID = "message-id";

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
   *          the sea to create the drop within.
   */
  private Drop(Sea sea) {
    mySea = sea;
    mySea.notify(this, DropEvent.Created);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object o) {
    return super.equals(o);
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
   * Gets this drop's result message
   */
  public AnalysisResultMessage getResultMessage() {
    return resultMessage;
  }

  /**
   * For now, this depends on the drop having the info to create a
   * JavaSourceReference
   */
  public void setResultMessage(int number, Object... args) {
    if (number < 1) {
      LOG.warning("Ignoring negative result number: " + number);
      return;
    }
    JavaSourceReference srcRef = createSourceRef();
    this.resultMessage = AnalysisResultMessage.getInstance(srcRef, number, args);
    this.message = resultMessage.getResultString();
  }

  protected JavaSourceReference createSourceRef() {
    throw new UnsupportedOperationException();
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
   *          the message to set for the UI about this drop.
   */
  public void setMessage(String message) {
    this.message = message;
    this.resultMessage = null;
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
   *          the message to set for the UI about this drop.
   */
  public void setMessage(String message, Object... args) {
    this.message = (args.length == 0) ? message : MessageFormat.format(message, args);
    this.resultMessage = null;
  }

  /**
   * Adds a dependent drop to this drop, meaning that the truth of the added
   * dependent drop relies upon the truth of this drop. This method makes this
   * drop a deponent for the dependent drop.
   * 
   * @param dependent
   *          the drop this drop is a deponent for.
   */
  final public void addDependent(Drop dependent) {
    if (!valid || dependent == null || !dependent.isValid()) {
      return;
    }
    if (dependent == this) {
      return;
    }
    if (dependents.add(dependent)) {
      dependent.addDeponent(this);
    }
  }

  /**
   * Adds a group of dependent drops to this drop, meaning that the truth of all
   * the added dependent drops relies upon the truth of this drop. This method
   * makes this drop a deponent for each of the dependent drops.
   * 
   * @param dependents
   *          the array of drops this drop is a deponent for.
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
   * Adds a group of dependent drops to this drop, meaning that the truth of all
   * the added dependent drops relies upon the truth of this drop. This method
   * makes this drop a deponent for each of the dependent drops.
   * 
   * @param dependents
   *          the collection of drops this drop is a deponent for.
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
   * Adds this drop as a dependent to each member of a group of deponent drops,
   * meaning that the truth of this drop relies upon the truth of each of the
   * deponent drops. This method makes this drop a dependent for each of the
   * deponent drops.
   * 
   * @param dependonts
   *          the collection of drops this drop is a dependent of.
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
   * @param p
   *          the drop predicate to use.
   * @return <code>true</code> if at least one of this drop's dependent drops
   *         matches the specified drop predicate.
   */
  final public boolean hasMatchingDependents(IDropPredicate p) {
    return Sea.hasMatchingDrops(p, dependents);
  }

  /**
   * Queries if any of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return <code>true</code> if at least one of this drop's deponent drops
   *         matches the specified drop predicate.
   */
  final public boolean hasMatchingDeponents(IDropPredicate p) {
    return Sea.hasMatchingDrops(p, deponents);
  }

  /**
   * Returns the set of this drop's dependents drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  public Set<Drop> getMatchingDependents(IDropPredicate p) {
    final Set<Drop> result = new HashSet<Drop>();
    Sea.addMatchingDropsFrom(deponents, p, result);
    return result;
  }

  /**
   * Returns the set of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  public Set<Drop> getMatchingDeponents(IDropPredicate p) {
    final Set<Drop> result = new HashSet<Drop>();
    Sea.addMatchingDropsFrom(deponents, p, result);
    return result;
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
   * Invalidates, makes false, the information that this drop represents.
   */
  final public void invalidate() {
    if (!valid) {
      return;
    }
    invalidate_internal();

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

  protected void invalidate_internal() {
    // Nothing to do right now
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
   * Utility function that helps build required drop dependencies upon
   * compilation unit drops, it causes this drop to be dependent upon the
   * compilation unit drop that the given fAST node exists within.
   * <p>
   * <b>HACK WARNING:</b> This method depends upon (in its implementation) the
   * existence of the {@link CUDrop} class. Fluid, within Eclipse, uses
   * compilation unit level change. As Fluid becomes more incremental, i.e., we
   * have a working incremental parser, this part of change dependency
   * management will have to be overhauled. I see two problems with the current
   * implementation of this method:
   * <ul>
   * <li>Drop depends upon CUDrop (a subclass), a design no-no</li>
   * <li>We are not flexible enough to allow dependencies as fine-grained as the
   * actual program analyses can support (e.g., constructor, method, etc.)</li>
   * </ul>
   * 
   * @param node
   *          the fAST node specifying the compilation unit this drop needs to
   *          depend upon. If this is <code>null</code> no dependency is added.
   */
  final public void dependUponCompilationUnitOf(IRNode node) {
    if (node == null)
      return;
    try {
      Operator op = JJNode.tree.getOperator(node);
      IRNode cu;
      if (CompilationUnit.prototype.includes(op)) {
        cu = node;
      } else if (TextFile.prototype.includes(op)) {
        // Not from a compilation unit
        return;
      } else {
        cu = VisitUtil.getEnclosingCompilationUnit(node);
      }
      if (cu == null) {
        LOG.log(Level.SEVERE, "unable to find enclosing compilation unit for " + DebugUnparser.toString(node));
      } else {
        CUDrop cuDrop = CUDrop.queryCU(cu);
        if (cuDrop == null) {
          IRNode type = VisitUtil.getEnclosingType(node);
          if (!PromiseConstants.ARRAY_CLASS_NAME.equals(JJNode.getInfo(type))) {
            LOG.log(Level.WARNING, "unable to find compilation unit drop for " + DebugUnparser.toString(node));
          }
        } else {
          // the promise depends upon the compilation unit it is
          // within
          cuDrop.addDependent(this);
        }
      }
    } catch (Throwable e) {
      LOG.log(Level.WARNING, "unable to find compilation unit drop for " + DebugUnparser.toString(node));
    }
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
   * invalidated as well. A subclass may override to change this behavior. This
   * method's behavior is consistent with truth maintenance system use, because
   * the truth of this drop should depend upon the truth of any deponent drop.
   * 
   * @param invalidDeponent
   *          the deponent that became invalid.
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
   *          the dependent drop to remove
   */
  private void removeDependent(Drop dependent) {
    if (!valid || dependent == null) {
      return;
    }
    if (dependent == this) {
      return;
    }
    if (dependents.remove(dependent)) {
      mySea.notify(this, DropEvent.DependentInvalidated);
      dependentInvalidAction();
    }
  }

  /**
   * Adds the specified drop to the list of this drop's deponent drops. This
   * method is private because deponent drops are internally managed, i.e.,
   * client code may only add dependent drops directly.
   * 
   * @param deponent
   *          the deponent drop to add
   */
  private void addDeponent(Drop deponent) {
    if (!valid || deponent == null) {
      return;
    }
    if (deponent == this) {
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
   *          the deponent drop to remove
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
  private String message = this.getClass().getSimpleName() + " (EMPTY)";

  /**
   * A mutable result message about this drop, usually used by the UI.
   */
  private AnalysisResultMessage resultMessage;

  /**
   * The set of drops whose truth depends upon this drop.
   * <p>
   * <b>Dependent</b>: (definition) Contingent on another. Subordinate. Relying
   * on or requiring the aid of another for support.
   */
  final private Set<Drop> dependents = new HashSet<Drop>();

  /**
   * The set of drops upon whose truth this drop depends upon.
   * <p>
   * <b>Deponent</b>: (definition) One who testifies under oath, especially in
   * writing.
   */
  final private Set<Drop> deponents = new HashSet<Drop>();

  /**
   * A link to the {@link Sea} object this drop exists within.
   */
  final private Sea mySea;

  public String getEntityName() {
    return "drop";
  }

  public void preprocessRefs(SeaSnapshot s) {
    for (Drop deponent : getDeponents()) {
      s.snapshotDrop(deponent);
    }
  }

  public void snapshotAttrs(XMLCreator.Builder s) {
    s.addAttribute(MESSAGE, Entities.escapeControlChars(getMessage()));
    if (resultMessage != null) {
      s.addAttribute(MESSAGE_ID, Entities.escapeControlChars(resultMessage.getResultStringCanonical()));
    }
  }

  public void snapshotRefs(SeaSnapshot s, Builder db) {
    /*
     * for (Drop dependent : getDependents()) { s.refDrop(DEPENDENT, dependent);
     * }
     */
    for (Drop deponent : getDeponents()) {
      s.refDrop(db, DEPONENT, deponent);
    }
  }

  /****************************************************************/
  @SuppressWarnings("unchecked")
  public <T> T getAdapter(Class<T> type) {
    if (type.isInstance(this)) {
      return (T) this;
    }
    throw new UnsupportedOperationException();
  }

  public String getType() {
    return getClass().getName();
  }

  public boolean isInstance(Class<?> type) {
    return type.isInstance(this);
  }

  public boolean requestTopLevel() {
    throw new UnsupportedOperationException();
  }

  public int count() {
    throw new UnsupportedOperationException();
  }

  public Category getCategory() {
    throw new UnsupportedOperationException();
  }

  public void setCategory(Category c) {
    throw new UnsupportedOperationException();
  }

  public ISrcRef getSrcRef() {
    return null;
  }

  public Collection<? extends IProposedPromiseDropInfo> getProposals() {
    throw new UnsupportedOperationException();
  }

  public Collection<ISupportingInformation> getSupportingInformation() {
    throw new UnsupportedOperationException();
  }

  public Long getTreeHash() {
    throw new UnsupportedOperationException();
  }

  public Long getContextHash() {
    throw new UnsupportedOperationException();
  }

  public String getAttribute(String key) {
    return null;
  }
}