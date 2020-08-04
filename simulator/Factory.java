/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

/**
 * Class responsible for creating simulator objects
 */

package simulator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Factory<T> {

	protected TreeMap<String,Class<T> > classMap;  //mapping from name to class it represents

	public Factory() {
		classMap = new TreeMap<String,Class<T> >();
	}

	//Parse the input string from command line
	static public ArrayList<String> ParseInput(String input) {
		ArrayList<String> ret=new ArrayList<String>();
		
		//If the input has parameters
		if(input.indexOf('[')!=-1) {
			//Add the name
			ret.add(input.substring(0,input.indexOf('[')));

			String currentParam="";
			int bracketLevel=0;
			for(int i=input.indexOf('[')+1;i<input.length();i++) {
				//We are past the last bracket and still reading, error
				if(bracketLevel==-1) {
					System.err.println("Parse error on command line near: "+input);
					System.exit(3);
				}
				if(input.charAt(i) == '[') {
					currentParam+='[';
					bracketLevel++;
					continue;
				}
				if(input.charAt(i) == ']') {
					if(bracketLevel==0 && !currentParam.equals("")) {
						ret.add(currentParam);
						currentParam="";
					}
					currentParam+="]";
					bracketLevel--;
					continue;
				}

				if(bracketLevel==0) {
					if(input.charAt(i) == ',') {
						ret.add(currentParam);
						currentParam="";
						continue;
					}
				}
				currentParam+=input.charAt(i);
			}
		} else  //No parameters, just return name 
			ret.add(input);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public void registerClass(String name,Class<? extends T> cls) {
		classMap.put(name,(Class<T>)cls);
	}

	//Create a new object based on an input string
	public T Create(String input) {
		
		ArrayList<String> params = ParseInput(input);

		Class<T> cls = classMap.get(params.get(0));

		if(cls == null) {
			//Class is not registered, error.
			Main.error(params.get(0) + " is not recognized by Factory");
		}

		try{
		    Method make = cls.getDeclaredMethod("Make",new Class[] {ArrayList.class});
		    T ret = cls.cast(make.invoke(null,new Object[]{params}));
		    return ret;
		} catch(java.lang.NoSuchMethodException e){
			Main.error(params.get(0) + " does not have required Make method");
		} catch(java.lang.IllegalAccessException e){
			Main.error("Access problems with constructor for " +
				   params.get(0));
		} catch(java.lang.reflect.InvocationTargetException e){
		    Main.error("Make method for " + params.get(0) +
			       " threw an exception. Check code");
		} catch(java.lang.Exception e){
		    Main.error("Problems with "+params.get(0)+"; check code.");
		}

		//We never get here
		Main.error("Reached end of creation method without creating "
			   + params.get(0));
		return null;
	}

	public T CreateSimple(String input) {
		ArrayList<String> params = ParseInput(input);
		
		Class<T> cls = classMap.get(params.get(0));
	
		if(cls == null) {
			//Class is not registered, error.
			Main.error(params.get(0) +
				   " not recognized by Factory");
		}

		try{
			Constructor<T> cst = cls.getConstructor();
			T ret = cst.newInstance(new Object[]{});
			return ret;
		} catch(Exception e) {
			Main.error("Problem with simple factory creation on "+input);
		}

		return null;
	}

	public String getList(boolean warnings,int tabs) {
		String ret="";
		for(Map.Entry<String,Class<T> > ent : classMap.entrySet()) {

			for(int i=0;i<tabs;i++)
			    ret+="\t";
			try{
				if(ent.getKey().equals("MM")) {
					ret+="MM: shortcut for nearest[intersect,L1,Pairwise]\n";
					continue;
				}
				if(ent.getKey().equals("MC1x1")) {
					ret+="MC1x1: shortcut for nearest[free,LInf,LInf]\n";
					continue;
				}
				if(ent.getKey().equals("genAlg")) {
					ret+="genAlg: shortcut for nearest[free,L1,Pairwise]";
					continue;
				}
				if(ent.getKey().equals("lesscons")) {
					ret+="lesscons[<comparator>, <bf_times>]\n" +
							"\tcomparator: Comparator for backfilling\n" +
							"\tbf_times: number of times to backfill before compressing\n";
					continue;
				}
				if(ent.getKey().equals("restric")) {
					ret+="restric[<comparator>]\n" +
							"\tcomparator: Comparator for backfilling\n";
					continue;
				}

				if(ent.getKey().equals("max")) {
					ret+="max[<comparator>]\n" +
							"\tcomparator: Comparator for backfilling\n";
					continue;
				}

				Class<T> cl = ent.getValue();
				Method help = cl.getMethod("getParamHelp",new Class[] {});
				String prt = (String)help.invoke(null,new Object[]{});
				ret+=ent.getKey()+prt+"\n";
			} catch(Exception e){
				if(warnings)
					ret+=ent.getKey() + " (Warning: No help available)"+"\n";
				else
					ret+=ent.getKey()+"\n";	
			}
		}		
		return ret;
	}

	static public void argsAtLeast(int num,ArrayList<String> params){
		if(params.size()-1 < num){
			Main.error("Too few arguments, need at least "+num+ " for "+params.get(0));
		}
	}

	static public void argsAtMost(int num,ArrayList<String> params){
		if(params.size()-1 > num){
			Main.error("Too many arguments, need at most "+num+ " for "+params.get(0));
		}

	}

}
