package com.surelogic.analysis.effects.targets.evidence;

public abstract class EvidenceProcessor implements EvidenceVisitor {
  private final boolean chain;
  
  protected EvidenceProcessor(final boolean chain) {
    this.chain = chain;
  }
  
  protected EvidenceProcessor() {
    this(false);
  }
  
  
  
  @Override
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
  

  
  @Override
  public void visitAggregationEvidence(final AggregationEvidence e) {
    visitElaborationEvidence(e);
  }
  
  @Override
  public void visitBCAEvidence(final BCAEvidence e) {
    visitElaborationEvidence(e);
  }

  @Override
  public void visitCallEvidence(final CallEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitElaborationEvidence(final ElaborationEvidence e) {
    visit(e);
    if (chain) accept(e.getMoreEvidence());
  }

  @Override
  public void visitEnclosingRefEvidence(final EnclosingRefEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitEmptyEvidence(final EmptyEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitMappedArgumentEvidence(final MappedArgumentEvidence e) {
    visitCallEvidence(e);
  }
  
  @Override
  public void visitNoEvidence(final NoEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitQualifiedReceiverConversionEvidence(final QualifiedReceiverConversionEvidence e) {
    visitCallEvidence(e);
  }
  
  @Override
  public void visitUnknownReferenceConversionEvidence(final UnknownReferenceConversionEvidence e) {
    visit(e);
  }
}
