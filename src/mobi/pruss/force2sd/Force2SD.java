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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.text.Layout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class Force2SD extends Activity {
	private Resources res;
    ListView[] listView = {null, null}; 
    PackageManager pm;
    Spinner spinner;
    int		mode;
    static final int MENU_MOVE = 0;
    static final int MENU_UNINSTALL = 1;
    static final int COMMAND_MOVE = 1;
    static final int COMMAND_UNINSTALL = 2;
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    private void viewListView() {
    	if (listView[0].getVisibility() == View.GONE &&
    			listView[1].getVisibility() == View.GONE)
    		return;
    	if (listView[mode].getVisibility() != View.GONE)
    		return;
    	listView[1-mode].setVisibility(View.GONE);
    	listView[mode].setVisibility(View.VISIBLE);
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
		
		mode = pref.getInt("mode", 0);
		spinner.setSelection(mode);
		viewListView();
	}
	
	private void savePrefs() {
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor ed = pref.edit();		
		
		ed.putInt("mode", mode);
		ed.commit();
	}

	private void populateList() {
		if (listView[mode].getAdapter() == null) 
			new PopulateListTask(this, pm, listView[mode], mode ).execute();
        updateTitle();
	}
	
	private void doMove(int pos) {
        MyApplicationInfo appInfo = (MyApplicationInfo) listView[mode].getAdapter().getItem(pos);
        String fname = appInfo.publicSourceDir;      
		
		new MoveTask(this, listView, mode, fname, pos, COMMAND_MOVE).execute();
	}
	
	private void doUninstall(int pos) {
        MyApplicationInfo appInfo = (MyApplicationInfo) listView[mode].getAdapter().getItem(pos);
        String fname = appInfo.packageName;      
		
		new MoveTask(this, listView, mode, fname, pos, COMMAND_UNINSTALL).execute();		
	}
	
	private void move(final int pos) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        MyApplicationInfo appInfo = 
        	(MyApplicationInfo) listView[mode].getAdapter().getItem(pos);
        
        alertDialog.setTitle(R.string.move_query_title);
        
        String message = 
        	(String)res.getText(R.string.move_query1) +
        	" " +
        	appInfo.getLabel() +
        	" " +
        	res.getText(mode==1? R.string.move_query2_fromsd : R.string.move_query2_tosd);

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
        
        listView[0] = (ListView) findViewById(R.id.movableApps0);
        
        listView[0].setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				move(position);				
			}        	
        });
        registerForContextMenu(listView[0]);
        
        listView[1] = (ListView) findViewById(R.id.movableApps1);
        
        listView[1].setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				move(position);				
			}        	
        });
        registerForContextMenu(listView[1]);
        
        spinner = (Spinner)findViewById(R.id.fromto);
        
        ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter.createFromResource(
        		this, R.array.fromto_options, android.R.layout.simple_spinner_item);        
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinAdapter);
        loadPrefs();
        OnItemSelectedListener spinListen = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mode = spinner.getSelectedItemPosition();
				savePrefs();
				viewListView();
				populateList();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        };
        spinner.setOnItemSelectedListener(spinListen);
        populateList();
    }
	
	static public String sizeText(long size) {
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
	
	public void updateTitle() {
		StatFs stats = new StatFs("/data");
		long free = (long)stats.getAvailableBlocks() * (long)stats.getBlockSize();
		String title = sizeText(free) + " free in internal area";
		setTitle(title);
	}
    
    @Override
    public void onResume() {
    	super.onResume();
    	populateList();
//    	root = new Root();
    }
    
    @Override
    public void onPause() {
    	super.onPause();    	
    	listView[0].setAdapter(null);
    	listView[1].setAdapter(null);
//    	savePrefs();
//    	root.close();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Operations");
		menu.add(0,MENU_MOVE,Menu.NONE,"Move");
		menu.add(0,MENU_UNINSTALL,Menu.NONE,"Uninstall");
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	int position =  
    		((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
    	
    	switch(item.getItemId()) {
    	case MENU_UNINSTALL:
    		doUninstall(position);
    		return true;
    	case MENU_MOVE:
    		doMove(position);
    		return true;
    	default:
    		return false;
    	}
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
	final Spinner  spinner;
	final int mode;
	
	PopulateListTask(Context c, PackageManager p, ListView l, int m) {
		context = c;
		pm		= p;
		listView = l;
		mode     = m;
		progress = (ProgressBar)((Activity)c).findViewById(R.id.progress);
		spinner = (Spinner)((Activity)c).findViewById(R.id.fromto);
	}
	
	private boolean movable(ApplicationInfo a) {
		if (a.publicSourceDir == null ||
				0 != (a.flags & MyApplicationInfo.FLAG_SYSTEM))
			return false;
		
		if (mode == 0) 
			return 0 == (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE);
		else
			return 0 != (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE);
	}

	@Override
	protected List<MyApplicationInfo> doInBackground(Void... c) {
		List<ApplicationInfo> list = 
			pm.getInstalledApplications(0);
		
		List<MyApplicationInfo> myList = new ArrayList<MyApplicationInfo>();
		
		for (int i = 0 ; i < list.size() ; i++) {
			if (movable(list.get(i))) {
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
		spinner.setClickable(false);
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
					.setText(Force2SD.sizeText(appInfo.get(position).getSize()));
				((TextView)v.findViewById(R.id.text2)).setGravity(Gravity.RIGHT);
				return v;
			}				
		};
		
		appInfoAdapter.sort(new
				 MyApplicationInfo.DisplayNameComparator(pm));
		
		listView.setAdapter(appInfoAdapter);
		progress.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);
		spinner.setClickable(true);
	}
}

class MoveTask extends AsyncTask<Void, Void, Boolean> {
	final Context	 context;
	final ProgressBar progress;
	final int    mode;
	final String fname;
	final int	  pos;
	final ListView[] listView;
	final Spinner  spinner;
	final int	command;
    static final String MODES[] = {"s","f"};
	
	MoveTask(Context c, ListView[] l, int m, String f, int p, int cmd) {
		context = c;
		mode	= m;
		fname	= f;
		pos		= p;
		listView = l;
		command	 = cmd;
		progress = (ProgressBar)((Activity)c).findViewById(R.id.progress);
		spinner = (Spinner)((Activity)c).findViewById(R.id.fromto);
	}
	
	@Override
	protected Boolean doInBackground(Void... c) {
		Root root = new Root();
		Boolean success = false;
		switch(command) {
		case Force2SD.COMMAND_MOVE:
			success = root.execOne("pm install -r -"+MODES[mode]+" \""+fname+"\"","Success.*");
			break;
		case Force2SD.COMMAND_UNINSTALL:
			success = root.execOne("pm uninstall "+fname,"Success.*");
			break;
		default:
			break;
		}
		root.close();
		return success;
	}
	
	@Override
	protected void onPreExecute() {
		progress.setVisibility(View.VISIBLE);
		listView[mode].setVisibility(View.GONE);
		spinner.setClickable(false);
	}
	
	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<MyApplicationInfo> a = 
				(ArrayAdapter<MyApplicationInfo>) listView[mode].getAdapter();
			a.remove(a.getItem(pos));
			listView[1-mode].setAdapter(null);
			Toast.makeText(context, command==Force2SD.COMMAND_MOVE?"Moved!":"Uninstalled!", Toast.LENGTH_SHORT).show();
		}
		
		progress.setVisibility(View.GONE);
		listView[mode].setVisibility(View.VISIBLE);
		spinner.setClickable(true);
		((Force2SD)context).updateTitle();
	}
}
