package com.ingenico.birt;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DbInterface {
	private final List<ReportRun> reportRuns;

	public class ReportRun {
		public final String designFile;
		public final String format;
		public final String outputFile;
		public final Map<String, String> parameters = new HashMap<>();

		public ReportRun(final String designFile, final String format, final String outputFile,
				final String parameters) {
			this.designFile = designFile;
			this.format = format;
			this.outputFile = outputFile;
			final String[] parts = parameters.split(", *");
			for (final String part : parts) {
				final int indexOfColon = part.indexOf("=");
				if (indexOfColon < 0) {
					throw new IllegalArgumentException("Incorrectly formatted parameter: " + part);
				}
				final String paramName = part.substring(0, indexOfColon).trim();
				final String paramValue = part.substring(indexOfColon + 1).trim();
				this.parameters.put(paramName, paramValue);
			}
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("ReportRun");
			sb.append(", designFile = ");
			sb.append(designFile);
			sb.append(", format = ");
			sb.append(format);
			sb.append(", outputFile = ");
			sb.append(outputFile);
			sb.append(", parameters = ");
			sb.append(parameters);
			return sb.toString();
		}
	}

	public DbInterface(final Configuration configuration) throws SQLException {
		if (configuration.dbDriver == null)
			throw new IllegalArgumentException("dbDriver is missing");
		if (configuration.dbUrl == null)
			throw new IllegalArgumentException("dbUrl is missing");
		final List<ReportRun> reportRuns = new ArrayList<ReportRun>();
		final ComboPooledDataSource pool = new ComboPooledDataSource();
		try {
			pool.setDriverClass(configuration.dbDriver);
		}
		catch (final PropertyVetoException e) {
			// this is never thrown
		}
		pool.setJdbcUrl(configuration.dbUrl);
		pool.setUser(configuration.dbUsername);
		pool.setPassword(configuration.dbPassword);
		pool.setMaxStatements(10);
		try {
			final Connection connection = pool.getConnection();
			try {
				final StringBuilder sql = new StringBuilder();
				sql.append(
					"select RPT_DESIGNFILE, RPT_FORMAT, RPT_OUTPUTFILE, RPT_PARAMETERS from SR_BIRT_PARAM");
				final PreparedStatement statement = connection.prepareStatement(sql.toString());
				try {
					final ResultSet resultSet = statement.executeQuery();
					try {
						while (resultSet.next()) {
							final ReportRun reportRun = new ReportRun(resultSet.getString(1),
									resultSet.getString(2), resultSet.getString(3),
									resultSet.getString(4));
							reportRuns.add(reportRun);
						}
					}
					finally {
						resultSet.close();
					}
				}
				finally {
					statement.close();
				}
			}
			finally {
				connection.close();
			}
		}
		finally {
			pool.close();
		}
		this.reportRuns = reportRuns;
	}

	public List<ReportRun> getReportRuns() {
		return reportRuns;
	}
}
