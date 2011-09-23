package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadFieldNotFinal {
  public static BadFieldNotFinal INSTANCE = new BadFieldNotFinal();
  
  private BadFieldNotFinal() {
    super();
  }
}
