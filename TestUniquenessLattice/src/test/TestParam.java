package test;

import com.surelogic.Borrowed;
import com.surelogic.BorrowedInRegion;
import com.surelogic.Immutable;
import com.surelogic.ReadOnly;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestParam {
	// No annotations
	// ----------------------------------------------------------------------
	private void m006(Object o) { /* empty */ }

	
	
	// One annotation
	// ----------------------------------------------------------------------
	private void m001(@Borrowed Object o) { /* empty */ }
	
	private void m004(@ReadOnly Object o) { /* empty */ }
	
	private void m005(@Immutable Object o) { /* empty */ }
	
	private void m007(@Unique Object o) { /* empty */ }
	
	private void m008(@Unique(allowRead=true) Object o) { /* empty */ }
	
	
	
	// Two annotations
	// ----------------------------------------------------------------------
	private void m015(@Borrowed @ReadOnly Object o) { /* empty */ }
	
	private void m016(@Borrowed @Immutable Object o) { /* empty */ }
	
	private void m017(@Borrowed @Unique Object o) { /* empty */ }
	
	private void m018(@Borrowed @Unique(allowRead=true) Object o) { /* empty */ }
		

	private void m039(@ReadOnly @Immutable Object o) { /* empty */ }
	
	private void m040(@ReadOnly @Unique Object o) { /* empty */ }
	
	private void m041(@ReadOnly @Unique(allowRead=true) Object o) { /* empty */ }
	
	
	private void m046(@Immutable @Unique Object o) { /* empty */ }
	
	private void m047(@Immutable @Unique(allowRead=true) Object o) { /* empty */ }



	// Three annotations
	// ----------------------------------------------------------------------
	private void m072(@Borrowed @ReadOnly @Immutable Object o) { /* empty */ }
	
	private void m073(@Borrowed @ReadOnly @Unique Object o) { /* empty */ }
	
	private void m074(@Borrowed @ReadOnly @Unique(allowRead=true) Object o) { /* empty */ }

	
	private void m079(@Borrowed @Immutable @Unique Object o) { /* empty */ }
	
	private void m080(@Borrowed @Immutable @Unique(allowRead=true) Object o) { /* empty */ }

		
	private void m135(@ReadOnly @Immutable @Unique Object o) { /* empty */ }
	
	private void m136(@ReadOnly @Immutable @Unique(allowRead=true) Object o) { /* empty */ }
}
