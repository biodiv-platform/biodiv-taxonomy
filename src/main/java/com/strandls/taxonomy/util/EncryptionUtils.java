package com.strandls.taxonomy.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.DatatypeConverter;

/**
 * @author Abhishek Rudra
 */
public class EncryptionUtils {

	private final Logger logger = LoggerFactory.getLogger(EncryptionUtils.class);
	private static final String ALGORITHM = "Blowfish";

	private String key;

	public EncryptionUtils() {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
			Properties properties = new Properties();
			try {
				properties.load(in);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}

			key = properties.getProperty("encryptKey");

			in.close();

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public String encrypt(String plainText) {
		String strData = "";

		try {
			SecretKeySpec skeyspec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM); // NOSONAR
			cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			strData = DatatypeConverter.printBase64Binary(encrypted);
			strData = URLEncoder.encode(strData, StandardCharsets.UTF_8.toString());

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return strData;
	}

	public String decrypt(String encryptedText) {
		String strData = "";

		try {
			SecretKeySpec skeyspec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM); // NOSONAR
			cipher.init(Cipher.DECRYPT_MODE, skeyspec);
			byte[] decrypted = cipher.doFinal(DatatypeConverter.parseBase64Binary(encryptedText));
			strData = new String(decrypted, StandardCharsets.UTF_8);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return strData;
	}
}
