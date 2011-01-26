package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentDropInfo {
	public static final boolean useInfo = true;
	
	private Collection<Info> dropInfo = Collections.emptyList();
	
	private PersistentDropInfo() {
		// Made to keep others from creating one
	}
	
	private static PersistentDropInfo instance = new PersistentDropInfo();
	
	public static PersistentDropInfo getInstance() {
		return instance;
	}
	
	public synchronized void setInfo(Collection<Info> info) {
		dropInfo = info;
	}
	
	public synchronized boolean dropsExist(Class<? extends Drop> type) {
		for(Info i : dropInfo) {
			if (i.isInstance(type)) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized <T extends IDropInfo, T2 extends Drop> Set<T> getDropsOfType(Class<T2> dropType) {
		if (useInfo) {
			if (!dropInfo.isEmpty()) {
				final Set<T> result = new HashSet<T>();			
				for(Info i : dropInfo) {
					if (i.isInstance(dropType)) {
						result.add((T) i);
					}
				}
				return result;
			}
			return Collections.emptySet();
		}
		return (Set<T>) Sea.getDefault().getDropsOfType(dropType);
	}
}
