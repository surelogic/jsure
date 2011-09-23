package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class GoodSerializable implements Serializable {
  public static final GoodSerializable INSTANCE = new GoodSerializable();
  
  private transient int x;
  private transient int y;
  
  private GoodSerializable() {
    super();
  }
  
  private Object readResolve() {
    return INSTANCE;
  }
}
