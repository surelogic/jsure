package com.surelogic.analysis.concurrency.driver;

import edu.cmu.cs.fluid.util.AbstractMessages;

public final class Messages extends AbstractMessages {
  
  // Prevent instantiation
  private Messages() {
    super();
  }
  
  // Drop-sea category messages
  public static final int DSC_LOCK_VIZ = 200;
  public static final int DSC_AGGREGATION_NEEDED = 201;
  public static final int DSC_FIELD_ACCESS_NOT_ASSURED = 202;
  public static final int DSC_FIELD_ACCESS_ASSURED = 203;
  public static final int DSC_PRECONDITIONS_ASSURED = 204;
  public static final int DSC_PRECONDITIONS_NOT_ASSURED = 205;
  public static final int DSC_INDIRECT_FIELD_ACCESS_NOT_ASSURED = 206;
  public static final int DSC_INDIRECT_FIELD_ACCESS_ASSURED = 207;
  public static final int DSC_RETURN_ASSURED = 208;
  public static final int DSC_RETURN_NOT_ASSURED = 209;
  public static final int DSC_UNIDENTIFIABLE_LOCK_WARNING = 210;
  public static final int DSC_SYNCHRONIZED_UNUSED_WARNING = 211;
  public static final int DSC_NONFINAL_EXPRESSION_WARNING = 212;
  public static final int DSC_REDUNDANT_SYNCHRONIZED = 213;
  public static final int DSC_MIXED_PARADIGM = 214;
  public static final int DSC_NOT_A_LOCK_METHOD = 215;
  public static final int DSC_MATCHING_CALLS = 216;
  public static final int DSC_UNSUPPORTED_MODEL = 217;
  public static final int DSC_FINAL_FIELDS = 218;

  
  
  // Drop-sea result messages
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
  public static final int LockAnalysis_ds_ConstructorIsSingleThreaded = 235;
  public static final int LockAnalysis_ds_OnBehalfOfConstructor = 236;
  public static final int LockAnalysis_ds_EnclosingConstructorNotProvenSingleThreaded = 238;
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
  public static final int VOUCHED_FINAL = 259;
  public static final int VOUCHED_FINAL_WITH_REASON = 260;
  public static final int FORMAL_PARAMETER_WRITTEN_TO = 261;
  public static final int SHOULD_BE_FINAL = 262;
  public static final int CONSTRUCTOR_IS_THREADCONFINED = 263;
  public static final int CONSTRUCTOR_IS_NOT_THREADCONFINED = 264;
  public static final int RECEIVER_IS_NOT_ALIASED = 265;
  public static final int STARTS_NO_THREADS_ETC = 266;
}
