package constructors;

import com.surelogic.Utility;

@Utility
public final class ThrowsError {
  private ThrowsError() {
    super();
    throw new AssertionError();
  }
}
