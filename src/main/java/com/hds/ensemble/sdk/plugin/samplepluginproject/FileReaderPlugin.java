/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hds.ensemble.sdk.plugin.samplepluginproject;

import com.hds.ensemble.sdk.config.Config;
import com.hds.ensemble.sdk.config.ConfigProperty;
import com.hds.ensemble.sdk.config.ConfigPropertyGroup;
import com.hds.ensemble.sdk.config.PropertyGroupType;
import com.hds.ensemble.sdk.config.PropertyType;
import com.hds.ensemble.sdk.connector.ConnectorMode;
import com.hds.ensemble.sdk.connector.ConnectorOptionalMethod;
import static com.hds.ensemble.sdk.connector.ConnectorOptionalMethod.LIST;
import static com.hds.ensemble.sdk.connector.ConnectorOptionalMethod.LIST_CONTAINERS;
import static com.hds.ensemble.sdk.connector.ConnectorOptionalMethod.ROOT;
import com.hds.ensemble.sdk.connector.ConnectorPlugin;
import com.hds.ensemble.sdk.connector.ConnectorPluginCategory;
import com.hds.ensemble.sdk.exception.ConfigurationException;
import com.hds.ensemble.sdk.exception.PluginOperationFailedException;
import com.hds.ensemble.sdk.exception.PluginOperationRuntimeException;
import com.hds.ensemble.sdk.model.Document;
import com.hds.ensemble.sdk.model.DocumentPagedResults;
import com.hds.ensemble.sdk.model.StreamingDocumentIterator;
import com.hds.ensemble.sdk.plugin.PluginCallback;
import com.hds.ensemble.sdk.plugin.PluginConfig;
import com.hds.ensemble.sdk.plugin.PluginSession;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author bhattp7
 */
public class FileReaderPlugin implements ConnectorPlugin {

    
    private final PluginConfig config;
    private final PluginCallback callback;
    
    private final String NAME = "com.hds.ensemble.sdk.plugin.samplepluginproject.FileReaderPlugin";
    private final String DISPLAYNAME = "com.hds.ensemble.sdk.plugin.samplepluginproject.FileReaderPlugin";
    private final String DESCRIPTION = "DIRECTORY LISTING PLUGIN Desc " ;
    private static final String LONG_DESCRIPTION = "File Listing plugin within an directory."; 
    
    private static final String HCI_DIR_PLUGIN_PATH = "hci.dir.plugin.path" ;
    
    private static final String SUBCATEGORY_EXAMPLE = "Example";
    
    public FileReaderPlugin() {
        this.config = null;
        this.callback = null;
    }

    // Constructor for configured plugin instances to be used in workflows
    private FileReaderPlugin(PluginConfig config, PluginCallback callback)
            throws ConfigurationException {
        this.config = config;
        this.callback = callback;
    }
    
    public static final ConfigProperty.Builder DIR_LOCATION = new ConfigProperty.Builder()
            .setName(HCI_DIR_PLUGIN_PATH)
            .setType(PropertyType.TEXT)
            .setValue("")
            .setRequired(true)
            .setUserVisibleName("Directory where the files have to be read.")
            .setUserVisibleDescription("Directory where the files have to be read.");

    private static List<ConfigProperty.Builder> group1Properties = new ArrayList<>();

    static {
        group1Properties.add(DIR_LOCATION);

    }

    public static final ConfigPropertyGroup.Builder PROPERTY_GROUP_ONE = new ConfigPropertyGroup.Builder(
            "Group One", null)
            .setType(PropertyGroupType.DEFAULT)
            .setConfigProperties(group1Properties);

    public static final PluginConfig DEFAULT_CONFIG = PluginConfig.builder()
            .addGroup(PROPERTY_GROUP_ONE)
            .build();

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.CRAWL_LIST;
    }

    @Override
    public PluginConfig getDefaultConfig() {
        return DEFAULT_CONFIG;
    }

    @Override
    public ConnectorPlugin build(PluginConfig config, PluginCallback callback) throws ConfigurationException {
        validateConfig(config);
        // This method is used as a factory to create a configured instance of this connector
        return new FileReaderPlugin(config, callback);
    }

    @Override
    public Document root(PluginSession session) throws ConfigurationException, PluginOperationFailedException {
          FileReaderPluginSession fileReaderPluginSession = getSession(session);
        return fileReaderPluginSession.getRootDocument();
    }
    
     private FileReaderPluginSession getSession(PluginSession session) {
        if (!(session instanceof FileReaderPluginSession)) {
            throw new PluginOperationRuntimeException(
                    "FileReaderPluginSession is not an instance of MyPluginSession", null);
        }
        return (FileReaderPluginSession) session;
    }

    @Override
    public Iterator<Document> listContainers(PluginSession session, Document dcmnt) throws ConfigurationException, PluginOperationFailedException {
         FileReaderPluginSession fileReaderPluginSession = getSession(session);
        return fileReaderPluginSession.listContainers(); // Only 1 container to list
    }

    @Override
    public Iterator<Document> list(PluginSession session, Document dcmnt) throws ConfigurationException, PluginOperationFailedException {
         FileReaderPluginSession fileReaderPluginSession = getSession(session);
        return fileReaderPluginSession.list(); // Only 1 container to list
    }

    @Override
    public Document getMetadata(PluginSession session, URI uri) throws ConfigurationException, PluginOperationFailedException {
         FileReaderPluginSession myPluginSession = getSession(session);
        return myPluginSession.getMetadata(uri);
    }

    @Override
    public DocumentPagedResults getChanges(PluginSession ps, String string) throws ConfigurationException, PluginOperationFailedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public InputStream get(PluginSession session, URI uri) throws ConfigurationException, PluginOperationFailedException {
          FileReaderPluginSession myPluginSession = getSession(session);
        return myPluginSession.get(uri);
    }

    @Override
    public InputStream openNamedStream(PluginSession ps, Document dcmnt, String string) throws ConfigurationException, PluginOperationFailedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void test(PluginSession ps) throws ConfigurationException, PluginOperationFailedException {
        
    }

    @Override
    public ConnectorPluginCategory getCategory() {
        return ConnectorPluginCategory.CUSTOM;
    }

    @Override
    public String getSubCategory() {
        return SUBCATEGORY_EXAMPLE; 
    }

    @Override
    public boolean supports(ConnectorOptionalMethod connectorOptionalMethod) {
           boolean supports = false;
        switch (connectorOptionalMethod) {
            case ROOT:
            case LIST_CONTAINERS:
            case LIST:
                // Leave out these APIs, since we throw not supported exceptions!
                // case GET_CHANGES:
                // case PUT:
                // case PUT_METADATA:
                // case DELETE:
                supports = true;
        }
        return supports;
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME ;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
      @Override
    public String getLongDescription() {
        // A user-visible local description string used to document the behavior of this plugin
        // Uses "markdown" syntax described here: https://daringfireball.net/projects/markdown/syntax
        return LONG_DESCRIPTION;
    }


  

    @Override
    public void validateConfig(PluginConfig config) throws ConfigurationException {
         if (config.getPropertyValue(DIR_LOCATION.getName()) == null) {
            throw new ConfigurationException("Missing Directory location where to search");
        } else {
             System.out.println("VALUE p = " + config.getPropertyValue(DIR_LOCATION.getName()));
         }
     
         //Config.validateConfig(getDefaultConfig(), config);
    }

    @Override
    public PluginSession startSession() throws ConfigurationException, PluginOperationFailedException {
        String dir = getDefaultConfig().getPropertyValue(HCI_DIR_PLUGIN_PATH) ;
        System.out.println("DIR VALUE IS = " + dir); ;
         return new FileReaderPluginSession(callback, dir);
    }

}
