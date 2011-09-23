package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadTooManyFields {
  public static final BadTooManyFields INSTANCE = new BadTooManyFields();

  public static final BadTooManyFields X = null;

  private BadTooManyFields() {
    super();
  }
}
