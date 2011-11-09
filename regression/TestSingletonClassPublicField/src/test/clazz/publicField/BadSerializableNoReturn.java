package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableNoReturn implements Serializable {
  public static final BadSerializableNoReturn INSTANCE = new BadSerializableNoReturn();
  
  private transient int x;
  private transient int y;
  
  private BadSerializableNoReturn() {
    super();
  }
  
  private Object readResolve() {
    throw new RuntimeException();
  }
}
