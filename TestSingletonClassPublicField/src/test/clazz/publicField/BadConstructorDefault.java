package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadConstructorDefault {
  public static final BadConstructorDefault INSTANCE = new BadConstructorDefault();
  
  BadConstructorDefault() {
    super();
  }
}
