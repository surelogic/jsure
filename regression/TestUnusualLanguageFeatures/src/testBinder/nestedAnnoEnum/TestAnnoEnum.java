package testBinder.nestedAnnoEnum;

import static testBinder.nestedAnnoEnum.InterfaceAudience.LimitedPrivate.*;
import static testBinder.nestedAnnoEnum.InterfaceAudience.*;

public class TestAnnoEnum {
	Project getProject() {
		return null;
	}
	
	@Public void foo() {}
	@LimitedPrivate(Project.COMMON) void bar() {}
	@Private void baz() {}
}
