package mobi.pruss.force2sd;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;
	private boolean setContext;

	public boolean isValid() {
		return rootCommands != null;
	}

	public Root(boolean setContext) {
		this.setContext = setContext;
		try {
			ProcessBuilder pb = new ProcessBuilder();
			if (setContext)
				pb.command("su", "-cn", "u:r:system_app:s0");
			else
				pb.command("su");
			rootShell = pb.redirectErrorStream(true)
				.start();
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
		}
		catch (Exception e) {
			rootCommands = null;
		}
	}
	
	public boolean execOne(String s, String successMarker) {
		try {
			DataInputStream rootOutput = new DataInputStream(rootShell.getInputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(rootOutput));
			Log.v("Force2SD", "root execOne: "+s);
			rootCommands.writeBytes(s + "\n");
			rootCommands.close();
			if (successMarker == null) {
				br.close();
				return true;
			}
			Log.v("Force2SD", "<");
			String line;
			while((line=br.readLine())!=null) {
				Log.v("Force2SD", "root >"+ line);
				if (line.trim().matches(successMarker)) {
					br.close();
					return true;
				}
			}
			return false;
		}
		catch (Exception e) {
			Log.e("Error executing",s);
			return false;
		}
	}
	
	public static boolean test() {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream out = new DataOutputStream(p.getOutputStream());
			out.close();
			if(p.waitFor() != 0) {
				return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public void close() {
		close(false);
	}
	
	public void close(boolean wait) {
		if (rootCommands != null) {
			try {
				rootCommands.close();
				if (wait)
					rootShell.waitFor();
			}
			catch (Exception e) {
			}
			rootCommands = null;
		}

		if (rootShell != null) {
			try {
					rootShell.destroy();
			}
			catch (Exception e) {
			}
			rootShell = null;
		}
	}
	
	public void exec( String s ) {
		try {
			Log.v("Force2SD", "root exec " + s);
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
			Log.v("Force2SD", "root exec <");
		}
		catch (Exception e) {
			Log.e("Error executing",s);
		}
	}
	
	public static String execGetOneLine(String cmd) {
		try {
			Process p = new ProcessBuilder() 
			.command("su")
			.redirectErrorStream(true)
			.start();
			Log.v("Force2SD", "root execGetOneLine " + cmd);
			DataOutputStream c = new DataOutputStream(p.getOutputStream());
			c.writeChars(cmd + "\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String s = br.readLine();
			
			if (s == null)
				s = "";
			
			if(p.waitFor() != 0) {
				return null;
			}
			Log.v("Force2SD", "root >" + s);
			return s;
		}
		catch (Exception e) {
			return null;
		}
	}	
}
