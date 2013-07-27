package mobi.pruss.force2sd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleanApp {
	public ArrayList<String> orphans;
	int	orphanSize;
	boolean valid;
	Context context;
	
	private void deleteOrphans() {
		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add("su");
		cmds.add("-c");
		cmds.add("rm");
		
		for (String o:orphans) {
			cmds.add("/mnt/secure/asec/" + o);
		}
		
		try {
			Process p;
			String[] c = new String[cmds.size()];
			cmds.toArray(c);
			p = Runtime.getRuntime().exec(c);
			
			p.waitFor();
			Toast.makeText(context, "Orphan files deleted.", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
			Log.e("CleanApp", ""+e);
			Toast.makeText(context, "Error deleting.", Toast.LENGTH_SHORT).show();				
			}
	}
	
	private String getOrphanNames() {
		String out = "";
		
		for (String o:orphans) {
			out += "•" + o + "\n";
		}
		
		return out;
	}
	
	public void handleOrphans() {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        
        alertDialog.setTitle("Orphan app files found");
        alertDialog.setMessage("The following orphan files have been found, " +
        		"occupying a total space of " + orphanSize + "kB:\n" + 
        		getOrphanNames() + "\nDo you wish to delete them?" );
        		
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"Yes", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {deleteOrphans();} });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"No", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {} });
        alertDialog.show();		
	}
	
	public CleanApp(Context context) {		
		this.context = context;
		
		ArrayList<FileInfo> files = new ArrayList<FileInfo>();
		
		try {
			Process p;		
			Boolean havePackages = false;
			p = Runtime.getRuntime().exec(new String[] { "su", "-c", "ls -s /mnt/secure/asec ; cat /data/system/packages.xml" } );
			DataInputStream suOut = new DataInputStream(p.getInputStream());

			BufferedReader br = new BufferedReader(new InputStreamReader(suOut));
			String line;
			Pattern pat = Pattern.compile("([0-9]+) +([^ ]+)\\.asec");
			while((line=br.readLine())!=null) {
				if(line.startsWith("<?xml ")) {
					havePackages = true;
					break;
				}
				Matcher m = pat.matcher(line);
				line = line.trim();
				if (m.find()) {
					files.add(new FileInfo(m.group(2), Integer.parseInt(m.group(1))));
				}
			}
			
			if (!havePackages) {
				valid = false;
				return;
			}
			
			pat = Pattern.compile("codePath=\"/mnt/asec/([^/]+)/");
			while((line=br.readLine())!=null) {
				Matcher m = pat.matcher(line);
				line = line.trim();
				if (m.find()) {
					String f = m.group(1);
					for (int i=files.size()-1; i>=0; i--) {
						if (files.get(i).name.equals(f)) {
//							Log.v("CleanApp", "n:"+f);
							files.remove(i);
							break;
						}
					}
				}
			}
			
			orphanSize = 0;
			orphans = new ArrayList<String>();
			for (FileInfo f: files) {
				orphanSize += f.size;
				orphans.add(f.name+".asec");
//				Log.v("CleanApp", "o:"+f.name);
			}
			valid = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			valid = false;
		}
	}

	static class FileInfo {
		public String name;
		public int size;

		public FileInfo(String name, int size) {
			this.name = name;
			this.size = size;
		}
	}
}
