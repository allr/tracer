package org.rx.rtrace;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.EventListener;
import java.util.Map;
import java.util.Properties;

public class Profile {
	
	private static final String PROFILE_EXTENSION = ".prof";
	private static final File PROFILE_DIR =
			new File(System.getProperty("user.home"), ".tracer");
	
	private static final FilenameFilter PROFILE_FILE_FILTER =
			new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(PROFILE_EXTENSION);
				}
			};
	
	private static Profile activeProfile;

	private static final String DEFAULT_PROFILE_NAME = "default";
	private static final Properties DEFAULT_PROPERTIES = new Properties();
	
	private static final Listener DUMMY_LISTENER = new ListenerAdapter();
	
	private String name;
	private File file;
	private Properties props = new Properties();
	
	private Map<PropertyKey, Property<?>> propertyMap;
	
	private Profile(String name) {
		this.name = name;
		this.file = getFile(name);
		this.props = new Properties(DEFAULT_PROPERTIES);
		this.propertyMap = new EnumMap<PropertyKey, Property<?>>(
				PropertyKey.class);
		// load all Properties via reflection (non-abstract classes which 
		// implement Property<T> and have a no-arg constructor)
		for (Class<?> clazz : Profile.class.getDeclaredClasses()) {
			if (!Modifier.isAbstract(clazz.getModifiers())) {
				try {
					@SuppressWarnings("unchecked")
					Constructor<Property<?>> cons = (Constructor<Property<?>>) 
							clazz.getDeclaredConstructor(Profile.class);
					Property<?> property = cons.newInstance(this);
					this.propertyMap.put(property.getKey(), property);
				} catch (ClassCastException ignorable) {
				} catch (InvocationTargetException e) {
					throw new IllegalStateException(e.getTargetException());
				} catch (Exception ignorable) {
				}
			}
		}
	}

	private Profile(File file) throws FileNotFoundException, IOException {
		this(file.getName());
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			props.load(bis);
		} finally {
			if (bis != null)
				bis.close();
		}
	}
	
	public String getName() {
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Property<T> getProperty(PropertyKey key) {
		return (Property<T>) propertyMap.get(key);
	}
	
	public void save(String comments) throws IOException {
		PROFILE_DIR.mkdir();
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			activeProfile.props.store(writer, comments);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch(IOException ignorable) {
				}
			}
		}
	}
	
	public static boolean loadProfile(String name, Listener listener) {
		if (listener == null)
			listener = DUMMY_LISTENER;
		
		return loadProfile(name, getFile(name), listener);
	}
	
	public static boolean loadDefaultProfile(Listener listener) {
		File f = new File(PROFILE_DIR, DEFAULT_PROFILE_NAME + PROFILE_EXTENSION);
		if (!f.exists()) {
			activeProfile = new Profile(DEFAULT_PROFILE_NAME);
			try {
				activeProfile.save(null);
			} catch (IOException e) {
				listener.ioException(DEFAULT_PROFILE_NAME, e);
			}
			// permit failure of not saving default profile, since not critical
			return true;
		}
		return loadProfile(DEFAULT_PROFILE_NAME, f, DUMMY_LISTENER);
	}
	
	public static Profile getActiveProfile() {
		return activeProfile;
	}
	
	public static String[] getProfileNames() {
		String[] profileNames = PROFILE_DIR.list(PROFILE_FILE_FILTER);
		for (int i = profileNames.length - 1; i >= 0; --i)
			profileNames[i] = profileNames[i].substring(0,
					profileNames[i].length() - PROFILE_EXTENSION.length());
		return profileNames;
	}
	
	private static boolean loadProfile(String name, File f, Listener listener) {
		boolean loadingOk = false;
		try {
			activeProfile = new Profile(f);
			loadingOk = true;
		} catch (FileNotFoundException e) {
			listener.fileMissing(name);
		} catch (IOException e) {
			listener.ioException(name, e);
		}
		if (!loadingOk)
			return false;
		
		// check values
		// TODO
		return true;
	}
	
	private static File getFile(String profileName) {
		return new File(PROFILE_DIR, profileName + PROFILE_EXTENSION);
	}
	
	
	public interface Listener extends EventListener {
		void fileMissing(String profileName);
		void ioException(String profileName, IOException e);
		void propertyMissing(String profileName, String key);
		void propertyInvalid(String profileName, String key, String value);
	}
	
	
	public static class ListenerAdapter implements Listener {

		@Override
		public void fileMissing(String profileName) {
		}
		
		@Override
		public void ioException(String profileName, IOException e) {
		}

		@Override
		public void propertyMissing(String profileName, String key) {
		}

		@Override
		public void propertyInvalid(String profileName, String key, String value) {
		}
	}
	
	
//	public static class PropertyNotSetException extends Exception {
//		public PropertyNotSetException(String msg) {
//			super(msg);
//		}
//	}
	
	
	public enum PropertyKey {
		R_INSTRUMENTED_PATH(String.class),
		R_TIMED_PATH(String.class);
		
		private PropertyKey(Class<?> valueType) {
			this.valueType = valueType;
		}
		
		Class<?> valueType;
	};
	
	
	public interface Property<T> {
		PropertyKey getKey();
		void setValue(T value) throws IllegalArgumentException;
		T getValue();
	}
	
	
	private abstract class BaseProperty<T> implements Property<T> {
		private final String keyString;
		
		private BaseProperty() {
			this.keyString = getKey().name().replace('_', '.').toLowerCase();
		}
		
		protected String getStringValue() 
//				throws PropertyNotSetException 
		{
			return props.getProperty(keyString);
//			if (value == null)
//				throw new PropertyNotSetException("Missing property: " +
//						keyString);
//			return value;
		}
		
		protected void setStringValue(String value) {
			props.setProperty(keyString, value);
		}
	}
	
	
	abstract class DefaultValueProperty<T> extends BaseProperty<T> {
		protected final T defaultValue;
		
		public DefaultValueProperty(T defaultValue) {
			this.defaultValue = defaultValue;
		}
	}
	
	
	class RInstrumentedPathProperty extends BaseProperty<String> {
		@Override
		public PropertyKey getKey() {
			return PropertyKey.R_INSTRUMENTED_PATH;
		}
		
		@Override
		public void setValue(String value) throws IllegalArgumentException {
			setStringValue(value);
		}

		@Override
		public String getValue() {
			return getStringValue();
		}
	}
	
	class RTimedPathProperty extends BaseProperty<String> {
		@Override
		public PropertyKey getKey() {
			return PropertyKey.R_TIMED_PATH;
		}
		
		@Override
		public void setValue(String value) throws IllegalArgumentException {
			setStringValue(value);
		}

		@Override
		public String getValue() {
			return getStringValue();
		}
	}
	
}
