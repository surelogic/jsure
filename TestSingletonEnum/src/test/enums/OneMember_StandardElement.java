package test.enums;

import com.surelogic.Singleton;

@Singleton
public enum OneMember_StandardElement {
  INSTANCE(1);
  
  private OneMember_StandardElement(int x) {
    // do nothing
  }
}
