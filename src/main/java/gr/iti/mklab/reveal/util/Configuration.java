package gr.iti.mklab.reveal.util;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by kandreadou on 2/2/15.
 */
public class Configuration {

    public static String MONGO_HOST;
    public static String QUEUE_IMAGE_PATH;

    public static void load(String file) throws ConfigurationException {
        PropertiesConfiguration conf = new PropertiesConfiguration(file);
        MONGO_HOST = conf.getString("mongoHost");
        QUEUE_IMAGE_PATH = conf.getString("queueImagePath");
    }

    public static void load(InputStream stream) throws ConfigurationException, IOException {
        Properties conf = new Properties();
        conf.load(stream);
    
        MONGO_HOST = conf.getProperty("mongoHost");
        QUEUE_IMAGE_PATH = conf.getProperty("queueImagePath");
    }
}
