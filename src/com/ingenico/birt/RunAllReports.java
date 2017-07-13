package com.ingenico.birt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLActionHandler;
import org.eclipse.birt.report.engine.api.HTMLCompleteImageHandler;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IEngineTask;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.IRunTask;
import org.eclipse.birt.report.engine.api.PDFRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;

public class RunAllReports {
	private static final Logger logger = Logger.getLogger(RunAllReports.class.getName());
	private static final SimpleDateFormat PARAM_DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
	private IReportEngine reportEngine = null;
	private String reportFormat = null;

	/**
	 * @param args
	 * @throws SQLException
	 */
	public static void main(final String[] args) throws SQLException {
		final Configuration configuration = new Configuration(args);
		if (configuration.doNotRun)
			return;
		final DbInterface db = new DbInterface(configuration);
		final BirtEnvironment env = new BirtEnvironment(configuration);
		logger.info("BIRT SETUP: " + env.logValues());
		final RunAllReports runner = new RunAllReports();
		runner.reportFormat = env.reportFormat;
		try {
			runner.reportEngine = env.getReportEngine();
			try {
				for (final DbInterface.ReportRun reportRun : db.getReportRuns()) {
					int status = 0;
					try {
						status = runner.runReport(reportRun, env);
					}
					catch (final Exception e) {
						logger.info(reportRun.designFile + " exception: ");
						e.printStackTrace();
						continue;
					}
					if (status < 0)
						logger.info("Report Failure " + reportRun.designFile);
					else
						logger.info("Success " + status + " : " + reportRun.designFile);
				}
			}
			finally {
				runner.reportEngine.destroy();
			}
		}
		catch (final Exception e) {
			logger.info("Trapped Exception ");
			e.printStackTrace();
		}
		finally {
			Platform.shutdown();
			logger.info("Finished " + new Date());
		}
	}

	@SuppressWarnings("unchecked")
	public int runReport(final DbInterface.ReportRun reportRun, final BirtEnvironment env)
			throws EngineException, IOException, SQLException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		logger.info("Executing " + reportRun);
		final File designFile = new File(env.workspace, reportRun.designFile);
		logger.warning("design file: " + designFile.getAbsolutePath());
		final FileInputStream fis = new FileInputStream(designFile);
		final IReportRunnable design = reportEngine.openReportDesign(fis);
		final boolean runOnly = false; // possible table column
		logger.info("Start run");
		final IEngineTask task;
		if (runOnly)
			task = reportEngine.createRunTask(design);
		else
			task = reportEngine.createRunAndRenderTask(design);
		final Map<String, Object> appContext = task.getAppContext();
		// final String resourcePath = reportEngine.getConfig().getResourcePath();
		// final DesignerBbUtil bbUtil = new DesignerBbUtil(resourcePath);
		// appContext.put("bbUtil", bbUtil);
		task.setAppContext(appContext);
		for (final String key : reportRun.parameters.keySet()) {
			final String val = reportRun.parameters.get(key);
			if (val.startsWith("{")) {
				// This will be a set of multi-selects
				final String stripBrack = val.substring(1, val.length() - 1);
				final String[] fieldStrings = stripBrack.split(", *");
				final Object[] fieldValues = new Object[fieldStrings.length];
				int i = 0;
				for (final String fieldString : fieldStrings) {
					fieldValues[i++] = getFieldObject(fieldString);
				}
				task.setParameterValue(key, fieldValues);
			}
			else {
				final Object fieldValue = getFieldObject(val);
				task.setParameterValue(key, fieldValue);
			}
		}
		task.validateParameters();
		if (task instanceof IRunAndRenderTask) {
			final IRunAndRenderTask rrTask = (IRunAndRenderTask) task;
			RenderOption options = null;
			String format = reportRun.format;
			if (format == null || format.length() < 1)
				format = this.reportFormat;
			if (format == null || format.length() < 1)
				format = RenderOption.OUTPUT_FORMAT_PDF;
			if (format.equalsIgnoreCase(RenderOption.OUTPUT_FORMAT_HTML)) {
				final HTMLRenderOption htmlOption = new HTMLRenderOption();
				htmlOption.setOutputFormat(RenderOption.OUTPUT_FORMAT_HTML);
				htmlOption.setActionHandler(new HTMLActionHandler());
				htmlOption.setImageHandler(new HTMLCompleteImageHandler());
				htmlOption.setBaseImageURL(env.baseImageURL);
				htmlOption.setImageDirectory("images");
				options = htmlOption;
			}
			if (format.equalsIgnoreCase(RenderOption.OUTPUT_FORMAT_PDF)) {
				options = new PDFRenderOption();
				options.setOutputFormat(RenderOption.OUTPUT_FORMAT_PDF);
			}
			else if (format.equalsIgnoreCase("XLS")) {
				options = new RenderOption();
				options.setOutputFormat("XLS");
			}
			else if (format.equalsIgnoreCase("DOC")) {
				options = new RenderOption();
				options.setOutputFormat("DOC");
			}
			final String outputFilename = reportRun.outputFile;
			final File outputFile = new File(outputFilename);
			options.setOutputFileName(outputFile.getAbsolutePath());
			options.setOutputFormat(format);
			rrTask.setRenderOption(options);
			logger.info("Building " + options.getOutputFileName());
			rrTask.run();
		}
		else if (task instanceof IRunTask) {
			final IRunTask runTask = (IRunTask) task;
			final String outputFilename = reportRun.outputFile;
			final File outputFile = new File(outputFilename);
			runTask.run(outputFile.getAbsolutePath());
		}
		final List<EngineException> errors = task.getErrors();
		for (final EngineException engineException : errors) {
			logger.warning("ERROR:\t" + engineException.getMessage());
		}
		return 1;
	}

	private Object getFieldObject(final String fieldString) {
		if ("true".equalsIgnoreCase(fieldString))
			return Boolean.TRUE;
		if ("false".equalsIgnoreCase(fieldString))
			return Boolean.FALSE;
		final String trimmedFieldString = fieldString.trim();
		if (trimmedFieldString.startsWith("\"") && trimmedFieldString.endsWith("\"")) {
			return trimmedFieldString.substring(1, trimmedFieldString.length() - 1);
		}
		try {
			final int intValue = Integer.parseInt(fieldString);
			return new Integer(intValue);
		}
		catch (final NumberFormatException e) {
		}
		try {
			final double dblValue = Double.parseDouble(fieldString);
			return new Double(dblValue);
		}
		catch (final NumberFormatException e) {
		}
		try {
			final Date valDate = PARAM_DATE_FORMAT.parse(fieldString);
			return new java.sql.Date(valDate.getTime());
		}
		catch (final ParseException e) {
		}
		return fieldString;
	}

	public static List<File> getPropFiles(final File baseDir) {
		final ArrayList<File> files = new ArrayList<File>();
		if (!baseDir.isDirectory())
			files.add(baseDir);
		else {
			final File[] dirFile = baseDir.listFiles(new PropFilter());
			for (int i = 0; i < dirFile.length; i++)
				files.add(dirFile[i]);
		}
		return files;
	}

	private final static class PropFilter implements FilenameFilter {
		private final String extension = ".properties";

		@Override
		public boolean accept(final File dir, final String name) {
			return name.toLowerCase().endsWith(extension);
		}
	}
}
