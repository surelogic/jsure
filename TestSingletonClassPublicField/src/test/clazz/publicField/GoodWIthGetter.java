package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class GoodWIthGetter {
  public static final GoodWIthGetter INSTANCE = new GoodWIthGetter();
  
  private GoodWIthGetter() {
    super();
  }
  
  /* Getter is useless, but harmless */
  public static final GoodWIthGetter getIntance() {
    return INSTANCE;
  }
}
