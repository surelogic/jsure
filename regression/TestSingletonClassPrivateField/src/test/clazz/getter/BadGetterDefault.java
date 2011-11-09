package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadGetterDefault {
  private static final BadGetterDefault INSTANCE = new BadGetterDefault();
  
  static BadGetterDefault getInstance() {
    return INSTANCE;
  }
  
  private BadGetterDefault() {
    super();
  }
}
