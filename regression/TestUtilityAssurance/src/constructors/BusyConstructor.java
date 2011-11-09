package constructors;

import com.surelogic.Utility;

@Utility
public final class BusyConstructor {
  private BusyConstructor() {
    super();
    Object o = new BusyConstructor();
  }
}
