package org.smartnow.birtengine.engine.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.report.engine.api.*;
import org.smartnow.birtengine.engine.ReportEngineApplication;
import org.smartnow.birtengine.engine.dto.OutputType;
import org.smartnow.birtengine.engine.dto.Report;
import org.smartnow.birtengine.engine.dto.ReportReq;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class BirtReportService implements ApplicationContextAware, DisposableBean {
	private static final Logger log = LogManager.getLogger(BirtReportService.class);
	
	@Value("${reports.relative.path}")
	private String reportsPath;
	@Value("$images.relative.path")
	private String imagesPath;
	
	private HTMLServerImageHandler htmlImageHandler = new HTMLServerImageHandler();
	
	@SuppressWarnings("unused")
	@Autowired
	private ServletConfig servletContext;
	
	private IReportEngine birtEngine;
	private ApplicationContext context;
	private String imageFolder;
	
	private Map<String, IReportRunnable> reports = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	protected void initialize() throws BirtException {
	    EngineConfig config = new EngineConfig();
	    config.getAppContext().put("spring", this.context);
	    Platform.startup(config);
	    IReportEngineFactory factory = (IReportEngineFactory) Platform
	      .createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
	    birtEngine = factory.createReportEngine(config);
	    imageFolder = System.getProperty("user.dir") + File.separatorChar + reportsPath + imagesPath;
        loadReports();
	}
	
	@Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }
	
	public void loadReports() throws EngineException {

        File folder = new File(reportsPath);
        for (String file : Objects.requireNonNull(folder.list())) {
            if (!file.endsWith(".rptdesign")) {
                continue;
            }
            reports.put(file.replace(".rptdesign", ""),
              birtEngine.openReportDesign(folder.getAbsolutePath() + File.separator + file));

        }
    }

    public List<Report> getReports() {
        List<Report> response = new ArrayList<>();
        for (Map.Entry<String, IReportRunnable> entry : reports.entrySet()) {
            IReportRunnable report = reports.get(entry.getKey());
            IGetParameterDefinitionTask task = birtEngine.createGetParameterDefinitionTask(report);
            Report reportItem = new Report(report.getDesignHandle().getProperty("title").toString(), entry.getKey());
            for (Object h : task.getParameterDefns(false)) {
                IParameterDefn def = (IParameterDefn) h;
                log.info(def);
                reportItem.getParameters()
                  .add(new Report.Parameter(def.getPromptText(), def.getName(), getParameterType(def)));
            }
            response.add(reportItem);
        }
        return response;
    }

    private Report.ParameterType getParameterType(IParameterDefn param) {
        if (IParameterDefn.TYPE_INTEGER == param.getDataType()) {
            return Report.ParameterType.INT;
        }
        return Report.ParameterType.STRING;
    }
    
	public void generateMainReport(String reportName, OutputType output, HttpServletResponse response, HttpServletRequest request) {
	    switch (output) {
	    case HTML:
	        generateHTMLReport(reports.get(reportName), response, request);
	        break;
	    case PDF:
	        generatePDFReport(reports.get(reportName), response, request);
	        break;
	    default:
	        throw new IllegalArgumentException("Output type not recognized:" + output);
	    }
	}

	    /**
	     * Generate a report as HTML
	     */
	@SuppressWarnings("unchecked")
	private void generateHTMLReport(IReportRunnable report, HttpServletResponse response, HttpServletRequest request) {
	    IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(report);
	    response.setContentType(birtEngine.getMIMEType("html"));
		IRenderOption options = new RenderOption();
		HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
		htmlOptions.setOutputFormat("html");
		htmlOptions.setBaseImageURL("/" + reportsPath + imagesPath);
	    htmlOptions.setImageDirectory(imageFolder);
	    htmlOptions.setImageHandler(htmlImageHandler);
	    htmlOptions.setEmbeddable(true);
	    runAndRenderTask.setRenderOption(htmlOptions);
	    runAndRenderTask.getAppContext().put(EngineConstants.APPCONTEXT_BIRT_VIEWER_HTTPSERVET_REQUEST, request);
	
	    try {
	        htmlOptions.setOutputStream(response.getOutputStream());
	        runAndRenderTask.run();
	    } catch (Exception e) {
	        throw new RuntimeException(e.getMessage(), e);
	    } finally {
	        runAndRenderTask.close();
	    }
	}
	
//	private void generateHTMLReportRenderTask(ReportReq report, HttpServletResponse response, HttpServletRequest request) {
//		
//		String rptDocument = createReportDocument(report);
//		
//		//Open previously created report document
//		IReportDocument iReportDocument = birtEngine.openReportDocument(rptDocument);
//		//Create Render Task
//		IRenderTask renderTask = birtEngine.createRenderTask(iReportDocument);
//		//Set parent classloader report engine
//		renderTask.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, 
//		     RenderTaskExample.class.getClassLoader()); 
//		IRenderOption options = new RenderOption();		
//		options.setOutputFormat("html");
//		options.setOutputFileName("output/resample/eventorder.html");
//		if( options.getOutputFormat().equalsIgnoreCase("html")){
//			HTMLRenderOption htmlOptions = new HTMLRenderOption( options);
//			htmlOptions.setImageDirectory("output/image");
//			htmlOptions.setHtmlPagination(false);
//			//set this if you want your image source url to be altered
//			//If using the setBaseImageURL, make sure
//			//to set image handler to HTMLServerImageHandler
//			htmlOptions.setBaseImageURL("http://myhost/prependme?image=");
//			htmlOptions.setHtmlRtLFlag(false);
//			htmlOptions.setEmbeddable(false);
//		}else if( options.getOutputFormat().equalsIgnoreCase("pdf") ){
//			PDFRenderOption pdfOptions = new PDFRenderOption( options );
//			pdfOptions.setOption( IPDFRenderOption.FIT_TO_PAGE, new Boolean(true) );
//			pdfOptions.setOption( IPDFRenderOption.PAGEBREAK_PAGINATION_ONLY, new Boolean(true) );
//		}
//		//Use this method if you want to provide your own action handler
//		options.setActionHandler(new MyActionHandler());
//		//file based images
//		//options.setImageHandler(new HTMLCompleteImageHandler())
//		//Web based images.  Allows setBaseImageURL to prepend to img src tag
//		options.setImageHandler(new HTMLServerImageHandler());
//		IRenderTask task = engine.createRenderTask(document); 		
//		task.setRenderOption(options);
//		task.setPageRange("1-2");
//		task.render();
//		iReportDocument.close();
//	}

	    /**
	     * Generate a report as PDF
	     */
	@SuppressWarnings("unchecked")
	private void generatePDFReport(IReportRunnable report, HttpServletResponse response, HttpServletRequest request) {
	    IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(report);
	    response.setContentType(birtEngine.getMIMEType("pdf"));
		IRenderOption options = new RenderOption();
		PDFRenderOption pdfRenderOption = new PDFRenderOption(options);
		pdfRenderOption.setOutputFormat("pdf");
	    runAndRenderTask.setRenderOption(pdfRenderOption);
	    runAndRenderTask.getAppContext().put(EngineConstants.APPCONTEXT_PDF_RENDER_CONTEXT, request);
	    
	
	    try {
	        pdfRenderOption.setOutputStream(response.getOutputStream());
	        runAndRenderTask.run();
	    } catch (Exception e) {
	        throw new RuntimeException(e.getMessage(), e);
	    } finally {
	        runAndRenderTask.close();
	    }
	}
	
	
	public IReportRunnable openRequestReport(ReportReq report) {
		byte[] decodedBytes = Base64.getDecoder().decode(report.getXml_report());
		String decodedString = new String(decodedBytes);
		
		InputStream designStream = new ByteArrayInputStream(decodedString.getBytes());		

		IReportRunnable tmp_report = null;
		try {
			tmp_report = birtEngine.openReportDesign(report.getName(), designStream);
		} catch (Exception e) {
//			System.out.println(e.getMessage());
			System.out.println("Aqui falla y continua");
			throw new RuntimeException(e.getMessage(), e);
		}
		return tmp_report;
	}
	
	public void generateMainReport(ReportReq report, 
								OutputType output, 
								HttpServletResponse response, 
								HttpServletRequest request) {
		
		IReportRunnable tmp_report = openRequestReport(report);		
		
//		InputStream designStream = new ByteArrayInputStream(report.getXml_report().getBytes());
//		
//		try {
//			tmp_report = birtEngine.openReportDesign(report.getName(), designStream);
//		} catch (Exception e) {
//			 throw new RuntimeException(e.getMessage(), e);
//		}
		
		if (tmp_report != null) {
			switch (output) {
			    case HTML:
			        generateHTMLReport(tmp_report, response, request);
			        break;
			    case PDF:
			        generatePDFReport(tmp_report, response, request);
			        break;
			    default:
			        throw new IllegalArgumentException("Output type not recognized:" + output);
		    }
			
		}
		
		    
	}
	
	public String extractParams(ReportReq report, 
				HttpServletResponse response, 
				HttpServletRequest request) throws EngineException  {
		
		IReportRunnable tmp_report = openRequestReport(report);
		
		IGetParameterDefinitionTask task = birtEngine.createGetParameterDefinitionTask( tmp_report );
		Collection<?> params = task.getParameterDefns( true );
		
		Iterator<?> iter = params.iterator( );
		//Iterate over all parameters
		while ( iter.hasNext( ) ){
			IParameterDefnBase param = (IParameterDefnBase) iter.next( );
			//Group section found
			if ( param instanceof IParameterGroupDefn ){
				//Get Group Name
				IParameterGroupDefn group = (IParameterGroupDefn) param;
				System.out.println( "Parameter Group: " + group.getName( ) );
				//Get the parameters within a group
				Iterator<?> i2 = group.getContents( ).iterator( );
				while ( i2.hasNext( ) ){
					IScalarParameterDefn scalar = (IScalarParameterDefn) i2.next( );
					System.out.println("\t" + scalar.getName());
				}
			}
			else{
				//Parameters are not in a group
				IScalarParameterDefn scalar = (IScalarParameterDefn) param;
				System.out.println(param.getName());
				//Parameter is a List Box
				if(scalar.getControlType() ==  IScalarParameterDefn.LIST_BOX){
				    Collection<?> selectionList = task.getSelectionList( param.getName() );
				    //Selection contains data    
					if ( selectionList != null ){
						for ( Iterator<?> sliter = selectionList.iterator( ); sliter.hasNext( ); ){
							//Print out the selection choices
							IParameterSelectionChoice selectionItem = (IParameterSelectionChoice) sliter.next( );
							String value = (String)selectionItem.getValue( );
							String label = selectionItem.getLabel( );
							System.out.println( label + "--" + value);
						}
					}		        
				}   
			}
		}
		
		task.close();
		return "ok";
		
		
	}
	
	public String extractDataCSV(ReportReq report, 
			HttpServletResponse response, 
			HttpServletRequest request) throws EngineException  {
		
		//Creates ReportDocument
		String reportDocument = createReportDocument(report);
		
		//Open previously created report document
		IReportDocument iReportDocument = birtEngine.openReportDocument(reportDocument);
		
		//Create Data Extraction Task		
		IDataExtractionTask iDataExtract = birtEngine.createDataExtractionTask(iReportDocument);
		
		//Get list of result sets		
		ArrayList<?> resultSetList = (ArrayList<?>)iDataExtract.getResultSetList();
		
		System.out.println("resultSetList");
		System.out.println(resultSetList);
		
		//Choose first result set
		for (Iterator<?> iterator = resultSetList.iterator(); iterator.hasNext();) {
			IResultSetItem resultItem = (IResultSetItem) iterator.next();
			String dispName = resultItem.getResultSetName( );
			iDataExtract.selectResultSet( dispName );
			
			System.out.println("resultItem");
			System.out.println(resultItem.getResultMetaData());
			System.out.println(resultItem.toString());
			
			IExtractionResults iExtractResults = iDataExtract.extract();
			IDataIterator iData = null;
			System.out.println(iExtractResults.toString());
			try{
				if ( iExtractResults != null ){
					iData = iExtractResults.nextResultIterator( );
					//iterate through the results
					if ( iData != null  ){
						while ( iData.next( ) ){
							
							Object objColumn1;
						    Object objColumn2;
						    Object objColumn3;
						    Object objColumn4;
						    System.out.println("iData");
							System.out.println(iData.getResultMetaData());
							System.out.println(iData.getResultMetaData().getColumnCount());
							System.out.println(iData.getResultMetaData().getColumnLabel(0));
							System.out.println(iData.getResultMetaData().getColumnName(0));
							System.out.println(iData.getResultMetaData().getColumnType(0));
							System.out.println(iData.getResultMetaData().getColumnTypeName(0));
							System.out.println(iData.getResultMetaData().getColumnLabel(1));
							System.out.println(iData.getResultMetaData().getColumnName(1));
							System.out.println(iData.getResultMetaData().getColumnType(1));
							System.out.println(iData.getResultMetaData().getColumnTypeName(1));
							System.out.println(iData.getResultMetaData().getColumnLabel(2));
							System.out.println(iData.getResultMetaData().getColumnName(2));
							System.out.println(iData.getResultMetaData().getColumnType(2));
							System.out.println(iData.getResultMetaData().getColumnTypeName(2));
							System.out.println(iData.getResultMetaData().getColumnLabel(3));
							System.out.println(iData.getResultMetaData().getColumnName(3));
							System.out.println(iData.getResultMetaData().getColumnType(3));
							System.out.println(iData.getResultMetaData().getColumnTypeName(3));
							System.out.println(iData.toString());
							try{
								objColumn1 = iData.getValue(0);
							}catch(DataException e){
								objColumn1 = new String("");
							}
							try{
								objColumn2 = iData.getValue(1);
							}catch(DataException e){
								objColumn2 = new String("");
							}
							try{
								objColumn3 = iData.getValue(2);
							}catch(DataException e){
								objColumn3 = new String("");
							}
							try{
								objColumn4 = iData.getValue(3);
							}catch(DataException e){
								objColumn4 = new String("");
							}
							System.out.println( objColumn1 + " , " + objColumn2 + " , " + objColumn3 + " , " + objColumn4);
						}
						iData.close();
					}
				}
			}catch( Exception e){
					e.printStackTrace();
			}
		}			
		
		iDataExtract.close();
		return "ok get data";
	}
	
	private String createReportDocument(ReportReq report) {
		
		System.out.println("Creando ReportDocument: "+report.getName());
		String reportDocument = reportsPath+report.getName()+".rptdocument";
		IReportRunnable tmp_report = null;
		IRunTask runTask = null;
		
		try {
			//Open a report design 		
			tmp_report = openRequestReport(report);
			 
			//Create task to run the report - use the task to execute the report and save to disk.
			runTask = birtEngine.createRunTask(tmp_report);			
		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
			System.out.println("Termina aqui");
		};
		
		
		if (tmp_report == null || runTask == null) {
			return null;
		}
		System.out.println("Termina aqui2");
		//Set parent classloader for engine
		runTask.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, ReportEngineApplication.class.getClassLoader()); 
		     
		//run the report and destroy the engine
		try {
			runTask.run(reportDocument);
		} catch (EngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			runTask.close();
		} finally {
			runTask.close();			
		}
		System.out.println("Reporte creado en path relativo: "+reportDocument);
		return reportDocument;
	}
	
	public String validateReport(ReportReq report, 
			HttpServletResponse response, 
			HttpServletRequest request) {
		String rptDocument = createReportDocument(report);
		System.out.println(rptDocument);
		if (rptDocument != null) {
			System.out.println("Termina aqui valid");
			return "valid";
		} else {
			System.out.println("Termina invalid");
			return "invalid";
		}

		
	}
	
	

    @Override
    public void destroy() {
        birtEngine.destroy();
        Platform.shutdown();
    }	 
}