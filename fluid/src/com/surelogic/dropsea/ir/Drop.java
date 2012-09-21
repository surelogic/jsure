package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_ABOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic.common.i18n.AnalysisResultMessage;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entities;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;

import edu.cmu.cs.fluid.java.ISrcRef;

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

  @NonNull
  public Class<?> getIRDropSeaClass() {
    return getClass();
  }

  public boolean instanceOfIRDropSea(Class<?> type) {
    return type.isInstance(this);
  }

  /**
   * Gets the sea that this drop is part of.
   * 
   * @return the sea this drop exists within.
   */
  @NonNull
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
   * This method sets the message for this drop. Calling this method is similar
   * to calling
   * 
   * <pre>
   * String.format(<i>formatString</i>, args)
   * </pre>
   * 
   * where <i>formatString</i> is obtained from the
   * <tt>SureLogicResults.properties</tt> file in the
   * <tt>com.surelogic.common.i18n</tt> package.
   * <p>
   * The <tt>number</tt> for the result message in the
   * <tt>SureLogicResults.properties</tt> file is <i>result.nnnnn</i>. For
   * example, if <tt>number == 2001</tt> would result in the string
   * <tt>"A singular problem."</tt> if the definition
   * 
   * <pre>
   * result.02001=A singular problem.
   * </pre>
   * 
   * is contained in the SureLogicResults.properties file. If the key is not
   * defined in the SureLogicResults.properties file an exception is thrown.
   * <p>
   * <i>Implementation Note:</i> The {@code getInstance} methods of
   * {@link AnalysisResultMessage} are used to an {@link AnalysisResultMessage}
   * which is stored internally to this drop.
   * 
   * @param number
   *          the number of the result in the
   *          <tt>SureLogicResults.properties</tt> file.
   * @param args
   *          arguments to <tt>String.format</tt>.
   * 
   * @see AnalysisResultMessage
   * @see I18N
   */
  public final void setMessage(int number, Object... args) {
    if (number < 1) {
      LOG.warning(I18N.err(247, number));
      return;
    }
    synchronized (f_seaLock) {
      JavaSourceReference srcRef = createSourceRef(); // may be null
      f_message = AnalysisResultMessage.getInstance(srcRef, number, args);
    }
  }

  public final void setMessage(String msg) {
    synchronized (f_seaLock) {
      JavaSourceReference srcRef = createSourceRef(); // may be null
      f_message = AnalysisResultMessage.getInstance(srcRef, 12, msg);
    }
  }

  /**
   * Used by {@link ProofDrop} to set the message based upon the verification
   * judgment.
   * 
   * @param value
   *          a message.
   */
  void setMessage(AnalysisResultMessage value) {
    if (value != null)
      f_message = value;
  }

  @Nullable
  public final Category getCategory() {
    synchronized (f_seaLock) {
      return f_category;
    }
  }

  /**
   * Sets the user interface reporting category for this drop.
   * 
   * @param category
   *          a category to set, or {@code null} to clear the category.
   */
  public final void setCategory(Category category) {
    synchronized (f_seaLock) {
      f_category = category;
    }
  }

  @RequiresLock("SeaLock")
  protected JavaSourceReference createSourceRef() {
    return JavaSourceReference.UNKNOWN;
  }

  @NonNull
  public final String getMessage() {
    synchronized (f_seaLock) {
      if (f_message == null)
        return getClass().getSimpleName() + " (EMPTY)";
      else
        return f_message.getResultString();
    }
  }

  @NonNull
  public String getMessageCanonical() {
    synchronized (f_seaLock) {
      if (f_message == null)
        return getClass().getSimpleName() + " (EMPTY)";
      else
        return f_message.getResultStringCanonical();
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
   * Gets the number of immediate dependent drops of this drop.
   * 
   * @return the number of immediate dependent drops of this drop.
   */
  public final int getDependentCount() {
    synchronized (f_seaLock) {
      return f_dependents.size();
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
   * Gets the number of immediate deponent drops of this drop.
   * 
   * @return the number of immediate deponent drops of this drop.
   */
  public final int getDeponentCount() {
    synchronized (f_seaLock) {
      return f_deponents.size();
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
   * Queries if any of this drop's dependent drops matches a drop predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return {@code true} if at least one of this drop's dependent drops matches
   *         the specified drop predicate, {@code false} otherwise.
   */
  public final boolean hasMatchingDependents(DropPredicate pred) {
    synchronized (f_seaLock) {
      return Sea.hasMatchingDrops(pred, f_dependents);
    }
  }

  /**
   * Queries if any of this drop's deponent drops matches a drop predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return {@code true} if at least one of this drop's deponent drops matches
   *         the specified drop predicate, {@code false} otherwise.
   */
  public final boolean hasMatchingDeponents(DropPredicate pred) {
    synchronized (f_seaLock) {
      return Sea.hasMatchingDrops(pred, f_deponents);
    }
  }

  /**
   * Returns the set of this drop's dependents drops matches a drop predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  @NonNull
  public final ArrayList<Drop> getMatchingDependents(DropPredicate pred) {
    final ArrayList<Drop> result;
    synchronized (f_seaLock) {
      result = Sea.filterDropsMatching(pred, f_dependents);
    }
    return result;
  }

  /**
   * Returns the set of this drop's deponent drops matches a drop predicate.
   * 
   * @param p
   *          the drop predicate to use.
   * @return a set of drops. This may be empty but will never be {@code null}.
   */
  @NonNull
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
   * @return {@code true} if the drop is invalid, {@code false} otherwise.
   */
  public final boolean isValid() {
    synchronized (f_seaLock) {
      return f_valid;
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

  @NonNull
  public final Set<IAnalysisHintDrop> getAnalysisHintsAbout() {
    final Set<IAnalysisHintDrop> result = new HashSet<IAnalysisHintDrop>();
    synchronized (f_seaLock) {
      for (Drop d : getDependentsReference()) {
        if (d instanceof IAnalysisHintDrop)
          result.add((IAnalysisHintDrop) d);
      }
    }
    return result;
  }

  /**
   * Notes if the drop has been invalidated by a call to {@link #invalidate()}.
   */
  @InRegion("DropState")
  private boolean f_valid = true;

  /**
   * An analysis result message about this drop, usually used by the UI.
   * <p>
   * May be {@code null}.
   */
  @InRegion("DropState")
  @Nullable
  private AnalysisResultMessage f_message;

  /**
   * A user interface reporting category for this drop.
   * 
   * @see Category
   */
  @InRegion("DropState")
  @Nullable
  private Category f_category = null;

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
  @NonNull
  final private Sea f_mySea;

  /**
   * An alias to the object returned by {@link Sea#getSeaLock()}.
   */
  @NonNull
  final protected Object f_seaLock;

  /*
   * XML output methods are invoked single-threaded
   */

  public String getXMLElementName() {
    return DROP;
  }

  @MustInvokeOnOverride
  public void snapshotAttrs(XMLCreator.Builder s) {
    s.addAttribute(MESSAGE, Entities.escapeControlChars(getMessage()));
    if (f_message != null)
      s.addAttribute(MESSAGE_ID, Entities.escapeControlChars(f_message.getResultStringCanonical()));

    final Category cat = getCategory();
    if (cat != null)
      s.addAttribute(CATEGORY_ATTR, cat.getKey());
  }

  @MustInvokeOnOverride
  public void preprocessRefs(SeaSnapshot s) {
    for (IAnalysisHintDrop c : getAnalysisHintsAbout()) {
      if (c instanceof Drop)
        s.snapshotDrop((Drop) c);
    }
  }

  @MustInvokeOnOverride
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    for (IAnalysisHintDrop c : getAnalysisHintsAbout()) {
      if (c instanceof Drop)
        s.refDrop(db, HINT_ABOUT, (Drop) c);
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

  public ISrcRef getSrcRef() {
    return null;
  }

  public Collection<? extends IProposedPromiseDrop> getProposals() {
    return Collections.emptyList();
  }

  public Long getTreeHash() {
    throw new UnsupportedOperationException();
  }

  public Long getContextHash() {
    throw new UnsupportedOperationException();
  }
}