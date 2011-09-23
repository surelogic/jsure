package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableReturnsNull implements Serializable {
  public static final BadSerializableReturnsNull INSTANCE = new BadSerializableReturnsNull();
  
  private transient int x;
  private transient int y;
  
  private BadSerializableReturnsNull() {
    super();
  }
  
  private Object readResolve() {
    return null;
  }
}
