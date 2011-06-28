package mobi.pruss.force2sd;

import java.util.List;
import mobi.pruss.force2sd.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Force2SD extends Activity {
	private Resources res;
    ListView listView; 
    PackageManager pm;

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

	private void populateList(ListView listView, String... rootCmds) {
        new PopulateListTask(this, pm, listView).execute(rootCmds);
	}
	private void doMove(ApplicationInfo appInfo) {
		populateList(listView, "pm install -r -s \""+appInfo.publicSourceDir+"\"");
	}
	
	private void move(final ApplicationInfo appInfo) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        
        alertDialog.setTitle(R.string.move_query_title);
        
        String message = 
        	(String)res.getText(R.string.move_query1) +
        	" " +
        	appInfo.loadLabel(getPackageManager()) +
        	" " +
        	res.getText(R.string.move_query2);

        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		res.getText(R.string.yes), 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {doMove(appInfo);} });
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
				move((ApplicationInfo) parent.getAdapter().getItem(position));				
			}        	
        });
        
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
//    	root.close();
    }
}

class PopulateListTask extends AsyncTask<String, Void, List<ApplicationInfo>> {
	final PackageManager pm;
	final Context	 context;
	final ListView listView;
	
	PopulateListTask(Context c, PackageManager p, ListView l) {
		context = c;
		pm		= p;
		listView = l;
	}
	
	private boolean movable(ApplicationInfo a) {
		String dir = a.publicSourceDir;
		
		if (dir == null)
			return false;
		
		if (!dir.startsWith("/data/app"))
			return false;
		
		// find some way of checking for widgets
		
		return true;
	}

	@Override
	protected List<ApplicationInfo> doInBackground(String... c) {
		if (c.length > 0) {
			Log.v("root shell",c[0]);
			Root root = new Root();
			root.exec(c[0]);
			root.close(true);
		}
		
		List<ApplicationInfo> list = 
			pm.getInstalledApplications(0);
		
		for (int i = list.size()-1; i >= 0; i--) {
			if (!movable(list.get(i))) {
				list.remove(i);
			}
		}
		
		return list;
	}
	
	@Override
	protected void onPreExecute() {
		listView.setVisibility(ListView.INVISIBLE);
	}
	
	@Override
	protected void onPostExecute(final List<ApplicationInfo> appInfo) {
		ArrayAdapter<ApplicationInfo> appInfoAdapter = 
			new ArrayAdapter<ApplicationInfo>(context, android.R.layout.simple_list_item_1, appInfo) {
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView v = (TextView)super.getView(position, convertView, parent);
				v.setText(appInfo.get(position).loadLabel(pm));
				return v;
			}				
		};
		
		appInfoAdapter.sort(new
				 ApplicationInfo.DisplayNameComparator(pm));
		
		listView.setAdapter(appInfoAdapter);
		listView.setVisibility(ListView.VISIBLE);
	}
}
