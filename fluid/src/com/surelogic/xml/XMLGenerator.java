package com.surelogic.xml;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Iterator;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRBooleanType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeType;
import edu.cmu.cs.fluid.ir.IRSequenceType;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.EmptyIterator;

/** This class is not thread safe!  Multiple instances of an XMLGenerator can 
 * not be run at the same time.
 * @author Spencer.Whitman
 */
public class XMLGenerator
{
	private static final SyntaxTreeInterface tree = JJNode.tree;
	private static String className = null;
	private static final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv(); 
	private static final IBinder binder = tEnv.getBinder();
	private static boolean generateShell;
	private static final int MAX_PARAMS = 3;

	/**
	 * Given the root of an AST, output the XML representation of the 
	 * annotations.
	 * @param root
	 */
	public static void generateXML(IRNode root, boolean shell) throws Exception {
		/** We're looking at a new package now; presumably we have not looked
		 * at a class in this package yet */
		className = null;
		generateShell = shell;

		String XML = "";
		String pkgName = VisitUtil.getPackageName(root);

		String XMLBody = generateChildren(root," ");
		HashMap<String,String> pkgMap = new HashMap<String,String>(1);
		pkgMap.put("name", pkgName);

		// Package node
		XML = XMLStringWriter.writeElement("package", pkgMap, XMLBody, "");

		/** Only print out XML if there is at least one promise */
		if(!XMLBody.equals("")) {
			if(className == null)
				reportError("No class name was set");

			
			BufferedWriter out = new BufferedWriter(PackageAccessor.writePackage(pkgName,className));
//			String dirName = PackageAccessor.packagePath(pkgName);
//
//			DirectoryFileLocator dir = new DirectoryFileLocator("C:\\\\Documents and Settings\\\\Spencer.Whitman\\\\Desktop\\\\XML Test stuff\\\\XML\\\\New Folder");
//			dir.setAndCreateDirPath(dirName);
//			
//			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(dir.openFileWrite(className + ".promises.xml")));
			
			out.write(XML);
			out.flush();
			out.close();
		}
	}

	public static String generateStringXML(IRNode root, boolean shell) {
		/** We're looking at a new package now; presumably we have not looked
		 * at a class in this package yet */
		className = null;
		generateShell = shell;

		String XML = "";
		String pkgName = VisitUtil.getPackageName(root);

		try {
			String XMLBody = generateChildren(root," ");
			HashMap<String,String> pkgMap = new HashMap<String,String>(1);
			pkgMap.put("name", pkgName);

			/** Only print out XML if there is at least one promise */
			if(!XMLBody.equals(""))
				XML = XMLStringWriter.writeElement("package", pkgMap, XMLBody, "");

			return XML;
		} catch (Exception e) {
			return "";
		}

	}

	/**
	 * @param node IRNode
	 * @return a String of the XML which represents the tree with root, node
	 */
	private static String generate(IRNode node, String space) throws Exception {

		Operator op = tree.getOperator(node);
		String elementName = "";
		HashMap<String, String> params = new HashMap<String,String>(MAX_PARAMS);

		if(op instanceof TypeDeclInterface) {
			// Class node
			/** Don't search nested classes */
			if(className != null)
				return "";
			String name = TypeDeclaration.getId(node);

			/** A name is required for a class element */
			checkParam(name, "Class", "name");

			elementName = "class";
			params.put("name", name); 
			className = name;
		} else if(op instanceof ConstructorDeclaration) {
			//Constructor node

			elementName = "constructor";
			params = putParams(ConstructorDeclaration.getParams(node), params);
			if(params.get("genericParams") != null) {
				params = putGenericType(ConstructorDeclaration.getTypes(node), params);
			}
		} else if(op instanceof MethodDeclaration) {
			//Method node

			String name = MethodDeclaration.getId(node);

			/** A name is required for a method element */
			checkParam(name, "Method", "name");			

			elementName = "method";

			params.put("name", name);
			params = putParams(MethodDeclaration.getParams(node), params);
			if(params.get("genericParams") != null) {
				params = putGenericType(MethodDeclaration.getTypes(node), params);
			}
		} else if(op instanceof FieldDeclaration) {
			//Field node
			String name = "";
			IRNode vds = FieldDeclaration.getVars(node);
			Iterator<IRNode> vdecls = VariableDeclarators.getVarIterator(vds);
			if(vdecls.hasNext()) {
				IRNode vdecl = vdecls.next();
				name = VariableDeclarator.getId(vdecl);
			}

			/** A name is required for a field element */
			checkParam(name, "Field", "name");

			elementName = "field";
			params.put("name", name);
		} else if(op instanceof ParameterDeclaration) {
			//Parameter node
			String name = ParameterDeclaration.getId(node);
			String index = Integer.toString(tree.childLocationIndex(node,
					tree.getLocation(node)));
			String gType = binder.getJavaType(ParameterDeclaration.getType(node)).getName();

			/** A name or index is required for a parameter element */
			try{
				checkParam(name, "Parameter", "name");
			} catch (Exception e) {
				checkParam(index, "Parameter", "index and name");
			}

			elementName = "parameter";
			if(!(name == null) && !"".equals(name))
				params.put("name", name);
			if(!(index == null) && !"".equals(index))
				params.put("index", index);
			if(!(gType == null) && !"".equals(gType)) {
				String eType = TypeErasure.calcTypeErasure(gType, tEnv);
				params.put("type", eType);

				if(!eType.equals(gType))
					params.put("genericType", gType);
			}
		} 

		/** 
		 * Return empty string if there are no promises attached to this node or
		 * any of its children
		 */
		if("".equals(elementName))
			return (generateShell ? "" : generatePromises(node,op,space)) 
			+ generateChildren(node, space);

		String promises = generatePromises(node,op,space + " ");
		String children = generateChildren(node, space + " ");

		if("".equals(promises) && "".equals(children) && !generateShell)
			return "";

		return XMLStringWriter.writeElement(elementName, params, 
				(generateShell ? "" : promises) + children, space);
	}

	
	private static HashMap<String,String> putGenericType(IRNode p, 
			HashMap<String, String> params) {
		
		String genericTypes = DebugUnparser.toString(p);
	
		if(!"".equals(genericTypes)) {
			params.put("genericType", genericTypes);
//			System.out.println("Type = " + genericTypes);
		}
		
		return params;
	}

	/** 
	 * @param p A parameter IRNode  
	 * @return A comma delimited String of the parameters or null if there are
	 * no parameters
	 */
	private static HashMap<String,String> putParams(IRNode p, 
			HashMap<String, String> params) {

		StringBuffer erasure = null;
		Iterator<IRNode> paramIter = Parameters.getFormalIterator(p);
		StringBuffer generic = null;
		boolean includeGeneric = false;

		while(paramIter.hasNext()) {
			String type = 
				binder.getJavaType(ParameterDeclaration.getType(paramIter.next())).getName();
			String eType = TypeErasure.calcTypeErasure(type, tEnv);

			if(!includeGeneric && !type.equals(eType))
				includeGeneric = true;

			if(generic == null)
				generic = new StringBuffer(type);
			else
				generic.append(", " + type);

			if(erasure == null)
				erasure = new StringBuffer(eType);
			else
				erasure.append(", " + eType);
		}

//		if(generic != null)
//			params.put("params", generic.toString());
//		
		if(erasure != null)
			params.put("params", erasure.toString());

		if(generic != null && includeGeneric)
			params.put("genericParams", generic.toString());

		return params;	
	}

	/**
	 * @param root IRNode
	 * @return a String of the XML representation for all the children of root
	 */
	private static String generateChildren(IRNode root, String space) throws Exception {
		Iterator<IRNode> kids = tree.children(root);
		StringBuffer s = new StringBuffer();

		while(kids.hasNext()) {
			s.append(generate(kids.next(),space));
		}
		return s.toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String generatePromises(IRNode node, Operator op, String space) {
		/** NOTE: <promise> tags to identify a "promise field" are depreciated */ 
		final Iterator<TokenInfo> tokenInfos = EmptyIterator.prototype();
			//PromiseFramework.getInstance().getTokenInfos(op);

		StringBuffer s = new StringBuffer();

		while(tokenInfos.hasNext()) {
			final TokenInfo info = tokenInfos.next();
			final IRType type = info.si.getType();

			/** Single keyword promise */
			if (type instanceof IRBooleanType) {				
				if (AbstractPromiseAnnotation.isX_filtered(info.si, node)) { 
					s.append(space +
							XMLStringWriter.writeXMLEmptyElement(info.token.toString()));
				}
				/** Single promise with contents */
			} else if (type instanceof IRNodeType) {					  
				IRNode sub = 
					AbstractPromiseAnnotation.getXorNull_filtered(info.si, node);

				if (sub != null) { 
					s.append(XMLStringWriter.writeXMLDataElement(info.token.toString(),
							DebugUnparser.toString(sub),space));
				}
			}
			/** Multiple promises with the same keyword */
			else if (type instanceof IRSequenceType) {
				final Iterator<IRNode> e = AbstractPromiseAnnotation.getEnum_filtered(info.si, node);

				while (e.hasNext()) {
					IRNode elt = e.next();
					if (elt != null) {
						s.append(XMLStringWriter.writeXMLDataElement(info.token.toString(),
								DebugUnparser.toString(elt),space));
					}
				}
			}			
			else {
				System.out.println("unknown type " + type);
			}
		}

		return s.toString();
	}

	private static void checkParam(String p, String name, String type) 
	throws Exception {
		if(p == null || "".equals(p))
			reportError(name + " node is missing required parameter " + type);
	}

	private static void reportError(String err) throws Exception {
		throw new Exception(err);
	}
}

