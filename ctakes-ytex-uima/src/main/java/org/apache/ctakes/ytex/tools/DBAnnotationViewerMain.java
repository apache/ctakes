/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.ytex.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.BrowserUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.docanalyzer.AnnotationViewerDialog;
import org.apache.uima.tools.docanalyzer.PrefsMediator;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.stylemap.ColorParser;
import org.apache.uima.tools.stylemap.StyleMapEntry;
import org.apache.uima.tools.util.gui.AboutDialog;
import org.apache.uima.tools.util.gui.Caption;
import org.apache.uima.tools.util.gui.FileSelector;
import org.apache.uima.tools.util.gui.SpringUtilities;
import org.apache.uima.tools.util.htmlview.AnnotationViewGenerator;
import org.apache.uima.tools.viewer.CasAnnotationViewer;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasToInlineXml;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XmlCasDeserializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Main Annotation Viewer GUI. Allows user to choose directory of XCAS or XMI
 * files, then launches the AnnotationViewerDialog.
 * 
 * copied from AnnotationViewerMain. Modified to load CAS from database.
 */
public class DBAnnotationViewerMain extends JFrame {
	private static final long serialVersionUID = -3201723535833938833L;

	/**
	 * the descriptor file. Save this so that we can initialaze the
	 * casDescriptor just 1x. When view is pressed, we see if this has changed;
	 * if yes, then we reinitialize
	 */
	private String strDescriptorFile = null;
	/**
	 * cas loaded from strDescriptorFile
	 */
	private CAS casDescriptor = null;
	/**
	 * properties loaded from ytex.properties file
	 */
	private Properties jdbcProperties = null;
	/**
	 * style map initialized with descriptor
	 */
	File styleMapFile;
	// copied from AnnotationViewerDialog
	protected AnnotationViewGenerator annotationViewGenerator = new AnnotationViewGenerator(
			this.createTempDir());
	private String defaultCasViewName = CAS.NAME_DEFAULT_SOFA;
	private PrefsMediator prefsMed;
	private boolean processedStyleMap = false;
	JRadioButton javaViewerRB = null;
	JRadioButton javaViewerUCRB = null;
	JRadioButton htmlRB = null;
	JRadioButton xmlRB = null;

	// new
	JTextField documentIDField = null;

	private static final String HELP_MESSAGE = "Instructions for using Annotation Viewer:\n\n"
			+ "1) In the \"TypeSystem or AE Descriptor File\" field, either type or use the browse\n"
			+ "button to select the TypeSystem or AE descriptor for the AE that generated the\n"
			+ "XMI or XCAS files.  (This is needed for type system infornation only.\n"
			+ "Analysis will not be redone.)\n\n"
			+ "2) Specify a Document ID.\n\n"
			+ "3) Click the \"View\" button at the buttom of the window.\n\n"
			+ "4) Select the view type -- either the Java annotation viewer, HTML,\n"
			+ "or XML.  The Java annotation viewer is recommended.\n\n";

	private File uimaHomeDir;

	// private FileSelector jdbcPropertiesFileSelector;

	private FileSelector taeDescriptorFileSelector;

	private JButton viewButton;

	private JDialog aboutDialog;

	/** Stores user preferences */
	private Preferences prefs = Preferences.userRoot().node(
			"ytex/tools/DBAnnotationViewer");

	/**
	 * Constructor. Sets up the GUI.
	 */
	public DBAnnotationViewerMain() {
		super("Annotation Viewer");

		// set UIMA home dir
		uimaHomeDir = new File(System.getProperty("uima.home",
				"C:/Program Files/apache-uima"));

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// I don't think this should ever happen, but if it does just print
			// error and continue
			// with defalt look and feel
			System.err
					.println("Could not set look and feel: " + e.getMessage());
		}
		// UIManager.put("Panel.background",Color.WHITE);
		// Need to set other colors as well

		// Set frame icon image
		try {
			 this.setIconImage(Images.getImage(Images.MICROSCOPE));
		} catch (IOException e) {
			System.err.println("Image could not be loaded: " + e.getMessage());
		}

		this.getContentPane().setBackground(Color.WHITE);

		// create about dialog
		aboutDialog = new AboutDialog(this, "About Annotation Viewer");

		// Create Menu Bar
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");

		// Menu Items
		JMenuItem aboutMenuItem = new JMenuItem("About");
		JMenuItem helpMenuItem = new JMenuItem("Help");
		JMenuItem exitMenuItem = new JMenuItem("Exit");

		fileMenu.add(exitMenuItem);
		helpMenu.add(aboutMenuItem);
		helpMenu.add(helpMenuItem);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);

		// Labels to identify the text fields
		// final Caption labelJdbcProps = new Caption("JDBC Properties File: ");
		final Caption labelStyleMapFile = new Caption(
				"TypeSystem or AE Descriptor File: ");

		JPanel controlPanel = new JPanel();
		controlPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		controlPanel.setLayout(new SpringLayout());

		// Once we add components to controlPanel, we'll
		// call SpringUtilities::makeCompactGrid on it.

		// controlPanel.setLayout(new GridLayout(4, 2, 8, 4));

		// // Set default values for input fields

		taeDescriptorFileSelector = new FileSelector("", "TAE Descriptor File",
				JFileChooser.FILES_ONLY, uimaHomeDir);

		File descriptorFile = new File(uimaHomeDir,
				"examples/descriptors/analysis_engine/PersonTitleAnnotator.xml");
		taeDescriptorFileSelector.setSelected(descriptorFile.getAbsolutePath());
		Caption labelDocumentID = new Caption("Document ID:");
		this.documentIDField = new JTextField();

		controlPanel.add(labelStyleMapFile);
		controlPanel.add(taeDescriptorFileSelector);
		controlPanel.add(labelDocumentID);
		controlPanel.add(documentIDField);

		// ------ copied here
		Caption displayFormatLabel = new Caption("Results Display Format:");
		controlPanel.add(displayFormatLabel);

		JPanel displayFormatPanel = new JPanel();
		displayFormatPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		displayFormatPanel.setBorder(BorderFactory
				.createEmptyBorder(0, 0, 0, 0));
		javaViewerRB = new JRadioButton("Java Viewer");
		javaViewerUCRB = new JRadioButton("JV user colors");
		htmlRB = new JRadioButton("HTML");
		xmlRB = new JRadioButton("XML");

		ButtonGroup displayFormatButtonGroup = new ButtonGroup();
		displayFormatButtonGroup.add(javaViewerRB);
		displayFormatButtonGroup.add(javaViewerUCRB);
		displayFormatButtonGroup.add(htmlRB);
		displayFormatButtonGroup.add(xmlRB);

		// select the appropraite viewer button according to user's prefs
		javaViewerRB.setSelected(true); // default, overriden below

		displayFormatPanel.add(javaViewerRB);
		displayFormatPanel.add(javaViewerUCRB);
		displayFormatPanel.add(htmlRB);
		displayFormatPanel.add(xmlRB);

		controlPanel.add(displayFormatPanel);
		// ------ END copied here

		SpringUtilities.makeCompactGrid(controlPanel, 3, 2, // rows, cols
				4, 4, // initX, initY
				4, 4); // xPad, yPad

		// Event Handlling of "Exit" Menu Item
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				savePreferences();
				System.exit(0);
			}
		});

		// Event Handlling of "About" Menu Item
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				aboutDialog.setVisible(true);
			}
		});

		// Event Handlling of "Help" Menu Item
		helpMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JOptionPane.showMessageDialog(DBAnnotationViewerMain.this,
						HELP_MESSAGE, "Annotation Viewer Help",
						JOptionPane.PLAIN_MESSAGE);
			}
		});

		// Add the panels to the frame
		Container contentPanel = getContentPane();
		contentPanel.add(controlPanel, BorderLayout.CENTER);

		// add banner
		JLabel banner = new JLabel(new ImageIcon(this.getClass().getResource(
				"/org/apache/ctakes/ctakes_logo.jpg")));
		contentPanel.add(banner, BorderLayout.NORTH);

		// Add the view Button to run TAE
		viewButton = new JButton("View");

		// Add the view button to another panel
		JPanel lowerButtonsPanel = new JPanel();
		lowerButtonsPanel.add(viewButton);

		contentPanel.add(lowerButtonsPanel, BorderLayout.SOUTH);
		setContentPane(contentPanel);

		// Event Handling of view Button
		viewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ee) {
				try {
					viewDocuments();
				} catch (Exception e) {
					displayError(e);
				}
			}
		});

		// load user preferences
		if (System.getProperty("uima.noprefs") == null) {
			restorePreferences();
		}
	}

	public void viewDocuments() throws InvalidXMLException, IOException,
			ResourceInitializationException {
		String newDescriptorFile = taeDescriptorFileSelector.getSelected();
		if (!newDescriptorFile.equals(this.strDescriptorFile)) {
			// reset instance variables
			this.strDescriptorFile = null;
			this.casDescriptor = null;
			this.styleMapFile = null;

			File descriptorFile = new File(newDescriptorFile);
			if (!descriptorFile.exists() || descriptorFile.isDirectory()) {
				displayError("Descriptor File \"" + descriptorFile.getPath()
						+ "\" does not exist.");
				return;
			}
			// File inputDir = new File(inputFileSelector.getSelected());
			// if (!inputDir.exists() || !inputDir.isDirectory()) {
			// displayError("Input Directory \"" + inputDir.getPath() +
			// "\" does not exist.");
			// return;
			// }

			// parse descriptor. Could be either AE or TypeSystem descriptor
			Object descriptor = UIMAFramework.getXMLParser().parse(
					new XMLInputSource(descriptorFile));
			// instantiate CAS to get type system. Also build style map file if
			// there is none.
			if (descriptor instanceof AnalysisEngineDescription) {
				casDescriptor = CasCreationUtils
						.createCas((AnalysisEngineDescription) descriptor);
				styleMapFile = getStyleMapFile(
						(AnalysisEngineDescription) descriptor,
						descriptorFile.getPath());
			} else if (descriptor instanceof TypeSystemDescription) {
				TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
				tsDesc.resolveImports();
				casDescriptor = CasCreationUtils.createCas(tsDesc, null,
						new FsIndexDescription[0]);
				styleMapFile = getStyleMapFile(
						(TypeSystemDescription) descriptor,
						descriptorFile.getPath());
			} else {
				displayError("Invalid Descriptor File \""
						+ descriptorFile.getPath()
						+ "\""
						+ "Must be either an AnalysisEngine or TypeSystem descriptor.");
				return;
			}
			// everything ok - save the desc file name
			this.strDescriptorFile = newDescriptorFile;
		}

		// create Annotation Viewer Main Panel
		prefsMed = new PrefsMediator();
		// set OUTPUT dir in PrefsMediator, not input dir.
		// PrefsMediator is also used in DocumentAnalyzer, where the
		// output dir is the directory containing XCAS files.
		// prefsMed.setOutputDir(inputDir.toString());
		prefsMed.setOutputDir(System.getProperty("user.home"));

		/*
		 * DBAnnotationViewerDialog viewerDialog = new
		 * DBAnnotationViewerDialog(this, "Analyzed Documents", prefsMed,
		 * styleMapFile, null, cas.getTypeSystem(), null, false, cas);
		 * viewerDialog.pack(); viewerDialog.setModal(true);
		 * viewerDialog.setVisible(true);
		 */
		this.launchThatViewer(this.documentIDField.getText(),
				casDescriptor.getTypeSystem(), null, javaViewerRB.isSelected(),
				javaViewerUCRB.isSelected(), xmlRB.isSelected(), styleMapFile,
				createTempDir());
	}

	/**
	 * @param tae
	 *            // *
	 * @param taeDescFileName
	 * @return
	 * @throws IOException
	 */
	private File getStyleMapFile(AnalysisEngineDescription tad,
			String descFileName) throws IOException {
		File styleMapFile = getStyleMapFileName(descFileName);
		if (!styleMapFile.exists()) {
			// generate default style map
			String xml = AnnotationViewGenerator.autoGenerateStyleMap(tad
					.getAnalysisEngineMetaData());

			PrintWriter writer;
			writer = new PrintWriter(new BufferedWriter(new FileWriter(
					styleMapFile)));
			writer.println(xml);
			writer.close();
		}
		return styleMapFile;
	}

	/**
	 * @param tae
	 *            // *
	 * @param taeDescFileName
	 * @return
	 * @throws IOException
	 */
	private File getStyleMapFile(TypeSystemDescription tsd, String descFileName)
			throws IOException {
		File styleMapFile = getStyleMapFileName(descFileName);
		if (!styleMapFile.exists()) {
			// generate default style map
			String xml = AnnotationViewGenerator.autoGenerateStyleMap(tsd);

			PrintWriter writer;
			writer = new PrintWriter(new BufferedWriter(new FileWriter(
					styleMapFile)));
			writer.println(xml);
			writer.close();
		}
		return styleMapFile;
	}

	/**
	 * Gets the name of the style map file for the given AE or TypeSystem
	 * descriptor filename.
	 */
	public File getStyleMapFileName(String aDescriptorFileName) {
		String baseName;
		int index = aDescriptorFileName.lastIndexOf(".");
		if (index > 0) {
			baseName = aDescriptorFileName.substring(0, index);
		} else {
			baseName = aDescriptorFileName;
		}
		return new File(baseName + "StyleMap.xml");
	}

	public static void main(String[] args) {
		final DBAnnotationViewerMain frame = new DBAnnotationViewerMain();

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				frame.savePreferences();
				System.exit(0);
			}
		});
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Save user's preferences using Java's Preference API.
	 */
	public void savePreferences() {
		// prefs.put("inDir", inputFileSelector.getSelected());
		prefs.put("taeDescriptorFile",
				this.taeDescriptorFileSelector.getSelected());
	}

	/**
	 * Reset GUI to preferences last saved via {@link #savePreferences}.
	 */
	public void restorePreferences() {
		// figure defaults
		File defaultTaeDescriptorFile = new File(uimaHomeDir,
				"examples/descriptors/analysis_engine/PersonTitleAnnotator.xml");

		// restore preferences
		this.taeDescriptorFileSelector.setSelected(prefs.get(
				"taeDescriptorFile", defaultTaeDescriptorFile.toString()));
	}

	/**
	 * Displays an error message to the user.
	 * 
	 * @param aErrorString
	 *            error message to display
	 */
	public void displayError(String aErrorString) {
		// word-wrap long mesages
		StringBuffer buf = new StringBuffer(aErrorString.length());
		final int CHARS_PER_LINE = 80;
		int charCount = 0;
		StringTokenizer tokenizer = new StringTokenizer(aErrorString, " \n",
				true);

		while (tokenizer.hasMoreTokens()) {
			String tok = tokenizer.nextToken();

			if (tok.equals("\n")) {
				buf.append("\n");
				charCount = 0;
			} else if ((charCount > 0)
					&& ((charCount + tok.length()) > CHARS_PER_LINE)) {
				buf.append("\n").append(tok);
				charCount = tok.length();
			} else {
				buf.append(tok);
				charCount += tok.length();
			}
		}

		JOptionPane.showMessageDialog(DBAnnotationViewerMain.this,
				buf.toString(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Displays an error message to the user.
	 * 
	 * @param aThrowable
	 *            Throwable whose message is to be displayed.
	 */
	public void displayError(Throwable aThrowable) {
		aThrowable.printStackTrace();

		String message = aThrowable.toString();

		// For UIMAExceptions or UIMARuntimeExceptions, add cause info.
		// We have to go through this nonsense to support Java 1.3.
		// In 1.4 all exceptions can have a cause, so this wouldn't involve
		// all of this typecasting.
		while ((aThrowable instanceof UIMAException)
				|| (aThrowable instanceof UIMARuntimeException)) {
			if (aThrowable instanceof UIMAException) {
				aThrowable = ((UIMAException) aThrowable).getCause();
			} else if (aThrowable instanceof UIMARuntimeException) {
				aThrowable = ((UIMARuntimeException) aThrowable).getCause();
			}

			if (aThrowable != null) {
				message += ("\nCausedBy: " + aThrowable.toString());
			}
		}

		displayError(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize() {
		return new Dimension(640, 270);
	}

	/**
	 * copied from AnnotationViewerDialog. Modified to load document from db.
	 * 
	 * @see AnnotationViewerDialog
	 */
	public void launchThatViewer(String documentID, TypeSystem typeSystem,
			final String[] aTypesToDisplay, boolean javaViewerRBisSelected,
			boolean javaViewerUCRBisSelected, boolean xmlRBisSelected,
			File styleMapFile, File viewerDirectory) {
		try {
			CAS cas = this.loadDocumentCas(documentID, typeSystem);

			// get the specified view
			cas = cas.getView(this.defaultCasViewName);

			// launch appropriate viewer
			if (javaViewerRBisSelected || javaViewerUCRBisSelected) { // JMP
				// record preference for next time
				prefsMed.setViewType(javaViewerRBisSelected ? "Java Viewer"
						: "JV User Colors");

				// create tree viewer component
				CasAnnotationViewer viewer = new CasAnnotationViewer();
				viewer.setDisplayedTypes(aTypesToDisplay);
				if (javaViewerUCRBisSelected)
					getColorsForTypesFromFile(viewer, styleMapFile);
				else
					viewer.setHiddenTypes(new String[] { "uima.cpm.FileLocation" });
				// launch viewer in a new dialog
				viewer.setCAS(cas);
				JDialog dialog = new JDialog(this,
						"Annotation Results for Document ID " + documentID); // JMP
				dialog.getContentPane().add(viewer);
				dialog.setSize(850, 630);
				dialog.pack();
				dialog.setVisible(true);
			} else {
				CAS defaultView = cas.getView(CAS.NAME_DEFAULT_SOFA);
				if (defaultView.getDocumentText() == null) {
					displayError("The HTML and XML Viewers can only view the default text document, which was not found in this CAS.");
					return;
				}
				// generate inline XML
				File inlineXmlFile = new File(viewerDirectory, "inline.xml");
				String xmlAnnotations = new CasToInlineXml()
						.generateXML(defaultView);
				FileOutputStream outStream = new FileOutputStream(inlineXmlFile);
				outStream.write(xmlAnnotations.getBytes("UTF-8"));
				outStream.close();

				if (xmlRBisSelected) // JMP passed in
				{
					// record preference for next time
					prefsMed.setViewType("XML");

					BrowserUtil.openUrlInDefaultBrowser(inlineXmlFile
							.getAbsolutePath());
				} else
				// HTML view
				{
					prefsMed.setViewType("HTML");
					// generate HTML view
					// first process style map if not done already
					if (!processedStyleMap) {
						if (!styleMapFile.exists()) {
							annotationViewGenerator.autoGenerateStyleMapFile(
									promptForAE().getAnalysisEngineMetaData(),
									styleMapFile);
						}
						annotationViewGenerator.processStyleMap(styleMapFile);
						processedStyleMap = true;
					}
					annotationViewGenerator.processDocument(inlineXmlFile);
					File genFile = new File(viewerDirectory, "index.html");
					// open in browser
					BrowserUtil.openUrlInDefaultBrowser(genFile
							.getAbsolutePath());
				}
			}

			// end LTV here

		} catch (Exception ex) {
			displayError(ex);
		}
	}

	/**
	 * copied from AnnotationViewerDialog.
	 * 
	 * If the current AE filename is not know ask for it. Then parse the
	 * selected file and return the AnalysisEngineDescription object.
	 * 
	 * @see AnnotationViewerDialog
	 * @return the selected AnalysisEngineDescription, null if the user
	 *         cancelled
	 */
	protected AnalysisEngineDescription promptForAE() throws IOException,
			InvalidXMLException, ResourceInitializationException {
		if (prefsMed.getTAEfile() != null) {
			File taeFile = new File(prefsMed.getTAEfile());
			XMLInputSource in = new XMLInputSource(taeFile);
			AnalysisEngineDescription aed = UIMAFramework.getXMLParser()
					.parseAnalysisEngineDescription(in);
			return aed;
		} else {
			String taeDir = prefsMed.getTAEfile();
			JFileChooser chooser = new JFileChooser(taeDir);
			chooser.setDialogTitle("Select the Analysis Engine that Generated this Output");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				XMLInputSource in = new XMLInputSource(
						chooser.getSelectedFile());
				return UIMAFramework.getXMLParser()
						.parseAnalysisEngineDescription(in);
			} else {
				return null;
			}
		}
	}

	/**
	 * copied from AnnotationViewerDialog.
	 */
	public void getColorsForTypesFromFile(CasAnnotationViewer viewer,
			File aStyleMapFile) {
		List colorList = new ArrayList();
		ArrayList typeList = new ArrayList();
		ArrayList notCheckedList = new ArrayList();
		ArrayList hiddenList = new ArrayList();
		hiddenList.add("uima.cpm.FileLocation");

		if (aStyleMapFile.exists()) {

			FileInputStream stream = null;
			Document parse = null;
			try {
				stream = new FileInputStream(aStyleMapFile);
				DocumentBuilder db = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
				parse = db.parse(stream);
			} catch (FileNotFoundException e) {
				throw new UIMARuntimeException(e);
			} catch (ParserConfigurationException e) {
				throw new UIMARuntimeException(e);
			} catch (FactoryConfigurationError e) {
				throw new UIMARuntimeException(e);
			} catch (SAXException e) {
				throw new UIMARuntimeException(e);
			} catch (IOException e) {
				throw new UIMARuntimeException(e);
			}
			Node node0 = parse.getDocumentElement();
			// Node node1 = getFirstChildByName(parse.getDocumentElement(),
			// "styleMap");
			// String node1Name = node1.getNodeName();

			NodeList nodeList = node0.getChildNodes();
			ColorParser cParser = new ColorParser();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				String nodeName = node.getNodeName();
				if (nodeName.equals("rule")) {
					NodeList childrenList = node.getChildNodes();
					String type = "";
					String label = "";
					StyleMapEntry sme = null;
					String colorText = "";
					for (int j = 0; j < childrenList.getLength(); j++) {
						Node child = childrenList.item(j);
						String childName = child.getNodeName();
						if (childName.equals("pattern")) {
							type = getTextValue(child);
						}
						if (childName.equals("label")) {
							label = getTextValue(child);
						}
						if (childName.equals("style")) {
							colorText = getTextValue(child);
						}

					}
					sme = cParser.parseAndAssignColors(type, label, label,
							colorText);
					if (!sme.getChecked()) {
						notCheckedList.add(sme.getAnnotationTypeName());
					}
					if (!sme.getHidden()) {
						colorList.add(sme.getBackground());
						typeList.add(sme.getAnnotationTypeName());
					} else {
						hiddenList.add(sme.getAnnotationTypeName());
					}

				}
			}

			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			viewer.assignColorsFromList(colorList, typeList);
			viewer.assignCheckedFromList(notCheckedList);
			String[] hiddenArr = new String[hiddenList.size()];
			hiddenList.toArray(hiddenArr);
			viewer.setHiddenTypes(hiddenArr);
		}

	}

	/**
	 * copied from AnnotationViewerDialog.
	 */
	static public String getTextValue(Node node) {
		Node first = node.getFirstChild();
		if (first != null) {
			Text text = (Text) node.getFirstChild();
			return text.getNodeValue().trim();
		} else
			return null;
	}

	/**
	 * Gets the first child with a given name. JMP
	 */
	static public Node getFirstChildByName(Node node, String name) {
		NodeList children = node.getChildNodes();
		for (int c = 0; c < children.getLength(); ++c) {
			Node n = children.item(c);
			if (n.getNodeName().equals(name))
				return n;
		}
		return null;
	}

	private Properties loadJDBCProperties() throws IOException {
		InputStream is = null;
		try {
			is = this.getClass().getResourceAsStream(
					"/org/apache/ctakes/ytex/ytex.properties");
			this.jdbcProperties = new Properties();
			this.jdbcProperties.load(is);
			// make sure required properties are specified
			if (!jdbcProperties.containsKey("db.url")
					|| !jdbcProperties.containsKey("db.driver")) {
				// null out properties
				this.jdbcProperties = null;
				throw new IOException(
						"Error: required jdbc properties (db.url / db.driver) not specified");
			}
			// set jdbcproperties file name to avoid reloading, return
			// properties
			return jdbcProperties;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	/**
	 * load document from db
	 */
	private CAS loadDocumentCas(String documentID, TypeSystem typeSystem)
			throws SQLException, IOException, SAXException,
			ResourceInitializationException, ClassNotFoundException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		GZIPInputStream gzIS = null;
		Properties jdbcProperties = loadJDBCProperties();

		CAS cas = CasCreationUtils.createCas(Collections.EMPTY_LIST,
				typeSystem,
				UIMAFramework.getDefaultPerformanceTuningProperties());
		try {
			Class.forName(jdbcProperties.getProperty("db.driver"));
			conn = DriverManager.getConnection(
					jdbcProperties.getProperty("db.url"),
					jdbcProperties.containsKey("db.username") ? jdbcProperties
							.getProperty("db.username") : null,
					jdbcProperties.containsKey("db.password") ? jdbcProperties
							.getProperty("db.password") : null);
			String strSQL = jdbcProperties.containsKey("db.schema") ? "select cas from "
					+ jdbcProperties.getProperty("db.schema")
					+ ".document where document_id = ?"
					: "select cas from document where document_id = ?";
			ps = conn.prepareStatement(strSQL);
			ps.setInt(1, Integer.parseInt(documentID));
			rs = ps.executeQuery();
			if (rs.next()) {
				gzIS = new GZIPInputStream(new BufferedInputStream(
						rs.getBinaryStream(1)));
				XmlCasDeserializer.deserialize(gzIS, cas, true);
			} else {
				throw new RuntimeException("No document with id = "
						+ documentID);
			}
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException e) {
			}
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
			}
			try {
				if (gzIS != null)
					gzIS.close();
			} catch (IOException e) {
			}
		}
		return cas;
	}

	/**
	 * copied from AnnotationViewerDialog.
	 */
	private File createTempDir() {
		File temp = new File(System.getProperty("java.io.tmpdir"),
				System.getProperty("user.name"));
		temp.mkdir();
		return temp;
	}
}
