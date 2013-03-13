package com.surelogic.dropsea.irfree;

import org.xml.sax.Attributes;

import com.surelogic.common.ref.*;
import com.surelogic.common.xml.Entity;

public class JSureEntity extends Entity {
	public JSureEntity(String name, Attributes a) {
		super(name, a);
	}
	
	public IJavaRef parsePersistedRef(String encode) {
		return JavaRef.parseEncodedForPersistence(encode);
	}	  
}
