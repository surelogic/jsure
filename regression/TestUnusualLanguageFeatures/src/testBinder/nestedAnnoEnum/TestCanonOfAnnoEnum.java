package testBinder.nestedAnnoEnum;

import static testBinder.nestedAnnoEnum.InterfaceAudience.LimitedPrivate.*;

public class TestCanonOfAnnoEnum {
	@InterfaceAudience.LimitedPrivate({Project.HDFS, Project.MAPREDUCE})
	void foo() {}
}
