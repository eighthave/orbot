package org.torproject.android.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.TorControlConnection;

import org.torproject.android.R;
import org.torproject.android.TorConstants;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class TorRuntimeNative implements TorRuntime, TorServiceConstants, TorConstants {

    
    private File fileTorOrig;
    private File fileTorLink;
    
    private File filePrivoxy;
    private File fileObfsProxy;
    private File fileXtables;
    
    private File fileTorRc;
    
    private TorService mTorService;
    

	private static int currentStatus = STATUS_OFF;
		
	
	private static final int MAX_START_TRIES = 3;

	private TorControlConnection conn = null;
	private Socket torConnSocket = null;
	
	
	@Override
	public void start() {
		
	}

	@Override
	public void stop() {

	}

	@Override
	public void initCallbacks(TorService torService) {
		
		mTorService = torService;
		
	}

	private void initTorPathLinkAndPerms () throws Exception
	{
		fileTorLink = new File(mTorService.getAppBinaryHome(),"tor");
		fileTorLink.getParentFile().mkdirs();
		
		if (fileTorOrig.getAbsolutePath().startsWith("/mnt"))
		{
			mTorService.logNotice("app installed on external storage - copying binaries to internal");
			
			//can't execute binaries off the external storage, so copy them internal
			StringBuilder log = new StringBuilder();
	    	int errCode = -1;

	    	if (!fileTorLink.exists()||(fileTorOrig.length()!=fileTorLink.length()))
	    	{
	    		log = new StringBuilder();
	    		String[] cmd = { SHELL_CMD_RM + ' ' + fileTorLink.getAbsolutePath() };
	    		errCode = TorServiceUtils.doShellCommand(cmd,log, false, true);
	    		mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
	    		
	    		log = new StringBuilder();
	    		String[] cmd1 = { SHELL_CMD_CP + ' ' + fileTorOrig.getAbsolutePath() + ' ' + fileTorLink.getAbsolutePath() };
	    		errCode = TorServiceUtils.doShellCommand(cmd1,log, false, true);
	    		mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
	    	}
			enableBinExec(fileTorLink);
						
			File filePrivoxyLink = new File(mTorService.getAppBinaryHome(),"privoxy");
			if (!filePrivoxyLink.exists()||(filePrivoxy.length()!=filePrivoxyLink.length()))
			{
				log = new StringBuilder();
				String[] cmd = { SHELL_CMD_RM + ' ' + filePrivoxyLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
				
				log = new StringBuilder();
		    	String[] cmd1 = { SHELL_CMD_CP + ' ' + filePrivoxy.getAbsolutePath() + ' ' + filePrivoxyLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd1,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
			}
			filePrivoxy = filePrivoxyLink;			
			enableBinExec(filePrivoxy);
			
			File fileObfsProxyLink = new File(mTorService.getAppBinaryHome(),"obfsproxy");
			if (!fileObfsProxyLink.exists()||(fileObfsProxy.length()!=fileObfsProxyLink.length()))
			{

				log = new StringBuilder();
				String[] cmd1 = { SHELL_CMD_RM + ' ' + fileObfsProxyLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd1,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
				

				log = new StringBuilder();
		    	String[] cmd2 = { SHELL_CMD_CP + ' ' + fileObfsProxy.getAbsolutePath() + ' ' + fileObfsProxyLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd2,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
			}
			fileObfsProxy = fileObfsProxyLink;
			enableBinExec(fileObfsProxy);
			
			
			File fileXtablesLink = new File(mTorService.getAppBinaryHome(),"xtables");
			if (!fileXtablesLink.exists()||(fileXtables.length()!=fileXtablesLink.length()))
			{
				log = new StringBuilder();
				String[] cmd1 = { SHELL_CMD_RM + ' ' + fileXtablesLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd1,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
				
				log = new StringBuilder();
		    	String[] cmd2 = { SHELL_CMD_CP + ' ' + fileXtables.getAbsolutePath() + ' ' + fileXtablesLink.getAbsolutePath() };
				errCode = TorServiceUtils.doShellCommand(cmd2,log, false, true);
				mTorService.logNotice("link CP err=" + errCode + " out: " + log.toString());
			}
			fileXtables = fileXtablesLink;
			enableBinExec(fileXtables);
			
		}
		else
		{
		
			StringBuilder log = new StringBuilder();
	    	String[] cmdDel = { SHELL_CMD_RM + ' ' + fileTorLink.getAbsolutePath() };
			int errCode = TorServiceUtils.doShellCommand(cmdDel,log, false, true);
			mTorService.logNotice("link RM err=" + errCode + " out: " + log.toString());
	    	
	    	log = new StringBuilder();
	    	String[] cmd = { SHELL_CMD_LINK + ' ' + fileTorOrig.getAbsolutePath() + ' ' + fileTorLink.getAbsolutePath() };
			errCode = TorServiceUtils.doShellCommand(cmd,log, false, true);
			mTorService.logNotice("link LN err=" + errCode + " out: " + log.toString());
			
			enableBinExec(fileTorOrig);
			enableBinExec(fileTorLink);
			enableBinExec(filePrivoxy);
			enableBinExec(fileObfsProxy);
			enableBinExec(fileXtables);
			
		}
		
	}
    
    private void killTorProcess () throws Exception
    {
    	StringBuilder log = new StringBuilder();
    	int procId = -1;
    	
    	if (conn != null)
		{
    		mTorService.logNotice("Using control port to shutdown Tor");
    		
    		
			try {
				mTorService.logNotice("sending SHUTDOWN signal to Tor process");
				conn.shutdownTor("SHUTDOWN");
				
				
			} catch (Exception e) {
				Log.d(TAG,"error shutting down Tor via connection",e);
			}
			
			conn = null;
		}
    	
    	int killDelayMs = 300;
    	int maxTry = 5;
    	int currTry = 0;
    	
		while ((procId = TorServiceUtils.findProcessId(fileTorLink.getAbsolutePath())) != -1 && currTry++ < maxTry)
		{
			mTorService.sendCallbackStatusMessage ("Found existing orphan Tor process; Trying to shutdown now (device restart may be needed)...");
			
			mTorService.logNotice("Found Tor PID=" + procId + " - attempt to shutdown now...");
			
			String[] cmd = { SHELL_CMD_KILL + ' ' + procId + "" };
			TorServiceUtils.doShellCommand(cmd,log, mTorService.hasRoot(), false);
			try { Thread.sleep(killDelayMs); }
			catch (Exception e){}
		}
		
		if (procId == -1)
		{
			while ((procId = TorServiceUtils.findProcessId(filePrivoxy.getAbsolutePath())) != -1)
			{
				
				mTorService.logNotice("Found Privoxy PID=" + procId + " - killing now...");
				String[] cmd = { SHELL_CMD_KILL + ' ' + procId + "" };
	
				TorServiceUtils.doShellCommand(cmd,log, mTorService.hasRoot(), false);
				try { Thread.sleep(killDelayMs); }
				catch (Exception e){}
			}
			
			while ((procId = TorServiceUtils.findProcessId(fileObfsProxy.getAbsolutePath())) != -1)
			{
				
				mTorService.logNotice("Found ObfsProxy PID=" + procId + " - killing now...");
				String[] cmd = { SHELL_CMD_KILL + ' ' + procId + "" };
	
				TorServiceUtils.doShellCommand(cmd,log, mTorService.hasRoot(), false);
				try { Thread.sleep(killDelayMs); }
				catch (Exception e){}
			}
		}
		else
		{
			throw new Exception("*** Unable to kill existing Tor process. Please REBOOT your device. ***");
		}
    }
    
    private boolean findExistingProc () 
    {
    //	android.os.Debug.waitForDebugger();
    	
    	if (fileTorLink != null)
    	{
	    	try
	    	{
		    	int procId = TorServiceUtils.findProcessId(fileTorLink.getAbsolutePath());
		
		 		if (procId != -1)
		 		{
		 					 			
		            mTorService.logNotice (mTorService.getString(R.string.found_existing_tor_process));
		
		 				currentStatus = STATUS_CONNECTING;
						
		 				initControlConnection();
						
						currentStatus = STATUS_ON;
						
						return true;
		 			
		 		}
		 		
		 		return false;
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e(TAG,"error finding proc",e);
	    		return false;
	    	}
    	}
    	else
    		return false;
    }
    

	
	public void addEventHandler () throws IOException
	{
	       // We extend NullEventHandler so that we don't need to provide empty
	       // implementations for all the events we don't care about.
	       // ...
		mTorService.logNotice( "adding control port event handler");

		conn.setEventHandler(mTorService);
	    
		conn.setEvents(Arrays.asList(new String[]{
	          "ORCONN", "CIRC", "NOTICE", "WARN", "ERR","BW"}));
	      // conn.setEvents(Arrays.asList(new String[]{
	        //  "DEBUG", "INFO", "NOTICE", "WARN", "ERR"}));

		mTorService.logNotice( "SUCCESS added control port event handler");

	}
	
    private void initTorPaths () throws Exception
    {
    	mTorService.setAppBinaryHome(mTorService.getDir("bin",Application.MODE_PRIVATE));
    	mTorService.setAppCacheHome(mTorService.getDir("data",Application.MODE_PRIVATE));
    	mTorService.setAppLibraryHome(new File(mTorService.getApplicationInfo().nativeLibraryDir));
    	
    	if (!mTorService.getAppLibraryHome().exists())
    		mTorService.setAppLibraryHome(new File(mTorService.getApplicationInfo().dataDir + "/lib"));
		
    	fileTorOrig = new File(mTorService.getAppLibraryHome(), TOR_BINARY_ASSET_KEY);
    	
    	if (fileTorOrig.exists())
    	{
    		mTorService.logNotice ("Tor binary exists: " + fileTorOrig.getAbsolutePath());
    	}
    	else
    	{
    		mTorService.setAppLibraryHome(new File(mTorService.getApplicationInfo().dataDir + "/lib"));
    		fileTorOrig = new File(mTorService.getAppLibraryHome(), TOR_BINARY_ASSET_KEY);
    		
    		if (fileTorOrig.exists())
    			mTorService.logNotice ("Tor binary exists: " + fileTorOrig.getAbsolutePath());
    		else
    			throw new RuntimeException("Tor binary not installed");
    	}
    	
		filePrivoxy = new File(mTorService.getAppLibraryHome(), PRIVOXY_ASSET_KEY);
		if (filePrivoxy.exists())
			   	mTorService.logNotice ("Privoxy binary exists: " + filePrivoxy.getAbsolutePath());
		else
    		throw new RuntimeException("Privoxy binary not installed");
    	
		fileObfsProxy = new File(mTorService.getAppLibraryHome(), OBFSPROXY_ASSET_KEY);
		if (fileObfsProxy.exists())
    		mTorService.logNotice ("Obfsproxy binary exists: " + fileObfsProxy.getAbsolutePath());    	
		else
    		throw new RuntimeException("Obfsproxy binary not installed");
    	
		fileTorRc = new File(mTorService.getAppBinaryHome(), TORRC_ASSET_KEY);
		
		if (!fileTorRc.exists())
		{
			TorResourceInstaller installer = new TorResourceInstaller(mTorService, mTorService.getAppBinaryHome()); 
			boolean success = installer.installResources();
				
		}
		
		fileXtables = new File(mTorService.getAppLibraryHome(), IPTABLES_BINARY_ASSET_KEY);
		if (fileXtables.exists())
			mTorService.logNotice("Xtables binary exists: " + fileXtables.getAbsolutePath());
		
		initTorPathLinkAndPerms();
		
    }

    private boolean enableBinExec (File fileBin) throws Exception
    {
    	
    	mTorService.logNotice(fileBin.getName() + ": PRE: Is binary exec? " + fileBin.canExecute());
    	
		StringBuilder log = new StringBuilder ();
		
		mTorService.logNotice("(re)Setting permission on binary: " + fileBin.getAbsolutePath());
		String[] cmd1 = {SHELL_CMD_CHMOD + ' ' + CHMOD_EXE_VALUE + ' ' + fileBin.getAbsolutePath()};
		TorServiceUtils.doShellCommand(cmd1, log, false, true);
	
		mTorService.logNotice(fileBin.getName() + ": POST: Is binary exec? " + fileBin.canExecute());
	
		return fileBin.canExecute();
    }
    
    private void runTorShellCmd() throws Exception
    {
    	
    	if (!fileTorLink.exists())
    		throw new RuntimeException("Sorry Tor binary not installed properly: " + fileTorLink.getAbsolutePath());
    	
    	if (!fileTorLink.canExecute())
    		throw new RuntimeException("Sorry can't execute Tor: " + fileTorLink.getAbsolutePath());
    	
		SharedPreferences prefs = mTorService.getSharedPrefs(mTorService.getApplicationContext());

    	StringBuilder log = new StringBuilder();
		
		String torrcPath = new File(mTorService.getAppBinaryHome(), TORRC_ASSET_KEY).getAbsolutePath();
		
		boolean transProxyTethering = prefs.getBoolean("pref_transparent_tethering", false);
 		
		if (transProxyTethering)
		{
			torrcPath = new File(mTorService.getAppBinaryHome(), TORRC_TETHER_KEY).getAbsolutePath();
		}
		
		String[] torCmd = {
				"export HOME=" + mTorService.getAppBinaryHome().getAbsolutePath(),
				fileTorLink.getAbsolutePath() + " DataDirectory " + mTorService.getAppCacheHome().getAbsolutePath() + " -f " + torrcPath  + " || exit\n"
				};
		
		boolean runAsRootFalse = false;
		boolean waitForProcess = false;
		
		int procId = -1;
		int attempts = 0;

		int torRetryWaitTimeMS = 2000;
		
		while (procId == -1 && attempts < MAX_START_TRIES)
		{
			log = new StringBuilder();
			
			mTorService.sendCallbackStatusMessage(mTorService.getString(R.string.status_starting_up));
			
			TorServiceUtils.doShellCommand(torCmd, log, runAsRootFalse, waitForProcess);
		
			Thread.sleep(torRetryWaitTimeMS);
			
			procId = TorServiceUtils.findProcessId(fileTorLink.getAbsolutePath());
			
			if (procId == -1)
			{
				Thread.sleep(torRetryWaitTimeMS);
				procId = TorServiceUtils.findProcessId(fileTorOrig.getAbsolutePath());
				attempts++;
			}
			else
			{
				mTorService.logNotice("got tor proc id: " + procId);
				
			}
		}
		
		if (procId == -1)
		{

			mTorService.logNotice(log.toString());
			mTorService.sendCallbackStatusMessage(mTorService.getString(R.string.couldn_t_start_tor_process_));
			
			throw new Exception ("Unable to start Tor");
		}
		else
		{
		
			mTorService.logNotice("Tor process id=" + procId);
			
			//showToolbarNotification(mTorService.getString(R.string.status_starting_up), NOTIFY_ID, R.drawable.ic_stat_tor);
			
			
	    }
    }
    
    
    private void runPrivoxyShellCmd () throws Exception
    {
    	
    	mTorService.logNotice( "Starting privoxy process");
    	
			int privoxyProcId = TorServiceUtils.findProcessId(filePrivoxy.getAbsolutePath());

			StringBuilder log = null;
			
			int attempts = 0;
			
    		if (privoxyProcId == -1)
    		{
    			log = new StringBuilder();
    			
    			String privoxyConfigPath = new File(mTorService.getAppBinaryHome(), PRIVOXYCONFIG_ASSET_KEY).getAbsolutePath();
    			
    			String[] cmds = 
    			{ filePrivoxy.getAbsolutePath() + " " + privoxyConfigPath + " &" };
    			
    			mTorService.logNotice (cmds[0]); 
    			
    			boolean runAsRoot = false;
    			boolean waitFor = false;
    			
    			TorServiceUtils.doShellCommand(cmds, log, runAsRoot, waitFor);
    			
    			//wait one second to make sure it has started up
    			Thread.sleep(1000);
    			
    			while ((privoxyProcId = TorServiceUtils.findProcessId(filePrivoxy.getAbsolutePath())) == -1  && attempts < MAX_START_TRIES)
    			{
    				mTorService.logNotice("Couldn't find Privoxy process... retrying...\n" + log);
    				Thread.sleep(3000);
    				attempts++;
    			}
    			
    			mTorService.logNotice(log.toString());
    		}
    		
			mTorService.logNotice(mTorService.getString(R.string.privoxy_is_running_on_port_) + PORT_HTTP);
			
    		mTorService.logNotice("Privoxy process id=" + privoxyProcId);
			
    		
    		
    }
    
    private void initControlConnection () throws Exception, RuntimeException
	{
			while (conn == null)
			{
				try
				{
					mTorService.logNotice( "Connecting to control port: " + TOR_CONTROL_PORT);
					
					
					torConnSocket = new Socket(IP_LOCALHOST, TOR_CONTROL_PORT);
			        conn = TorControlConnection.getConnection(torConnSocket);
			        
			      //  conn.authenticate(new byte[0]); // See section 3.2

			        mTorService.logNotice( "SUCCESS connected to control port");
			        
			        File fileCookie = new File(mTorService.getAppCacheHome(), TOR_CONTROL_COOKIE);
			        
			        if (fileCookie.exists())
			        {
				        byte[] cookie = new byte[(int)fileCookie.length()];
				        new FileInputStream(fileCookie).read(cookie);
				        conn.authenticate(cookie);
				        		
				        mTorService.logNotice( "SUCCESS authenticated to control port");
				        
						mTorService.logNotice(mTorService.getString(R.string.tor_process_starting) + ' ' + mTorService.getString(R.string.tor_process_complete));
	
						addEventHandler();
				        
			        }
			        
			        break; //don't need to retry
				}
				catch (Exception ce)
				{
					conn = null;
					Log.d(TAG,"Attempt: Error connecting to control port: " + ce.getLocalizedMessage(),ce);
					
					mTorService.sendCallbackStatusMessage(mTorService.getString(R.string.tor_process_waiting));

					Thread.sleep(3000);
										
				}	
			}
		
		

	}

	@Override
	public String getInfo(String key) throws IOException {
		
		return conn.getInfo(key);
	}

	@Override
	public List<ConfigEntry> getConfiguration(String key) throws IOException {

		return conn.getConf(key);
	}

	@Override
	public int getStatus() {
		return 0;
	}


	@Override
	public void resetConf(ArrayList<String> keys) throws IOException {
		conn.resetConf(keys);
	}

	@Override
	public void signal(String key) throws IOException {
		conn.signal(key);
	}

	@Override
	public void setConfiguration(ArrayList<String> keys) throws IOException {
		conn.setConf(keys);
	}

}
