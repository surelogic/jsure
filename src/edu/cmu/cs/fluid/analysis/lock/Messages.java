package edu.cmu.cs.fluid.analysis.lock;

import edu.cmu.cs.fluid.sea.Category;

final class Messages {  
  private Messages() {
    // private constructor to prevent instantiation
  }

  
  
  public static final Category DSC_PUBLIC_STATIC_FIELD = Category.getInstance(70);
  public static final Category DSC_PUBLIC_STATIC_OBJECT_FIELD = Category.getInstance(71);
  public static final Category DSC_PUBLIC_STATIC_ARRAY = Category.getInstance(72);
  public static final Category DSC_STATIC_FIELD = Category.getInstance(73);
  public static final Category DSC_STATIC_OBJECT_FIELD = Category.getInstance(74);  
  public static final Category DSC_STATIC_ARRAY = Category.getInstance(75);

  
  
  public static final int FIELD = 70;
  public static final int ARRAY = 71;
  public static final int OBJECT_FIELD = 72;
  
  

  /* Not needed yet */
  
//  private static Map<Integer,String> code2name = new HashMap<Integer, String>();
//  static {
//  }
//  
//  public static String toString(int code) {
//	  return code2name.get(code);
//  }
}
