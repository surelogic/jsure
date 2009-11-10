package aggregate.mustMapInstance;

import com.surelogic.Borrowed;

public class D {
  protected int f1;
  protected int f2;
  
  @Borrowed("this")
  public D() {
  	super();
  }
}
