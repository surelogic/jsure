package com.surelogic.analysis.locks;

import java.util.*;

import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.util.AbstractMessages;

public final class Messages extends AbstractMessages {
  // Prevent instantiation
  private Messages() {
    super();
  }
  
  
  
  // Drop-sea category messages
  public static final String LockAnalysis_dsc_LockViz = "lock field(s) less visible than the region(s) being protected";
  public static final Category DSC_LOCK_VIZ = Category.getInstance(LockAnalysis_dsc_LockViz);

  public static final String LockAnalysis_dsc_AggregationNeeded = "protected reference(s) to a possibly shared unprotected object; possible race condition detected";
  public static final Category DSC_AGGREGATION_NEEDED = Category.getInstance(LockAnalysis_dsc_AggregationNeeded);

  public static final String LockAnalysis_dsc_FieldAccessNotAssured = "unprotected field access(es); possible race condition detected";
  public static final Category DSC_FIELD_ACCESS_NOT_ASSURED = Category.getInstance(LockAnalysis_dsc_FieldAccessNotAssured);

  public static final String LockAnalysis_dsc_FieldAccessAssured = "protected field access(es)";
  public static final Category DSC_FIELD_ACCESS_ASSURED = Category.getInstance(LockAnalysis_dsc_FieldAccessAssured);

  public static final String LockAnalysis_dsc_PreconditionsAssured = "lock precondition(s) satisfied";
  public static final Category DSC_PRECONDITIONS_ASSURED = Category.getInstance(LockAnalysis_dsc_PreconditionsAssured);

  public static final String LockAnalysis_dsc_PreconditionsNotAssured = "lock precondition(s) not satisfied; possible race conditions enabled";
  public static final Category DSC_PRECONDITIONS_NOT_ASSURED = Category.getInstance(LockAnalysis_dsc_PreconditionsNotAssured);

  public static final String LockAnalysis_dsc_IndirectFieldAccessNotAssured = "unprotected indirect field access(es); possible race condition detected";
  public static final Category DSC_INDIRECT_FIELD_ACCESS_NOT_ASSURED = Category.getInstance(LockAnalysis_dsc_IndirectFieldAccessNotAssured);

  public static final String LockAnalysis_dsc_IndirectFieldAccessAssured = "protected indirect field access(es)";
  public static final Category DSC_INDIRECT_FIELD_ACCESS_ASSURED = Category.getInstance(LockAnalysis_dsc_IndirectFieldAccessAssured);

  public static final String LockAnalysis_dsc_ReturnAssured = "return statement(s) returning the correct lock";
  public static final Category DSC_RETURN_ASSURED = Category.getInstance(LockAnalysis_dsc_ReturnAssured);

  public static final String LockAnalysis_dsc_ReturnNotAssured = "return statement(s) possibly returning the wrong lock";
  public static final Category DSC_RETURN_NOT_ASSURED = Category.getInstance(LockAnalysis_dsc_ReturnNotAssured);

  public static final String LockAnalysis_dsc_UnidentifiableLockWarning = "unidentifiable lock(s); what is the name of the lock? what state is being protected?";
  public static final Category DSC_UNIDENTIFIABLE_LOCK_WARNING = Category.getInstance(LockAnalysis_dsc_UnidentifiableLockWarning);

  public static final String LockAnalysis_dsc_SynchronizationUnusedWarning = "synchronized block(s) not protecting any state; what state is being protected?";
  public static final Category DSC_SYNCHRONIZED_UNUSED_WARNING = Category.getInstance(LockAnalysis_dsc_SynchronizationUnusedWarning);

  public static final String LockAnalysis_dsc_NonfinalExpressionWarning = "non-final lock expression(s); analysis cannot determine which lock is being acquired";
  public static final Category DSC_NONFINAL_EXPRESSION_WARNING = Category.getInstance(LockAnalysis_dsc_NonfinalExpressionWarning);

  public static final String LockAnalysis_dsc_RedundantSynchronized = "redundant lock acquisition(s)";
  public static final Category DSC_REDUNDANT_SYNCHRONIZED = Category.getInstance(LockAnalysis_dsc_RedundantSynchronized);

  public static final String LockAnalysis_dsc_MixedParadigm = "mixed java.util.concurrent/intrinsic locking usage(s)";
  public static final Category DSC_MIXED_PARADIGM = Category.getInstance(LockAnalysis_dsc_MixedParadigm);

  public static final String LockAnalysis_dsc_NotALockMethod = "calls to methods masquerading as java.util.concurrent.Lock methods";
  public static final Category DSC_NOT_A_LOCK_METHOD = Category.getInstance(LockAnalysis_dsc_NotALockMethod);
  
  public static final String LockAnalysis_dsc_MatchingCalls = "lock()\u2013unlock() matches";
  public static final Category DSC_MATCHING_CALLS = Category.getInstance(LockAnalysis_dsc_MatchingCalls);
  
  public static final String LockAnalysis_dsc_UnsupportedModel = "unsupported lock model(s)";
  public static final Category DSC_UNSUPPORTED_MODEL = Category.getInstance(LockAnalysis_dsc_UnsupportedModel);

  
  
  // Labels for the single-threaded result disjunction
  public static final String LockAnalysis_ds_SingleThreadedUniqueReturn = "by unique return";
  public static final String LockAnalysis_ds_SingleThreadedEffects = "by effects";
  public static final String LockAnalysis_ds_SingleThreadedBorrowedThis = "by borrowed receiver";


  
  // Drop-sea result messages
  public static final int LockAnalysis_ds_SynchronizedConstructorAssured = 200;
  public static final int LockAnalysis_ds_AggregationNeeded = 201;
  public static final int LockAnalysis_ds_AggregationNeeded2 = 202;
  public static final int LockAnalysis_ds_FieldAccessAssured = 203;
  public static final int LockAnalysis_ds_FieldAccessAssuredAlternative = 204;
  public static final int LockAnalysis_ds_FieldAccessNotAssured = 205;
  public static final int LockAnalysis_ds_FieldAccessNotResolvable = 206;
  public static final int LockAnalysis_ds_PreconditionsAssured = 207;
  public static final int LockAnalysis_ds_PreconditionsAssuredAlternative = 208;
  public static final int LockAnalysis_ds_PreconditionsNotAssured = 209;
  public static final int LockAnalysis_ds_PreconditionNotResolvable = 210;
  public static final int LockAnalysis_ds_IndirectFieldAccessAssured = 211;
  public static final int LockAnalysis_ds_IndirectFieldAccessAssuredAlternative = 212;
  public static final int LockAnalysis_ds_IndirectFieldAccessNotAssured = 213;
  public static final int LockAnalysis_ds_ReturnAssured = 214;
  public static final int LockAnalysis_ds_ReturnNotAssured = 215;
  public static final int LockAnalysis_ds_SynchronizedMethodWarningDetails = 216;
  public static final int LockAnalysis_ds_SynchronizedStaticMethodWarningDetails = 217;
  public static final int LockAnalysis_ds_SynchronizationUnused = 218;
  public static final int LockAnalysis_ds_NonfinalExpression = 219;
  public static final int LockAnalysis_ds_UnidentifiableLock = 220;
  public static final int LockAnalysis_ds_RedundantSynchronized = 221;
  public static final int LockAnalysis_ds_SyncedJUCLock = 222;
  public static final int LockAnalysis_ds_MasqueradingCall = 223;
  public static final int LockAnalysis_ds_MasqueradingCall2 = 224;
  public static final int LockAnalysis_ds_JUCLockFields = 225;
  public static final int LockAnalysis_ds_JUCLockFields2 = 226;
  public static final int LockAnalysis_ds_DeclaredJUCLockField = 227;
  public static final int LockAnalysis_ds_PoisonedLockCall=228;
  public static final int LockAnalysis_ds_NoMatchingUnlocks=229;
  public static final int LockAnalysis_ds_MatchingUnlock=230;
  public static final int LockAnalysis_ds_PoisonedUnlockCall=231;
  public static final int LockAnalysis_ds_NoMatchingLocks=232;
  public static final int LockAnalysis_ds_MatchingLock=233;
  public static final int LockAnalysis_ds_AggregationEvidence=234;
  public static final int LockAnalysis_ds_ConstructorIsSingleThreaded = 235;
  public static final int LockAnalysis_ds_OnBehalfOfConstructor = 236;
  public static final int LockAnalysis_ds_EnclosingConstructorIsSingleThreaded = 237;
  public static final int LockAnalysis_ds_EnclosingConstructorNotProvenSingleThreaded = 238;
  public static final int LockAnalysis_ds_FieldDeclaration = 239;
  public static final int LockAnalysis_ds_AssumedHeld = 240;
  public static final int LockAnalysis_ds_HeldLock = 241;
  public static final int LockAnalysis_ds_HeldJUCLock = 242;
  public static final int LockAnalysis_ds_PreconditionsNotResolvable = 243;
  public static final int LockAnalysis_ds_IndirectFieldAccessNotResolvable = 244;
  public static final int LockAnalysis_ds_FieldAccessOkayClassInit = 245;
  public static final int LockAnalysis_ds_FieldAccessOkayThreadConfined = 246;
  public static final int LockAnalysis_ds_FieldAccessOkayClassInitAlternative = 247;
  public static final int LockAnalysis_ds_FieldAccessOkayThreadConfinedAlternative = 248;
  public static final int LockAnalysis_ds_PreconditionsOkayClassInit = 249;
  public static final int LockAnalysis_ds_PreconditionsOkayThreadConfined = 250;
  public static final int LockAnalysis_ds_PreconditionsOkayClassInitAlternative = 251;
  public static final int LockAnalysis_ds_PreconditionsOkayThreadConfinedAlternative = 252;
  public static final int LockAnalysis_ds_IndirectFieldAccessOkayClassInit = 253;
  public static final int LockAnalysis_ds_IndirectFieldAccessOkayThreadConfined = 254;
  public static final int LockAnalysis_ds_IndirectFieldAccessOkayClassInitAlternative = 255;
  public static final int LockAnalysis_ds_IndirectFieldAccessOkayThreadConfinedAlternative = 256;
  public static final int LockAnalysis_ds_LockViz = 257;
  public static final int LockAnalysis_ds_UnsupportedModel = 258;

  
  
  // For ThreadSafe assurance
  public static final int PRIMITIVE_TYPE = 400;
  public static final int FINAL_AND_THREADSAFE = 401;
  public static final int VOLATILE_AND_THREADSAFE = 402;
  public static final int PROTECTED_AND_THREADSAFE = 403;
  public static final int UNIQUE_AND_CONTAINABLE = 404;
  public static final int UNSAFE_FIELD = 405;
  public static final int UNSAFE_REFERENCE = 406;
  
  
  
  // For Containable assurance
  public static final int CONSTRUCTOR_UNIQUE_RETURN = 450;
  public static final int CONSTRUCTOR_BORROWED_RECEVIER = 451;
  public static final int CONSTRUCTOR_BAD = 452;
  public static final int METHOD_BORROWED_RECEIVER = 453;
  public static final int METHOD_BAD = 454;
  public static final int FIELD_CONTAINED_PRIMITIVE = 455;
  public static final int FIELD_CONTAINED_OBJECT = 456;
  public static final int FIELD_BAD = 457;
  public static final int FIELD_NOT_CONTAINABLE = 458;
  public static final int FIELD_NOT_UNIQUE = 459;
  public static final int FIELD_NOT_AGGREGATED = 460;
  

 
  private static final Map<Integer,String> code2name = new HashMap<Integer,String>();

  /** To support JSure-Sierra integration
  */
  public static String toString(int code) {
	  return code2name.get(code);
  }

  static {
    collectCodeNames(Messages.class, code2name);
  }
}
