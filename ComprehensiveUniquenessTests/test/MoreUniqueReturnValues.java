package test;

/**
 * Return values: Test methods with multiple return values; Some with 
 * all good; some with all bad; some with good + bad.
 * 
 * @author aarong
 *
 */
public class MoreUniqueReturnValues {
  /**
   * @return {@unique}
   */
  public Object goodReturnUnique1(boolean b) {
    if (b) {
      return new Object();
    } else {
      return new Object();
    }
  }
  
  /**
   * @unique x, y
   * @return {@unique}
   */
  public Object goodReturnUnique2(boolean b, Object x, Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }
  
  /**
   * @unique x
   * @return {@unique}
   */
  public Object badReturnUnique1(boolean b, Object x, Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }
  
  /**
   * @return {@unique}
   */
  public Object badReturnUnique2(boolean b, Object x, Object y) {
    if (b) {
      return x;
    } else {
      return y;
    }
  }

}
