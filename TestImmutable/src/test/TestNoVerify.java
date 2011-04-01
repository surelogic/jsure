package test;

import com.surelogic.Immutable;

@Immutable(verify=false)
public class TestNoVerify {
  // GOOD:  final and primitive
  protected final int good1 = 1;
  
  // BAD: non-final primitive
  protected int bad1 = 2;
  
  
  
  // GOOD: final and Immutable
  protected final Point good2 = new Point(1, 2);
  
  // BAD: not-final and Immutable
  protected Point bad2 = new Point(1, 2);
  
  // BAD: final and Mutable
  protected final Object bad3 = new Object();
  
  // BAD: non-final and mutable
  protected Object bad4 = null;
}
