package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadNoGetter {
  private static final BadNoGetter INSTANCE = new BadNoGetter();
  
  private BadNoGetter() {
    super();
  }
}
