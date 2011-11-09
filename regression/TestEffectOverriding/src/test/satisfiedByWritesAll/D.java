package test.satisfiedByWritesAll;

public class D extends C {
  // Writes All
  
  // GOOD
  @Override
  public void unannotated() {}
  
  // GOOD
  @Override
  public void writesImplicitAll() {}
  
  // GOOD
  @Override
  public void writesQualifiedAll() {}
  
  
  
  // reads All

  // BAD
  @Override
  public void readsImplicitAll() {}
  
  // BAD
  @Override
  public void readsQualifiedAll() {}

  
  
  // Other Writes
  
  // BAD
  @Override
  public void writesImplicitThis() {}
  
  // BAD
  @Override
  public void writesImplicitStatic() {}
  
  // BAD
  @Override
  public void writesAny() {}
  
  // BAD
  @Override
  public void writesQualifiedThis() {}
  
  // BAD
  @Override
  public void writesQualifiedStatic() {}
  
  // BAD
  @Override
  public void writesExplicitThis() {}
  
  // BAD
  @Override
  public void writesParam(final C x) {}

  
  
  // Other Reads
  
  // BAD
  @Override
  public void readsImplicitThis() {}
  
  // BAD
  @Override
  public void readsImplicitStatic() {}
  
  // BAD
  @Override
  public void readsAny() {}
  
  // BAD
  @Override
  public void readsQualifiedThis() {}
  
  // BAD
  @Override
  public void readsQualifiedStatic() {}
  
  // BAD
  @Override
  public void readsExplicitThis() {}
  
  // BAD
  @Override
  public void readsParam(final C x) {}
}
