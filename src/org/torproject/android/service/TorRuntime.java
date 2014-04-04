package org.torproject.android.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.freehaven.tor.control.ConfigEntry;

public interface TorRuntime {

	public void start ();
	
	public void stop ();
	
	public void initCallbacks (TorService torService);
	
	public String getInfo (String key) throws IOException;
	
	public List<ConfigEntry> getConfiguration (String key) throws IOException;
	
	public int getStatus ();
	
	public void resetConf(ArrayList<String> keys) throws IOException;
	
	public void setConfiguration(ArrayList<String> keys) throws IOException;
	
	public void signal (String key) throws IOException;
}
