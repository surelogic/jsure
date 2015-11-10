package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CATEGORY_ATTR;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DIFF_INFO;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FROM_SRC;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.HINT_ABOUT;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.MESSAGE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.MESSAGE_ID;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROPOSED_PROMISE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.Vouch;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.common.ref.IJavaRef.Position;
import com.surelogic.common.xml.*;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IHintDrop.HintType;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.irfree.DiffHeuristics;
import com.surelogic.dropsea.ir.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.util.TypeUtil;

/**
 * The abstract base class for all drops within the sea, intended to be
 * subclassed and extended. This class forms the basis for a truth maintenance
 * system by managing dependent and deponent drops. Each instance represents a
 * <i>drop</i> of information within a <i>sea</i> of knowledge.
 * <p>
 * Dependent drops are specified by clients though the public interface and
 * deponent drops are automatically tracked.
 * <p>
 * This implementation always tracks a reference to an {@link IRNode}.
 * 
 * @see Sea
 */
@Region("protected DropState")
@RegionLock("SeaLock is f_seaLock protects DropState")
public abstract class Drop implements IDrop {

  /**
   * Constructs a drop referencing the passed node.
   * 
   * @param node
   *          a fAST node related to this drop.
   * 
   * @throws IllegalArgumentException
   *           if the passed node is {@code null}.
   */
  protected Drop(@NonNull final IRNode node) {
    this(Sea.getDefault(), node);
  }

  /**
   * Constructs a drop within the specified sea.
   * <p>
   * This constructor is rarely used, see {@link #Drop(IRNode)}.
   * 
   * @param sea
   *          the {@link Sea} instance to create the drop within.
   * @param node
   *          a fAST node related to this drop.
   */
  protected Drop(@Nullable Sea sea, @NonNull final IRNode node) {
    if (sea == null)
      sea = Sea.getDefault();
    if (node == null)
      throw new IllegalArgumentException(I18N.err(44, "node"));
    f_node = node;

    f_mySea = sea;
    f_seaLock = sea.getSeaLock();
    f_mySea.notify(this, DropEvent.Created);
  }

  public boolean includeInDiff() {
	return true;
  }
  
  //@Override
  @NonNull
  public Class<?> getIRDropSeaClass() {
    return getClass();
  }

  //@Override
  public boolean instanceOfIRDropSea(Class<?> type) {
    return type.isInstance(this);
  }

  @NonNull
  public final String getSimpleClassName() {
	  return getClass().getSimpleName();
  }
  
  @NonNull
  public final String getFullClassName() {
	  return getClass().getName();
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
   * @return the lock for this sea that this drop is part of.
   */
  @NonNull
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
   * is contained in the <tt>SureLogicResults.properties</tt> file. If the key
   * is not defined in the file an exception is thrown.
   * <p>
   * If the message is for a {@link IResultFolderDrop} special processing is
   * done, post format, but prior to display in the UI as described in
   * {@link I18N#toStringForUIFolderLabel(String, int)}.
   * 
   * @param number
   *          the number of the result in the
   *          <tt>SureLogicResults.properties</tt> file.
   * @param args
   *          arguments to <tt>String.format</tt>.
   * 
   * @see I18N#res(int)
   * @see I18N#res(int, Object...)
   * @see I18N#toStringForUIFolderLabel(String, int)
   */
  public final void setMessage(int number, Object... args) {
    synchronized (f_seaLock) {
      setMessageHelper(number, args);
    }
  }

  /**
   * Meant to be called from the drop constructor
   */
  protected final void setMessageHelper(int number, Object... args) {
    if (number < 1) {
      SLLogger.getLogger().warning(I18N.err(257, number));
      return;
    }
    // f_message = args.length == 0 ? I18N.res(number) : I18N.res(number, args);
    // f_messageCanonical = args.length == 0 ? I18N.resc(number) :
    // I18N.resc(number, args);
    f_message = args.length == 0 ? resolveMessage(number) : resolveMessage(number, args);
    f_messageCanonical = args.length == 0 ? resolveMessageCanonical(number) : resolveMessageCanonical(number, args);
  }

  protected String resolveMessage(final int number) {
    return I18N.res(number);
  }

  protected String resolveMessage(final int number, final Object... args) {
    return I18N.res(number, args);
  }

  protected String resolveMessageCanonical(final int number) {
    return I18N.resc(number);
  }

  protected String resolveMessageCanonical(final int number, final Object... args) {
    return I18N.resc(number, args);
  }

  /**
   * This method sets the message for this drop.
   * <p>
   * Whenever possible {@link #setMessage(int, Object...)} should be used
   * instead of this method, because it handles small changes to a message when
   * comparisons are done by the regression test suite.
   * 
   * @param value
   *          a message.
   */
  public final void setMessage(String value) {
    synchronized (f_seaLock) {
      f_message = value;
      f_messageCanonical = null;
    }
  }

  /**
   * Used by {@link ProofDrop} to set the message based upon the verification
   * judgment.
   * 
   * @param message
   *          a message describing this drop, usually used by the UI.
   * @param messageCanonical
   *          a canonical version of the message describing this drop, usually
   *          used by the regression test suite for comparisons.
   */
  @RequiresLock("SeaLock")
  protected final void setMessageHelper(String message, String messageCanonical) {
    f_message = message;
    f_messageCanonical = messageCanonical;
  }

  @Override
  @NonNull
  public final String getMessage() {
    synchronized (f_seaLock) {
      if (f_message == null)
        return "(EMPTY)";
      else
        return f_message;
    }
  }

  @Override
  @Nullable
  public String getMessageCanonical() {
    synchronized (f_seaLock) {
      return f_messageCanonical;
    }
  }

  /**
   * This method sets the categorizing message for this drop. These strings are
   * only used by the UI to improve how results are presented to the user.
   * Calling this method is similar to calling
   * 
   * <pre>
   * String.format(<i>formatString</i>, args)
   * </pre>
   * 
   * where <i>formatString</i> is obtained from the
   * <tt>SureLogicResults.properties</tt> file in the
   * <tt>com.surelogic.common.i18n</tt> package.
   * <p>
   * The <tt>number</tt> for the categorizing message in the
   * <tt>SureLogicResults.properties</tt> file is <i>category.nnnnn</i>. For
   * example, if <tt>number == 2001</tt> would result in the string
   * <tt>"### java.lang.Runnable subtype instance {{{created|||creations}}} - not{{{ a|||}}} Thread{{{|||s}}}"</tt>
   * if the definition
   * 
   * <pre>
   * category.02001=### java.lang.Runnable subtype instance {{{created|||creations}}} - not{{{ a|||}}} Thread{{{|||s}}}
   * </pre>
   * 
   * is contained in the <tt>SureLogicResults.properties</tt> file. If the key
   * is not defined in the file an exception is thrown.
   * <p>
   * Special processing is applied to all categorizing messages prior to their
   * display in the UI as described in
   * {@link I18N#toStringForUIFolderLabel(String, int)}. This special processing
   * allows the string to react to the number of items, <i>c</i>, contained in
   * the category. In the case of category 2001 (shown above) the UI would
   * display
   * 
   * <ul>
   * <li><tt>"1 java.lang.Runnable subtype instance created - not a Thread"</tt>
   * when <i>c</i> = 1, and</li>
   * <li>
   * <tt>"2 java.lang.Runnable subtype instance creations - not Threads"</tt>
   * when <i>c</i> = 2 (or more).</li>
   * </ul>
   * 
   * @param number
   *          the number of the result in the
   *          <tt>SureLogicResults.properties</tt> file.
   * @param args
   *          arguments to <tt>String.format</tt>.
   * 
   * @see I18N#cat(int)
   * @see I18N#cat(int, Object...)
   * @see I18N#toStringForUIFolderLabel(String, int)
   */
  public final void setCategorizingMessage(int number, Object... args) {
    if (number < 1) {
      SLLogger.getLogger().warning(I18N.err(261, number));
      return;
    }
    synchronized (f_seaLock) {
      f_categorizingMessage = args.length == 0 ? I18N.cat(number) : I18N.cat(number, args);
    }
  }

  /**
   * This method sets the categorizing message for this drop. These strings are
   * only used by the UI to improve how results are presented to the user.
   * <p>
   * Whenever possible {@link #setCategorizingMessage(int, Object...)} should be
   * used instead of this method.
   * 
   * @param value
   *          a categorizing string.
   */
  public final void setCategorizingMessage(String value) {
    synchronized (f_seaLock) {
      f_categorizingMessage = value;
    }
  }

  @Override
  @Nullable
  public String getCategorizingMessage() {
    synchronized (f_seaLock) {
      return f_categorizingMessage;
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
      return new HashSet<>(f_dependents);
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
      return new HashSet<>(f_deponents);
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

  @Override
  @NonNull
  public final Set<HintDrop> getHints() {
    final Set<HintDrop> result = new HashSet<>();
    synchronized (f_seaLock) {
      for (Drop d : getDependentsReference()) {
        if (d instanceof HintDrop)
          result.add((HintDrop) d);
      }
    }
    return result;
  }

  /**
   * Gets if this drop has any hint drops of {@link IHintDrop.HintType#WARNING
   * WARNING} type.
   * 
   * @return {@code true} if this drop has warning hint drops, {@code false}
   *         otherwise.
   */
  public boolean hasWarningHints() {
    synchronized (f_seaLock) {
      for (Drop d : getDependentsReference()) {
        if (d instanceof HintDrop)
          if (((HintDrop) d).getHintType() == IHintDrop.HintType.WARNING)
            return true;
      }
    }
    return false;
  }

  /**
   * Sets if this drop ignores any Java code reference contained on its fAST
   * node.
   * 
   * @param value
   *          {@code true} to ignore the Java code reference, {@code false} to
   *          use the Java code reference.
   */
  public void setIgnoreJavaRef(boolean value) {
    synchronized (f_seaLock) {
      f_ignoreJavaRef = value;
    }
  }

  @Nullable
  @Override
  public final IJavaRef getJavaRef() {
    Pair<IJavaRef, IRNode> p = getJavaRefAndCorrespondingNode();
    if (p == null) {
      return null;
    }
    return p.first();
  }

  /**
   * Gets a pair consisting of (a) the Java code reference this information is
   * about and (b) the IR node this information was derived from , or
   * {@code null} if none.
   * 
   * @return a pair consisting of (a) the Java code reference this information
   *         is about and (b) the IR node this information was derived from , or
   *         {@code null} if none.
   */
  @Nullable
  @MustInvokeOnOverride
  protected Pair<IJavaRef, IRNode> getJavaRefAndCorrespondingNode() {
    synchronized (f_seaLock) {
      if (f_ignoreJavaRef)
        return null;
    }

    final IJavaRef javaRef = JavaNode.getJavaRef(f_node);
    if (javaRef != null)
      return new Pair<>(javaRef, f_node);
    final IRNode parent = JavaPromise.getParentOrPromisedFor(f_node);
    final IJavaRef parentRef = JavaNode.getJavaRef(parent);
    if (parentRef == null) {
      return null;
    }
    return new Pair<>(parentRef, parent);
  }

  protected final Pair<IJavaRef, IRNode> computeRefWithContext(Pair<IJavaRef, IRNode> info, IRNode context) {
	  if (info == null)
		  throw new IllegalStateException(I18N.err(292, getMessage()));

	  if (context == null) {
		  return info;
	  }
	  
	  final IJavaRef contextRef = JavaNode.getJavaRef(context);
	  final IJavaRef bestRef;
	  final IRNode bestNode;
	  if (contextRef != null) {
		  bestNode = context;
		  bestRef = contextRef;
	  } else {
		  bestNode = info.second();
		  bestRef = info.first();
	  }
	  final JavaRef.Builder builder = new JavaRef.Builder(bestRef);
	  builder.setDeclaration(info.first().getDeclaration());
	  Position position = info.first().getPositionRelativeToDeclaration();
	  if (position == Position.IS_DECL)
		  position = Position.ON_DECL;
	  builder.setPositionRelativeToDeclaration(position);
	  return new Pair<>(builder.build(), bestNode);
  }
  
  /**
   * Gets the fAST node associated with this drop.
   * 
   * @return a fAST node
   */
  public final IRNode getNode() {
    return f_node;
  }

  @Override
  public boolean isFromSrc() {
    final IRNode n = getNode();
    if (n != null) {
      return !TypeUtil.isBinary(n);
    }
    return false;
  }

  public final HintDrop addInformationHint(IRNode link, int num, Object... args) {
    return addHint(HintType.INFORMATION, -1, link, num, args);
  }

  public final HintDrop addInformationHintWithCategory(IRNode link, int catNum, int num, Object... args) {
    return addHint(HintType.INFORMATION, catNum, link, num, args);
  }

  public final HintDrop addInformationHint(IRNode link, String msg) {
    return addHint(HintType.INFORMATION, -1, link, msg);
  }

  public final HintDrop addInformationHintWithCategory(IRNode link, int catNum, String msg) {
    return addHint(HintType.INFORMATION, catNum, link, msg);
  }

  public final HintDrop addWarningHint(IRNode link, int num, Object... args) {
    return addHint(HintType.WARNING, -1, link, num, args);
  }

  public final HintDrop addWarningHintWithCategory(IRNode link, int catNum, int num, Object... args) {
    return addHint(HintType.WARNING, catNum, link, num, args);
  }

  public final HintDrop addWarningHint(IRNode link, String msg) {
    return addHint(HintType.WARNING, -1, link, msg);
  }

  public final HintDrop addWarningHintWithCategory(IRNode link, int catNum, String msg) {
    return addHint(HintType.WARNING, catNum, link, msg);
  }

  private HintDrop addHint(IHintDrop.HintType hintType, int catNum, IRNode link, int num, Object... args) {
    if (link == null)
      link = getNode();
    final HintDrop hint = new HintDrop(link, hintType);
    if (catNum > 0)
      hint.setCategorizingMessage(catNum);
    hint.setMessage(num, args);
    addDependent(hint);
    return hint;
  }

  private HintDrop addHint(IHintDrop.HintType hintType, int catNum, IRNode link, String msg) {
    if (link == null)
      link = getNode();
    final HintDrop hint = new HintDrop(link, hintType);
    if (catNum > 0)
      hint.setCategorizingMessage(catNum);
    hint.setMessage(msg);
    addDependent(hint);
    return hint;
  }

  /**
   * Asks subtypes if they have any other proposals to add to the set of
   * promises proposed by this drop.
   * <p>
   * It is okay to return a reference to an internal collection because
   * {@link #getProposals()} will copy the elements out of the returned
   * collection and not keep an alias.
   * 
   * @return a possibly empty list of proposed promises.
   */
  @RequiresLock("SeaLock")
  @NonNull
  protected List<ProposedPromiseDrop> getConditionalProposals() {
    return Collections.emptyList();
  }

  /**
   * Adds a proposed promise to this drop for the tool user to consider.
   * 
   * @param proposal
   *          the proposed promise.
   */
  public final void addProposal(ProposedPromiseDrop proposal) {
    if (proposal != null) {
      synchronized (f_seaLock) {
        if (f_proposals == null) {
          f_proposals = new ArrayList<>(1);
        }
        f_proposals.add(proposal);
      }
    }
  }

  @NonNull
  @Override
  public final List<ProposedPromiseDrop> getProposals() {
    synchronized (f_seaLock) {
      final List<ProposedPromiseDrop> conditionalProposals = getConditionalProposals();
      if (f_proposals == null && conditionalProposals.isEmpty())
        return Collections.emptyList();
      else {
        final List<ProposedPromiseDrop> result = new ArrayList<>();
        if (f_proposals != null)
          result.addAll(f_proposals);
        result.addAll(conditionalProposals);
        return result;
      }
    }
  }

  @Override
  public final boolean containsDiffInfoKey(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return true;
    }
    return false;
  }

  @Override
  public final String getDiffInfoOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsString();
    }
    return null;
  }

  @Override
  public final long getDiffInfoAsLong(String key, long valueIfNotRepresentable) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsLong(valueIfNotRepresentable);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public final int getDiffInfoAsInt(String key, int valueIfNotRepresentable) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsInt(valueIfNotRepresentable);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public final <T extends Enum<T>> T getDiffInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsEnum(valueIfNotRepresentable, elementType);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public IJavaRef getDiffInfoAsJavaRefOrThrow(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsJavaRefOrThrow();
    }
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IJavaRef getDiffInfoAsJavaRefOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsJavaRefOrNull();
    }
    return null;
  }

  @Override
  public IDecl getDiffInfoAsDeclOrThrow(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsDeclOrThrow();
    }
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IDecl getDiffInfoAsDeclOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_diffInfos)
        if (di.getKey().equals(key))
          return di.getValueAsDeclOrNull();
    }
    return null;
  }

  /**
   * Adds a new diff-info value, or replaces an existing one with the same
   * {@link IKeyValue#getKey()} value.
   * <p>
   * To construct the {@link IKeyValue} instance to pass to this method, please
   * use one of:
   * <ul>
   * <li>{@link KeyValueUtility#getStringInstance(String, String)}</li>
   * <li>{@link KeyValueUtility#getIntInstance(String, int)}</li>
   * <li>{@link KeyValueUtility#getLongInstance(String, long)}</li>
   * <li>{@link KeyValueUtility#getEnumInstance(String, Enum)}</li>
   * </ul>
   * 
   * @param value
   *          a diff-info value.
   * 
   * @throws IllegalArgumentException
   *           if value is null.
   */
  public void addOrReplaceDiffInfo(@NonNull final IKeyValue value) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    synchronized (f_seaLock) {
      for (Iterator<IKeyValue> iterator = f_diffInfos.iterator(); iterator.hasNext();) {
        final IKeyValue existing = iterator.next();
        if (existing.getKey().equals(value.getKey()))
          iterator.remove();
      }
      f_diffInfos.add(value);
    }
  }

  /**
   * The fAST node that this drop is associated with.
   */
  @NonNull
  private final IRNode f_node;

  /**
   * Notes if the Java code reference for this drop should be ignored. In some
   * cases the analysis doesn't want to "link" to a particular code location
   * despite the given value of {@link #f_node}.
   */
  @InRegion("DropState")
  private boolean f_ignoreJavaRef = false;

  /**
   * Notes if the drop has been invalidated by a call to {@link #invalidate()}.
   */
  @InRegion("DropState")
  private boolean f_valid = true;

  /**
   * A message describing this drop, usually used by the UI.
   */
  @InRegion("DropState")
  @Nullable
  private String f_message;

  /**
   * A canonical version of the message describing this drop, usually used by
   * the regression test suite for comparisons.
   */
  @InRegion("DropState")
  @Nullable
  private String f_messageCanonical;

  /**
   * A categorizing message for this drop, usually used by the UI.
   */
  @InRegion("DropState")
  @Nullable
  private String f_categorizingMessage = null;

  /**
   * The set of drops whose truth depends upon this drop.
   * <p>
   * <b>Dependent</b>: (definition) Contingent on another. Subordinate. Relying
   * on or requiring the aid of another for support.
   */
  @UniqueInRegion("DropState")
  final private Set<Drop> f_dependents = new HashSet<>();

  /**
   * The set of drops upon whose truth this drop depends upon.
   * <p>
   * <b>Deponent</b>: (definition) One who testifies under oath, especially in
   * writing.
   */
  @UniqueInRegion("DropState")
  final private Set<Drop> f_deponents = new HashSet<>();

  /**
   * A link to the {@link Sea} object this drop exists within.
   */
  @NonNull
  final private Sea f_mySea;

  /**
   * Holds the set of promises proposed by this drop.
   */
  @UniqueInRegion("DropState")
  private List<ProposedPromiseDrop> f_proposals = null;

  /**
   * Holds the set of diff-info values for this drop.
   */
  @UniqueInRegion("DropState")
  private final List<IKeyValue> f_diffInfos = new ArrayList<>();

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

  @RequiresLock("SeaLock")
  @MustInvokeOnOverride
  public void snapshotAttrs(XmlCreator.Builder s) {
    if (f_message != null)
      s.addAttribute(MESSAGE, getMessage());
    if (getMessageCanonical() != null)
      s.addAttribute(MESSAGE_ID, getMessageCanonical());

    final String cat = getCategorizingMessage();
    if (cat != null)
      s.addAttribute(CATEGORY_ATTR, cat);

    s.addAttribute(FROM_SRC, isFromSrc());

    /*
     * Compute diff information we want to pass along into the results
     */
    final Pair<IJavaRef, IRNode> loc = getJavaRefAndCorrespondingNode();
    if (loc == null) {
      getJavaRefAndCorrespondingNode();
    }
    DiffHeuristicsComputation.computeDiffInfo(this, loc);
    addOrReplaceDiffInfo(KeyValueUtility.getLongInstance(DiffHeuristics.FAST_TREE_HASH, SeaSnapshot.computeHash(getNode())));
    addOrReplaceDiffInfo(KeyValueUtility.getLongInstance(DiffHeuristics.FAST_CONTEXT_HASH,
        SeaSnapshot.computeContextHash(getNode())));
    /*
     * Output diff information
     * 
     * We want this to encode whitespace in the strings so we use a special
     * Entities instance for that purpose. Should be all on one line.
     */
    if (!f_diffInfos.isEmpty())
      s.addAttribute(DIFF_INFO, KeyValueUtility.encodeListForPersistence(f_diffInfos), Entities.Holder.DEFAULT_PLUS_WHITESPACE);
  }

  @MustInvokeOnOverride
  public void preprocessRefs(SeaSnapshot s) {
    for (Drop c : getHints()) {
      s.snapshotDrop(c);
    }
    for (ProposedPromiseDrop pd : getProposals()) {
      s.snapshotDrop(pd);
    }
  }

  @MustInvokeOnOverride
  public void snapshotRefs(SeaSnapshot s, XmlCreator.Builder db) {
    for (Drop c : getHints()) {
      s.refDrop(db, HINT_ABOUT, c);
    }
    for (ProposedPromiseDrop pd : getProposals()) {
      s.refDrop(db, PROPOSED_PROMISE, pd);
    }
  }
}
