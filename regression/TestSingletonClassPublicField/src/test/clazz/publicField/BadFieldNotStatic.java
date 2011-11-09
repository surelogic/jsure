package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadFieldNotStatic {
  public final BadFieldNotStatic INSTANCE = new BadFieldNotStatic();
  
  private BadFieldNotStatic() {
    super();
  }
}
