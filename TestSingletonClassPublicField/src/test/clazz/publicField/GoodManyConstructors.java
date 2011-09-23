package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class GoodManyConstructors {
  public static final GoodManyConstructors INSTANCE = new GoodManyConstructors();
  
  private GoodManyConstructors() {
    this(10);
  }
  
  private GoodManyConstructors(final int z) {
    super();
  }
}
