package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class GoodSimple {
  public static final GoodSimple INSTANCE = new GoodSimple();
  
  private GoodSimple() {
    super();
  }
}
