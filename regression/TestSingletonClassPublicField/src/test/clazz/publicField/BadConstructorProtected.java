package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadConstructorProtected {
  public static final BadConstructorProtected INSTANCE = new BadConstructorProtected();
  
  protected BadConstructorProtected() {
    super();
  }
}
