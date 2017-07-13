package com.ingenico.birt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BatchFormatter extends Formatter {
	@Override
	public String format(final LogRecord record) {
		final DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		final StringBuffer sb = new StringBuffer();
		final Date dt = new Date(record.getMillis());
		// Get the level name and add it to the buffer
		sb.append(sdf.format(dt)).append("\t=>\t").append(record.getMessage()).append("\n");
		return sb.toString();
	}
}
