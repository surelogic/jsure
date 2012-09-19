package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public interface IDropSeparator<K> {
	public K makeKey(IDrop d);
}
