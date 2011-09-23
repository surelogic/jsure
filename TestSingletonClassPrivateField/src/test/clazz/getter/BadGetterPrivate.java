package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadGetterPrivate {
  private static final BadGetterPrivate INSTANCE = new BadGetterPrivate();
  
  private static BadGetterPrivate getInstance() {
    return INSTANCE;
  }
  
  private BadGetterPrivate() {
    super();
  }
}
