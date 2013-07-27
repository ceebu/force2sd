package mobi.pruss.force2sd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.R.string;
import android.app.Service;
import android.content.pm.IPackageMoveObserver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class PackageManagerWrapper {
	private Object iPackageManager;
	private Method movePackage;
	public static final int MOVE_SUCCEEDED = 1;

	@SuppressWarnings("rawtypes")
	public PackageManagerWrapper() {
		try {
			Class cServiceManager = Class.forName("android.os.ServiceManager");
			Method ServiceManager_getService;
			ServiceManager_getService = cServiceManager.getMethod("getService", 
					new Class[] { String.class });
			IBinder packageManager = (IBinder) ServiceManager_getService.invoke(null, 
					"package");
			Class cIPackageManager_Stub = Class.forName("android.content.pm.IPackageManager$Stub");
			Method IPackageManager_Stub_asInterface =
				cIPackageManager_Stub.getMethod("asInterface", IBinder.class);

			iPackageManager = IPackageManager_Stub_asInterface.invoke(null, packageManager);
			Class cIPackageManager = Class.forName("android.content.pm.IPackageManager");
			
			movePackage = cIPackageManager.getMethod("movePackage", 
					String.class, 
					Class.forName("android.content.pm.IPackageMoveObserver"),
					Integer.TYPE);
		} catch (InvocationTargetException e) {
			System.out.println("Failure: wrapper: "+e.getCause());
			iPackageManager = null;
		}
		catch (Exception e) {
			System.out.println("Failure: wrapper: "+e);
			iPackageManager = null;
			return;
		}		
	}
	
	public void movePackage(String packageName, int dest) {
		try {

			MyPackageMoveObserver observer = new MyPackageMoveObserver(); 

			System.out.println("Moving "+packageName+" to "+dest);
			movePackage.invoke(iPackageManager, packageName, observer, dest);
			
			synchronized(observer) {
				while(!observer.done) {
					try {
						observer.wait();
					}
					catch(InterruptedException e) {
						System.out.println("Failure: Interruption");
						return;
					}
				}
				if (observer.returnCode == MOVE_SUCCEEDED) {
					System.out.println("Success: Moved "+packageName+" to "+dest);
				}
				else {
					System.out.println("Failure: Error "+observer.returnCode);
					return;
				}
			}
		} catch (InvocationTargetException e) {
			System.out.println("Failure: "+e.getCause());
		} catch (Exception e) {
			System.out.println("Failure: "+e);
		}
	}

//	public class MoveObserverProxyListener implements java.lang.reflect.InvocationHandler {
//
//		@Override
//		public Object invoke(Object proxy, Method m, Object[] args)
//				throws Throwable {
//			if (m.getName().equals("packageMoved")) {
//				Integer mode = (Integer)args[0];
//				Log.v("PackageManagerWrapper", "result="+mode.intValue());
//			}
//			return null;
//		}		
//	}
	
	public class MyPackageMoveObserver extends android.content.pm.IPackageMoveObserver.Stub {
		boolean done = false;
		int returnCode = -1;

		@Override
		synchronized public void packageMoved(String packageName, int returnCode)
				throws RemoteException {
			
				this.returnCode = returnCode;
				this.done = true;

				notifyAll();
		}
		
	}
}
