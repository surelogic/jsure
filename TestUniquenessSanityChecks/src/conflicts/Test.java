package conflicts;

import com.surelogic.Borrowed;
import com.surelogic.NotUnique;
import com.surelogic.Unique;

public class Test {
  // Parameters
  public void method1_good(@Unique(/* is CONSISTENT */) Object p) {}
  public void method2_good(@Borrowed(/* is CONSISTENT */) Object p) {}
  public void method3_good(@NotUnique(/* is CONSISTENT */) Object p) {}
  public void method4_bad(@Unique(/* is INVALID */) @Borrowed(/* is INVALID */) Object p) {}
  public void method5_bad(@Unique(/* is INVALID */) @NotUnique(/* is INVALID */) Object p) {}
  public void method6_good(@Borrowed(/* is CONSISTENT */) @NotUnique(/* is CONSISTENT */) Object p) {}
  public void method7_bad(@Borrowed(/* is INVALID */) @Unique(/* is INVALID */) @NotUnique(/* is INVALID */) Object p) {}

  
  
  // Receiver
  
  @Unique("this" /* is CONSISTENT */)
  public void xxx1_good() {}
  
  @Borrowed("this" /* is CONSISTENT */)
  public void xxx2_good() {}
  
  @NotUnique("this" /* is CONSISTENT */)
  public void xxx3_good() {}
  
  @Unique("this" /* is INVALID */)
  @Borrowed("this" /* is INVALID */)
  public void xxx4_bad() {}
  
  @Unique("this" /* is INVALID */)
  @NotUnique("this" /* is INVALID */)
  public void xxx5_bad() {}
  
  @Borrowed("this" /* is CONSISTENT */)
  @NotUnique("this" /* is CONSISTENT */)
  public void xxx6_good() {}
  
  @Borrowed("this" /* is INVALID */)
  @Unique("this" /* is INVALID */)
  @NotUnique("this" /* is INVALID */)
  public void xxx7_bad() {}
}
