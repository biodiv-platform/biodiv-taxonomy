package com.strandls.taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.logging.log4j.core.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyConfig {

	private TaxonomyConfig() {}

	private static final Logger logger = LoggerFactory.getLogger(TaxonomyConfig.class);

	private static Properties properties;

	static {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("config.properties");

		properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	public static String getString(String key) {
		return properties.getProperty(key);
	}
	
	public static String fetchFileAsString(String fileName) throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		return IOUtils.toString(new InputStreamReader(in));
	}
	
	public static int getInt(String key) {
		return Integer.parseInt(getString(key));
	}
	
}
