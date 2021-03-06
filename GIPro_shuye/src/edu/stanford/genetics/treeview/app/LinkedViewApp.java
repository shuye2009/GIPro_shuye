/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: LinkedViewApp.java,v $
 * $Revision: 1.34 $
 * $Date: 2010-05-02 13:55:11 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER */
package edu.stanford.genetics.treeview.app;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LinkedViewFrame;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.TreeViewApp;
import edu.stanford.genetics.treeview.Util;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;
import edu.stanford.genetics.treeview.core.PluginManager;


/**
 *  Main class of LinkedView application. 
 * Mostly manages windows, and communication between windows, as well as communication between them.
 *
 *There are two differences between this class and the TreeViewApp
 * - which <code>ViewFrame</code> they use. 
 *   <code>LinkedViewApp</code> uses <code>LinkedViewFrame</code>.
 * - LinkedViewApp scans for plugins explicitly, TreeViewApp doesn't anymore
 * 
 * @author     Alok Saldanha <alok@genome.stanford.edu>
 * @version    $Revision: 1.34 $ $Date: 2010-05-02 13:55:11 $
 */
public class LinkedViewApp extends TreeViewApp {
	/**  Constructor for the LinkedViewApp object */
	//			"edu.stanford.genetics.treeview.plugin.scatterview.ScatterplotFactory"
	//"edu.stanford.genetics.treeview.plugin.treeanno.GeneAnnoFactory",
	//"edu.stanford.genetics.treeview.plugin.treeanno.ArrayAnnoFactory"
	//	"edu.stanford.genetics.treeview.plugin.karyoview.KaryoscopeFactory"
	public LinkedViewApp() {
		super();// does not call XmlConfig constructor
		//scanForPlugins();
	}
	/**
	* Constructor for the TreeViewApp object
	* takes configuration from the passed in XmlConfig.
	*/
	public LinkedViewApp(XmlConfig xmlConfig) {
		super(xmlConfig, false);
		//scanForPlugins();
	}
	
	private void scanForPlugins() {
		URL fileURL = getCodeBase();
		//String dir = Util.URLtoFilePath(fileURL.getPath()+"/plugins");
                String dir = Util.URLtoFilePath(fileURL.getPath());
		File[] files = PluginManager.getPluginManager().readdir(dir);
		if (files == null) {
			LogBuffer.println("Directory "+dir+" returned null");
			File f_currdir = new File(".");
			try {
				dir = f_currdir.getCanonicalPath() + File.separator +"plugins" + File.separator;
				LogBuffer.println("failing over to "+dir);
				files = PluginManager.getPluginManager().readdir(dir);
				if (files != null) {
					setCodeBase(f_currdir.toURI().toURL());
				}
			} catch (IOException e1) {
				// this might happen when the dir is bad.
				e1.printStackTrace();
			}
		}
		if (files == null || files.length == 0) {
			LogBuffer.println("Directory "+dir+" contains no plugins");
		} else {
			PluginManager.getPluginManager().loadPlugins(files, false);
		}
		PluginManager.getPluginManager().pluginAssignConfigNodes(getGlobalConfig().getNode("Plugins"));
	}
	
	private void dealWithRegistration() {
		ConfigNode node = getGlobalConfig().getNode("Registration");
		if (node != null) {
			try {
				edu.stanford.genetics.treeview.reg.RegEngine.verify(node);
				getGlobalConfig().store();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "registration error "+e);
				LogBuffer.println("registration error "+e);
				e.printStackTrace();
			}
		}		
	}
	
	/* inherit description */
	public ViewFrame openNew() {
		// setup toplevel
		LinkedViewFrame tvFrame  =
				new LinkedViewFrame(this);
		tvFrame.addWindowListener(this);
		return tvFrame;
	}


	/* inherit description */
	public ViewFrame openNew(FileSet fileSet) throws LoadException {
		// setup toplevel
		LinkedViewFrame tvFrame  =
				new LinkedViewFrame(this);
		try {
			tvFrame.loadFileSet(fileSet);
			tvFrame.setLoaded(true);
		} catch (LoadException e) {
			tvFrame.dispose();
			throw e;
		}

		tvFrame.addWindowListener(this);
		return tvFrame;
	}

	/**
	* same as above, but doesn't open a loading window (damn deadlocks!)
	*/
	public ViewFrame openNewNW(FileSet fileSet) throws LoadException {
		// setup toplevel
		LinkedViewFrame tvFrame  = new LinkedViewFrame(this);
		if (fileSet != null) {
			try {
				tvFrame.loadFileSetNW(fileSet);
				tvFrame.setLoaded(true);
			} catch (LoadException e) {
				tvFrame.dispose();
				throw e;
			}
		}
		tvFrame.addWindowListener(this);
		return tvFrame;
	}
	private MainProgramArgs args;
	protected void standardStartup(String astring[]) {
		args = new MainProgramArgs(astring);
		final String sFilePath = args.getFilePath();
		
		// setup toplevel
		if (sFilePath != null) {
			final String frameType = args.getFrameType();
			final String exportType = args.getExportType();

			FileSet fileSet;
			if (sFilePath.startsWith("http://")) {
				fileSet = new FileSet(sFilePath,"");
			} else {
				File file = new File(sFilePath);
				fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
			}
			fileSet.setStyle(frameType);

			try {
				ViewFrame tvFrame  = openNewNW(fileSet);
				tvFrame.setVisible(true);
				// tvFrame.loadNW(fileSet);
				if (exportType != null) {
					try {
						attemptExport(exportType, tvFrame);
					} catch (ExportException e) {
						System.err.println(e.getMessage());
						e.printStackTrace(System.err);
					}
					tvFrame.setVisible(false);
					endProgram();
					return;
				}
			} catch (LoadException e) {
				e.printStackTrace();
			}
		} else {
			if (args.getExportType() == null)
				openNew().setVisible(true);
			else
				System.err.println("Must specify file/url to load (using -r) when specifying export with -x");
		}
	}
	private boolean attemptExport(final String exportType, ViewFrame tvFrame) throws ExportException {
		for (MainPanel mainPanel :tvFrame.getMainPanels()) {
			if (exportType.equalsIgnoreCase(mainPanel.getName())) {
				mainPanel.export(args);
				return true;
			}
		}
		System.err.println("Error exporting, could not find plugin of type " + exportType);
		return false;
	}
	/**
	 *  Main method for TreeView application.
	 *
	 * Usage: java -jar treeview.jar -r <my cdt> -t [auto|classic|kmeans|linked].
	 *
	 * uses auto by default.
	 *
	 * @param  astring  Standard argument string.
	 */
	public static void main(String astring[]) {

		LinkedViewApp statView  = new LinkedViewApp();
		//statView.dealWithRegistration();
		// setup toplevel
		statView.standardStartup(astring);
	}
	private URL codeBase = null;
	private void setCodeBase(URL url) {
		codeBase = url;
	}

	/**
	 * sometimes the location of the jar is not the location where
	 * the plugins and coordiates can be found. This is particularly
	 * the case with mac os X.I have added detection code in 
	 * scanForPlugins that detects this and updates the codebase so 
	 * that the coordinates settings will be done correctly.
	 */
	public URL getCodeBase() {
		if (codeBase != null) {
			return codeBase;
		}
		try {

			// from http://weblogs.java.net/blog/ljnelson/archive/2004/09/cheap_hack_i_re.html
			URL location;
			String classLocation = LinkedViewApp.class.getName().replace('.', '/') + ".class";
			ClassLoader loader = LinkedViewApp.class.getClassLoader();
			if (loader == null) {
				location = ClassLoader.getSystemResource(classLocation);
			} else {
				location = loader.getResource(classLocation);
			}
			String token = null;
			if (location != null && "jar".equals(location.getProtocol())) {
				String urlString = location.toString();
				if (urlString != null) {
					final int lastBangIndex = urlString.lastIndexOf("!");
					if (lastBangIndex >= 0) {
						urlString = urlString.substring("jar:".length(), lastBangIndex);
						if (urlString != null) {
							int lastSlashIndex = urlString.lastIndexOf("/");
							if (lastSlashIndex >= 0) {
								token = urlString.substring(0, lastSlashIndex);
							}
						}
					}
				}
			}
			if (token == null) {
				return (new File(".")).toURI().toURL();
			} else {
				return new URL(token);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e);
			return null;
		}
	}
	protected void endProgram() {
		if (getGlobalConfig() != null) {
			getGlobalConfig().store();
		}
		closeAllWindows();
	}
}

