package test;

import com.surelogic.Immutable;
import com.surelogic.Vouch;

@Immutable
public class TestVouch {
  // GOOD:  final and primitive
  protected final int good1 = 1;
  
  
  
  // GOOD: final and Immutable
  protected final Point good2 = new Point(1, 2);
  
  // BAD: not-final and Immutable
  @Vouch("Immutable")
  protected Point bad2 = new Point(1, 2);
  
  // BAD: final and Mutable
  @Vouch("Immutable")
  protected final Object bad3 = new Object();
  
  // BAD: non-final and mutable
  @Vouch("Immutable")
  protected Object bad4 = null;
}
