package test.clazz.getter;

import com.surelogic.Singleton;

@Singleton
public final class BadTooManyFields {
  private static final BadTooManyFields INSTANCE = new BadTooManyFields();
  
  private static final BadTooManyFields X = null;
  
  public static BadTooManyFields getInstance() {
    return INSTANCE;
  }
  
  private BadTooManyFields() {
    super();
  }
}
