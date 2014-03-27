package org.torproject.android.service;

import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;

public class TorRuntimeOrchid implements TorRuntime {

	private TorClient mTorClient;
	private TorService mTorService;
	
	@Override
	public void start() {
		mTorClient.start();
		mTorClient.enableSocksListener();
	}

	@Override
	public void stop() {
		mTorClient.stop();

	}

	@Override
	public void initCallbacks(TorService torService) {
		
		mTorService = torService;
		mTorClient = new TorClient();
		
		mTorClient.addInitializationListener(new TorInitializationListener()
		{

			@Override
			public void initializationCompleted() {
		
				
				
			}

			@Override
			public void initializationProgress(String arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
		});

		
	}

}
