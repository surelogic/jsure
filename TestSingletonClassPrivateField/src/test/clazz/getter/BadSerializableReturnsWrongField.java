package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableReturnsWrongField implements Serializable {
  private static final BadSerializableReturnsWrongField INSTANCE = new BadSerializableReturnsWrongField();
  
  private static Integer count = new Integer(0);

  private transient int x;
  private transient int y;
  
  public static BadSerializableReturnsWrongField getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableReturnsWrongField() {
    super();
  }
  
  private Object readResolve() {
    return count;
  }
}
