package ca.wise.fgm.license;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Stores the list of third party licenses used by the
 * various parts of W.I.S.E..
 */
public class License {
	
	private List<ComponentType> components;
	private String name;
	private String url;
	private LicenseType license;
	
	private License() {
	}
	
	/**
	 * Get the components of W.I.S.E. that the library is used in. The returned
	 * list is immutable.s
	 */
	public List<ComponentType> getComponents() { return components; }
	
	/**
	 * Get the name of the library.
	 */
	public String getName() { return name; }
	
	/**
	 * Get a URL to the libraries homepage.
	 */
	public String geturl() { return url; }
	
	/**
	 * Get the license that the library is licenses with.
	 */
	public LicenseType getLicense() { return license; }
	
	/**
	 * Get the name of the license used.
	 */
	public String getLicenseName() {
		switch (license) {
		case APACHE2:
			return "Apache2";
		case GOOGLE_PROTOBUF:
			return "Protocol Bufferse License";
		case EPL1:
			return "Ecplise Public License v1.0";
		case OSHI:
			return "MIT License";
		case BSD_RSYNTAX:
			return "Modified BSD";
		case MIT:
			return "MIT License";
		case BOOST:
			return "Boost Software License";
		case MINIZIP:
			return "Minizip License";
		default:
			return "Unknown License";
		}
	}
	
	/**
	 * Get the URL of the license text.
	 */
	public String getLicenseUrl() {
		switch (license) {
		case APACHE2:
			return "http://www.apache.org/licenses/LICENSE-2.0";
		case GOOGLE_PROTOBUF:
			return "https://github.com/protocolbuffers/protobuf/blob/master/LICENSE";
		case EPL1:
			return "https://www.eclipse.org/legal/epl-v10.html";
		case OSHI:
			return "https://github.com/oshi/oshi/blob/master/LICENSE_HEADER";
		case BSD_RSYNTAX:
			return "https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/dist/RSyntaxTextArea.License.txt";
		case MIT:
			return "https://opensource.org/licenses/MIT";
		case BOOST:
			return "https://www.boost.org/LICENSE_1_0.txt";
		case MINIZIP:
			return "https://github.com/nmoinvaz/minizip/blob/master/LICENSE";
		default:
			return "";
		}
	}
	
	/**
	 * Get the list of licenses used by the various parts of W.I.S.E..
	 */
	public static List<License> getList() {
		ImmutableList.Builder<License> builder = ImmutableList.builder();
		try (Reader reader = new InputStreamReader(License.class.getResourceAsStream("/licenses.csv"))) {
			Iterable<CSVRecord> records = CSVFormat.RFC4180
					.withFirstRecordAsHeader()
					.parse(reader);
			for (CSVRecord record : records) {
				License license = new License();
				license.components = buildComponentTypes(record.get("component"));
				license.name = record.get("name");
				license.url = record.get("url");
				license.license = parseLicenseType(record.get("license"));
				builder.add(license);
			}
		}
		catch (IOException ex) {
		}
		return builder.build();
	}
	
	private static LicenseType parseLicenseType(String type) {
		LicenseType retval = LicenseType.NONE;
		if (!Strings.isNullOrEmpty(type)) {
			if (type.equals("apache-2"))
				retval = LicenseType.APACHE2;
			else if (type.equals("google-proto"))
				retval = LicenseType.GOOGLE_PROTOBUF;
			else if (type.equals("eclipse-1"))
				retval = LicenseType.EPL1;
			else if (type.equals("oshi"))
				retval = LicenseType.OSHI;
			else if (type.equals("bsd-syntax"))
				retval = LicenseType.BSD_RSYNTAX;
			else if (type.equals("mit"))
				retval = LicenseType.MIT;
			else if (type.equals("boost"))
				retval = LicenseType.BOOST;
			else if (type.equals("minizip"))
				retval = LicenseType.MINIZIP;
		}
		return retval;
	}
	
	private static List<ComponentType> buildComponentTypes(String types) {
		ImmutableList.Builder<ComponentType> builder = ImmutableList.builder();
		if (!Strings.isNullOrEmpty(types)) {
			String[] split = types.split(",");
			for (String s : split) {
				s = s.toLowerCase();
				if (s.equals("manager"))
					builder.add(ComponentType.MANAGER);
				else if (s.equals("builder"))
					builder.add(ComponentType.BUILDER);
				else if (s.equals("wise_lin"))
					builder.add(ComponentType.WISE_LINUX);
				else if (s.equals("wise_win"))
					builder.add(ComponentType.WISE_WINDOWS);
			}
		}
		return builder.build();
	}
	
	/*
	 * Which part of W.I.S.E. the library is used in.
	 */
	public enum ComponentType {
		MANAGER,
		BUILDER,
		WISE_LINUX,
		WISE_WINDOWS;
		
		/**
		 * Get a lowercase string name to be used when streaming the licenses.
		 */
		public String stream() {
			return name().toLowerCase();
		}
	}
	
	/**
	 * Different licenses that may be used by
	 * third party components in W.I.S.E..
	 */
	public enum LicenseType {
		//The Apache-2 license
		APACHE2,
		//A special license placed on protobuf by Google
		GOOGLE_PROTOBUF,
		//Eclipse Public License v1.0
		EPL1,
		//A special license for OSHI
		OSHI,
		//The Modified BSD license location for RSyntaxTextArea
		BSD_RSYNTAX,
		//The MIT license
		MIT,
		//The Boost software license
		BOOST,
		//The Minizip license
		MINIZIP,
		//The license for the library is unknown
		NONE
	}
}
