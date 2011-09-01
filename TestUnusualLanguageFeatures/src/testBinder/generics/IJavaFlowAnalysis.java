package testBinder.generics;

public interface IJavaFlowAnalysis<T, L extends Lattice<T>> {
	T getAfter(IRNode expr, WhichPort port);
}
