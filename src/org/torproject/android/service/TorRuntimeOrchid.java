package org.torproject.android.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.torproject.android.TorConstants;

import net.freehaven.tor.control.ConfigEntry;

import android.app.Application;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

public class TorRuntimeOrchid implements TorRuntime, TorConstants, TorServiceConstants {

	private TorClient mTorClient;
	private TorService mTorService;
	
	private int mStatus;
	
	@Override
	public void start() {
		mTorClient.start();
		mTorClient.enableSocksListener();
		mStatus = STATUS_CONNECTING;
	}

	@Override
	public void stop() {
		mTorClient.stop();
		mStatus = STATUS_OFF;
	}

	@Override
	public void initCallbacks(TorService torService) {
		
		mTorService = torService;

    	mTorService.setAppBinaryHome(mTorService.getDir("bin",Application.MODE_PRIVATE));
    	mTorService.setAppCacheHome(mTorService.getDir("data",Application.MODE_PRIVATE));
    	mTorService.setAppLibraryHome(new File(mTorService.getApplicationInfo().nativeLibraryDir));
		
		mTorClient = new TorClient();
		
		mTorClient.getConfig().setDataDirectory(mTorService.getAppCacheHome());
		mTorClient.getConfig().setWarnUnsafeSocks(true);
		
		mTorClient.addInitializationListener(new TorInitializationListener()
		{

			@Override
			public void initializationCompleted() {
		
				mStatus = STATUS_ON;
				
			}

			@Override
			public void initializationProgress(String arg0, int arg1) {
				mStatus = STATUS_CONNECTING;
			
				mTorService.logNotice(arg1 + "% " + arg0);
			}
			
		});

		
	}

	@Override
	public String getInfo(String key) throws IOException {
		return null;
	}

	@Override
	public List<ConfigEntry> getConfiguration(String key) throws IOException {
		return null;
	}

	@Override
	public int getStatus() {
		return mStatus;
	}

	@Override
	public void resetConf(ArrayList<String> keys) throws IOException {
		
	}

	@Override
	public void setConfiguration(ArrayList<String> keys) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void signal(String key) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
