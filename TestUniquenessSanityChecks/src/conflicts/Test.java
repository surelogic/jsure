package conflicts;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class Test {
  // Parameters
  public void method1_good(@Unique(/* is CONSISTENT */) Object p) {}
  public void method2_good(@Borrowed(/* is CONSISTENT */) Object p) {}
  public void method4_bad(@Unique(/* is INVALID */) @Borrowed(/* is INVALID */) Object p) {}

  
  
  // Receiver
  
  @Unique("this" /* is CONSISTENT */)
  public void xxx1_good() {}
  
  @Borrowed("this" /* is CONSISTENT */)
  public void xxx2_good() {}
  
  @Unique("this" /* is INVALID */)
  @Borrowed("this" /* is INVALID */)
  public void xxx4_bad() {}
}
