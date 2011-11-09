package test;

import com.surelogic.Borrowed;
import com.surelogic.BorrowedInRegion;
import com.surelogic.Immutable;
import com.surelogic.ReadOnly;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestFields {
	// No annotations
	// ----------------------------------------------------------------------
	// LEGAL
	private final Object f006 = null;

	
	
	// One annotation
	// ----------------------------------------------------------------------
	// LEGAL
	@Borrowed
	private final Object f001 = null;
	
	// LEGAL
	@BorrowedInRegion("Instance")
	private final Object f002 = null;

	// LEGAL
	@BorrowedInRegion("Instance into Instance")
	private final Object f003 = null;
	
	// LEGAL
	@ReadOnly
	private final Object f004 = null;
	
	// LEGAL
	@Immutable
	private final Object f005 = null;
	
	// LEGAL
	@Unique
	private final Object f007 = null;
	
	// LEGAL
	@Unique(allowRead=true)
	private final Object f008 = null;
	
	// LEGAL
	@UniqueInRegion("Instance")
	private final Object f009 = null;
	
	// LEGAL
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f010 = null;
	
	// LEGAL
	@UniqueInRegion("Instance into Instance")
	private final Object f011 = null;
	
	// LEGAL
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f012 = null;
	
	
	
	// Two annotations
	// ----------------------------------------------------------------------
	@Borrowed
	@BorrowedInRegion("Instance")
	private final Object f013 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	private final Object f014 = null;
	
	// LEGAL
	@Borrowed
	@ReadOnly
	private final Object f015 = null;
	
	@Borrowed
	@Immutable
	private final Object f016 = null;
	
	@Borrowed
	@Unique
	private final Object f017 = null;
	
	@Borrowed
	@Unique(allowRead=true)
	private final Object f018 = null;
	
	@Borrowed
	@UniqueInRegion("Instance")
	private final Object f019 = null;
	
	@Borrowed
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f020 = null;
	
	@Borrowed
	@UniqueInRegion("Instance into Instance")
	private final Object f021 = null;
	
	@Borrowed
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f022 = null;
	
	

	// LEGAL
	@BorrowedInRegion("Instance")
	@ReadOnly
	private final Object f023 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	private final Object f024 = null;
	
	@BorrowedInRegion("Instance")
	@Unique
	private final Object f025 = null;
	
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	private final Object f026 = null;
	
	@BorrowedInRegion("Instance")
	@UniqueInRegion("Instance")
	private final Object f027 = null;
	
	@BorrowedInRegion("Instance")
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f028 = null;
	
	@BorrowedInRegion("Instance")
	@UniqueInRegion("Instance into Instance")
	private final Object f029 = null;
	
	@BorrowedInRegion("Instance")
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f030 = null;
	
	

	// LEGAL
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	private final Object f031 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	private final Object f032 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique
	private final Object f033 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	private final Object f034 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion("Instance")
	private final Object f035 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f036 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion("Instance into Instance")
	private final Object f037 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f038 = null;
	
		
	@ReadOnly
	@Immutable
	private final Object f039 = null;
	
	@ReadOnly
	@Unique
	private final Object f040 = null;
	
	@ReadOnly
	@Unique(allowRead=true)
	private final Object f041 = null;
	
	@ReadOnly
	@UniqueInRegion("Instance")
	private final Object f042 = null;
	
	@ReadOnly
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f043 = null;
	
	@ReadOnly
	@UniqueInRegion("Instance into Instance")
	private final Object f044 = null;
	
	@ReadOnly
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f045 = null;
	
	
	@Immutable
	@Unique
	private final Object f046 = null;
	
	@Immutable
	@Unique(allowRead=true)
	private final Object f047 = null;
	
	@Immutable
	@UniqueInRegion("Instance")
	private final Object f048 = null;
	
	@Immutable
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f049 = null;
	
	@Immutable
	@UniqueInRegion("Instance into Instance")
	private final Object f050 = null;
	
	@Immutable
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f051 = null;
	
	
	@Unique
	@UniqueInRegion("Instance")
	private final Object f052 = null;
	
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f053 = null;
	
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f054 = null;
	
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f055 = null;
	
	
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f052b = null;
	
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f053b = null;
	
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f054b = null;
	
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f055b = null;



	// Three annotations
	// ----------------------------------------------------------------------
	@Borrowed
	@BorrowedInRegion("Instance") // bad
	@ReadOnly // Allowed
	private final Object f056 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@Immutable
	private final Object f057 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@Unique
	private final Object f058 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	private final Object f059 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@UniqueInRegion("Instance")
	private final Object f060 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f061 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@UniqueInRegion("Instance into Instance")
	private final Object f062 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance")
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f063 = null;


	@Borrowed
	@BorrowedInRegion("Instance into Instance") // bad
	@ReadOnly // Allowed
	private final Object f064 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	private final Object f065 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@Unique
	private final Object f066 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	private final Object f067 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion("Instance")
	private final Object f068 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f069 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion("Instance into Instance")
	private final Object f070 = null;
	
	@Borrowed
	@BorrowedInRegion("Instance into Instance")
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f071 = null;


	@Borrowed
	@ReadOnly // Allowed
	@Immutable
	private final Object f072 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@Unique
	private final Object f073 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@Unique(allowRead=true)
	private final Object f074 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@UniqueInRegion("Instance")
	private final Object f075 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f076 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@UniqueInRegion("Instance into Instance")
	private final Object f077 = null;
	
	@Borrowed
	@ReadOnly // Allowed
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f078 = null;


	@Borrowed
	@Immutable
	@Unique
	private final Object f079 = null;
	
	@Borrowed
	@Immutable
	@Unique(allowRead=true)
	private final Object f080 = null;
	
	@Borrowed
	@Immutable
	@UniqueInRegion("Instance")
	private final Object f081 = null;
	
	@Borrowed
	@Immutable
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f082 = null;
	
	@Borrowed
	@Immutable
	@UniqueInRegion("Instance into Instance")
	private final Object f083 = null;
	
	@Borrowed
	@Immutable
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f084 = null;


	@Borrowed
	@Unique
	@UniqueInRegion("Instance")
	private final Object f085 = null;
	
	@Borrowed
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f086 = null;
	
	@Borrowed
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f087 = null;
	
	@Borrowed
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f088 = null;


	@Borrowed
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f089 = null;
	
	@Borrowed
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f090 = null;
	
	@Borrowed
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f091 = null;
	
	@Borrowed
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f092 = null;
	
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@Immutable
	private final Object f093 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@Unique
	private final Object f094 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@Unique(allowRead=true)
	private final Object f095 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@UniqueInRegion("Instance")
	private final Object f096 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f097 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@UniqueInRegion("Instance into Instance")
	private final Object f098 = null;
	
	@BorrowedInRegion("Instance")
	@ReadOnly
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f099 = null;
	
	
	@BorrowedInRegion("Instance")
	@Immutable
	@Unique
	private final Object f100 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	@Unique(allowRead=true)
	private final Object f101 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	@UniqueInRegion("Instance")
	private final Object f102 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f103 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	@UniqueInRegion("Instance into Instance")
	private final Object f104 = null;
	
	@BorrowedInRegion("Instance")
	@Immutable
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f105 = null;
	
	
	@BorrowedInRegion("Instance")
	@Unique
	@UniqueInRegion("Instance")
	private final Object f106 = null;
	
	@BorrowedInRegion("Instance")
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f107 = null;
	
	@BorrowedInRegion("Instance")
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f108 = null;
	
	@BorrowedInRegion("Instance")
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f109 = null;
	
	
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f110 = null;
	
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f111 = null;
	
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f112 = null;
	
	@BorrowedInRegion("Instance")
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f113 = null;
	
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@Immutable
	private final Object f114 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@Unique
	private final Object f115 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@Unique(allowRead=true)
	private final Object f116 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@UniqueInRegion("Instance")
	private final Object f117 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f118 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@UniqueInRegion("Instance into Instance")
	private final Object f119 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@ReadOnly
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f120 = null;
	
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@Unique
	private final Object f121 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@Unique(allowRead=true)
	private final Object f122 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@UniqueInRegion("Instance")
	private final Object f123 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f124 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@UniqueInRegion("Instance into Instance")
	private final Object f125 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Immutable
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f126 = null;
	
	
	@BorrowedInRegion("Instance into Instance")
	@Unique
	@UniqueInRegion("Instance")
	private final Object f127 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f128 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f129 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f130 = null;
	
	
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f131 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f132 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f133 = null;
	
	@BorrowedInRegion("Instance into Instance")
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f134 = null;
	
	
	@ReadOnly
	@Immutable
	@Unique
	private final Object f135 = null;
	
	@ReadOnly
	@Immutable
	@Unique(allowRead=true)
	private final Object f136 = null;
	
	@ReadOnly
	@Immutable
	@UniqueInRegion("Instance")
	private final Object f137 = null;
	
	@ReadOnly
	@Immutable
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f138 = null;
	
	@ReadOnly
	@Immutable
	@UniqueInRegion("Instance into Instance")
	private final Object f139 = null;
	
	@ReadOnly
	@Immutable
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f140 = null;
	
	
	@ReadOnly
	@Unique
	@UniqueInRegion("Instance")
	private final Object f141 = null;
	
	@ReadOnly
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f142 = null;
	
	@ReadOnly
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f143 = null;
	
	@ReadOnly
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f144 = null;
	
	
	@ReadOnly
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f145 = null;
	
	@ReadOnly
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f146 = null;
	
	@ReadOnly
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f147 = null;
	
	@ReadOnly
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f148 = null;

	
	@Immutable
	@Unique
	@UniqueInRegion("Instance")
	private final Object f149 = null;
	
	@Immutable
	@Unique
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f150 = null;
	
	@Immutable
	@Unique
	@UniqueInRegion("Instance into Instance")
	private final Object f151 = null;
	
	@Immutable
	@Unique
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f152 = null;

	
	@Immutable
	@Unique(allowRead=true)
	@UniqueInRegion("Instance")
	private final Object f153 = null;
	
	@Immutable
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance", allowRead=true)
	private final Object f154 = null;
	
	@Immutable
	@Unique(allowRead=true)
	@UniqueInRegion("Instance into Instance")
	private final Object f155 = null;
	
	@Immutable
	@Unique(allowRead=true)
	@UniqueInRegion(value="Instance into Instance", allowRead=true)
	private final Object f156 = null;
}
