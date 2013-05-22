package org.rx.rtrace.util;



public class CommandLineUtils {

	private static final CharacterEscaper ARGUMENT_ESCAPER =
		new CharacterEscaper(
				new char[]{'\\', ' ', '\t', '\n'},
				new char[]{'\\', '_', 't', 'n'},
				'\\');
	
	public static String[] splitArguments(String argsString) {
		argsString = StringUtils.strip(argsString, ' ');
		if (argsString.isEmpty())
			return new String[]{};

		String[] args = argsString.split(" ");
		for (int argInd = 0; argInd < args.length; ++argInd) {
			args[argInd] = ARGUMENT_ESCAPER.unescape(args[argInd]);
//			List<Integer> escapedInds = new ArrayList<Integer>();
//			args[argInd] = ARGUMENT_ESCAPER.unescape(args[argInd], escapedInds);
//			if ((escapedInds.isEmpty() || escapedInds.get(0) != 0) &&
//					args[argInd].startsWith("'")) {
//				assert(args[argInd].endsWith("'") &&
//						(escapedInds.isEmpty() ||
//								escapedInds.get(escapedInds.size() - 1) != 0));
//				args[argInd] = args[argInd].substring(1,
//						args[argInd].length() - 1);
//			}
		}
		return args;
	}

	public static String joinArguments(String[] args) {
		if (args.length == 0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(ARGUMENT_ESCAPER.escape(arg));
		}
		return sb.toString();
	}
	
	public static void checkCommandLineArgs(String[] args) {
		for (String arg : args) {
			if (arg == null)
				throw new IllegalArgumentException(
						"Command-line argument cannot be null");
			if (arg.isEmpty())
				throw new IllegalArgumentException(
						"Command-line argument must be non-empty");
		}
	}
	
	private CommandLineUtils() {
	}

}
