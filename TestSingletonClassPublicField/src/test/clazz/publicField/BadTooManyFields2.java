package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadTooManyFields2 {
  public static final BadTooManyFields2 INSTANCE = new BadTooManyFields2();

  private BadTooManyFields2 X = null;

  private BadTooManyFields2() {
    super();
  }
}
