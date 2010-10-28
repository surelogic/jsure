package com.surelogic.analysis.messages;

import java.util.*;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  private static final String BUNDLE_NAME = "com.surelogic.analysis.messages"; //$NON-NLS-1$

  public static String ColorSecondPass_inferredColor = "Inferred @ThreadRole {0} for {1}";

  public static String ColorSecondPass_inheritedColor = "Inherited @ThreadRole {0} for {1}";

  public static String ColorSecondPass_inheritedTransparent = "Inherited @ThreadRoleTransparent for {0}";

  public static String ColorSecondPass_colorContextDrop = "{0} is accessed from ThreadRole context {1}";

  // Drop-sea category messages
  public static String LockAnalysis_dsc_LockViz = "lock field(s) less visible than the region(s) being protected";
  
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
  
  public static String LockAnalysis_dsc_UnsupportedModel = "unsupported lock model(s)";
  
  
  
  // Drop-sea result messages
  public static int LockAnalysis_ds_SynchronizedConstructorAssured = 200;
  public static int LockAnalysis_ds_AggregationNeeded = 201;
  public static int LockAnalysis_ds_AggregationNeeded2 = 202;
  public static int LockAnalysis_ds_FieldAccessAssured = 203;
  public static int LockAnalysis_ds_FieldAccessAssuredAlternative = 204;
  public static int LockAnalysis_ds_FieldAccessNotAssured = 205;
  public static int LockAnalysis_ds_FieldAccessNotResolvable = 206;
  public static int LockAnalysis_ds_PreconditionsAssured = 207;
  public static int LockAnalysis_ds_PreconditionsAssuredAlternative = 208;
  public static int LockAnalysis_ds_PreconditionsNotAssured = 209;
  public static int LockAnalysis_ds_PreconditionNotResolvable = 210;
  public static int LockAnalysis_ds_IndirectFieldAccessAssured = 211;
  public static int LockAnalysis_ds_IndirectFieldAccessAssuredAlternative = 212;
  public static int LockAnalysis_ds_IndirectFieldAccessNotAssured = 213;
  public static int LockAnalysis_ds_ReturnAssured = 214;
  public static int LockAnalysis_ds_ReturnNotAssured = 215;
  public static int LockAnalysis_ds_SynchronizedMethodWarningDetails = 216;
  public static int LockAnalysis_ds_SynchronizedStaticMethodWarningDetails = 217;
  public static int LockAnalysis_ds_SynchronizationUnused = 218;
  public static int LockAnalysis_ds_NonfinalExpression = 219;
  public static int LockAnalysis_ds_UnidentifiableLock = 220;
  public static int LockAnalysis_ds_RedundantSynchronized = 221;
  public static int LockAnalysis_ds_SyncedJUCLock = 222;
  public static int LockAnalysis_ds_MasqueradingCall = 223;
  public static int LockAnalysis_ds_MasqueradingCall2 = 224;
  public static int LockAnalysis_ds_JUCLockFields = 225;
  public static int LockAnalysis_ds_JUCLockFields2 = 226;
  public static int LockAnalysis_ds_DeclaredJUCLockField = 227;
  public static int LockAnalysis_ds_PoisonedLockCall=228;
  public static int LockAnalysis_ds_NoMatchingUnlocks=229;
  public static int LockAnalysis_ds_MatchingUnlock=230;
  public static int LockAnalysis_ds_PoisonedUnlockCall=231;
  public static int LockAnalysis_ds_NoMatchingLocks=232;
  public static int LockAnalysis_ds_MatchingLock=233;
  public static int LockAnalysis_ds_AggregationEvidence=234;
  public static int LockAnalysis_ds_ConstructorIsSingleThreaded = 235;
  public static int LockAnalysis_ds_OnBehalfOfConstructor = 236;
  public static int LockAnalysis_ds_EnclosingConstructorIsSingleThreaded = 237;
  public static int LockAnalysis_ds_EnclosingConstructorNotProvenSingleThreaded = 238;
  public static int LockAnalysis_ds_FieldDeclaration = 239;
  public static int LockAnalysis_ds_AssumedHeld = 240;
  public static int LockAnalysis_ds_HeldLock = 241;
  public static int LockAnalysis_ds_HeldJUCLock = 242;
  public static int LockAnalysis_ds_PreconditionsNotResolvable = 243;
  public static int LockAnalysis_ds_IndirectFieldAccessNotResolvable = 244;
  public static int LockAnalysis_ds_FieldAccessOkayClassInit = 245;
  public static int LockAnalysis_ds_FieldAccessOkayThreadConfined = 246;
  public static int LockAnalysis_ds_FieldAccessOkayClassInitAlternative = 247;
  public static int LockAnalysis_ds_FieldAccessOkayThreadConfinedAlternative = 248;
  public static int LockAnalysis_ds_PreconditionsOkayClassInit = 249;
  public static int LockAnalysis_ds_PreconditionsOkayThreadConfined = 250;
  public static int LockAnalysis_ds_PreconditionsOkayClassInitAlternative = 251;
  public static int LockAnalysis_ds_PreconditionsOkayThreadConfinedAlternative = 252;
  public static int LockAnalysis_ds_IndirectFieldAccessOkayClassInit = 253;
  public static int LockAnalysis_ds_IndirectFieldAccessOkayThreadConfined = 254;
  public static int LockAnalysis_ds_IndirectFieldAccessOkayClassInitAlternative = 255;
  public static int LockAnalysis_ds_IndirectFieldAccessOkayThreadConfinedAlternative = 256;
  public static int LockAnalysis_ds_LockViz = 257;
  public static int LockAnalysis_ds_UnsupportedModel = 258;

  
  
  // For ThreadSafe assurance
  public static int FINAL_AND_THREADSAFE = 275;
  public static int VOLATILE_AND_THREADSAFE = 276;
  public static int PROTECTED_AND_THREADSAFE = 277;
  public static int UNSAFE_FIELD = 278;
  public static int PRIMITIVE_TYPE = 279;
  
  

  
  
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