
package gdalTest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.ListView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import JP2.Association;
import JP2.Box;
import JP2.FileType;
import JP2.ContigousCodestream;
import JP2.JP2Stream;
import JP2.Label;
import JP2.ResourceRequirements;
import JP2.XMLBox;

public class gmljp2 extends JFrame implements ActionListener{

      BufferedImage image = null;
      static JTextPane canvas = null;
      JButton load = null;
      
      static String[] nodeValues;
      static boolean reset = false;
      static int totalElements = 0;
      static int counter = 0;

      static List<String> listTest = new ArrayList<>();

      static {
            System.out.println("GDAL init...");
            gdal.AllRegister();
            int count = gdal.GetDriverCount();
            
            System.out.println(count + " available Drivers");
            for (int i = 0; i < count; i++) {
                  try {
                        Driver driver = gdal.GetDriver(i);
                        System.out.println(" " + driver.getShortName() + " : "
                                    + driver.getLongName());
                  } catch (Exception e) {
                        System.err.println("Error loading driver " + i);
                  }
            }
      }
      
      public gmljp2() {           
            load = new JButton("Load Image");
            load.addActionListener(this);

            canvas = new JTextPane();
            canvas.setContentType( "text/html" );
            canvas.setSize(1024, 768);


            this.getContentPane().setLayout(new BorderLayout());
            this.getContentPane().add(load, BorderLayout.NORTH);
            this.getContentPane().add(canvas, BorderLayout.SOUTH);
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            this.setSize(1024, 768);
            this.show();
      }
      
	

    /**
     * main
     */
	public static void main(String[] args) {
          gmljp2 test = new gmljp2();
    }

    public void actionPerformed(ActionEvent arg0) {
		System.out.println("Loading file chooser...");
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(this);
		if(result == JFileChooser.APPROVE_OPTION) {

			File file = chooser.getSelectedFile();
		
			InputStream targetStream = null;
			try {
				targetStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			JP2Stream jp2s = new JP2Stream(targetStream);
			
			//Find list boxes
			rootInstance = false;
			Box xmlBox = findXMLbox(jp2s.Boxes);
			XMLBox auxXmlBox = (XMLBox)xmlBox;

			

			//String lblAsoc = findLabelGMLData(jp2s.Boxes);
			//String auxLblAsoc = (String)lblAsoc.Boxes.
			listTest.clear();
	    	listTest.add("Image Name : " + file.getName());

            Dataset poDataset = null;
            try {
                  poDataset = (Dataset) gdal.Open(file.getAbsolutePath(),
                              gdalconst.GA_ReadOnly);
                  if (poDataset == null) {
                	  listTest.add("The image could not be read.");  
                	  System.out.println("The image could not be read.");
                  }
            } catch(Exception e) {
            	listTest.add("Exception caught: " + e.getMessage());    
            	System.err.println("Exception caught.");
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

            listTest.add("Driver: " + poDataset.GetDriver().GetDescription());
            System.out.println("Driver: " + poDataset.GetDriver().GetDescription());
	    	
	    	
			if (xmlBox == null){
				listTest.add("JP2 does not contain XMLBox.");
				listTest.add("Test not Passed.");
			} 
			else
			{
			    try {

			    	DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
			                                 .newDocumentBuilder();

			    	Document doc = convertStringToDocument(auxXmlBox.xmldata);

			    	if (doc.hasChildNodes()) {

		    			//test:A.1.1 - GMLJP2 file contains a GMLCOV coverage
			    		String A11 = findAttribute(doc.getChildNodes(), "xmlns:gmlcov","http://www.opengis.net/gmlcov/1.0");
						if (A11 != "Not found")
							listTest.add("Test A.1.1 passed");
						else
							listTest.add("Test A.1.1 not passed");

		//PENDIENTE: ninguna imagen de prueba tiene incluido header
		                  //test:A.1.2 - GMLJP2 coverage metadata coherence with JPEG2000 header
						Box ContigousCodestream = findContigousCodestream(jp2s.Boxes);
						ContigousCodestream auxContigousCodestream = (ContigousCodestream)ContigousCodestream;
						
						if (auxContigousCodestream != null) {
						
							//Extract Xsize and Ysize from codestream
							int[] fileContigousCodestream = auxContigousCodestream.ContigousCodestreamData;
							//Extract width and height gml:high xmlBox
							
							String A12 = getNodeValue(doc.getChildNodes(), "gml:high");
							String[] gmlHigh = A12.split(" ");
							//GMLJP2 coverage metadata coherence
							if (Integer.parseInt(gmlHigh[0]) + 1 == fileContigousCodestream[0] &&
								Integer.parseInt(gmlHigh[1]) + 1 == fileContigousCodestream[1])	
								listTest.add("Test A.1.2 passed");
							else
								listTest.add("Test A.1.2 not passed");
							
				    	}
				    	else {
				    		listTest.add("Test A.1.2 JPEG2000 header not found");
				    	}
						
		                  //gml:domainSet y gmlcov:rangeType
		                  //test:A.1.3 - GMLJP2 file GMLCOV precedence
			    		String[] A13_0 = findElementContains(doc.getChildNodes(), "gmlcov:metadata");
			    		String[] A13_1 = findElementContains(doc.getChildNodes(), "gml:domainSet");
			    		String[] A13_2 = findElementContains(doc.getChildNodes(), "gmlcov:rangeType");
			    		if (A13_0[0] == "Not found")
							listTest.add("Test A.1.3 gmlcov:metadata not found");
						else if (A13_1[0] == "Not found" || A13_2[0] == "Not found")
							listTest.add("Test A.1.3 gmlcov:rangeType or gml:domainSet not found");
						else {
							if (A13_0.length > 0) {
								if (A13_1.length > 0 || A13_2.length > 0) {
									
									boolean coherence = false;
									for (int n = 0; n < A13_1.length; n++) {
										coherence = Arrays.asList(A13_1).contains(A13_0[n]);
										if (!coherence)
											coherence = Arrays.asList(A13_2).contains(A13_0[n]);
											if (!coherence)
												listTest.add("Test A.1.3 gmlcov:metadata value " + A13_0[n] + " not included in gmlcov:rangeType or gml:domainSet");
										if (!coherence)
											break;
									}
								}
								else {
									listTest.add("Test A.1.3 gml:domainSet or gmlcov:rangeType elements not found");
								}
							}
						}
		                  
	                	//test:A.1.4 - Usage of gmlcov:metadata instead of gml:metaDataProperty
	                	// Verify that gml:metaDataProperty is not used in the coverage collection and in the individual coverages. Test passes if it is not used.
			    		String A14 = findElement(doc.getChildNodes(), "gml:metaDataProperty");
						if (A14 == "Not found")
							listTest.add("Test A.1.4 passed");
						else
							listTest.add("Test A.1.4 not passed");

		                  //test:A.1.5 - CRS is well defined externally by URI
		                  //Verify that CRS are declared using URIs. Test passes if all CRSs are URIs
						//String A15 = getNodeValue(doc.getChildNodes(), "gml:RectifiedGrid");
						reset = true;
			    		String[] A15 = getNodeAttributeValueArray(doc.getChildNodes(), "gml:RectifiedGrid", "srsName");
			    		boolean containhttp = true;
			    		for (int a = 0; a < A15.length; a++) {
			    			if (!A15[a].contains("http"))
			    				containhttp = false;
			    		}
						if (containhttp)
							listTest.add("Test A.1.5 passed");
						else
							listTest.add("Test A.1.5 CRS at least one element not contains URI, not passed");
						
		                  //test:A.1.6 -  CRS is defined for rectified coverages
		                  //Verify that all GMLJP2RectifiedGridCoverage have CRS defined in the domainSet. Test passes all GMLJP2RectifiedGridCoverage have a CRSs defined.
						reset = true;
			    		String[] A16 = getNodeAttributeValueArray(doc.getChildNodes(), "gml:RectifiedGrid", "srsName");
			    		containhttp = true;
			    		for (int a = 0; a < A16.length; a++) {
			    			if (!A16[a].contains("http"))
			    				containhttp = false;
			    		}
						if (containhttp)
							listTest.add("Test A.1.6 passed");
						else
							listTest.add("Test A.1.6 GMLJP2RectifiedGridCoverage-CRS at least one element not contains URI, not passed");
						
		                  
						//test:A.1.7 -  UoM in rangeType are defined when applicable
						//Verify that all swe:DataRecords that declare variables that requires units have them populated (gmlcov:rangeType/swe:DataRecord/swe:uom). Test passes if they are present.
						String A17elements[] = findElementContains(doc.getChildNodes(), "swe:DataRecords");
						
						if (A17elements[0] != "Not found") {
							boolean defined = false;
							for (int n = 0; n < A17elements.length; n++) {
								if (A17elements[n].contains("gmlcov:rangeType") 
								 || A17elements[n].contains("swe:DataRecord")
								 || A17elements[n].contains("swe:uom")) {
									defined = true;
								}
								
								if (!defined)
									listTest.add("Test A.1.7 swe:DataRecords at least one element has been not populated in gmlcov:rangeType/swe:DataRecord/swe:uom. Test not passed.");
								else
									listTest.add("Test A.1.7 passed");
									
							}
						} else {
							listTest.add("Test A.1.7 swe:DataRecords not found");
						}
						
						
		                  //test:A.1.8 -  UoM are defined by reference
		                  //Verify if all UoM in the GEMLJP2 XML document are defined using URIs. Test passes if all are URIs.
			    		String[] A18 = findElementContains(doc.getChildNodes(), "swe:uom");
			    		if (A18[0] != "Not found") {
				    		if (A18.length > 0) {
								
								boolean defined = false;
								for (int n = 0; n < A18.length; n++) {
									defined = A18[n].contains("http");
									listTest.add("Test A.1.8 swe:uom at least one element not contains URI. Test not passed.");
									break;
								}
								if (defined)
									listTest.add("Test A.1.8 passed");
							}
							else {
								listTest.add("Test A.1.8 swe:uom elements not found");
							}
			    		} else
							listTest.add("Test A.1.8 swe:uom not found");
		                  
		                  //test:A.1.9 -  GMLJP2 file gmlcov-nil-values
		                  //Verify that the tag nil-values have value and a reason. Test passes if all these have it.
			    		String[] A19 = findElementContains(doc.getChildNodes(), "nil-values");
			    		if (A19[0] != "Not found") {
				    		if (A19.length > 0) {
								
								boolean haveValue = false;
								for (int n = 0; n < A19.length; n++) {
									haveValue = !A19[n].contains("");
									listTest.add("Test A.1.9 [Optional] nil-values at least one element have not a reason. Test not passed.");
									break;
								}
								if (haveValue)
									listTest.add("Test A.1.9 passed");
							}
							else {
								listTest.add("Test A.1.9 [Optional] nil-values elements not found");
							}
			    		} else
							listTest.add("Test A.1.9 [Optional] nil-values not found");

		                  
		                  //test:A.1.10 -  Nil-values by reference
		                  //Verify that the all reasons for nill values are defined as URI’s. Test passes if there are.
			    		String[] A110 = findElementContains(doc.getChildNodes(), "nil-values");
			    		if (A110[0] != "Not found") {
				    		if (A110.length > 0) {
								
								boolean haveURI = false;
								for (int n = 0; n < A110.length; n++) {
									haveURI = A110[n].contains("http");
									listTest.add("Test A.1.10 [Optional] nil-values at least one element have not URI. Test not passed.");
									break;
								}
								if (haveURI)
									listTest.add("Test A.1.10 passed");
							}
							else {
								listTest.add("Test A.1.10 [Optional] nil-values elements not found");
							}
			    		} else
							listTest.add("Test A.1.10 [Optional] nil-values not found");

			    		
			    		//test:A.1.11 -  GMLJP2 file root is a coverage collection
		                  //Verify that the root element is a gmljp2:GMLJP2CoverageCollection and the elements gml:domainSet, the gml:rangeSet and the gmlcov:rangeType have been left blank as possible. Test passes if the root is as expected.

						/*reset = true;
			    		String A111_0 = getNodeValue(doc.getChildNodes(), "gmljp2:GMLJP2CoverageCollection");
			    		if (A111_0 == "not found")
			    			listTest.add("Test A.1.11 gmljp2:GMLJP2CoverageCollection not exists");*/
			    		
			    		String[] elements = {
			    				"gmljp2:GMLJP2CoverageCollection",
			    				"gml:domainSet",
			    				"gml:rangeSet",
			    				"gmlcov:rangeType"
			    		};
						String A111 = findElementsArray(doc.getChildNodes(), elements);
						if (A111 == "True")
							listTest.add("Test A.1.11 passed");
						else if (A111 == "False")
							listTest.add("Test A.1.11 not passed");
						else
							listTest.add("Test A.1.11 gmljp2:GMLJP2CoverageCollection not exists");
		                  
		                  //test:A.1.12 -  GMLJP2 file coverages
		                  //Verify that there are as many gmljp2:featureMembers derived from gmlcov:AbstractCoverageType as codestreams are present in the image. Test passes if both numbers are equal.
			    		String[] elements2 = {
			    				"gmlcov:AbstractCoverageType",
			    				"gmljp2:featureMembers"
			    		};
						String A112 = findElementsArray(doc.getChildNodes(), elements2);
						if (A112 == "True")
							listTest.add("Test A.1.12 passed");
						else if (A112 == "False")
							listTest.add("Test A.1.12 not passed");
						else
							listTest.add("Test A.1.12 gmlcov:AbstractCoverageType not exists");
		                  
		                  //test:A.1.13 - GMLJP2 file gmlcov-metadata
		                  //Verify the presence of the gmlcov-metadata if metadata is available. If so, test passes if gmlcov-metadata is populated.
			    		String A113 = findElement(doc.getChildNodes(), "gmlcov:metadata");
						if (A113 != "Not found")
							listTest.add("Test A.1.13 passed");
						else
							listTest.add("Test A.1.13 [Optional] gmlcov:metadata not found");
		                  
		                  //test:A.1.14 - GMLJP2 file features
		                  //Verify that gmljp2:GMLJP2Features (for features common to all codestreams) or gmljp2:feature (for features that are related to a single codestream) contain features as necessary that are not coverages or annotations. If so, test passes if these features are not coverages or annotations.
						reset = true;
						String[] A114_1 = getNodeValueArray(doc.getChildNodes(), "gmljp2:GMLJP2Features");
						reset = true;
			    		String[] A114_2 = getNodeValueArray(doc.getChildNodes(), "gmljp2:feature");

						//String[] A114_1 = findElementContains(doc.getChildNodes(), "gmljp2:GMLJP2Features");
			    		//String[] A114_2 = findElementContains(doc.getChildNodes(), "gmljp2:feature");
			    		String textoA114 = "Test A.1.14 passed";
						if (A114_1[0] == "Not found")
							textoA114 = "Test A.1.14 gmljp2:GMLJP2Features not found";
						else if (A114_2[0] == "Not found")
							textoA114 = "Test A.1.14 gmljp2:feature not found";
						else {
							if (A114_1.length > 0) {
								for (int d = 0; d < A114_1.length; d++) {
									if (A114_1[d].contains("annotation") || A114_1[d].contains("coverage"))
									{
										textoA114 = "Test A.1.14 not passed";
										break;
									}
								}
							} else if (A114_2.length > 0) {
								for (int d = 0; d < A114_2.length; d++) {
									if (A114_2[d].contains("annotation") || A114_2[d].contains("coverage"))
									{
										textoA114 = "Test A.1.14 not passed";
										break;
									}
								}
							} else
								textoA114 = "Test A.1.14 elements not found";
						}
						listTest.add(textoA114);

		                  //test:A.1.15 - GMLJP2 file annotations
		                  //Verify that annotations are contained only in the gmljp2:annotation element as specified. Test passes if they are.
			    		String A115 = getNodeValue(doc.getChildNodes(), "gmljp2:annotation");
			    		//String[] A115 = findElementContains(doc.getChildNodes(), "gmljp2:annotation");
			    		if (A115 != "Not found")
							listTest.add("Test A.1.15 passed");
						else
							listTest.add("Test A.1.15 [Optional] gmljp2:annotation not found");

						
		                  //test:A.1.16 - GMLJP2 file styles
		                  //Verify that style information is contained only in the gmljp2:style element as specified. If so, test passes.
			    		String A116 = findElement(doc.getChildNodes(), "gmljp2:style");
						if (A116 == "Not found")
							listTest.add("Test A.1.16 passed");
						else
							listTest.add("Test A.1.16 not passed");
		                  
		                  //test:A.1.17 - GMLJP2 file /req/gmlcov-filename-codestream
		                  //Verify the correspondence of the rangeSet members fileName and fileStructure are populated as gmljp2://codestream/# (# being a number) and inapplicable. If so, test passes.

						reset = true;
			    		String[] A117 = getNodeValueArray(doc.getChildNodes(), "gml:fileName");
			    		boolean containFilenameCodestream = true;
			    		for (int a = 0;a < A117.length; a++) {
			    			if (!A117[a].contains("gmljp2://codestream/"))
			    				containFilenameCodestream = false;
			    		}
						if (containFilenameCodestream)
							listTest.add("Test A.1.17 passed");
						else
							listTest.add("Test A.1.17 at least one element not contains gmljp2://codestream/, not passed");
						
						
		                  //test:A.1.18 - GMLJP2 file XML boxes
		                  //Verify that the image file has an XML box and association box with label that may serve as an identifier in GMLJP2 descriptions. If so, test passes.
						listTest.add("Test A.1.18 passed");
		                  
						
		                  //test:A.1.19 - GMLJP2 file XML boxes signaled correctly
		                  //Verify that the use of JPX format extension is signalled with the value ‘jpx\040’ in the brand field of the file type box and that the XML box is signaled with the value 67 indicating GML or Geographic metadata (XMLGISMetaData). If so, test passes.
			    		
						//PART 1:
						//Verify that the use of JPX format extension is signalled with the value ‘jpx\040’ in the brand field of the file type box
						Box fileType = findFileType(jp2s.Boxes);
						FileType auxFileType = (FileType)fileType;
						
						String fileTypeData = auxFileType.fileTypeData;
						
						String A119_1 = "";
						
						if (fileTypeData.contains("jpx\040"))
							A119_1 = "jpx\040 Found in File Type Box";
						else
							A119_1 = "jpx\040 not found in File Type Box";
						//************************************************************
						//PART 2:
						//that the XML box is signaled with the value 67 indicating GML or Geographic metadata (XMLGISMetaData)
						Box resourceRequirements = findResourceRequirements(jp2s.Boxes);
						ResourceRequirements rreq = (ResourceRequirements)resourceRequirements;
						
			    		int A119_2 = verifyBytes(rreq.rreqData);
						if (A119_2 == 67)
							listTest.add("Test A.1.19 passed, " + A119_1);
						else
							listTest.add("Test A.1.19 not passed because box RREQ not contains value 67, and " + A119_1);
		                  
		//Pendiente:               
		                  //test:A.1.20 - GMLJP2 file is a jpx and jp2 compatible
		                  //Verify that the JPEG 2000 is marked as “jpx” in the compatibility list. Verify that the JPEG 2000 is marked as “jp2” in the compatibility list (except if opacity channel is specified outside the scope of jp2). If so, test passes.
		                  boolean A120 = fileTypeData.contains("jpx");
		                  String A120txt = "";
		                  if (A120)
		                	  A120txt = "jpx compatible, ";
		                  else
		                	  A120txt = "jpx not compatible, ";
		                  
		                  A120 = fileTypeData.contains("jp2");
		                  if (A120)
		                	  A120txt += "jp2 compatible";
		                  else
		                	  A120txt += "jp2 not compatible";
		                  
		                  if (A120txt.contains("not compatible"))
		                	  listTest.add("Test A.1.20 not passed, " + A120txt);
		                  else
		                	  listTest.add("Test A.1.20 passed, " + A120txt);

		                  
		                  //test:A.1.21 - GMLJP2 file /req/ jp2-outer-box
		                  //Verify the structure and naming of the boxes and outer box is as specified, with the XML instance data preceded by a label box with the label gml.root-instance. If so, test passes.
		                  boolean A121 = existsGMLData(jp2s.Boxes);
		                  if (A121)
		                	  listTest.add("test A.1.21 passed");
		                  else
		                	  listTest.add("test A.1.21 not passed");
		                	  
		                  
		                  //test:A.1.22 - GMLJP2 file /req/jp2-other-outer-box
		                  //Verify the structure and naming of the boxes is as specified. If so, test passes.
		                  Boolean A122 = testStructureXMLBox(jp2s.Boxes);
		                  if (A122)
		                	  listTest.add("test A.1.22 passed");
		                  else
		                	  listTest.add("test A.1.22 not passed");
		                	  
		                  
		                  //test:A.1.23 - GMLJP2 file /req/gmlcov-schemalocation
		                  //Verify that when a XML resource embedded in a JPEG200 file  includes a schema definition, a reference to a schemaLocation is provided. If so, test passes.
			    		String A123 = findElement(doc.getChildNodes(), "xsi:schemaLocation");
						if (A123 != "Not found")
							listTest.add("Test A.1.23 passed");
						else
							listTest.add("Test A.1.23 not passed");
						
						
		                  //test:A.1.24 - GMLJP2 file /req/external-references
		                  //Verify that the external references to schemaLocations are made using http references. If so, test passes.
			    		String A124 = findAttributeValue(doc.getChildNodes(), "xsi:schemaLocation");
						if (A124.contains("http"))
							listTest.add("Test A.1.24 passed");
						else
							listTest.add("Test A.1.24 not passed");
		                  

						//test:A.1.25 - GMLJP2 file /req/internal-references
		                  //Verify that the internal references to schemaLocations are made using gmljp2: references. If so, test passes.
			    		String A125 = findAttributeValue(doc.getChildNodes(), "gmljp2:references");
						if (A125 != "Not found")
							listTest.add("Test A.1.25 passed");
						else
							listTest.add("Test A.1.25 not passed");
		                  

		                  //test:A.1.26 - GMLJP2 file /req/internal-references-to-xmlbox
		                  //Verify that the internal references to schemaLocations in xmlboxes are made using gmljp2://xml/ references. If so, test passes.
						reset = true;
			    		String[] A126 = getNodeAttributeValueArray(doc.getChildNodes(), "gml:FeatureCollection", "xsi:schemaLocation");

			    		boolean containxml = true;
			    		for (int a=0;a<A126.length;a++) {
			    			if (!A126[a].contains("gmljp2://xml/"))
			    				containxml = false;
			    		}
						if (containxml)
							listTest.add("Test A.1.26 passed");
						else
							listTest.add("Test A.1.26 at least one element not contains gmljp2://xml/, not passed");

						
		                  //test:A.1.27 - GMLJP2 file /req/internal-references-to-codestream
		                  //Verify that the internal references to schemaLocations in codestreams are made using gmljp2://codestream/ references. If so, test passes.
			    		//String A127 = findAttributeValue(doc.getChildNodes(), "gmljp2://codestream/");
						reset = true;
			    		String[] A127 = getNodeValueArray(doc.getChildNodes(), "gml:fileName");
			    		boolean containCodestream = true;
			    		for (int a=0;a<A127.length;a++) {
			    			if (!A127[a].contains("gmljp2://codestream/"))
			    				containCodestream = false;
			    		}
						if (containCodestream)
							listTest.add("Test A.1.27 passed");
						else
							listTest.add("Test A.1.27 at least one element not contains gmljp2://codestream/, not passed");
			    	
			    	}

			 	} catch (Exception e) {
			        	System.out.println(e.getMessage());
				}
	        }
			canvasAddText(listTest);
          }
		
		//System.exit(0);
    }
    private Boolean rootInstance = false;
    
    public Box findXMLbox(List<Box> boxes){
    	Box XMLBox = null;
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		if (auxBox instanceof Association){
    			XMLBox = findXMLbox(auxBox.Boxes);
    			if (XMLBox != null)
    				return XMLBox;
    		}
    		else if (auxBox instanceof Label) {
    			Label auxLabel = (Label)auxBox;
    			//System.out.println(auxLabel.xmldata);
    			if (auxLabel.xmldata.contains("gml.root-instance"))
    				rootInstance = true;
   		}
    		else if(auxBox instanceof XMLBox && rootInstance)
    		{
    			return auxBox;
    		}
		}
		return null;
    	
    }
   
    public Box findFileType(List<Box> boxes){
    	Box XMLBox = null;
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		if (auxBox instanceof Association){
    			XMLBox = findFileType(auxBox.Boxes);
    			if (XMLBox != null)
    				return XMLBox;
    		}
    		else if(auxBox instanceof FileType)
    		{
    			return auxBox;
    		}
		}
		return null;
    	
    }
    
    public Box findResourceRequirements(List<Box> boxes){
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		if(auxBox instanceof ResourceRequirements)
    		{
    			return auxBox;
    		}
		}
		return null;
    }
   
    public Box findContigousCodestream(List<Box> boxes){
    	Box XMLBox = null;
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		/*if (auxBox instanceof Association){
    			XMLBox = findImageHeader(auxBox.Boxes);
    			if (XMLBox != null)
    				return XMLBox;
    		} else */
    		if(auxBox instanceof ContigousCodestream)
    		{
    			return auxBox;
    		}
		}
		return null;
    	
    }

    public Boolean existsGMLData(List<Box> boxes) {
		
		Boolean existsGMLData = false;
		
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		if (auxBox instanceof Association){
    			for (int d = 0; d < auxBox.Boxes.size(); d++) {
    				Box auxBox2 = auxBox.Boxes.get(d);
    				if (auxBox2 instanceof Label) {
    	    			Label auxLabel = (Label)auxBox2;
    	    			if (auxLabel.xmldata.contains("gml.data"))
    	    				existsGMLData = true;
    				}
    			}
    		}
    	}
    	return existsGMLData;
    }

    public Boolean testStructureXMLBox(List<Box> boxes) {
		
    	Boolean structAssoc = false;
    	Boolean structLabel = false;
    	
    	for (int i = 0; i < boxes.size(); i++) {
    		Box auxBox = boxes.get(i);
    		if (auxBox instanceof Association){
    			structAssoc = true;
    			for (int d = 0; d < auxBox.Boxes.size(); d++) {
    				Box auxBox2 = auxBox.Boxes.get(d);
    				if (auxBox2 instanceof Label) {
    	    			Label auxLabel = (Label)auxBox2;
    	    			structLabel = true;
    				}
    			}
    		}
    	}
    	if (structAssoc && structLabel)
    		return true;
    	else
    		return false;
    }
    
    private static String findAttribute(NodeList nodeList, String element, String Attribute) {

        for (int count = 0; count < nodeList.getLength(); count++) {

	    	Node tempNode = nodeList.item(count);
	
	    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
	
    			//System.out.println("Node Value =" + tempNode.getTextContent());
    			NamedNodeMap nodeMap = tempNode.getAttributes();
	    			
    			for (int i = 0; i < nodeMap.getLength(); i++) {
    				Node node = nodeMap.item(i);
    				if (node.getNodeName().contains(element) && node.getNodeValue().contains(Attribute)) {
    					System.out.println("attr value : " + node.getNodeName() + "-" + node.getNodeValue());
    					return node.getNodeValue();
    				}
    			}

	    	}
    		if (tempNode.hasChildNodes()) {

    			// loop again if has child nodes
    			findAttribute(tempNode.getChildNodes(), element, Attribute);

    		}

        }
        return "Not found";
    }
    
    private static String findAttributeValue(NodeList nodeList, String element) {

        for (int count = 0; count < nodeList.getLength(); count++) {

	    	Node tempNode = nodeList.item(count);
	
	    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
	
    			//System.out.println("Node Value =" + tempNode.getTextContent());
    			NamedNodeMap nodeMap = tempNode.getAttributes();
	    			
    			for (int i = 0; i < nodeMap.getLength(); i++) {
    				Node node = nodeMap.item(i);
    				if (node.getNodeName().contains(element)) {
    					System.out.println("attr value : " + node.getNodeName() + "-" + node.getNodeValue());
    					return node.getNodeValue();
    				}
    			}

	    	}
    		if (tempNode.hasChildNodes()) {

    			// loop again if has child nodes
    			findAttributeValue(tempNode.getChildNodes(), element);

    		}

        }
        return "Not found";
    }

    private static String findElement(NodeList nodeList, String element) {

        for (int count = 0; count < nodeList.getLength(); count++) {

	    	Node tempNode = nodeList.item(count);
	
	    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
	
	    		
	    		if (tempNode.hasAttributes()) {
	
	    			NamedNodeMap nodeMap = tempNode.getAttributes();
	
	    			for (int i = 0; i < nodeMap.getLength(); i++) {
	    				Node node = nodeMap.item(i);
	    				if (node.getNodeName().contains(element)) {
	    					return node.getNodeName();
	    				}
	    			}
	
	    		}
	    	}
    		if (tempNode.hasChildNodes()) {

    			// loop again if has child nodes
    			findElement(tempNode.getChildNodes(), element);

    		}

        }
        return "Not found";
    }

    private static String findElementsArray(NodeList nodeList, String[] elements) {

		boolean exists = false;
		boolean[] nodeExists = new boolean[elements.length];
		nodeExists[0] = false;
		
        for (int count = 0; count < nodeList.getLength(); count++) {

	    	Node tempNode = nodeList.item(count);
	
	    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
	
	    		
	    		if (tempNode.hasAttributes()) {
	
	    			NamedNodeMap nodeMap = tempNode.getAttributes();
	    			String mainNode = elements[0];
	    			for (int i = 0; i < nodeMap.getLength(); i++) {
	    				Node node = nodeMap.item(i);
	    				if (node.getNodeName().contains(mainNode)) {
	    					nodeExists[0] = true;
	    					NodeList childrenNodes = node.getChildNodes();

	    					for (int n = 1; n < elements.length; n++) {
	    						nodeExists[n] = false;
		    					for (int d = 0; d < childrenNodes.getLength(); d++) {
		    						if (childrenNodes.item(d).toString() == elements[n])
		    							nodeExists[n] = true;
		    					}
	    					}
	    				}
	    			}
	    		}
	    		/*if (tempNode.hasChildNodes()) {

	    			// loop again if has child nodes
	    			findElementsArray(tempNode.getChildNodes(), elements);

	    		}*/
	
	    	}
	    }
        if (!nodeExists[0])
        	return elements[0] + " not found";
        else {
        	boolean allTrue = true;
        	for (int d = 1; d < nodeExists.length; d++) {
        		if (!nodeExists[d])
        			allTrue = false;
        	}
        	if (allTrue)
        		return "True";
        	else
        		return "False";
        }
    }

	static boolean exists = false;
	static String[] results = null;
    private static String[] findElementContains(NodeList nodeList, String element) {

		
        for (int count = 0; count < nodeList.getLength(); count++) {

	    	Node tempNode = nodeList.item(count);
	
	    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
	
	    		
	    		if (tempNode.hasAttributes()) {
	
	    			NamedNodeMap nodeMap = tempNode.getAttributes();
	    			for (int i = 0; i < nodeMap.getLength(); i++) {
	    				Node node = nodeMap.item(i);
	    				if (node.getNodeName().contains(element)) {
	    					exists = true;
	    					NodeList childrenNodes = node.getChildNodes();
	    					results = new String[childrenNodes.getLength()];
	    					for (int d = 0; d < childrenNodes.getLength(); d++) {
	    						results[d] = childrenNodes.item(d).toString();
	    					}
	    				}
	    			}
	    		}
	    		/*if (tempNode.hasChildNodes()) {

	    			// loop again if has child nodes
	    			findElementContains(tempNode.getChildNodes(), element);

	    		}*/

	
	    	}
	    }
        if (!exists) {
        	results = new String[1];
        	results[0] = "Not found";
        }
        else {
        	results = new String[1];
        	if (results.length == 0)
        		results[0] = "Count 0";
        }
        return results;
    }
    
	static String result = "";
    private static String getNodeValue(NodeList nodeList, String element) {
        for (int count = 0; count < nodeList.getLength(); count++) {

        	Node tempNode = nodeList.item(count);

        	// make sure it's element node.
        	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
    		
        		if (tempNode.getNodeName().contains(element)) {
        			String getVal = tempNode.getTextContent();
        			if (getVal != null){
        				result = getVal;
    		
			    		// get node name and value
			    		System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
			    		System.out.println("Node Value =" + tempNode.getTextContent());
        			}
        		}

    			if (tempNode.hasChildNodes()) {

    				// loop again if has child nodes
    				getNodeValue(tempNode.getChildNodes(), element);

    			}

    			//System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

    			}

        	}
        	return result;
      }
    
    private static String[] getNodeValueArray(NodeList nodeList, String element) {
    	if (reset)
    		totalElements = 0;
    	countElementsNode(nodeList, element);
    	if (reset) {
			//init static variables
		    if (totalElements == 0)
		    	totalElements = 1;
		    nodeValues = new String[totalElements];
		    nodeValues[0] = "Not found";
		    	
		    //totalElements = 0;
		    counter = 0;
		    reset = false;
    	}

        for (int count = 0; count < nodeList.getLength(); count++) {

        	Node tempNode = nodeList.item(count);

        	// make sure it's element node.
        	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
    		
        		if (tempNode.getNodeName().contains(element)) {
        			String getVal = tempNode.getTextContent();
        			if (getVal != null){
        				nodeValues[counter] = getVal;
        				counter++;
        			}
        		}

    			if (tempNode.hasChildNodes()) {

    				// loop again if has child nodes
    				getNodeValueArray(tempNode.getChildNodes(), element);

    			}

    			//System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

    			}

        	}
        	return nodeValues;
	}
    
    private static String[] getNodeAttributeValueArray(NodeList nodeList, String element, String attribute) {
    	if (reset)
    		totalElements = 0;
    	countElementsNode(nodeList, element);
     	if (reset) {
			//init static variables
		    if (totalElements == 0)
		    	totalElements = 1;
		    nodeValues = new String[totalElements];
		    nodeValues[0] = "Not found";
		    //totalElements = 0;
		    counter = 0;
		    reset = false;
    	}
   		/*try {
				int a = nodeValues.length;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				nodeValues = new String[totalElements];
			}*/ 
        for (int count = 0; count < nodeList.getLength(); count++) {

        	Node tempNode = nodeList.item(count);

        	// make sure it's element node.
        	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
    		
        		if (tempNode.getNodeName().contains(element)) {
        			NamedNodeMap nodeMap = tempNode.getAttributes();
        			for (int i = 0; i < nodeMap.getLength(); i++) {

        				Node node = nodeMap.item(i);
        				
        				if (node.getNodeName().contains(attribute)){
                			String getVal = node.getTextContent();

        					nodeValues[counter] = getVal;
            				counter++;
        				}
        				//System.out.println("attr name : " + node.getNodeName());
        				//System.out.println("attr value : " + node.getNodeValue());

        			}

        		}

    			if (tempNode.hasChildNodes()) {

    				// loop again if has child nodes
    				getNodeAttributeValueArray(tempNode.getChildNodes(), element, attribute);

    			}

    			//System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

    			}

        	}
        	return nodeValues;
	}
    private static void countElementsNode(NodeList nodeList, String element) {

    	for (int count = 0; count < nodeList.getLength(); count++) {

        	Node tempNode = nodeList.item(count);

        	// make sure it's element node.
        	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
    		
        		if (tempNode.getNodeName().contains(element)) {
        			String getVal = tempNode.getTextContent();
        			if (getVal != null){
        				totalElements++;
    		
			    		// get node name and value
			    		//System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
			    		//System.out.println("Node Value =" + tempNode.getTextContent());
        			}
        		}

    			if (tempNode.hasChildNodes()) {

    				// loop again if has child nodes
    				countElementsNode(tempNode.getChildNodes(), element);

    			}

    			//System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

    			}

        	}
      }
    
    private static int verifyBytes(byte[] req) {
    	byte[] _DataTemp = new byte[req.length];
    	int position = 0;
    	int valorFinal = 0;
    	if (req[0] != 0){
    		int maskLength = req[position];
    		position += maskLength;
    		int fuam = 0;
    		for (int a = 0; a < maskLength; a++){
    			fuam += req[position + a];
    			position ++;
    		}
    		int dcm = 0;
    		for (int a = 0; a < maskLength; a++){
    			dcm += req[position + a];
    			position ++;
    		}
    		int nsf = req[position] + req[position + 1];
    		position = position + 2;
    		
    		int[] sfi = new int[nsf];
    		int[] smi = new int[nsf];
    		
    		for (int a = 0; a < nsf; a++){
        		sfi[a] = req[position] + req[position + 1];
        		position = position + 2;
        		for (int b = 0; b < maskLength; b++){
        			smi[a] += req[position + b];
        			position ++;
        		}
    		}
    		valorFinal = sfi[1];
    	}  
    	return valorFinal;
    }

    private static void canvasAddText(List<String> text){
    	canvas.setText("");
    	String texto = "<html><body>";
    	for (String string : text) {
    		if (string.contains("[Optional]"))
    			texto += "<font color='blue'>" + string + "</font><br>";
    		else if (string.contains(" not ") || string.contains("Exception"))
    			texto += "<font color='red'>" + string + "</font><br>";
    		else
    			texto += "<font color='green'>" + string + "</font><br>";
		}
    	texto += "</body></html>";
    	canvas.setText(texto);
    }
 
    private static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
        DocumentBuilder builder;  
        try 
        {  
            builder = factory.newDocumentBuilder();  
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr.trim() ) ) ); 
            return doc;
        } catch (Exception e) {  
            e.printStackTrace();  
        } 
        return null;
    }    
    private static void printNote(NodeList nodeList) {

        for (int count = 0; count < nodeList.getLength(); count++) {

    	Node tempNode = nodeList.item(count);

    	// make sure it's element node.
    	if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

    		// get node name and value
    		System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
    		System.out.println("Node Value =" + tempNode.getTextContent());

    		if (tempNode.hasAttributes()) {

    			// get attributes names and values
    			NamedNodeMap nodeMap = tempNode.getAttributes();

    			for (int i = 0; i < nodeMap.getLength(); i++) {

    				Node node = nodeMap.item(i);
    				System.out.println("attr name : " + node.getNodeName());
    				System.out.println("attr value : " + node.getNodeValue());

    			}

    		}

    		if (tempNode.hasChildNodes()) {

    			// loop again if has child nodes
    			printNote(tempNode.getChildNodes());

    		}

    		System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");

    	}

        }

      }

 
}
