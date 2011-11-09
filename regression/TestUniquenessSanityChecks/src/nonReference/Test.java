package nonReference;

import com.surelogic.Borrowed;
import com.surelogic.Unique;

public class Test {
  @Unique(/* is UNASSOCIATED */)
  public int bad;
  
  @Unique(/* is CONSISTENT */)
  public Test good;
  
  
  
  @Unique("return" /* is UNASSOCIATED */)
  public int badReturn() {
    return 0;
  }

  @Unique("return" /* is CONSISTENT */)
  public Test goodReturn() {
    return null;
  }

  
  
  @Unique("this" /* is CONSISTENT */)
  public void goodUniqueReceiver() {
    // do stuff
  }
  

  
  public void goodUnique(@Unique(/* is CONSISTENT */) Test p) {
    // do stuff
  }

  public void badUnique(@Unique(/* is UNASSOCIATED */) int p) {
    // do stuff
  }
  
  
  
  @Borrowed("this" /* is CONSISTENT */)
  public void goodBorrowedReceiver() {
    // do stuff
  }

  
  
  public void goodBorrowed(@Borrowed(/* is CONSISTENT */) Test p) {
    // do stuff
  }

  public void badBorrowed(@Borrowed(/* is UNASSOCIATED */) int p) {
    // do stuff
  }
}
