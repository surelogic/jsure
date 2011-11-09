package constructors;

import com.surelogic.Utility;

@Utility
public final class ThrowsWrong {
  private ThrowsWrong() {
    super();
    throw new Error();
  }
}
