package com.surelogic.analysis.concurrency.model;

/**
 * Place holder interface for lock implementations that are not named.
 * An explicit distinction from {@link NamedLockImplementation}.  Useful
 * for type parameterizations.
 */
public interface UnnamedLockImplementation extends LockImplementation {
  // Nothing specific here
}
