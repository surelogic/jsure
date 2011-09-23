package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableFieldNotTransient implements Serializable {
  public static final BadSerializableFieldNotTransient INSTANCE = new BadSerializableFieldNotTransient();
  
  private transient int x;
  private int y;
  
  private BadSerializableFieldNotTransient() {
    super();
  }
  
  private Object readResolve() {
    return INSTANCE;
  }
}
