package test.enums;

import com.surelogic.Singleton;

@Singleton
public enum NoMembers_Fancy {
  ;
  
  public static final int x = 10;
  
  public static void foo() {}
}
