package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public class BadNotFinal {
  public static BadNotFinal INSTANCE = new BadNotFinal();
  
  private BadNotFinal() {
    super();
  }
}
