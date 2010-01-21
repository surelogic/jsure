package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

@RegionLock("LOCK is lock protects Instance")
public class C {
	public final Lock lock = new ReentrantLock();
	
	public int value;
}
