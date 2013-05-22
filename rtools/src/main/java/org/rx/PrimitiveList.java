package org.rx;

public class PrimitiveList {
	public static void main(String[] args) {
		int found = 0;
		boolean show_special = true, show_id = false;
		for(String arg: args)
			if(arg.equalsIgnoreCase("--special") || arg.equalsIgnoreCase("-s"))
				show_special = true;
			else if(arg.equalsIgnoreCase("--builtin") || arg.equalsIgnoreCase("-b"))
				show_special = false;
			else if(arg.equalsIgnoreCase("--id") || arg.equalsIgnoreCase("-i"))
				show_id = true;
			

		for(int i = 0; i < PrimitiveNames.primitive_names.length; i++)
			if(PrimitiveNames.is_special_primitive[i] == show_special)
				if(++found % 10 == 0)
					System.out.println(PrimitiveNames.primitive_names[i]+(show_id ? ":"+i : ""));
				else
					System.out.print(PrimitiveNames.primitive_names[i]+(show_id ? ":"+i : "")+" ");
		if(found % 10 != 0)
			System.out.println();
	}
}
