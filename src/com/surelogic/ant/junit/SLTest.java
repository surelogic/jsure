package com.surelogic.ant.junit;

import junit.framework.Test;

public interface SLTest extends Test {
  String getClassName();
  String getTestName();
}
