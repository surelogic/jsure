package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadGetterProtected {
  private static final BadGetterProtected INSTANCE = new BadGetterProtected();
  
  protected static BadGetterProtected getInstance() {
    return INSTANCE;
  }
  
  private BadGetterProtected() {
    super();
  }
}
