package test.enums;

import com.surelogic.Singleton;

@Singleton
public enum OneMember_ClassElement {
  INSTANCE {
    @Override
    public void doStuff() {
      // does nothing
    }
  };
  
  public abstract void doStuff(); 
}
