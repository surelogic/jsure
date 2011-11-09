/**
 * Test that "read effects on immutable are discarded" and that
 * write effects on immutable are illegal.  The second of these
 * is already checked by the flow analysis.  The first needs to be
 * handled by effects analysis.
 */
package test.immutableRef.array.indirect;
