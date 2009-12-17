/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/messages/Messages.java,v 1.22 2009/02/17 19:48:34 aarong Exp $*/
package com.surelogic.analysis.messages;

import java.util.*;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  private static final String BUNDLE_NAME = "com.surelogic.analysis.messages"; //$NON-NLS-1$

  public static String ColorSecondPass_inferredColor = "Inferred @colorConstraint {0} for {1}";

  public static String ColorSecondPass_inheritedColor = "Inherited @colorConstraint {0} for {1}";

  public static String ColorSecondPass_inheritedTransparent = "Inherited @transparent for {0}";

  public static String ColorSecondPass_colorContextDrop = "{0} is accessed from color context {1}";

  public static String ThreadEffectsAnalysis_noThreadsDrop = "No threads started within {0}";

  public static String ThreadEffectsAnalysis_threadEffectDrop = "Thread effect declaration prohibits: {0}";

  public static String ThreadEffectsAnalysis_callPromiseDrop = "Call \"{0}\" promises to start nothing";

  public static String ThreadEffectsAnalysis_callNotPromiseDrop = "Call \"{0}\" does not promise to start nothing";

  // Drop-sea category messages
  public static String LockAnalysis_dsc_AggregationNeeded = "protected reference(s) to a possibly shared unprotected object; possible race condition detected";

  public static String LockAnalysis_dsc_FieldAccessNotAssured = "unprotected field access(es); possible race condition detected";

  public static String LockAnalysis_dsc_FieldAccessAssured = "protected field access(es)";

  public static String LockAnalysis_dsc_PreconditionsAssured = "lock precondition(s) satisfied";

  public static String LockAnalysis_dsc_PreconditionsNotAssured = "lock precondition(s) not satisfied; possible race conditions enabled";

  public static String LockAnalysis_dsc_IndirectFieldAccessNotAssured = "unprotected indirect field access(es); possible race condition detected";

  public static String LockAnalysis_dsc_IndirectFieldAccessAssured = "protected indirect field access(es)";

  public static String LockAnalysis_dsc_ReturnAssured = "return statement(s) returning the correct lock";

  public static String LockAnalysis_dsc_ReturnNotAssured = "return statement(s) possibly returning the wrong lock";

  public static String LockAnalysis_dsc_UnidentifiableLockWarning = "unidentifiable lock(s); what is the name of the lock? what state is being protected?";

  public static String LockAnalysis_dsc_SynchronizationUnusedWarning = "synchronized block(s) not protecting any state; what state is being protected?";

  public static String LockAnalysis_dsc_NonfinalExpressionWarning = "non-final lock expression(s); analysis cannot determine which lock is being acquired";

  public static String LockAnalysis_dsc_RedundantSynchronized = "redundant lock acquisition(s)";

  public static String LockAnalysis_dsc_MixedParadigm = "mixed java.util.concurrent/intrinsic locking usage(s)";

  public static String LockAnalysis_dsc_NotALockMethod = "calls to methods masquerading as java.util.concurrent.Lock methods";
  
  public static String LockAnalysis_dsc_MatchingCalls = "lock()\u2013unlock() matches";
  
  // Drop-sea result messages
  public static String LockAnalysis_ds_SynchronizedConstructorAssured = "single-threaded constructor supported";

  public static String LockAnalysis_ds_AggregationNeeded = "Field reference \"{0}\" may be to a shared unprotected object";

  public static String LockAnalysis_ds_AggregationNeeded2 = "Receiver \"{0}\" may be a shared unprotected object";

  public static String LockAnalysis_ds_FieldAccessAssured = "Lock \"{0}\" held when accessing {1}";

  public static String LockAnalysis_ds_FieldAccessAssuredAlternative = "Lock \"{0}\" held as \"{2}\" when accessing {1}";

  public static String LockAnalysis_ds_FieldAccessNotAssured = "Lock \"{0}\" not held when accessing {1}";

  public static String LockAnalysis_ds_FieldAccessNotResolvable = "Lock \"{0}\", needed for accessing {1}, is not held within the anonymous class and cannot be resolved in the calling context; lock must be held within the anonymous class";

  public static String LockAnalysis_ds_PreconditionsAssured = "Lock \"{0}\" held when invoking {1}; precondition satisfied";

  public static String LockAnalysis_ds_PreconditionsAssuredAlternative = "Lock \"{0}\" held as \"{2}\" when invoking {1}; precondition satisfied";

  public static String LockAnalysis_ds_PreconditionsNotAssured = "Lock \"{0}\" not held when invoking {1}; precondition unsatisfied";

  public static String LockAnalysis_ds_PreconditionNotResolvable = "Lock specification \"{0}\" cannot be resolved in the calling context for {1}; precondition unsatisfied";

  public static String LockAnalysis_ds_IndirectFieldAccessAssured = "Lock \"{0}\" held when invoking {1}";

  public static String LockAnalysis_ds_IndirectFieldAccessAssuredAlternative = "Lock \"{0}\" held as \"{2}\" when invoking {1}";

  public static String LockAnalysis_ds_IndirectFieldAccessNotAssured = "Lock \"{0}\" not held when invoking {1}";

  public static String LockAnalysis_ds_ReturnAssured = "Return statement correctly returns lock \"{0}\"";

  public static String LockAnalysis_ds_ReturnNotAssured = "Return statement expected to return lock \"{0}\"";

  public static String LockAnalysis_ds_SynchronizedMethodWarningDetails = "Synchronized method {0}: \"this\" is not identifiable as a programmer-declared lock";

  public static String LockAnalysis_ds_SynchronizedStaticMethodWarningDetails = "Synchronized method {0}: \"{1}.class\" is not identifiable as a programmer-declared lock";

  public static String LockAnalysis_ds_SynchronizationUnused = "Locks {0} not needed by body of synchronized block";

  public static String LockAnalysis_ds_NonfinalExpression = "Lock expression \"{0}\" is not final";

  public static String LockAnalysis_ds_UnidentifiableLock = "Lock expression \"{0}\" does not name a programmer-declared lock; consider declaring what state is protected by the referenced lock";

  public static String LockAnalysis_ds_RedundantSynchronized = "Acquisition of lock \"{0}\" may be redundant";

  public static String LockAnalysis_ds_SyncedJUCLock = "Using a java.util.concurrent.locks lock object \"{0}\" in a syncronized statement";

  public static String LockAnalysis_ds_MasqueradingCall = "\"{0}\" does not call a method from java.util.concurrent.locks.Lock";

  public static String LockAnalysis_ds_MasqueradingCall2 = "\"{0}\" does not call a method from java.util.concurrent.locks.ReadWriteLock";

  public static String LockAnalysis_ds_JUCLockFields = "The object referenced by \"{0}\" is not a known lock, but its {1,choice,1#field|1<fields} {2} {1,choice,1#is a|1<are} java.util.concurrent {1,choice,1#lock|1<locks}.";
  
  public static String LockAnalysis_ds_DeclaredJUCLockField = "The object referenced by \"{0}\" is not a known lock, but its field \"{1}\" is declared to be java.util.concurrent lock \"{2}\".";
  
  
  
  // Drop-sea Supporting information messages
  public static String LockAnalysis_ds_OnBehalfOfConstructor = "Analyzed on behalf of constructor \"{0}\"";

  public static String LockAnalysis_ds_ConstructorIsSingleThreaded = "Constructor \"{0}\" is single-threaded";
  
  public static String LockAnalysis_ds_EnclosingConstructorIsSingleThreaded = "Enclosing constructor \"{0}\" is single-threaded";
  
  public static String LockAnalysis_ds_EnclosingConstructorNotProvenSingleThreaded = "Enclosing constructor \"{0}\" cannot be proven to be single-threaded";
    
  public static String LockAnalysis_ds_FieldDeclaration = "Field Declaration: {0}";
  
  public static String LockAnalysis_ds_AssumedHeld = "Assuming lock \"{0}\" is held";

  public static String LockAnalysis_ds_HeldLock = "Intrinsic lock \"{0}\" is held";

  public static String LockAnalysis_ds_HeldJUCLock = "java.util.concurrent lock \"{0}\" is held";

  public static String LockAnalysis_ds_PoisonedLockCall="{0}() call has a different number of matching unlock() calls along different control paths";
  
  public static String LockAnalysis_ds_NoMatchingUnlocks="{0}() call has no matching unlock() calls";
  
  public static String LockAnalysis_ds_MatchingUnlock="{0}() call has matching unlock() call at line {1}";
  
  public static String LockAnalysis_ds_PoisonedUnlockCall="unlock() call has a different number of matching lock() calls along different control paths";
  
  public static String LockAnalysis_ds_NoMatchingLocks="unlock() call has no matching lock() calls";
  
  public static String LockAnalysis_ds_MatchingLock="unlock() call has matching {0}() call at line {1}";

  public static String LockAnalysis_ds_AggregationEvidence="Method effect {0} affects region {1} of {2} which is aggregated into region {3}";
  
  
  // Labels for the single-threaded result disjunction
  public static String LockAnalysis_ds_SingleThreadedUniqueReturn = "by unique return";
  
  public static String LockAnalysis_ds_SingleThreadedEffects = "by effects";
  
  public static String LockAnalysis_ds_SingleThreadedBorrowedThis = "by borrowed receiver";

  
  
  private static final Map<String,String> value2field = new HashMap<String,String>();
  
  
  
  public static String getName(String msg) {
	  return value2field.get(msg);
  }
  
  private Messages() {
    // private constructor to prevent instantiation
  }

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
    
    collectConstantNames(Messages.class, value2field);
  }
}