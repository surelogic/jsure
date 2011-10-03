/**
 * Test that "read effects on ReadOnly count as a read on shared" and that
 * "write effects are not allowed on ReadOnly."  The second should be checked
 * by the flow-analysis.
 */
package test.readOnlyRef;
