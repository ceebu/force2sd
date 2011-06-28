package mobi.pruss.force2sd;

import java.io.DataOutputStream;
import android.util.Log;

public class Root {
	private DataOutputStream rootCommands;
	private Process rootShell;

	public Root() {
		try {
			rootShell = Runtime.getRuntime().exec("su");
			rootCommands = new DataOutputStream(rootShell.getOutputStream());
		}
		catch (Exception e) {
			rootCommands = null;
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
}
