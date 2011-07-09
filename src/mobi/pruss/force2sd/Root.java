package mobi.pruss.force2sd;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;

import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;

	public boolean isValid() {
		return rootCommands != null;
	}

	public Root() {		
		try {
			rootShell = new ProcessBuilder() 
				.command("su")
				.redirectErrorStream(true)
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
			rootCommands.writeBytes(s + "\n");
			rootCommands.close();
			String line;
			while((line=br.readLine())!=null) {
				Log.v("Root", line);
				if (line.trim().matches(successMarker)) {
					br.close();
					Log.v("Root", successMarker);
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
			rootCommands.writeBytes(s + "\n");
			rootCommands.flush();
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
			DataOutputStream c = new DataOutputStream(p.getOutputStream());
			c.writeChars(cmd + "\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String s = br.readLine();
			
			if (s == null)
				s = "";
			
			if(p.waitFor() != 0) {
				return null;
			}
			return s;
		}
		catch (Exception e) {
			return null;
		}
	}	
}
