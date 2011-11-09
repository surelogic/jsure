package test;

import com.surelogic.Borrowed;
import com.surelogic.BorrowedInRegion;
import com.surelogic.Immutable;
import com.surelogic.ReadOnly;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestReceiver {
	// No annotations
	// ----------------------------------------------------------------------
	private void m006() { /* empty */ }

	
	
	// One annotation
	// ----------------------------------------------------------------------
	@Borrowed("this")
	private void m001() { /* empty */ }
	
	@ReadOnly("this")
	private void m004() { /* empty */ }
	
	@Immutable("this")
	private void m005() { /* empty */ }
	
	@Unique("this")
	private void m007() { /* empty */ }
	
	@Unique(value="this", allowRead=true)
	private void m008() { /* empty */ }
	
	
	
	// Two annotations
	// ----------------------------------------------------------------------
	@Borrowed("this")
	@ReadOnly("this")
	private void m015() { /* empty */ }
	
	@Borrowed("this")
	@Immutable("this")
	private void m016() { /* empty */ }
	
	@Borrowed("this")
	@Unique("this")
	private void m017() { /* empty */ }
	
	@Borrowed("this")
	@Unique(value="this", allowRead=true)
	private void m018() { /* empty */ }
		

	@ReadOnly("this")
	@Immutable("this")
	private void m039() { /* empty */ }
	
	@ReadOnly("this")
	@Unique("this")
	private void m040() { /* empty */ }
	
	@ReadOnly("this")
	@Unique(value="this", allowRead=true)
	private void m041() { /* empty */ }
	
	
	@Immutable("this")
	@Unique("this")
	private void m046() { /* empty */ }
	
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	private void m047() { /* empty */ }



	// Three annotations
	// ----------------------------------------------------------------------
	@Borrowed("this")
	@ReadOnly("this")
	@Immutable("this")
	private void m072() { /* empty */ }
	
	@Borrowed("this")
	@ReadOnly("this")
	@Unique("this")
	private void m073() { /* empty */ }
	
	@Borrowed("this")
	@ReadOnly("this")
	@Unique(value="this", allowRead=true)
	private void m074() { /* empty */ }


	@Borrowed("this")
	@Immutable("this")
	@Unique("this")
	private void m079() { /* empty */ }
	
	@Borrowed("this")
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	private void m080() { /* empty */ }

	
	@ReadOnly("this")
	@Immutable("this")
	@Unique("this")
	private void m135() { /* empty */ }
	
	@ReadOnly("this")
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	private void m136() { /* empty */ }
}
