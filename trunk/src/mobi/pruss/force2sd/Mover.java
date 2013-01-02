package mobi.pruss.force2sd;

public final class Mover {
	public static void main(String[] args) {
		System.out.println("Hello");
		
		if (args.length < 1) {
			System.out.println("Error: No arguments");
			return;
		}
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
