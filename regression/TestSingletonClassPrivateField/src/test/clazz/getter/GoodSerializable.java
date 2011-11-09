package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class GoodSerializable implements Serializable {
  private static final GoodSerializable INSTANCE = new GoodSerializable();
  
  private transient int x;
  private transient int y;
  
  public static GoodSerializable getInstance() {
    return INSTANCE;
  }
  
  private GoodSerializable() {
    super();
  }
  
  private Object readResolve() {
    return INSTANCE;
  }
}
