package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableReturnsWrongField implements Serializable {
  public static final BadSerializableReturnsWrongField INSTANCE = new BadSerializableReturnsWrongField();
  
  private static Integer count = new Integer(0);

  private transient int x;
  private transient int y;
  
  private BadSerializableReturnsWrongField() {
    super();
  }
  
  private Object readResolve() {
    return count;
  }
}
