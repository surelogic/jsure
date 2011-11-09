package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class Bad {
  public static final Bad INSTANCE = new Bad();
  
  private Object o = new Bad();
  
  private Bad() {
    super();
  }
  
  public void x() {
    m(new Bad());
  }
  
  public void m(final Object o) {
    // do nothing
  }
}
