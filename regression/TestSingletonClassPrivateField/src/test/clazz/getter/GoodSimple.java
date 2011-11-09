package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class GoodSimple {
  private static final GoodSimple INSTANCE = new GoodSimple();
  
  public static GoodSimple getInstance() {
    return INSTANCE;
  }
  
  private GoodSimple() {
    super();
  }
}
