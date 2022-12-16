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
package org.apache.ctakes.ytex.tools.docanalyzer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
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
import org.apache.uima.tools.docanalyzer.PrefsMediator;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.stylemap.ColorParser;
import org.apache.uima.tools.stylemap.StyleMapEditor;
import org.apache.uima.tools.stylemap.StyleMapEntry;
import org.apache.uima.tools.util.gui.Caption;
import org.apache.uima.tools.util.gui.SpringUtilities;
import org.apache.uima.tools.util.htmlview.AnnotationViewGenerator;
import org.apache.uima.tools.viewer.CasAnnotationViewer;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasToInlineXml;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XmlCasDeserializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Dialog that loads analyzed documents stored in XMI or XCAS format and allows them to be viewed
 * using the Java-based CAS viewer or a web browser, in either an HTML/Javascript format or in the
 * inline XML format.
 * 
 * copied from AnnotationViewerDialog.
 * Modified to load CAS from database.
 * 
 */
public class DBAnnotationViewerDialog extends JDialog implements ActionListener {

  private static final long serialVersionUID = -7259891069111863433L;

  private File tempDir = createTempDir();

  protected AnnotationViewGenerator annotationViewGenerator; // JMP

  private StyleMapEditor styleMapEditor;

  private PrefsMediator med1;

  private File styleMapFile;

//  JList analyzedResultsList;

  String inputDirPath = null;

  TypeSystem typeSystem = null;

  String[] typesToDisplay = null;

  JRadioButton javaViewerRB = null;

  JRadioButton javaViewerUCRB = null;

  JRadioButton htmlRB = null;

  JRadioButton xmlRB = null;
  
  JTextField documentIDField = null; 

  private CAS cas;

  private boolean processedStyleMap = false;
  
  private String defaultCasViewName = CAS.NAME_DEFAULT_SOFA;

  /**
   * Create an AnnotationViewer Dialog
   * 
   * @param aParentFrame
   *          frame containing this panel
   * @param aTitle
   *          title to display for the dialog
   * @param aInputDir
   *          directory containing input files (in XCAS foramt) to read
   * @param aStyleMapFile
   *          filename of style map to be used to view files in HTML
   * @param aPerformanceStats
   *          string representaiton of performance statistics, optional.
   * @param aTypeSystem
   *          the CAS Type System to which the XCAS files must conform.
   * @param aTypesToDisplay
   *          array of types that should be highlighted in the viewer. This can be set to the output
   *          types of the Analysis Engine. A value of null means to display all types.
   */
  public DBAnnotationViewerDialog(JFrame aParentFrame, String aDialogTitle, PrefsMediator med,
          File aStyleMapFile, String aPerformanceStats, TypeSystem aTypeSystem,
          final String[] aTypesToDisplay, String interactiveTempFN, boolean javaViewerRBisSelected,
          boolean javaViewerUCRBisSelected, boolean xmlRBisSelected, CAS cas) {
    super(aParentFrame, aDialogTitle);
    // create the AnnotationViewGenerator (for HTML view generation)
    this.med1 = med;
    this.cas = cas;
    annotationViewGenerator = new AnnotationViewGenerator(tempDir);

    launchThatViewer(med.getOutputDir(), interactiveTempFN, aTypeSystem, aTypesToDisplay,
            javaViewerRBisSelected, javaViewerUCRBisSelected, xmlRBisSelected, aStyleMapFile,
            tempDir);
  }

  public DBAnnotationViewerDialog(JFrame aParentFrame, String aDialogTitle, PrefsMediator med,
          File aStyleMapFile, String aPerformanceStats, TypeSystem aTypeSystem,
          final String[] aTypesToDisplay, boolean generatedStyleMap, CAS cas) {

    super(aParentFrame, aDialogTitle);
    this.med1 = med;
    this.cas = cas;

    styleMapFile = aStyleMapFile;
    final String performanceStats = aPerformanceStats;
    typeSystem = aTypeSystem;
    typesToDisplay = aTypesToDisplay;

    // create the AnnotationViewGenerator (for HTML view generation)
    annotationViewGenerator = new AnnotationViewGenerator(tempDir);

    // create StyleMapEditor dialog
    styleMapEditor = new StyleMapEditor(aParentFrame, cas);
    JPanel resultsTitlePanel = new JPanel();
    resultsTitlePanel.setLayout(new BoxLayout(resultsTitlePanel, BoxLayout.Y_AXIS));

//    resultsTitlePanel.add(new JLabel("These are the Analyzed Documents."));
//    resultsTitlePanel.add(new JLabel("Select viewer type and double-click file to open."));
    
    JLabel documentIDLabel = new JLabel("Document ID:");
    JButton viewDocumentButton = new JButton("View");
    this.documentIDField = new JTextField(10);
    JPanel documentIDPanel = new JPanel();
    documentIDPanel.add(documentIDLabel);
    documentIDPanel.add(documentIDField);
    documentIDPanel.add(viewDocumentButton);

//    // create an jlist to list the the analyzed documents
//    inputDirPath = med.getOutputDir();
//    File dir = new File(inputDirPath);
//    // Select documents via filter. JMP
//    FilenameFilter iFilter = new InteractiveFilter();
//    String[] documents = dir.list(iFilter);
//    //create an empty array to display
//    if(documents == null) {
//       documents = new String[] {};
//    }
//    analyzedResultsList = new JList(documents);
    
    /*
     * File[] documents = dir.listFiles(); Vector docVector = new Vector(); for (int i = 0; i <
     * documents.length; i++) { if (documents[i].isFile()) { docVector.add(documents[i].getName()); } }
     * final JList analyzedResultsList = new JList(docVector);
     */
//    JScrollPane scrollPane = new JScrollPane();
//    scrollPane.getViewport().add(analyzedResultsList, null);

    JPanel southernPanel = new JPanel();
    southernPanel.setLayout(new BoxLayout(southernPanel, BoxLayout.Y_AXIS));

    JPanel controlsPanel = new JPanel();
    controlsPanel.setLayout(new SpringLayout());

    Caption displayFormatLabel = new Caption("Results Display Format:");
    controlsPanel.add(displayFormatLabel);

    JPanel displayFormatPanel = new JPanel();
    displayFormatPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    displayFormatPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
    if ("Java Viewer".equals(med.getViewType())) {
      javaViewerRB.setSelected(true);
    } else if ("JV User Colors".equals(med.getViewType())) {
      javaViewerUCRB.setSelected(true);
    } else if ("HTML".equals(med.getViewType())) {
      htmlRB.setSelected(true);
    } else if ("XML".equals(med.getViewType())) {
      xmlRB.setSelected(true);
    }

    displayFormatPanel.add(javaViewerRB);
    displayFormatPanel.add(javaViewerUCRB);
    displayFormatPanel.add(htmlRB);
    displayFormatPanel.add(xmlRB);

    controlsPanel.add(displayFormatPanel);

    SpringUtilities.makeCompactGrid(controlsPanel, 1, 2, // rows, cols
            4, 4, // initX, initY
            0, 0); // xPad, yPad

    JButton editStyleMapButton = new JButton("Edit Style Map");

    // event for the editStyleMapButton button
    editStyleMapButton.addActionListener(this);

    southernPanel.add(controlsPanel);

    // southernPanel.add( new JSeparator() );

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    // APL: edit style map feature disabled for SDK
    buttonsPanel.add(editStyleMapButton);

    if (performanceStats != null) {
      JButton perfStatsButton = new JButton("Performance Stats");
      perfStatsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JOptionPane.showMessageDialog((Component) ae.getSource(), performanceStats, null,
                  JOptionPane.PLAIN_MESSAGE);
        }
      });
      buttonsPanel.add(perfStatsButton);
    }

    JButton closeButton = new JButton("Close");
    buttonsPanel.add(closeButton);

    southernPanel.add(buttonsPanel);

    // add jlist and panel container to Dialog
    getContentPane().add(resultsTitlePanel, BorderLayout.NORTH);
//    getContentPane().add(scrollPane, BorderLayout.CENTER);
    getContentPane().add(documentIDPanel, BorderLayout.CENTER);
    getContentPane().add(southernPanel, BorderLayout.SOUTH);

    // event for the closeButton button
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        DBAnnotationViewerDialog.this.setVisible(false);
      }
    });

    // event for analyzedResultsDialog window closing
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLF(); // set default look and feel
//    analyzedResultsList.setCellRenderer(new MyListCellRenderer());

//    // doubleclicking on document shows the annotated result
//    MouseListener mouseListener = new ListMouseAdapter();
    // styleMapFile, analyzedResultsList,
    // inputDirPath,typeSystem , typesToDisplay ,
    // javaViewerRB , javaViewerUCRB ,xmlRB ,
    // viewerDirectory , this);

//    // add mouse Listener to the list
//    analyzedResultsList.addMouseListener(mouseListener);

  }

  // unwound from small anonymous inner class
  public void actionPerformed(ActionEvent arg0) {
    // read style map XML file if it exists
    String styleMapXml = null;
    AnalysisEngineDescription selectedAE = null;
    try {
      if (styleMapFile.exists()) {
        styleMapXml = FileUtils.file2String(styleMapFile);
      }

      // have user select AE if they haven't done so already
      if (selectedAE == null) {
        selectedAE = promptForAE();
      }
      if (selectedAE != null) {
        styleMapEditor.setAnalysisEngine(selectedAE);
        // launch StyleMapEditor GUI
        String newStyleMap = styleMapEditor.launchEditor(selectedAE.getAnalysisEngineMetaData(),
                styleMapXml, cas);

        if (newStyleMap != null) {
          // write new file using AE+StyleMap convention
          styleMapFile = med1.getStylemapFile();
          // write new style map to disk
          FileWriter writer = new FileWriter(styleMapFile);
          writer.write(newStyleMap);
          writer.close();
          // process generated style map
          annotationViewGenerator.processStyleMap(styleMapFile);
        }
      }
    } catch (Exception e) {
      displayError(e);
    }
  }

  /**
   * Filter to not show the two interactive-mode directories in the file list
   */
  static class InteractiveFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
      if (name.equals("interactive_temp"))
        return false;
      if (name.equals("interactive_out"))
        return false;
      return true;
    }
  }

  /**
   * Gets the name of the CAS View that will be displayed first in 
   * the annotation viewer.
   */
  public String getDefaultCasViewName() {
    return defaultCasViewName;
  }

  /**
   * Sets the name of the CAS View that will be displayed first in 
   * the annotation viewer.  It not set, defaults to {@link CAS#NAME_DEFAULT_SOFA}.
   */
  public void setDefaultCasViewName(String defaultCasViewName) {
    this.defaultCasViewName = defaultCasViewName;
  }
  
  // Common code to launch viewer for both the Run (file-based) and
  // Interactive modes
  // JMP

  public void launchThatViewer(String inputDirPath, String fileName, TypeSystem typeSystem,
          final String[] aTypesToDisplay, boolean javaViewerRBisSelected,
          boolean javaViewerUCRBisSelected, boolean xmlRBisSelected, File styleMapFile,
          File viewerDirectory) {
    try {

      File xcasFile = new File(inputDirPath, fileName);
      // create a new CAS
      CAS cas = CasCreationUtils.createCas(Collections.EMPTY_LIST, typeSystem, UIMAFramework
              .getDefaultPerformanceTuningProperties());
      // deserialize XCAS into CAS
      FileInputStream xcasInStream = null;
      try {
        xcasInStream = new FileInputStream(xcasFile);
        XmlCasDeserializer.deserialize(xcasInStream, cas, true);
      } finally {
        if (xcasInStream != null)
          xcasInStream.close();
      }
      
      //get the specified view
      cas = cas.getView(this.defaultCasViewName);

      // launch appropriate viewer
      if (javaViewerRBisSelected || javaViewerUCRBisSelected) { // JMP
        // record preference for next time
        med1.setViewType(javaViewerRBisSelected ? "Java Viewer" : "JV User Colors");

        // create tree viewer component
        CasAnnotationViewer viewer = new CasAnnotationViewer();
        viewer.setDisplayedTypes(aTypesToDisplay);
        if (javaViewerUCRBisSelected)
          getColorsForTypesFromFile(viewer, styleMapFile);
        else
          viewer.setHiddenTypes(new String[] { "uima.cpm.FileLocation" });
        // launch viewer in a new dialog
        viewer.setCAS(cas);
        JDialog dialog = new JDialog(DBAnnotationViewerDialog.this, "Annotation Results for "
                + fileName + " in " + inputDirPath); // JMP
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
        String xmlAnnotations = new CasToInlineXml().generateXML(defaultView);
        FileOutputStream outStream = new FileOutputStream(inlineXmlFile);
        outStream.write(xmlAnnotations.getBytes("UTF-8"));
        outStream.close();

        if (xmlRBisSelected) // JMP passed in
        {
          // record preference for next time
          med1.setViewType("XML");

          BrowserUtil.openUrlInDefaultBrowser(inlineXmlFile.getAbsolutePath());
        } else
        // HTML view
        {
          med1.setViewType("HTML");
          // generate HTML view
          // first process style map if not done already
          if (!processedStyleMap) {
            if (!styleMapFile.exists()) {
              annotationViewGenerator.autoGenerateStyleMapFile(
                      promptForAE().getAnalysisEngineMetaData(), styleMapFile);
            }
            annotationViewGenerator.processStyleMap(styleMapFile);
            processedStyleMap = true;
          }
          annotationViewGenerator.processDocument(inlineXmlFile);
          File genFile = new File(viewerDirectory, "index.html");
          // open in browser
          BrowserUtil.openUrlInDefaultBrowser(genFile.getAbsolutePath());
        }
      }

      // end LTV here

    } catch (Exception ex) {
      displayError(ex);
    }
  }


  /**
   * Assumes node has a text field and extracts its value. JMP
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

  /**
   * Reads in annotation-color associations from stylemap file. JMP Also reads checked value if
   * present.
   */

  public void getColorsForTypesFromFile(CasAnnotationViewer viewer, File aStyleMapFile) {
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
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
          sme = cParser.parseAndAssignColors(type, label, label, colorText);
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
   * Displays an error message to the user.
   * 
   * @param aErrorString
   *          error message to display
   */
  public void displayError(String aErrorString) {
    // word-wrap long mesages
    StringBuffer buf = new StringBuffer(aErrorString.length());
    final int CHARS_PER_LINE = 80;
    int charCount = 0;
    StringTokenizer tokenizer = new StringTokenizer(aErrorString, " \n", true);

    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();

      if (tok.equals("\n")) {
        buf.append("\n");
        charCount = 0;
      } else if ((charCount > 0) && ((charCount + tok.length()) > CHARS_PER_LINE)) {
        buf.append("\n").append(tok);
        charCount = tok.length();
      } else {
        buf.append(tok);
        charCount += tok.length();
      }
    }

    JOptionPane.showMessageDialog(DBAnnotationViewerDialog.this, buf.toString(), "Error",
            JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays an error message to the user.
   * 
   * @param aThrowable
   *          Throwable whose message is to be displayed.
   */
  public void displayError(Throwable aThrowable) {
    aThrowable.printStackTrace();

    String message = aThrowable.toString();

    // For UIMAExceptions or UIMARuntimeExceptions, add cause info.
    // We have to go through this nonsense to support Java 1.3.
    // In 1.4 all exceptions can have a cause, so this wouldn't involve
    // all of this typecasting.
    while ((aThrowable instanceof UIMAException) || (aThrowable instanceof UIMARuntimeException)) {
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

  /**
   * If the current AE filename is not know ask for it. Then parse the selected file and return the
   * AnalysisEngineDescription object.
   * 
   * @return the selected AnalysisEngineDescription, null if the user cancelled
   */
  protected AnalysisEngineDescription promptForAE() throws IOException, InvalidXMLException,
          ResourceInitializationException {
    if (med1.getTAEfile() != null) {
      File taeFile = new File(med1.getTAEfile());
      XMLInputSource in = new XMLInputSource(taeFile);
      AnalysisEngineDescription aed = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              in);
      return aed;
    } else {
      String taeDir = med1.getTAEfile();
      JFileChooser chooser = new JFileChooser(taeDir);
      chooser.setDialogTitle("Select the Analysis Engine that Generated this Output");
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      int returnVal = chooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        XMLInputSource in = new XMLInputSource(chooser.getSelectedFile());
        return UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      } else {
        return null;
      }
    }
  }

  /** set default look and feel */
  private static void setLF() {
    // Force SwingApp to come up in the System L&F
    String laf = UIManager.getSystemLookAndFeelClassName();
    try {
      UIManager.setLookAndFeel(laf);
    } catch (UnsupportedLookAndFeelException exc) {
      System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
    } catch (Exception exc) {
      System.err.println("Error loading " + laf + ": " + exc);
    }
  }
  
  private File createTempDir() {
    File temp = new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name"));
    temp.mkdir();
    return temp;
  }

  // create and call the list cell renderer to set the selected color and
  // image icon
  static class MyListCellRenderer extends JLabel implements ListCellRenderer {
    private static final long serialVersionUID = 7231915634689270693L;

    public MyListCellRenderer() {
      setOpaque(true);
    }

    public Component getListCellRendererComponent(JList analyzedResultsList, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
      ImageIcon xmlIcon = Images.getImageIcon(Images.XML_DOC);
      setIcon(xmlIcon);
      setText(value.toString());
      setBackground(isSelected ? Color.orange : Color.white);
      setForeground(isSelected ? Color.black : Color.black);
      return this;
    }
  }

//  class ListMouseAdapter extends MouseAdapter {
//
//    public void mouseClicked(MouseEvent e) {
//      try {
//        if (e.getClickCount() == 2) {
//          String fileName = ((String) analyzedResultsList.getSelectedValue());
//          if (fileName != null) {
//            analyzedResultsList.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//            // Start LTV here
//            launchThatViewer(inputDirPath, fileName, typeSystem, typesToDisplay, javaViewerRB
//                    .isSelected(), javaViewerUCRB.isSelected(), xmlRB.isSelected(), styleMapFile,
//                    tempDir);
//
//            analyzedResultsList.setCursor(Cursor.getDefaultCursor());
//          }
//        }
//      } catch (Exception ex) {
//        displayError(ex);
//      }
//    }
//  }
}
