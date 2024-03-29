package mobi.pruss.force2sd;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;


public class Force2SD extends SherlockActivity {
	private Resources res;
    ListView[] listView = {null, null}; 
    PackageManager pm;
    Spinner spinner;
    int		mode;
    int     sort;
    static final int MENU_MOVE = 0;
    static final int MENU_UNINSTALL = 1;
    static final int COMMAND_MOVE = 1;
    static final int COMMAND_UNINSTALL = 2;
    static final int SORT_ALPHA = 0;
    static final int SORT_INC_SIZE = 1;
    static final int SORT_DEC_SIZE = 2;
    static final int NUM_SORTS = 3;
    static final String[] sortNames = { "alphabetical", "increasing size", "decreasing size" };
    boolean bulk;
	SharedPreferences options;

	static int limit;
    public int count = 0;
	private boolean initialized;
	private boolean setContext;
//    static boolean quickExit = false;
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v("Force2SD", "conf changed");
    }
    
	public static boolean match(String p, List<ResolveInfo> m) {
		if (m==null)
			return false;
		
		int length = m.size();
		
		for (int i=0; i<length; i++)
			if (p.equals(m.get(i).activityInfo.packageName))
				return true;
		
		return false;
	}
	

	private List<ResolveInfo> getWidgets() {
        Intent i = new Intent();
        i.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        return pm.queryBroadcastReceivers(i, 0);
    }
    
    private boolean isWidget(String p) {
    	return match(p, getWidgets());
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
//		quickExit = true;
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
		mode = options.getInt("mode", 0);
		sort = options.getInt("sort", SORT_ALPHA);
		count = options.getInt("count3", 0);
		spinner.setSelection(mode);
		viewListView();
	}
	
	private void savePrefs() {
		SharedPreferences.Editor ed = options.edit();		
		
		ed.putInt("mode", mode);
		ed.putInt("sort", sort);
		ed.commit();
	}
	
	@SuppressWarnings("unchecked")
	public void sortList(ListView list) {
		ArrayAdapter<MyApplicationInfo> adapter = 
			(ArrayAdapter<MyApplicationInfo>) list.getAdapter();
		
		switch(sort) {
		default: /* SORT_ALPHA */
//			adapter.sort(new MyApplicationInfo.DisplayNameComparator(pm));
//			break;
		case SORT_DEC_SIZE:
		case SORT_INC_SIZE:
			adapter.sort(new MyComparator(sort));
			break;
		}
	}

	private void populateList() {
		if (listView[mode].getAdapter() == null) {
			Log.v("Force2SD", "running populate task");
			new PopulateListTask(listView[mode], mode ).execute();
		}
		else { 
			Log.v("Force2SD", "sorting");
			sortList(listView[mode]);
		}
        updateTitle();
	}
	
	public void incCount() {
		if (limit > 0) {
			count++;
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor ed = pref.edit();		
			ed.putInt("count3", count);
			ed.commit();
		}
	}
	
	private void doMove(int pos) {
        MyApplicationInfo appInfo = (MyApplicationInfo) listView[mode].getAdapter().getItem(pos);

        if (!appInfo.getMovable()) {
			Toast.makeText(this, "This is not movable", Toast.LENGTH_SHORT).show();
			return;
        }
        
        if (mode == 0 && limit > 0) {
			if (count>=limit) {
				pleaseBuy(true);
				return;
			}
		}
		
        String fname = appInfo.publicSourceDir;
        Log.v("Force2SD", "flags: "+appInfo.flags);
        final String modes[] = {"s","f"};
        
        String commandOptions = "-"+modes[mode];
        
        String installer = pm.getInstallerPackageName(appInfo.packageName);
        
        if (installer != null && installer.length()>0) {
        	commandOptions = commandOptions + " -i \"" + installer + "\"";
        }

        new MoveTask(this, listView, mode, fname, pos, COMMAND_MOVE, 
        		Options.getMethod(options)).execute(
        		appInfo.packageName,
        		modes[mode],
        		installer
        		);		
	}
	
	private void doUninstall(int pos) {
        MyApplicationInfo appInfo = (MyApplicationInfo) listView[mode].getAdapter().getItem(pos);
        String fname = appInfo.packageName;      
		
		new MoveTask(this, listView, mode, fname, pos, COMMAND_UNINSTALL, 
				Options.getMethod(options)).execute(
				appInfo.packageName
		);		
	}
	
	private void move(final int pos) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        MyApplicationInfo appInfo = 
        	(MyApplicationInfo) listView[mode].getAdapter().getItem(pos);
        
        if (!appInfo.getMovable()) {
			Toast.makeText(this, "This is not movable", Toast.LENGTH_SHORT).show();
			return;
        }        
        
        String message;
        
        if (mode == 0 && isWidget(appInfo.packageName)) {
        	alertDialog.setTitle(R.string.move_query_title_widget);
            
            message = 
            	(String)res.getText(R.string.move_query1_widget) +
            	" " +
            	appInfo.getLabel() +
            	" " +
            	res.getText(R.string.move_query2_widget);
        }
        else {
	        alertDialog.setTitle(R.string.move_query_title);
	        
	        message = 
	        	(String)res.getText(R.string.move_query1) +
	        	" " +
	        	appInfo.getLabel() +
	        	" " +
	        	res.getText(mode==1? R.string.move_query2_fromsd : R.string.move_query2_tosd);
        }

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
	
	public void showUpdates() {		
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle("Force2SD Updates");
        
        alertDialog.setMessage(
        		"1.26:\n"+
        		"- Holo\n"+
        		"1.25:\n"+
        		"- Autoscan for orphan files on SD (can disable in MENU, Settings...)\n"+
        		"1.20:\n"+
        		"- New, probably better, move method (if it doesn't work for you, "+
        			"press MENU, Settings...)\n");
        
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"OK", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            } });
        alertDialog.show();				
	}	
	
	private void pleaseBuy(boolean expired) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle(expired?"Lite version expired":"Lite version");
        
        alertDialog.setMessage("The lite version of Force 2SD only lets you "+
        		"move five apps to SD or external storage, though it lets you "+
        		"move an unlimited number of apps back from SD.");
        
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"Get full version", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	MarketDetector.launch(Force2SD.this);
            } });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"Not yet", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {} });
        alertDialog.show();		
	}
	
	private int getVersion() {
		int v;
		try {
			v = getPackageManager()
				.getPackageInfo(getPackageName(),0).versionCode;
		} catch (NameNotFoundException e) {
			v = 0;
		}
		
		return v;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pm = getPackageManager();
        
        setContext = false;
        
        if (Build.VERSION.SDK_INT >= 17) {
        	PackageInfo pi;
			try {
				pi = pm.getPackageInfo("eu.chainfire.supersu",0);
	        	if (pi != null && pi.versionCode >= 190) {
	        		Log.v("Force2SD", "supersu "+pi.versionCode);
	        		setContext = true;
	        	}
			} catch (NameNotFoundException e) {
			}
        } 
        
        initialized = false;

        options = PreferenceManager.getDefaultSharedPreferences(this);
        
        bulk = options.getBoolean(Options.PREF_BULK, true);
        
        if (getPackageName().contains("lite")) {
        	limit = 5;
        }
        else {
        	limit = 0;
        }
        
        setContentView(R.layout.main);
        res = getResources();
        
		getSupportActionBar().show();
		getSupportActionBar().setDisplayShowTitleEnabled(false);


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
				if (mode != spinner.getSelectedItemPosition()) {
					mode = spinner.getSelectedItemPosition();
					savePrefs();
					viewListView();
					populateList();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        };
        spinner.setOnItemSelectedListener(spinListen);
        if (limit>0)
        	pleaseBuy(count >= limit);
        
        if (getVersion() != options.getInt("updateVersion", 0)) {
        	options.edit().putInt("updateVersion", getVersion()).commit();
        	showUpdates();
        }        
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
		TextView tv = (TextView)findViewById(R.id.space);
		tv.setText(title);
	}
    
//    @Override
//    public void onStart() {
//    	super.onStart();
//    	Log.v("Force2SD", "onStart");
//
//    	listView[0].setAdapter(null);
//    	listView[1].setAdapter(null);
//    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
//    	if (quickExit) { 
//    		finish();
//    		return;
//    	}
    	
    	Log.v("Force2SD", "onStart");
    	listView[0].setAdapter(null);
    	listView[1].setAdapter(null);
    	populateList();
    }
    
    @Override
    public void onPause() {
    	super.onPause();    	
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Operations");
		menu.add(0,MENU_MOVE,Menu.NONE,v.getId() == R.id.movableApps0 ? "Move to SD" : "Move from SD");
		menu.add(0,MENU_UNINSTALL,Menu.NONE,"Uninstall");
    }
    
    private void setSort(int s) {
    	sort = s;
    	if (listView[mode].getVisibility() != View.GONE) {
    		sortList(listView[mode]);
    		listView[mode].setSelection(0);
    		if (listView[1-mode] != null)
    			listView[1-mode].setSelection(0);
    	}
    	savePrefs();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.please_buy:
    		pleaseBuy(false);
    		return true;
    	case R.id.sort:
    		setSort((sort + 1) % NUM_SORTS);
    		Toast.makeText(this, sortNames[sort], Toast.LENGTH_SHORT).show();
    		return true;
		case R.id.options:
			startActivity(new Intent(this, Options.class));			
			return true;
    	default:
    		return false;
    	}
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
    	int position =  
    		((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
    	
    	switch(item.getItemId()) {
    	case MENU_UNINSTALL:
    		doUninstall(position);
    		return true;
    	case MENU_MOVE:
    		if (mode == 0 && 
    			isWidget(((MyApplicationInfo) listView[mode].getAdapter().getItem(position)).packageName)) {
    			move(position);
    		}
    		else {
    			doMove(position);
    		}
    		return true;
    	default:
    		return false;
    	}
    } 
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		if (!getPackageName().contains("lite")) {
			menu.findItem(R.id.please_buy).setVisible(false);
		}
	    return true;
	}

	public class PopulateListTask extends AsyncTask<Void, Integer, List<MyApplicationInfo>> {
		final PackageManager pm;
		final ListView listView;
		ProgressDialog progress;
		final Spinner  spinner;
		final int mode;
		private boolean haveRoot = false;
		private CleanApp cleanApp = null;
		
		PopulateListTask(ListView l, int m) {			
			pm		= Force2SD.this.pm;
			listView = l;
			mode     = m;
			spinner = (Spinner)Force2SD.this.findViewById(R.id.fromto);
		}
		
		public boolean matchIM(String p, List<InputMethodInfo> m) {
			if (m==null)
				return false;
			
			int length = m.size();
			
			for (int i=0; i<length; i++)
				if (p.equals(m.get(i).getPackageName()))
					return true;
			
			return false;
		}
		
		private boolean onRightStorage(ApplicationInfo a) {
			if (a.publicSourceDir == null ||
					0 != (a.flags & MyApplicationInfo.FLAG_SYSTEM))
				return false;
			
			if (mode == 0 && 0 != (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE))
				return false;
			if (mode == 1 && 0 == (a.flags & MyApplicationInfo.FLAG_EXTERNAL_STORAGE))
				return false;	
			
			return true;
		}
		
		private boolean movable(ApplicationInfo a, List<ResolveInfo> match1,
				List<InputMethodInfo> match2) {
			if (! a.sourceDir.equals(a.publicSourceDir)) 
				return false; // forward locked
			
			if (Force2SD.match(a.packageName, match1))
				return false;
			
			if (matchIM(a.packageName, match2))
				return false;
			
			return true;
		}
		
		private boolean initialize() {
			if (Force2SD.this.initialized) {
				haveRoot = true; 	
				return true;
			}
			
			Log.v("Force2SD", "initializing");
			
	        if (Build.VERSION.SDK_INT < 21 && Force2SD.this.options.getBoolean(Options.PREF_CHECK_ORPHANS, true)) {
	        	cleanApp = new CleanApp(Force2SD.this, Force2SD.this.setContext);
	        	if (cleanApp.valid) {
	    			Log.v("Force2SD", "did cleanApp scan");
	        		haveRoot = true; 
	        	}
	        	else {
	    			Log.v("Force2SD", "testing root");
	        		haveRoot = Root.test();
	        	}
	        }
	        else {
	        	haveRoot = Root.test();
	        }
	        
			Log.v("Force2SD", "root mode "+haveRoot);
	        Force2SD.this.initialized = haveRoot;
	        return haveRoot;	    
		}

		@Override
		protected List<MyApplicationInfo> doInBackground(Void... c) {
			if (!initialize())
				return null;
			
			List<ApplicationInfo> list = 
				pm.getInstalledApplications(0);
			
			List<ResolveInfo> launchers = null;
			List<InputMethodInfo> inputMethods = null;
			
			if (mode == 0) {
				Intent i = new Intent(); 
		        i.setAction(Intent.ACTION_MAIN); 
		        i.addCategory(Intent.CATEGORY_HOME);
		        launchers = pm.queryIntentActivities(i, 0);
		        
		        InputMethodManager mgr = (InputMethodManager)Force2SD.this.getSystemService(Context.INPUT_METHOD_SERVICE);
		        inputMethods = mgr.getInputMethodList();
			}
			
			List<MyApplicationInfo> myList = new ArrayList<MyApplicationInfo>();
			
			MyCache cache = new MyCache(MyCache.genFilename(Force2SD.this, "app_labels"));

			for (int i = list.size()-1 ; i >= 0 ; i--) {
				if (!onRightStorage(list.get(i))) {
					list.remove(i);
				}
			}
			
			for (int i = 0 ; i < list.size() ; i++) {
				publishProgress(i, list.size());
				 
				MyApplicationInfo myAppInfo;
				ApplicationInfo app = list.get(i);
				myAppInfo = new MyApplicationInfo(
							cache, pm, app);
				myAppInfo.setMovable(movable(app, launchers, inputMethods));
				myList.add(myAppInfo);
			}
			cache.commit();
			
			publishProgress(list.size(), list.size());

			return myList;
		}
		
		@Override
		protected void onPreExecute() {
			cleanApp = null;
			haveRoot = false;
			listView.setVisibility(View.GONE);
			spinner.setClickable(false);
			progress = new ProgressDialog(Force2SD.this);
			try {
				progress.setCancelable(false);
				progress.setMessage("Initializing...");
				progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progress.setIndeterminate(true);
				progress.show();
			} catch(Exception e) {}
		}
		
		protected void onProgressUpdate(Integer... p) {
			progress.setIndeterminate(false);
			progress.setMax(p[1]);
			progress.setProgress(p[0]);
		}
		
		@Override
		protected void onPostExecute(final List<MyApplicationInfo> appInfo) {
			try {
				progress.dismiss();
			} catch(Exception e) {};

			if (!haveRoot) {
				Force2SD.this.fatalError(R.string.need_root_title, R.string.need_root);
				return;
			}
			
			ArrayAdapter<MyApplicationInfo> appInfoAdapter = 
				new ArrayAdapter<MyApplicationInfo>(Force2SD.this, 
						R.layout.twoline, 
						appInfo) {

				public View getView(int position, View convertView, ViewGroup parent) {
					View v;				
					
					if (convertView == null) {
		                v = View.inflate(Force2SD.this, R.layout.twoline, null);
		            }
					else {
						v = convertView;
					}
					
					((TextView)v.findViewById(R.id.text1))
						.setText(appInfo.get(position).getLabel());
					
					MyApplicationInfo info = appInfo.get(position);
					String sizeInfo = Force2SD.sizeText(info.getSize());
					if (!info.getMovable()) {
						if (info.sourceDir.equals(info.publicSourceDir)) {
							sizeInfo += " [not movable]";
						}
						else {
							sizeInfo += " [not movable due to DRM]";						
						}
					}
					
					((TextView)v.findViewById(R.id.text2))
						.setText(sizeInfo);
					((TextView)v.findViewById(R.id.text2)).setGravity(Gravity.RIGHT);
					return v;
				}				
			};
			
			listView.setAdapter(appInfoAdapter);
			Force2SD.this.sortList(listView);		
			listView.setVisibility(View.VISIBLE);
			spinner.setClickable(true);
			Log.v("Force2SD", "handle orphans");
			if (cleanApp != null && cleanApp.valid && cleanApp.orphans.size() > 0) 
				cleanApp.handleOrphans();			
		}
	}
	

	class MoveTask extends AsyncTask<String, Void, Boolean> {
		final Context	 context;
		ProgressDialog progress;
		final int    mode;
		final String fname;
		final int	  pos;
		final ListView[] listView;
		final Spinner  spinner;
		final int	command;
		final int method;
		
		MoveTask(Context c, ListView[] l, int m, String f, int p, int cmd, int method) {
			context = c;
			mode	= m;
			fname	= f;
			pos		= p;
			listView = l;
			command	 = cmd;
			this.method = method;
			spinner = (Spinner)((Activity)c).findViewById(R.id.fromto);
		}
		
		@Override
		protected Boolean doInBackground(String... opt) {
			Root root = new Root(Force2SD.this.setContext);
			root.exec("export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
			Boolean success = false;
			switch(command) {
			case Force2SD.COMMAND_MOVE:
				String moveCommand;
				
				switch(method) {
				case Options.METHOD_NEW:
					moveCommand = "CLASSPATH=\""+context.getPackageCodePath()+"\" app_process . "+Mover.class.getName()+
						" move "+opt[0]+" "+(opt[1].equals("f") ? 1 : 2);
					break;
				default:
					moveCommand = "pm install -r "+"-"+opt[1];
					if (opt[2] != null && opt[2].length()>0) 
						moveCommand += " -i \"" + opt[2] + "\"";
					moveCommand += " \""+fname+"\"";
					break;
				}
				
				Log.v("Force2SD", "moveCommand");
				success = root.execOne("killall -9 "+opt[0]+"; "+moveCommand,"Success.*");
				break;
			case Force2SD.COMMAND_UNINSTALL:
				success = root.execOne("killall -9 "+opt[0]+"; pm uninstall "+fname,"Success.*");
				break;
			default:
				break;
			}
			root.close();
			return success;
		}
		
		@Override
		protected void onPreExecute() {
			String msg;
			switch(command) {
			case Force2SD.COMMAND_MOVE:
				msg = "Moving...";
				break;
			case Force2SD.COMMAND_UNINSTALL:
				msg = "Uninstalling...";
				break;
			default:
				msg = "Please Wait...";
				break;
			}

			progress = ProgressDialog.show(context, "", msg, true, false);
			 
			listView[mode].setVisibility(View.GONE);
			spinner.setClickable(false);
		}
		
		@Override
		protected void onPostExecute(Boolean success) {
			if (success != null && success) {
				@SuppressWarnings("unchecked")
				ArrayAdapter<MyApplicationInfo> a = 
					(ArrayAdapter<MyApplicationInfo>) listView[mode].getAdapter();
				if (a != null)
					a.remove(a.getItem(pos));
				listView[1-mode].setAdapter(null);
				Toast.makeText(context, command==Force2SD.COMMAND_MOVE?"Moved!":"Uninstalled!", Toast.LENGTH_SHORT).show();
				if (mode == 0 && command==Force2SD.COMMAND_MOVE) {
					((Force2SD)context).incCount();
				}
			}
			else {
				Toast.makeText(context, "Didn't work, maybe try another method?", Toast.LENGTH_LONG).show();
			}
			
			progress.dismiss();
			listView[mode].setVisibility(View.VISIBLE);
			spinner.setClickable(true);
			((Force2SD)context).updateTitle();
		}
	}
}

class MyComparator implements Comparator<MyApplicationInfo> {
	int sort;
	
	public MyComparator(int s) {
		sort = s;
	}
	
	public int compare(MyApplicationInfo a, MyApplicationInfo b) {
		int c;
		
		if (sort == Force2SD.SORT_ALPHA) {
			return a.getLabel().compareToIgnoreCase(b.getLabel());
		}
		
		if (a.getSize() < b.getSize()) {
			c = -1;
		}
		else if (b.getSize() < a.getSize()) {
			c = 1;
		}
		else {
			c = 0;
		}
		if (sort == Force2SD.SORT_DEC_SIZE)
			return -c;
		else
			return c;
	}
}

class MyApplicationInfo extends ApplicationInfo {
	private long size;
	private String label;
	private int versionCode;
	private boolean movable;
	
	String getKey() {
		return Locale.getDefault().toString() + ".v" + versionCode + "." + size + "." + packageName;
	}
	
	public MyApplicationInfo(MyCache cache, PackageManager pm, ApplicationInfo a) {
		super(a);
		
		File f = new File(publicSourceDir);
		size = f.length();

		try {
			versionCode = (pm.getPackageInfo(packageName, 0)).versionCode;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			versionCode = 0;
		}
		
		if (cache != null) {
			String cached = cache.lookup(getKey());
			if (cached != null) {
				label = cached;
				return;
			}
		}
		
		CharSequence l = pm.getApplicationLabel((ApplicationInfo)this);
		if (l == null) {
			label = packageName;
		}
		else {			
			label = (String)l;
			if (label.equals("Angry Birds")) {
				if(packageName.startsWith("com.rovio.angrybirdsrio")) {
					label = label + " Rio";
				}
				else if (packageName.startsWith("com.rovio.angrybirdsseasons")) {
					label = label + " Seasons";
				}
			}
			if (cache != null)
				cache.add(getKey(), label);
		}
	}
	
	public boolean getMovable() {
		return movable;
	}
	
	public void setMovable(boolean movable) {
		this.movable = movable;
	}
	
	public long getSize() {
		return size;
	}

	public String getLabel() {
		return label;
	}	
}
