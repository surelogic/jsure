package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadManyConstructors {
  public static final BadManyConstructors INSTANCE = new BadManyConstructors();
  
  private BadManyConstructors() {
    this(10);
  }
  
  public BadManyConstructors(final int z) {
    super();
  }
}
