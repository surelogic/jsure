package test_selfProtected;

import com.surelogic.ThreadSafe;

@ThreadSafe // 2010-10-28: Used to be okay, but now is rejected by the promise scrubber because C is not ThreadSafe
public class D extends C {

}
