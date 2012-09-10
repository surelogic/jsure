package edu.cmu.cs.fluid.sea;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
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
@Region("DropState")
@RegionLock("SeaLock is f_seaLock protects DropState")
public abstract class Drop implements IDrop {

  /**
   * Checks if message from drop starts with a string and outputs debug
   * information on it. If this is set to {@code null} debug information on all
   * drops is output.
   */
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
    if (sea == null)
      sea = Sea.getDefault();

    f_mySea = sea;
    f_seaLock = sea.getSeaLock();
    f_mySea.notify(this, DropEvent.Created);
  }

  /**
   * Gets the sea that this drop is part of.
   * 
   * @return the sea this drop exists within.
   */
  public final Sea getSea() {
    return f_mySea;
  }

  /**
   * Gets the lock for this sea that this drop is part of.
   * 
   * @return the non-null lock for this sea that this drop is part of.
   */
  public final Object getSeaLock() {
    return f_seaLock;
  }

  /**
   * For now, this depends on the drop having the info to create a
   * JavaSourceReference
   */
  public final void setResultMessage(int number, Object... args) {
    if (number < 1) {
      LOG.warning("Ignoring negative result number: " + number);
      return;
    }
    synchronized (f_seaLock) {
      JavaSourceReference srcRef = createSourceRef();
      if (srcRef == null)
        srcRef = new JavaSourceReference();
      this.f_resultMessage = AnalysisResultMessage.getInstance(srcRef, number, args);
      this.f_message = f_resultMessage.getResultString();
    }
  }

  @RequiresLock("SeaLock")
  protected JavaSourceReference createSourceRef() {
    return new JavaSourceReference();
  }

  /**
   * Gets this drop's message.
   * 
   * @return the message set for this drop, usually used by the UI.
   */
  public final String getMessage() {
    synchronized (f_seaLock) {
      return f_message;
    }
  }

  /**
   * Sets this drop's message.
   * 
   * @param message
   *          the message to set for the UI about this drop.
   */
  public final void setMessage(String message) {
    synchronized (f_seaLock) {
      this.f_message = message;
      this.f_resultMessage = null;
    }
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
  public final void setMessage(String message, Object... args) {
    synchronized (f_seaLock) {
      this.f_message = (args.length == 0) ? message : MessageFormat.format(message, args);
      this.f_resultMessage = null;
    }
  }

  /**
   * Adds a dependent drop to this drop, meaning that the truth of the added
   * dependent drop relies upon the truth of this drop. This method makes this
   * drop a deponent for the dependent drop.
   * 
   * @param dependent
   *          the drop this drop is a deponent for.
   */
  public final void addDependent(Drop dependent) {
    synchronized (f_seaLock) {
      if (!f_valid || dependent == null || !dependent.isValid()) {
        return;
      }
      if (dependent == this) {
        return;
      }
      if (f_dependents.add(dependent)) {
        dependent.addDeponent(this);
      }
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
  public final void addDependents(Drop[] dependents) {
    synchronized (f_seaLock) {
      if (!f_valid || dependents == null) {
        return;
      }
      for (int i = 0; i < dependents.length; i++) {
        addDependent(dependents[i]);
      }
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
  public final void addDependents(Collection<? extends Drop> dependents) {
    synchronized (f_seaLock) {
      if (!f_valid || dependents == null) {
        return;
      }
      for (Drop drop : dependents) {
        addDependent(drop);
      }
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
  public final void addDeponents(Collection<? extends Drop> deponents) {
    synchronized (f_seaLock) {
      if (!f_valid || f_dependents == null) {
        return;
      }
      for (Drop drop : deponents) {
        drop.addDependent(this);
      }
    }
  }

  /**
   * Returns all immediate dependent drops of this drop.
   * 
   * @return the set of dependent drops
   */
  public final HashSet<Drop> getDependents() {
    synchronized (f_seaLock) {
      return new HashSet<Drop>(f_dependents);
    }
  }

  /**
   * Returns the internal set that tracks dependent drops of this drop.
   * <p>
   * Callers <b>must not</b> mutate this set.
   */
  @RequiresLock("SeaLock")
  @Vouch("controlled alias of f_dependents for performance")
  protected final Set<Drop> getDependentsReference() {
    return f_dependents;
  }

  /**
   * Returns all immediate deponent drops of this drop.
   * 
   * @return the set of deponent drops
   */
  public final HashSet<Drop> getDeponents() {
    synchronized (f_seaLock) {
      return new HashSet<Drop>(f_deponents);
    }
  }

  /**
   * Returns the internal set that tracks deponent drops of this drop.
   * <p>
   * Callers <b>must not</b> mutate this set.
   */
  @RequiresLock("SeaLock")
  @Vouch("controlled alias of f_dependents for performance")
  protected final Set<Drop> getDeponentsReference() {
    return f_deponents;
  }

  /**
   * Queries if any of this drop's dependent drops matches the given drop
   * predicate.
   * 
   * @param pred
   *          a drop predicate.
   * @return <code>true</code> if at least one of this drop's dependent drops
   *         matches the specified drop predicate.
   */
  public final boolean hasMatchingDependents(DropPredicate pred) {
    synchronized (f_seaLock) {
      return Sea.hasMatchingDrops(pred, f_dependents);
    }
  }

  /**
   * Queries if any of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param pred
   *          a drop predicate.
   * @return <code>true</code> if at least one of this drop's deponent drops
   *         matches the specified drop predicate.
   */
  public final boolean hasMatchingDeponents(DropPredicate pred) {
    synchronized (f_seaLock) {
      return Sea.hasMatchingDrops(pred, f_deponents);
    }
  }

  /**
   * Returns a new list containing of this drop's dependents drops that match a
   * drop predicate.
   * 
   * @param pred
   *          a drop predicate.
   * @return a list of drops. This may be empty but will never be {@code null}.
   */
  public final ArrayList<Drop> getMatchingDependents(DropPredicate pred) {
    final ArrayList<Drop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsMatching(pred, f_dependents);
    }
    return result;
  }

  /**
   * Returns the set of this drop's deponent drops matches the given drop
   * predicate.
   * 
   * @param pred
   *          a drop predicate.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  public final ArrayList<Drop> getMatchingDeponents(DropPredicate pred) {
    final ArrayList<Drop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsMatching(pred, f_deponents);
    }
    return result;
  }

  /**
   * Indicates if this drop has any deponent drops.
   * 
   * @return <code>true</code> if this drop has one or more deponent drops,
   *         <code>false</code> otherwise.
   */
  public final boolean hasDeponents() {
    synchronized (f_seaLock) {
      return !f_deponents.isEmpty();
    }
  }

  /**
   * Indicates if this drop has any dependent drops.
   * 
   * @return <code>true</code> if this drop has one or more dependent drops,
   *         <code>false</code> otherwise.
   */
  public final boolean hasDependents() {
    synchronized (f_seaLock) {
      return !f_dependents.isEmpty();
    }
  }

  /**
   * Invalidates, makes false, the information that this drop represents.
   */
  public final void invalidate() {
    synchronized (f_seaLock) {
      if (!f_valid) {
        return;
      }
      invalidate_internal();

      f_valid = false;
      // inform deponent drops
      for (Iterator<Drop> i = f_deponents.iterator(); i.hasNext();) {
        Drop deponent = i.next();
        deponent.removeDependent(this);
      }
      f_deponents.clear(); // consistent state
      // inform dependents
      for (Iterator<Drop> i = f_dependents.iterator(); i.hasNext();) {
        Drop dependent = i.next();
        dependent.removeDeponent(this);
      }
      f_dependents.clear(); // consistent state
      f_mySea.notify(this, DropEvent.Invalidated);
    }
  }

  @RequiresLock("SeaLock")
  protected void invalidate_internal() {
    // by default do nothing
  }

  /**
   * Returns whether this drop is valid or not. A drop being valid indicates
   * that the knowledge represented by the drop is currently still supported
   * within the truth maintenance system.
   * 
   * @return <code>true</code> if the drop is invalid, <code>false</code>
   *         otherwise
   */
  public final boolean isValid() {
    synchronized (f_seaLock) {
      return f_valid;
    }
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
  public final void dependUponCompilationUnitOf(IRNode node) {
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
          /*
           * the promise depends upon the compilation unit it is within
           */
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
  @RequiresLock("SeaLock")
  protected void dependentInvalidAction() {
    // by default do nothing
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
  @RequiresLock("SeaLock")
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
  @RequiresLock("SeaLock")
  private void removeDependent(Drop dependent) {
    if (!f_valid || dependent == null) {
      return;
    }
    if (dependent == this) {
      return;
    }
    if (f_dependents.remove(dependent)) {
      f_mySea.notify(this, DropEvent.DependentInvalidated);
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
  @RequiresLock("SeaLock")
  private void addDeponent(Drop deponent) {
    if (!f_valid || deponent == null) {
      return;
    }
    if (deponent == this) {
      return;
    }
    f_deponents.add(deponent);
  }

  /**
   * Removes the specified drop from the list of this drop's deponent drops
   * adding it into this drop's past deponent drops. Causes
   * {@link #deponentInvalidAction(Drop)}to be invoked.
   * 
   * @param deponent
   *          the deponent drop to remove
   */
  @RequiresLock("SeaLock")
  private void removeDeponent(Drop deponent) {
    if (!f_valid || deponent == null) {
      return;
    }
    if (f_deponents.remove(deponent)) {
      f_mySea.notify(this, DropEvent.DeponentInvalidated);
      deponentInvalidAction(deponent);
    }
  }

  /**
   * Notes if the drop has been invalidated by a call to {@link #invalidate()}.
   */
  @InRegion("DropState")
  private boolean f_valid = true;

  /**
   * A mutable text message about this drop, usually used by the UI.
   */
  @InRegion("DropState")
  private String f_message = this.getClass().getSimpleName() + " (EMPTY)";

  /**
   * A mutable result message about this drop, usually used by the UI.
   */
  @InRegion("DropState")
  private AnalysisResultMessage f_resultMessage;

  /**
   * The set of drops whose truth depends upon this drop.
   * <p>
   * <b>Dependent</b>: (definition) Contingent on another. Subordinate. Relying
   * on or requiring the aid of another for support.
   */
  @UniqueInRegion("DropState")
  final private Set<Drop> f_dependents = new HashSet<Drop>();

  /**
   * The set of drops upon whose truth this drop depends upon.
   * <p>
   * <b>Deponent</b>: (definition) One who testifies under oath, especially in
   * writing.
   */
  @UniqueInRegion("DropState")
  final private Set<Drop> f_deponents = new HashSet<Drop>();

  /**
   * A link to the {@link Sea} object this drop exists within.
   */
  final private Sea f_mySea;

  /**
   * An alias to the object returned by {@link Sea#getSeaLock()}.
   */
  final protected Object f_seaLock;

  /*
   * XML Methods are invoked single-threaded
   */

  @RequiresLock("SeaLock")
  public String getXMLElementName() {
    return "drop";
  }

  @RequiresLock("SeaLock")
  public void preprocessRefs(SeaSnapshot s) {
    for (Drop deponent : getDeponentsReference()) {
      s.snapshotDrop(deponent);
    }
  }

  @RequiresLock("SeaLock")
  public void snapshotAttrs(XMLCreator.Builder s) {
    s.addAttribute(MESSAGE, Entities.escapeControlChars(getMessage()));
    if (f_resultMessage != null) {
      s.addAttribute(MESSAGE_ID, Entities.escapeControlChars(f_resultMessage.getResultStringCanonical()));
    }
  }

  @RequiresLock("SeaLock")
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    for (Drop deponent : getDeponentsReference()) {
      s.refDrop(db, DEPONENT, deponent);
    }
  }

  /****************************************************************/

  @SuppressWarnings("unchecked")
  public <T> T getAdapter(Class<T> type) {
    if (type.isInstance(this)) {
      return (T) this;
    } else
      throw new UnsupportedOperationException();
  }

  public String getTypeName() {
    return getClass().getName();
  }

  public boolean instanceOf(Class<?> type) {
    return type.isInstance(this);
  }

  public boolean requestTopLevel() {
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

  public Collection<? extends IProposedPromiseDrop> getProposals() {
    return Collections.emptyList();
  }

  public Collection<ISupportingInformation> getSupportingInformation() {
    return Collections.emptyList();
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