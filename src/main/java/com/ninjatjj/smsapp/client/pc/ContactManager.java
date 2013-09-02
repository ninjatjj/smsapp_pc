package com.ninjatjj.smsapp.client.pc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

public class ContactManager {

	private static Properties addressToDisplayName = new Properties();
	private static Properties displayNameToAddress = new Properties();

	static {

		File file = new File("contacts.properties");
		if (file.exists()) {
			InputStream in;
			try {
				in = new FileInputStream(file);
				addressToDisplayName.load(in);
				in.close();
			} catch (Exception e) {
				System.out.println(new Date()
						+ "Could not load contact mappings");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		Enumeration<?> propertyNames = addressToDisplayName.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String address = (String) propertyNames.nextElement();
			displayNameToAddress.put(addressToDisplayName.getProperty(address),
					address);
		}
	}

	public static synchronized String getDisplayName(String address) {
		return addressToDisplayName.getProperty(address.replace(" ", ""),
				address).intern();
	}

	public static String getAddress(String displayName) {
		return displayNameToAddress.getProperty(displayName, displayName);
	}

	public static Set<Object> getDisplayNames() {
		return displayNameToAddress.keySet();
	}
}
