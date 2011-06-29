package mobi.pruss.force2sd;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import mobi.pruss.force2sd.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TwoLineListItem;

public class Force2SD extends Activity {
	private Resources res;
    ListView listView; 
    PackageManager pm;
    Spinner spinner;
    
    private boolean fromSD() {
    	return spinner.getSelectedItemPosition() == 1;
    }

	private void fatalError(int title, int msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        
        Log.e("fatalError", (String) res.getText(title));

        alertDialog.setTitle(res.getText(title));
        alertDialog.setMessage(res.getText(msg));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.ok), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {finish();} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {finish();} });
        alertDialog.show();		
	}
	
	private void loadPrefs() {
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		
		spinner.setSelection(pref.getBoolean("fromSD", false) ? 1 : 0 );
	}
	
	private void savePrefs() {
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor ed = pref.edit();		
		
		ed.putBoolean("fromSD", fromSD());
		ed.commit();
	}

	private void populateList(ListView listView) {
        new PopulateListTask(this, pm, listView, fromSD() ).execute();
	}
	
	private void doMove(int pos) {
		String mode = fromSD() ? "f" : "s";
        MyApplicationInfo appInfo = (MyApplicationInfo) listView.getAdapter().getItem(pos);
        String fname = appInfo.publicSourceDir;      
		
		new MoveTask(this, listView, mode, fname, pos).execute();
	}
	
	private void move(final int pos) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        MyApplicationInfo appInfo = (MyApplicationInfo) listView.getAdapter().getItem(pos);
        
        alertDialog.setTitle(R.string.move_query_title);
        
        String message = 
        	(String)res.getText(R.string.move_query1) +
        	" " +
        	appInfo.getLabel() +
        	" " +
        	res.getText(fromSD()? R.string.move_query2_fromsd : R.string.move_query2_tosd);

        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.yes), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {doMove(pos);} });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		res.getText(R.string.no), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.show();		
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pm = getPackageManager();
        
        setContentView(R.layout.main);
        res = getResources();

        if (!Root.test()) {
        	fatalError(R.string.need_root_title, R.string.need_root);
        	return;
        }
        
        listView = (ListView) findViewById(R.id.movableApps);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				move(position);				
			}        	
        });
        
        spinner = (Spinner)findViewById(R.id.fromto);
        
        ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter.createFromResource(
        		this, R.array.fromto_options, android.R.layout.simple_spinner_item);        
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinAdapter);

        OnItemSelectedListener spinListen = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				savePrefs();
				populateList(listView);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        };
        spinner.setOnItemSelectedListener(spinListen);
        loadPrefs();
        populateList(listView);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
//    	root = new Root();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
//    	savePrefs();
//    	root.close();
    }
}

class MyApplicationInfo extends ApplicationInfo {
	private long size;
	private String label;
	
	public MyApplicationInfo(PackageManager pm, ApplicationInfo a) {
		super(a);
		
		File f = new File(publicSourceDir);
		size = f.length();
		
		CharSequence l = loadLabel(pm);
		if (l == null) {
			label = packageName;
		}
		else {
			label = (String)l;
		}
	}
	
	public long getSize() {
		return size;
	}

	public String getLabel() {
		return label;
	}
}

class PopulateListTask extends AsyncTask<Void, Void, List<MyApplicationInfo>> {
	final PackageManager pm;
	final Context	 context;
	final ListView listView;
	final ProgressBar progress;
	final boolean fromSD;
	
	PopulateListTask(Context c, PackageManager p, ListView l, boolean f) {
		context = c;
		pm		= p;
		listView = l;
		fromSD   = f;
		Log.v("pop","1");
		progress = (ProgressBar)((Activity)c).findViewById(R.id.progress);
	}
	
	private String sizeText(long size) {
		DecimalFormat df0 = new DecimalFormat("#"); 
		DecimalFormat df = new DecimalFormat("#.#"); 
		
		if (size<1024) {
			return ""+size+"B";
		}
		else if (size<10.05*1024) {
			return df.format(size/1024)+"kB";
		}
		else if (size<1023.5*1024l) {
			return df0.format(size/1024.)+"kB";
		}
		else if (size<9.5*1024l*1024l) {
			return df.format(size/(1024.*1024))+"MB";
		}
		else {
			return df0.format(size/(1024.*1024))+"MB";
		}
	}
	
	private boolean movable(ApplicationInfo a, boolean fromSD) {
		if (a.publicSourceDir == null ||
				0 != (a.flags & MyApplicationInfo.FLAG_SYSTEM))
			return false;
		
		if (fromSD) 
			return 0 != (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE);
		else
			return 0 == (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE);
	}

	@Override
	protected List<MyApplicationInfo> doInBackground(Void... c) {
		List<ApplicationInfo> list = 
			pm.getInstalledApplications(0);
		
		List<MyApplicationInfo> myList = new ArrayList<MyApplicationInfo>();
		
		for (int i = 0 ; i < list.size() ; i++) {
			if (movable(list.get(i), fromSD)) {
				MyApplicationInfo myAppInfo;
				myAppInfo = new MyApplicationInfo(pm, list.get(i));
				myList.add(myAppInfo);
			}
		}
		
		return myList;
	}
	
	@Override
	protected void onPreExecute() {
		listView.setVisibility(View.GONE);
		progress.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onPostExecute(final List<MyApplicationInfo> appInfo) {
		
		ArrayAdapter<MyApplicationInfo> appInfoAdapter = 
			new ArrayAdapter<MyApplicationInfo>(context, 
					R.layout.twoline, 
					appInfo) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View v;				
				
				if (convertView == null) {
	                v = View.inflate(context, R.layout.twoline, null);
	            }
				else {
					v = convertView;
				}

				((TextView)v.findViewById(R.id.text1))
					.setText(appInfo.get(position).getLabel());
				((TextView)v.findViewById(R.id.text2))
					.setText(sizeText(appInfo.get(position).getSize()));
				((TextView)v.findViewById(R.id.text2)).setGravity(Gravity.RIGHT);
				return v;
			}				
		};
		
		appInfoAdapter.sort(new
				 MyApplicationInfo.DisplayNameComparator(pm));
		
		listView.setAdapter(appInfoAdapter);
		progress.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);
	}
}


class MoveTask extends AsyncTask<Void, Void, Boolean> {
	final Context	 context;
	final ProgressBar progress;
	final String mode;
	final String fname;
	final int	  pos;
	final ListView listView;
	
	MoveTask(Context c, ListView l, String m, String f, int p) {
		context = c;
		mode	= m;
		fname	= f;
		pos		= p;
		listView = l;
		progress = (ProgressBar)((Activity)c).findViewById(R.id.progress);
	}
	
	@Override
	protected Boolean doInBackground(Void... c) {
		Root root = new Root();
		root.exec("pm install -r -"+mode+" \""+fname+"\"");
		root.close(true);
		return true;
	}
	
	@Override
	protected void onPreExecute() {
		progress.setVisibility(View.VISIBLE);
		listView.setVisibility(View.GONE);
	}
	
	@Override
	protected void onPostExecute(Boolean b) {
		File f = new File(fname);
		
		if (!f.exists()) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<MyApplicationInfo> a = (ArrayAdapter<MyApplicationInfo>) listView.getAdapter();
			a.remove(a.getItem(pos));
		}
		
		progress.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);
	}
}
