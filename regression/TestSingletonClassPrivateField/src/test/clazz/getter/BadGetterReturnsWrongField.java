package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadGetterReturnsWrongField {
  private static final BadGetterReturnsWrongField INSTANCE = new BadGetterReturnsWrongField();
  
  public static BadGetterReturnsWrongField getInstance() {
    // right type, static final field, but from wrong class
    return Other.OTHER_INSTANCE;
  }
  
  private BadGetterReturnsWrongField() {
    super();
  }
}

class Other {
  public static final BadGetterReturnsWrongField OTHER_INSTANCE = BadGetterReturnsWrongField.getInstance();
}