package com.surelogic.analysis.effects.targets;

public abstract class EvidenceProcessor implements EvidenceVisitor {
  protected EvidenceProcessor() {
    super();
  }
  
  
  
  public final void accept(final TargetEvidence e) {
    if (e != null) e.visit(this);
  }
  
  
  
  /**
   * Generic visit method: if no specific {@code visit} method handles the 
   * evidence, this method handles it.  
   */
  protected void visit(final TargetEvidence e) {
    // do nothing
  }
  

  
  public void visitAggregationEvidence(final AggregationEvidence e) {
    visitElaborationEvidence(e);
  }

  public void visitAnonClassEvidence(final AnonClassEvidence e) {
    visit(e);
  }
  
  public void visitBCAEvidence(final BCAEvidence e) {
    visitElaborationEvidence(e);
  }

  public void visitCallEvidence(final CallEvidence e) {
    visit(e);
  }
  
  public void visitElaborationEvidence(final ElaborationEvidence e) {
    visit(e);
  }

  public void visitEmptyEvidence(final EmptyEvidence e) {
    visit(e);
  }

  public void visitIteratorEvidence(final IteratorEvidence e) {
    visit(e);
  }
  
  public void visitMappedArgumentEvidence(final MappedArgumentEvidence e) {
    visitCallEvidence(e);
  }
  
  public void visitNoEvidence(final NoEvidence e) {
    visit(e);
  }
  
  public void visitQualifiedReceiverConversionEvidence(final QualifiedReceiverConversionEvidence e) {
    visitCallEvidence(e);
  }
  
  public void visitUnknownReferenceConversionEvidence(final UnknownReferenceConversionEvidence e) {
    visitAnonClassEvidence(e);
  }
}
