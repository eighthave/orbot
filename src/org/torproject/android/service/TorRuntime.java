package org.torproject.android.service;

public interface TorRuntime {

	public void start ();
	
	public void stop ();
	
	public void initCallbacks (TorService torService);
	
}
