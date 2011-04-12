package constructors;

import com.surelogic.Utility;

@Utility
public final class TooManyConstructors {
  private TooManyConstructors() {
    super();
  }
  
  private TooManyConstructors(final int a) {
    super();
  }
}
