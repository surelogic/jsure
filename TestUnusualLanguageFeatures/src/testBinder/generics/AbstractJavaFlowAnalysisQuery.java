package testBinder.generics;

public abstract class AbstractJavaFlowAnalysisQuery<T1, R, T, L extends Lattice<T>> {

	public AbstractJavaFlowAnalysisQuery(
			IThunk<? extends IJavaFlowAnalysis<T, L>> thunk) {
		// TODO Auto-generated constructor stub
	}

	protected abstract R getBottomReturningResult(L lattice, IRNode expr);

	protected abstract R getEvaluatedAnalysisResult(IJavaFlowAnalysis<T, L> analysis,
			L lattice, IRNode expr);

}
