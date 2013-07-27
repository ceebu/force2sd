package mobi.pruss.force2sd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

public final class Mover {
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error: No arguments");
			return;
		}
//		if (args[0].equalsIgnoreCase("test")) {
////			ActivityThread at = ActivityThread.currentActivityThread();
////			if (at == null)
////				at = ActivityThread.systemMain();
////			Context c = at.getSystemContext();
//			Class c_ActivityThread;
//			try {
//				System.out.println("Looper");
//				Looper.prepareMainLooper();
//				c_ActivityThread = Class.forName("android.app.ActivityThread");
//				System.out.println("aa");
//				Constructor cons = c_ActivityThread.getDeclaredConstructor();
//				cons.setAccessible(true);
//				Object at = cons.newInstance();
//				System.out.println("b");
//
//				Class c_ContextImpl = Class.forName("android.app.ContextImpl");
//				cons = c_ContextImpl.getDeclaredConstructor();
//				cons.setAccessible(true);
//				Object context = cons.newInstance();
//				System.out.println("bbc");
//				
//				Method getTopLevelResources = c_ActivityThread.getDeclaredMethod("getTopLevelResources", 
//						String.class, Class.forName("android.content.res.CompatibilityInfo"));
//				System.out.println("f");
//				getTopLevelResources.setAccessible(true);
//				System.out.println("g");
//				Resources r = (Resources)getTopLevelResources.invoke(at, "/data/local", (Object)null);
//				System.out.println("d");
//				
//				Method init = c_ContextImpl.getDeclaredMethod("init", Resources.class, c_ActivityThread);
//				init.setAccessible(true);
//				init.invoke(r, at);
//				
//				
////				Method attach = c_ActivityThread.getDeclaredMethod("attach",Boolean.TYPE);
////				System.out.println("c");
////				attach.setAccessible(true);
////				System.out.println("d");
////				attach.invoke(at, false);
////				System.out.println("e");
////				System.out.println("c");
////				Looper.loop();
////				System.out.println("d");
////				c_ActivityThread.getMethod("detach").invoke(at);
////				System.out.println("e");
//				
//				
////				Object at = c_ActivityThread.getMethod("currentActivityThread").invoke(null);
////				System.out.println("at null "+(at == null));
////				System.out.println("b");
////				if (at == null)
////					at = c_ActivityThread.getMethod("systemMain").invoke("null");
////				System.out.println("c");
////				System.out.println("at null "+(at == null));
//				Context c = (Context)context;//c_ActivityThread.getMethod("getSystemContext").invoke(at);
//				System.out.println("z");
//				int b =
//					android.provider.Settings.System.getInt(c.getContentResolver(),
//							android.provider.Settings.System.SCREEN_BRIGHTNESS,
//							-1);
//				System.out.println("brightness "+b);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			};
//		}
		if (args[0].equalsIgnoreCase("move")) {
			if (args.length < 3) {
				System.out.println("Error: Too few arguments");
				return;
			}
			int dest;
			try {
				dest = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException e) {
				System.out.println("Error: Invalid argument");
				return;
			}
			move(args[1],dest);			
		}
	}
	
	public static void move(String packageName, int dest) {
		PackageManagerWrapper wrap = new PackageManagerWrapper();
		wrap.movePackage(packageName, dest);
	}
	
}


