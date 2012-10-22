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

  public final ResultDrop createRootResult(
      final boolean isConsistent, final IRNode node, 
      final int msg, final Object... args) {
    final ResultDrop result = new ResultDrop(node);
    result.addChecked(promiseDrop);
    result.setConsistent(isConsistent);
    result.setMessage(msg, args);
    return result;
  }

  public final ResultDrop createRootResult(
      final IRNode node, final boolean isConsistent,  
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultDrop result = new ResultDrop(node);
    result.addChecked(promiseDrop);
    result.setConsistent(isConsistent);
    result.setMessagesByJudgement(trueMsg, falseMsg, args);
    return result;
  }

  public static ResultDrop createResult(
      final AnalysisResultDrop parent, final IRNode node, final boolean isConsistent,
      final int trueMsg, final int falseMsg, final Object... args) {
    final ResultDrop result = new ResultDrop(node);
    parent.addTrusted(result);
    result.setConsistent(isConsistent);
    result.setMessagesByJudgement(trueMsg, falseMsg, args);
    return result;
  }

  public static ResultDrop createResult(
      final boolean isConsistent, final AnalysisResultDrop parent,
      final IRNode node, final int msg, final Object... args) {
    final ResultDrop result = new ResultDrop(node);
    parent.addTrusted(result);
    result.setConsistent(isConsistent);
    result.setMessage(msg, args);
    return result;
  }
}
