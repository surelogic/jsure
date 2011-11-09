package test;

import com.surelogic.Unique;

/**
 * Return values: Test methods with multiple return values; Some with
 * all good; some with all bad; some with good + bad.
 */
public class MoreUniqueReturnValues {
  @Unique("return")
  public Object goodReturnUnique1(boolean b) {
    if (b) {
      return new Object();
    } else {
      return new Object();
    }
  }

  @Unique("return")
  public Object goodReturnUnique2(boolean b, @Unique Object x, @Unique Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }

  @Unique("return")
  public Object badReturnUnique1(boolean b, @Unique Object x, Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }

  @Unique("return")
  public Object badReturnUnique2(boolean b, Object x, Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }
}
