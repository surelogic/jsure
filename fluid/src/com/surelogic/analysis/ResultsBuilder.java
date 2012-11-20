package com.surelogic.analysis;

import com.surelogic.dropsea.ir.AnalysisResultDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public final class ResultsBuilder {
  private final PromiseDrop<?> promiseDrop;

  public ResultsBuilder(final PromiseDrop<?> pd) {
    promiseDrop = pd;
  }

  
  
  // ----------------------------------------------------------------------
  // -- Create result folders that check the root promise drop
  // ----------------------------------------------------------------------
  
  public final ResultFolderDrop createRootAndFolder(
      final IRNode node, final int trueMsg, final int falseMsg, 
      final Object... args) {
    final ResultFolderDrop folder = ResultFolderDrop.newAndFolder(node);
    folder.addChecked(promiseDrop);
    folder.setMessagesByJudgement(trueMsg, falseMsg, args);
    return folder;
    
  }

  public final ResultFolderDrop createRootOrFolder(
      final IRNode node, final int trueMsg, final int falseMsg, 
      final Object... args) {
    final ResultFolderDrop folder = ResultFolderDrop.newOrFolder(node);
    folder.addChecked(promiseDrop);
    folder.setMessagesByJudgement(trueMsg, falseMsg, args);
    return folder;
    
  }

  
  
  // ----------------------------------------------------------------------
  // -- Create result folders that are trusted by other results
  // ----------------------------------------------------------------------
  
  public static ResultFolderDrop createAndFolder(
      final AnalysisResultDrop parent, final IRNode node,
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultFolderDrop folder = ResultFolderDrop.newAndFolder(node);
    parent.addTrusted(folder);
    folder.setMessagesByJudgement(trueMsg, falseMsg, args);
    return folder;
    
  }

  public static ResultFolderDrop createOrFolder(
      final AnalysisResultDrop parent, final IRNode node,
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultFolderDrop folder = ResultFolderDrop.newOrFolder(node);
    parent.addTrusted(folder);
    folder.setMessagesByJudgement(trueMsg, falseMsg, args);
    return folder;
    
  }

  
  
  // ----------------------------------------------------------------------
  // -- Create results that check the root promise drop
  // ----------------------------------------------------------------------
  
  public final ResultDrop createRootResult(
      final boolean isConsistent, final IRNode node, 
      final int msg, final Object... args) {
    return createResult(isConsistent, promiseDrop, node, msg, args);
  }

  public final ResultDrop createRootResult(
      final IRNode node, final boolean isConsistent,  
      final int trueMsg, final int falseMsg, final Object... args) {
    return createResult(node, promiseDrop, isConsistent, trueMsg, falseMsg, args);
  }
  
  public final ResultDrop createRootResult(
      final boolean isConsistent, final IRNode node, final IRNode proofContext,
      final int msg, final Object... args) {
    return createResult(isConsistent, promiseDrop, node, proofContext, msg, args);
  }

  public final ResultDrop createRootResult(
      final IRNode node, final IRNode proofContext, final boolean isConsistent,  
      final int trueMsg, final int falseMsg, final Object... args) {
    return createResult(node, proofContext, promiseDrop, isConsistent, trueMsg, falseMsg, args);
  }

  
  
  // ----------------------------------------------------------------------
  // -- Create results that are trusted by other results
  // ----------------------------------------------------------------------
  
  public static ResultDrop createResult(
      final AnalysisResultDrop parent, final IRNode node, final boolean isConsistent,
      final int trueMsg, final int falseMsg, final Object... args) {
    return createResult(parent, node, null, isConsistent, trueMsg, falseMsg, args);
  }

  public static ResultDrop createResult(
      final boolean isConsistent, final AnalysisResultDrop parent,
      final IRNode node, final int msg, final Object... args) {
    return createResult(isConsistent, parent, node, null, msg, args);
  }
  
  public static ResultDrop createResult(final AnalysisResultDrop parent,
      final IRNode node, final IRNode proofContext, 
      final boolean isConsistent,
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultDrop result = new ResultDrop(node, proofContext);
    parent.addTrusted(result);
    result.setConsistent(isConsistent);
    result.setMessagesByJudgement(trueMsg, falseMsg, args);
    return result;
  }

  public static ResultDrop createResult(
      final boolean isConsistent, final AnalysisResultDrop parent,
      final IRNode node, final IRNode proofContext,
      final int msg, final Object... args) {
    final ResultDrop result = new ResultDrop(node, proofContext);
    parent.addTrusted(result);
    result.setConsistent(isConsistent);
    result.setMessage(msg, args);
    return result;
  }

  
  
  // ----------------------------------------------------------------------
  // -- Create results that check a given promise drop
  // ----------------------------------------------------------------------

  public static ResultDrop createResult(
      final boolean isConsistent, final PromiseDrop<?> checks, final IRNode node, 
      final int msg, final Object... args) {
    return createResult(isConsistent, checks, node, null, msg, args);
  }

  public static ResultDrop createResult(
      final IRNode node, final PromiseDrop<?> checks, final boolean isConsistent,  
      final int trueMsg, final int falseMsg, final Object... args) {
    return createResult(node, null, checks, isConsistent, trueMsg, falseMsg, args);
  }

  public static ResultDrop createResult(
      final boolean isConsistent, final PromiseDrop<?> checks,
      final IRNode node, final IRNode proofContext, 
      final int msg, final Object... args) {
    final ResultDrop result = new ResultDrop(node, proofContext);
    result.addChecked(checks);
    result.setConsistent(isConsistent);
    result.setMessage(msg, args);
    return result;
  }

  public static ResultDrop createResult(
      final IRNode node, final IRNode proofContext,
      final PromiseDrop<?> checks, final boolean isConsistent,  
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultDrop result = new ResultDrop(node, proofContext);
    result.addChecked(checks);
    result.setConsistent(isConsistent);
    result.setMessagesByJudgement(trueMsg, falseMsg, args);
    return result;
  }
}
