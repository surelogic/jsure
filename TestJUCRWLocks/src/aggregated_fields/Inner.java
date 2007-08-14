package aggregated_fields;

import com.surelogic.Borrowed;
import com.surelogic.Reads;
import com.surelogic.Writes;

public class Inner {
  public int f1;
  public int f2;
  
  @Borrowed("this")
  public Inner() {
    // do stuff
  }
  
  @Writes("Instance")
  @Borrowed("this")
  public void setBoth(int a, int b) {
    f1 = a;
    f2 = b;
  }
  
  @Reads("Instance")
  @Borrowed("this")
  public int sum() {
    return f1 + f2;
  }
}
