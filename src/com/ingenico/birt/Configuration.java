package com.ingenico.birt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Configuration {
	String workspace = null;
	String birtRuntimeHome = null;
	String resourcePath = null;
	String scriptLib = null;
	boolean doNotRun = false;
	String reportFormat = null;
	String baseImageURL = null;
	String dbDriver = null;
	String dbUrl = null;
	String dbUsername = null;
	String dbPassword = null;

	public void copy(final Configuration configuration) {
		if (configuration != null) {
			this.workspace = configuration.workspace;
			this.birtRuntimeHome = configuration.birtRuntimeHome;
			this.resourcePath = configuration.resourcePath;
			this.scriptLib = configuration.scriptLib;
			this.doNotRun = configuration.doNotRun;
			this.reportFormat = configuration.reportFormat;
			this.baseImageURL = configuration.baseImageURL;
			this.dbDriver = configuration.dbDriver;
			this.dbUrl = configuration.dbUrl;
			this.dbUsername = configuration.dbUsername;
			this.dbPassword = configuration.dbPassword;
		}
	}

	public Configuration(final String[] args) {
		this(args, null);
	}

	public Configuration(final String[] args, final Configuration defaults) {
		copy(defaults);
		int i = 0;
		while (i < args.length) {
			final String arg = args[i++];
			if (arg.startsWith("-")) {
				final String option = arg.substring(1).toUpperCase();
				final Processor processor = MAP.get(option);
				if (processor == null)
					throw new IllegalArgumentException("Unrecognized option: " + option);
				i = processor.process(i, args, this);
			}
			else {
			}
		}
	}

	public Configuration(final Properties properties) {
		this(properties, null);
	}

	public Configuration(final Properties properties, final Configuration defaults) {
		this.workspace = properties.getProperty("birt.runner.workspace",
			defaults == null ? null : defaults.workspace);
		this.birtRuntimeHome = properties.getProperty("birt.runner.runtime",
			defaults == null ? null : defaults.birtRuntimeHome);
		this.resourcePath = properties.getProperty("birt.runner.resources",
			defaults == null ? null : defaults.resourcePath);
		this.scriptLib = properties.getProperty("birt.runner.scriptlib",
			defaults == null ? null : defaults.scriptLib);
		this.reportFormat = properties.getProperty("birt.runner.reportFormat",
			defaults == null ? null : defaults.reportFormat);
		this.baseImageURL = properties.getProperty("birt.runner.baseImageURL",
			defaults == null ? null : defaults.baseImageURL);
		this.dbDriver = properties.getProperty("birt.runner.db.driver",
			defaults == null ? null : defaults.dbDriver);
		this.dbUrl = properties.getProperty("birt.runner.db.url",
			defaults == null ? null : defaults.dbUrl);
		this.dbUsername = properties.getProperty("birt.runner.db.username",
			defaults == null ? null : defaults.dbUsername);
		this.dbPassword = properties.getProperty("birt.runner.db.password",
			defaults == null ? null : defaults.dbPassword);
	}

	interface Processor {
		int process(int i, String[] args, Configuration argsObj);
	}

	private static Map<String, Processor> MAP;
	static {
		MAP = new HashMap<String, Processor>();
		MAP.put("W", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.workspace = args[i];
				return i + 1;
			}
		});
		MAP.put("B", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.birtRuntimeHome = args[i];
				return i + 1;
			}
		});
		MAP.put("R", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.resourcePath = args[i];
				return i + 1;
			}
		});
		MAP.put("S", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.scriptLib = args[i];
				return i + 1;
			}
		});
		MAP.put("F", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.reportFormat = args[i];
				return i + 1;
			}
		});
		MAP.put("I", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.baseImageURL = args[i];
				return i + 1;
			}
		});
		MAP.put("DD", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.dbDriver = args[i];
				return i + 1;
			}
		});
		MAP.put("DU", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.dbUrl = args[i];
				return i + 1;
			}
		});
		MAP.put("DN", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.dbUsername = args[i];
				return i + 1;
			}
		});
		MAP.put("DP", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.dbPassword = args[i];
				return i + 1;
			}
		});
		MAP.put("C", new Processor() {
			@Override
			public int process(final int i, final String[] args,
					final Configuration configuration) {
				final String filename = args[i];
				final File file = new File(filename);
				final Properties properties = new Properties();
				try {
					final InputStream is = new FileInputStream(file);
					try {
						properties.load(is);
					}
					finally {
						is.close();
					}
				}
				catch (final FileNotFoundException e) {
					throw new IllegalArgumentException("Configuration file not found", e);
				}
				catch (final IOException e) {
					throw new IllegalArgumentException(e);
				}
				final Configuration config = new Configuration(properties);
				configuration.copy(config);
				return i + 1;
			}
		});
		MAP.put("H", new Processor() {
			@Override
			public int process(final int i, final String[] args, final Configuration argsObj) {
				argsObj.doNotRun = true;
				usage();
				return i;
			}
		});
	}

	public static void usage() {
		System.out.println("Usage java -jar <jarfile> [options]");
		System.out.println("Options:");
		System.out.println(" -H display this message");
		System.out.println(" -W <workspacedir> specify the workspace location");
		System.out.println(" -B <runtimehome> specify the BIRT runtime home location");
		System.out.println(" -R <resourcepath> specify the resource location");
		System.out.println(" -S <scriptlib> specify the script library location");
		System.out.println(" -P <runfile> specify the report properties file or a directory");
		System.out.println(" -DD <drivername> specify the database driver class name");
		System.out.println(" -DU <jdbcurl> specify the database JDBC URL");
		System.out.println(" -DN <username> specify the database username");
		System.out.println(" -DP <password> specify the database password");
		System.out.println(" -C <filename> specify a configuration properties file");
	}
}
