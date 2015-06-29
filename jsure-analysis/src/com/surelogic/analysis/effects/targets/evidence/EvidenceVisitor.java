package com.surelogic.analysis.effects.targets.evidence;

public interface EvidenceVisitor {
  /* 'e' may be null */
  public void accept(TargetEvidence e);
  
  public void visitAggregationEvidence(AggregationEvidence e);
  public void visitAnonClassEvidence(AnonClassEvidence e);
  public void visitBCAEvidence(BCAEvidence e);
  public void visitCallEvidence(CallEvidence e);
  public void visitElaborationEvidence(ElaborationEvidence e);
  public void visitEmptyEvidence(EmptyEvidence e);
  public void visitMappedArgumentEvidence(MappedArgumentEvidence e);
  public void visitNoEvidence(NoEvidence e);
  public void visitQualifiedReceiverConversionEvidence(QualifiedReceiverConversionEvidence e);
  public void visitUnknownReferenceConversionEvidence(UnknownReferenceConversionEvidence e);
}
