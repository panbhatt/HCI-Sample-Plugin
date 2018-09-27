/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hds.ensemble.sdk.plugin.samplepluginproject;

/**
 *
 * @author bhattp7
 */

import com.hds.ensemble.sdk.action.Action;
import com.hds.ensemble.sdk.action.ActionType;
import com.hds.ensemble.sdk.action.DocumentActionProcessor;
import com.hds.ensemble.sdk.config.Config;
import com.hds.ensemble.sdk.config.ConfigProperty;
import com.hds.ensemble.sdk.config.ConfigPropertyGroup;
import com.hds.ensemble.sdk.config.PropertyGroupType;
import com.hds.ensemble.sdk.config.PropertyType;
import com.hds.ensemble.sdk.connector.ConnectorMode;
import com.hds.ensemble.sdk.connector.ConnectorOptionalMethod;
import com.hds.ensemble.sdk.connector.ConnectorPlugin;
import com.hds.ensemble.sdk.connector.ConnectorPluginCategory;
import com.hds.ensemble.sdk.exception.ConfigurationException;
import com.hds.ensemble.sdk.exception.PluginOperationFailedException;
import com.hds.ensemble.sdk.exception.PluginOperationRuntimeException;
import com.hds.ensemble.sdk.model.Document;
import com.hds.ensemble.sdk.model.DocumentBuilder;
import com.hds.ensemble.sdk.model.DocumentPagedResults;
import com.hds.ensemble.sdk.model.StandardFields;
import com.hds.ensemble.sdk.model.StreamingDocumentIterator;
import com.hds.ensemble.sdk.model.StringDocumentFieldValue;
import com.hds.ensemble.sdk.plugin.PluginCallback;
import com.hds.ensemble.sdk.plugin.PluginConfig;
import com.hds.ensemble.sdk.plugin.PluginSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Example implementation of a ConnectorPlugin.
 *
 * This connector plugin provides examples of various configuration options for new plugin
 * development. It will generate a single example document through the pipeline each time it is
 * crawled.
 */
public class SampleConnectorPlugin implements ConnectorPlugin, DocumentActionProcessor {

    private static final String PLUGIN_NAME = "com.hds.ensemble.sdk.plugin.samplepluginproject.SampleConnectorPlugin";
    private static final String DISPLAY_NAME = "Example connector";
    private static final String DESCRIPTION = "An example connector plugin intended as a starting point for development";
    private static final String LONG_DESCRIPTION = "An example connector plugin."
            + "\n\nThis connector is intended as a starting point for development."
            + "\n"
            + "\nYou can configure an " + DISPLAY_NAME + " stage to:"
            + "\n"
            + "\n* Demonstrate how a *connector plugin* functions.\n"
            + "\n"
            + "\n* Demonstrate the various plugin configuration options available."
            + "\n"
            + "\n* Generate example documents."
            + "\n\n"
            + "\n*Note*: All description fields support [markdown syntax](https://en.wikipedia.org/wiki/Markdown) for formatting."
            + "\n"
            + "\n* Example markdown syntax: "
            + "\n"
            + "\n   Spacing:"
            + "\n\n"
            + "\n         Paragraphs are separated by a blank line."
            + "\n         Two spaces at the end of a line leave a line break."
            + "\n"
            + "\n   Headings:"
            + "\n\n"
            + "\n         # Heading"
            + "\n         ## Sub-headings"
            + "\n         ### Another deeper heading"
            + "\n"
            + "\n   Text attributes:"
            + "\n\n"
            + "\n         _italic_"
            + "\n         *italic*"
            + "\n         __bold__"
            + "\n         **bold**"
            + "\n         `monospace`"
            + "\n\n"
            + "\n   Horizontal rule:"
            + "\n\n"
            + "\n     ---"
            + "\n\n"
            + "\n   Bullet list:"
            + "\n\n"
            + "\n         * apples"
            + "\n         * oranges"
            + "\n         * pears"
            + "\n\n"
            + "\n   Numbered list:"
            + "\n\n"
            + "\n         1. apples"
            + "\n         2. oranges"
            + "\n         3. pears"
            + "\n\n"
            + "\n   Link:"
            + "\n"
            + "\n         [displayed text](http://example.com)"
            + "\n"
            + "\n";

    private static final String SUBCATEGORY_EXAMPLE = "Example";

    private final PluginConfig config;
    private final PluginCallback callback;

    // Configuration property examples
    //
    // All plugins have the ability to define a set of default configuration properties.
    // Each of these properties may be typed and presented to end users in different ways.
    // When a user configures this plugin in the UI, they start with the plugin specified
    // default properties, which there are examples of below. This allows plugin
    // developers to define and validate how users interact with their plugin.

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_ONE = new ConfigProperty.Builder()
            // The name of the property must be unique but is not user-visible.
            .setName("hci.example.propertyOne")
            // The value here is what is set as the default for this property.
            .setValue("This is an example of a text input field. Users may edit this text and plugins can access it.")
            // The type of the property defines what type of control it will be in the UI.
            // A TEXT property will appear as a text entry field in the UI.
            // This is the default type if one is not selected.
            .setType(PropertyType.TEXT)
            // Does the property require user input in order for the config to be valid?
            .setRequired(true)
            // The name displayed to the end user in the UI.
            .setUserVisibleName("Example Property One")
            // The description displayed to the end user in the UI.
            .setUserVisibleDescription("This is an example text property.");

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_TWO = new ConfigProperty.Builder()
            .setName("hci.example.propertyTwo")
            // A CHECKBOX property will display a checkbox-like control for a boolean value.
            .setType(PropertyType.CHECKBOX)
            .setValue("true")
            .setRequired(true)
            .setUserVisibleName("Example Property Two")
            .setUserVisibleDescription("This is an example checkbox property.");

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_THREE = new ConfigProperty.Builder()
            .setName("hci.example.propertyThree")
            // A TEXT_AREA plugin works identically to a TEXT plugin except that it will display a
            // larger, multi-line text entry field for longer strings.
            .setType(PropertyType.TEXT_AREA)
            .setValue("Four score and seven years ago...")
            .setRequired(false)
            .setUserVisibleName("Property Three")
            .setUserVisibleDescription("This is a text area property")
            // Additionally, properties may have visibility triggers. A property may be set to
            // be visible only if a certain option is made on another property in its group.
            // For example, this property will only display to the user if the previous checkbox
            // property is set to true.
            // Visibility triggers work best when triggered by checkbox or radio property types.
            .setPropertyVisibilityTrigger("hci.example.propertyTwo", "true");

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_FOUR = new ConfigProperty.Builder()
            .setName("hci.example.propertyFour")
            // A password property will is a text-entry property that will have the value obfuscated
            // in the UI when entered, like a typical password-entry field.
            .setType(PropertyType.PASSWORD)
            .setValue("letMeIn")
            .setRequired(false)
            .setUserVisibleName("Property Four")
            .setUserVisibleDescription("This is a password property");

    private static List<String> radioOptions = new ArrayList<>();

    static {
        radioOptions.add("radio option 1");
        radioOptions.add("radio option 2");
        radioOptions.add("radio option 3");
    }

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_FIVE = new ConfigProperty.Builder()
            .setName("hci.example.propertyFive")
            .setType(PropertyType.RADIO)
            // A RADIO property will present multiple options for the end user to select exactly one
            // of. Its options are configured by passing a List of text options to choose from.
            .setOptions(radioOptions)
            .setValue(radioOptions.get(1))
            .setRequired(false)
            .setUserVisibleName("Property Five")
            .setUserVisibleDescription("This is a radio button property");

    private static List<String> selectOptions = new ArrayList<>();

    static {
        selectOptions.add("select option 1");
        selectOptions.add("select option 2");
        selectOptions.add("select option 3");
    }

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_SIX = new ConfigProperty.Builder()
            .setName("hci.example.propertySix")
            // A SELECT property is similar to a RADIO option, but presents its choices as a
            // drop-down menu in the UI. The user may still only choose one.
            // Its options are configured by passing a List of text options to choose from.
            .setType(PropertyType.SELECT)
            .setOptions(selectOptions)
            .setValue(selectOptions.get(1))
            .setRequired(false)
            .setUserVisibleName("Property Six")
            .setUserVisibleDescription("This is a selector property")
            // This property will only display to the user if the above radio property has its
            // second option selected.
            .setPropertyVisibilityTrigger("hci.example.propertyFive", "radio option 2");

    private static List<String> multiOptions = new ArrayList<>();

    static {
        multiOptions.add("multi-select option 1");
        multiOptions.add("multi-select option 2");
        multiOptions.add("multi-select option 3");
    }

    public static final ConfigProperty.Builder PROPERTY_EXAMPLE_SEVEN = new ConfigProperty.Builder()
            .setName("hci.example.propertySeven")
            // A MULTI-SELECT property is similar to a SELECT option, but allows the user to choose
            // multiple options from the drop-down instead of just one.
            // Its user selections will be entered as a comma-separated String.
            // Its options are configured by passing a List of text options to choose from.
            .setType(PropertyType.MULTI_SELECT)
            .setOptions(multiOptions)
            .setValue(multiOptions.get(1))
            .setRequired(false)
            .setUserVisibleName("Property Seven")
            .setUserVisibleDescription("This is a multi-selector property");

    // Configuration Group 1
    // "Default"-type property groups allow each property to be displayed
    // as its own control in the UI
    private static List<ConfigProperty.Builder> group1Properties = new ArrayList<>();

    static {
        group1Properties.add(PROPERTY_EXAMPLE_ONE);
        group1Properties.add(PROPERTY_EXAMPLE_TWO);
        group1Properties.add(PROPERTY_EXAMPLE_THREE);
        group1Properties.add(PROPERTY_EXAMPLE_FOUR);
        group1Properties.add(PROPERTY_EXAMPLE_FIVE);
        group1Properties.add(PROPERTY_EXAMPLE_SIX);
        group1Properties.add(PROPERTY_EXAMPLE_SEVEN);
    }

    public static final ConfigPropertyGroup.Builder PROPERTY_GROUP_ONE = new ConfigPropertyGroup.Builder(
            "Group One", null)
                    .setType(PropertyGroupType.DEFAULT)
                    .setConfigProperties(group1Properties);

    // Configuration Group 2
    // A group may be set as a SINGLE_VALUE_TABLE. This overrides the type and configuration of
    // any property contained in it and all properties become rows in a table.
    // This group type is used for variable-length tables with a single text entry field in each
    // row. Rows may be added and deleted as desired by the user.
    private static List<ConfigProperty.Builder> group2Properties = new ArrayList<>();

    static {
        // Default properties in a single value table may be included but can
        // be deleted by the end user.
        group2Properties.add(new ConfigProperty.Builder()
                .setName("hci.example.svt.property1")
                .setValue("default SVT property value"));
    }

    public static final ConfigPropertyGroup.Builder PROPERTY_GROUP_TWO = new ConfigPropertyGroup.Builder(
            "Group Two", null)
                    .setType(PropertyGroupType.SINGLE_VALUE_TABLE)
                    .setConfigProperties(group2Properties);

    // Configuration Group 3
    // A group may be set as a KEY_VALUE_TABLE. This overrides the type and configuration of
    // any property contained in it and all properties become rows in a table.
    // This group type is used for variable-length tables with a two text entry fields in each
    // row. Rows may be added and deleted as desired by the user.
    private static List<ConfigProperty.Builder> group3Properties = new ArrayList<>();

    static {
        // Default properties in a single value table may be included but can
        // be deleted by the end user.
        group3Properties.add(new ConfigProperty.Builder().setName("hci.example.kvt.property1")
                .setValue("default KVT property value"));
    }

    public static final ConfigPropertyGroup.Builder PROPERTY_GROUP_THREE = new ConfigPropertyGroup.Builder(
            "Group Three", null)
                    .setType(PropertyGroupType.KEY_VALUE_TABLE)
                    .setConfigProperties(group3Properties);

    // Default config
    // This default configuration will be returned to callers of getDefaultConfig().
    public static final PluginConfig DEFAULT_CONFIG = PluginConfig.builder()
            .addGroup(PROPERTY_GROUP_ONE)
            .addGroup(PROPERTY_GROUP_TWO)
            .addGroup(PROPERTY_GROUP_THREE)
            .build();

    // Action configuration

    private static final ConfigProperty PROPERTY_EXAMPLE_ACTION = new ConfigProperty.Builder()
            .setName("example.setting")
            .setValue(null)
            .setRequired(true)
            .setUserVisibleName("Example Setting")
            .setUserVisibleDescription("The example setting")
            .setType(PropertyType.TEXT)
            .build();

    private static final String EXAMPLE_ACTION_CONFIG_GROUP_NAME = "Example Action Config Settings";
    private static final PluginConfig EXAMPLE_ACTION_CONFIG = PluginConfig.builder()
            .setGroup(new ConfigPropertyGroup.Builder()
                    .setName(EXAMPLE_ACTION_CONFIG_GROUP_NAME)
                    .setConfigProperties(Collections
                            .singletonList(new ConfigProperty.Builder(PROPERTY_EXAMPLE_ACTION))))
            .build();

    private static final String EXAMPLE_NAME = "example";

    private static final Action EXAMPLE_ACTION = Action.builder().name(EXAMPLE_NAME)
            .description("Example connector").config(EXAMPLE_ACTION_CONFIG)
            .types(EnumSet.of(ActionType.OUTPUT, ActionType.STAGE)).build();

    // Default constructor for new unconfigured plugin instances (can obtain default config)
    public SampleConnectorPlugin() {
        this.config = null;
        this.callback = null;
    }

    // Constructor for configured plugin instances to be used in workflows
    private SampleConnectorPlugin(PluginConfig config, PluginCallback callback)
            throws ConfigurationException {
        this.config = config;
        this.callback = callback;
    }

    @Override
    public void validateConfig(PluginConfig config) throws ConfigurationException {
        // This method is used to ensure that the specified configuration is valid for this
        // connector, i.e. that required properties are present in the configuration and no invalid
        // values are set.

        // This method handles checking for non-empty and existing required properties.
        // It should typically always be called here.
        Config.validateConfig(getDefaultConfig(), config);

        // Individual property values may be read and their values checked for validity
        if (config.getPropertyValue(PROPERTY_EXAMPLE_ONE.getName()) == null) {
            throw new ConfigurationException("Missing Property One");
        }
        if (config.getPropertyValue(PROPERTY_EXAMPLE_TWO.getName()) == null) {
            throw new ConfigurationException("Missing Property Two");
        }
    }

    @Override
    public SampleConnectorPlugin build(PluginConfig config, PluginCallback callback)
            throws ConfigurationException {
        validateConfig(config);

        // This method is used as a factory to create a configured instance of this connector
        return new SampleConnectorPlugin(config, callback);
    }

    @Override
    public String getName() {
        // The fully qualified class name of this plugin
        return PLUGIN_NAME;
    }

    @Override
    public String getDisplayName() {
        // The name of this plugin to display to end users
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        // A user-visible description string describing this plugin
        return DESCRIPTION;
    }

    @Override
    public String getLongDescription() {
        // A user-visible local description string used to document the behavior of this plugin
        // Uses "markdown" syntax described here: https://daringfireball.net/projects/markdown/syntax
        return LONG_DESCRIPTION;
    }

    @Override
    public PluginConfig getDefaultConfig() {
        // This method is used to specify a default plugin configuration.
        // Configuration properties are defined here, including their type, how they are
        // presented, and whether or not they require valid user input before the connector
        // can be used.
        // This default configuration is also what will appear in the UI when a new instance of your
        // plugin is created.
        return DEFAULT_CONFIG;
    }

    private MyPluginSession getMyPluginSession(PluginSession session) {
        if (!(session instanceof MyPluginSession)) {
            throw new PluginOperationRuntimeException(
                    "PluginSession is not an instance of MyPluginSession", null);
        }
        return (MyPluginSession) session;
    }

    @Override
    public PluginSession startSession() {
        return new MyPluginSession(callback);
    }

    @Override
    public String getHost() {
        // If this plugin utilizes SSL to talk to remote servers, the hostname of the server being
        // connected to should be returned here. Plugins which do not use SSL can safely return
        // null.
        return null;
    }

    @Override
    public Integer getPort() {
        // If this plugin utilizes SSL to talk to remote servers, the port of the server being
        // connected to should be returned here. Plugins which do not use SSL can safely return
        // null.
        return null;
    }

    @Override
    public Document root(PluginSession session) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.getRootDocument();
    }

    @Override
    public Iterator<Document> listContainers(PluginSession session, Document startDocument) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.listContainers(); // Only 1 container to list
    }

    @Override
    public Iterator<Document> list(PluginSession session, Document startDocument) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.list(); // Only 1 container to list
    }

    @Override
    public Document getMetadata(PluginSession session, URI uri)
            throws PluginOperationFailedException {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.getMetadata(uri);
    }

    @Override
    public InputStream get(PluginSession session, URI uri) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.get(uri);
    }

    @Override
    public InputStream openNamedStream(PluginSession session, Document doc,
                                       String streamName) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.openNamedStream(doc, streamName);
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
    public ConnectorMode getMode() {
        // This method defines the operating mode of the connector.
        // You may utilize the PluginConfig to have end users configure the operating mode
        // of this connector. CRAWL_LIST mode, for example, indicates that the crawler
        // should utilize the "list" API to obtain documents. It could alternatively be
        // configured to use the "getChanges" API by specifying CRAWL_GET_CHANGES here.
        return ConnectorMode.CRAWL_LIST;
    }

    @Override
    public DocumentPagedResults getChanges(PluginSession pluginSession, String eventToken)
            throws ConfigurationException, PluginOperationFailedException {
        // This plugin does not support this API, and declares so in the supports() method below.
        throw new PluginOperationFailedException("Operation not supported");
    }

    @Override
    public void test(PluginSession pluginSession)
            throws ConfigurationException, PluginOperationFailedException {
        // Implement test operations for this plugin here and
        // throw PluginOperationFailedException if the test did not succeed.
    }

    @Override
    public boolean supports(ConnectorOptionalMethod connectorOptionalMethod) {
        // Returns that methods that this plugin supports, and are allowed
        // be called by the system using this data connection.
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
    public Action getAction(String name) throws ConfigurationException {
        switch (name) {
            case EXAMPLE_NAME:
                return EXAMPLE_ACTION;
            default:
                throw new ConfigurationException(name + " not a valid action name");
        }
    }

    @Override
    public DocumentActionProcessor getActionProcessor() {
        return this;
    }

    @Override
    public List<Action> listActions(Boolean all) {
        List<Action> actions = new ArrayList<>();
        actions.add(EXAMPLE_ACTION);
        return actions;
    }

    @Override
    public Iterator<Document> executeAction(PluginSession session, Action action,
                                            List<Document> documents) {
        List<Document> docs = new ArrayList<>();
        documents.forEach(d -> {
            DocumentBuilder builder = callback.documentBuilder().copy(d);
            switch (action.getName()) {
                case EXAMPLE_NAME:
                    builder.setMetadata("Example",
                                        StringDocumentFieldValue.builder()
                                                .setString("example-action").build());
                    break;
            }
            docs.add(builder.build());
        });
        return docs.iterator();
    }

    @Override
    public void flush(PluginSession session) {
    }

    // An example plugin session object
    private class MyPluginSession implements PluginSession {
        PluginCallback callback;

        MyPluginSession(PluginCallback callback) {
            // Can initiate a connection to a remote server here, and utilize this
            // session across API calls.
            this.callback = callback;
        }

        @Override
        public void close() {
            // Can shutdown the connection to remote servers and free resources here.
        }

        // For example purposes, return a root Document
        public Document getRootDocument() {
            return callback.documentBuilder()
                    .setIsContainer(true)
                    .addMetadata("rootMessage", StringDocumentFieldValue.builder()
                            .setString("This is a root document folder").build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString("example://folder1").build())
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString("example://folder1").build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString("Folder1").build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .build();
        }

        // For example purposes, return a container Document
        public Iterator<Document> listContainers() {
            return new StreamingDocumentIterator() {

                // Computes the next Document to return to the processing pipeline.
                // When there are no more documents to return, returns endOfDocuments().
                @Override
                protected Document getNextDocument() {
                    // No containers other than the root() in this example
                    return endOfDocuments();
                }
            };
        }

        // For example purposes, get metadata for an example Document
        public Document getMetadata(URI uri) throws PluginOperationFailedException {
            if (uri.toString().endsWith("folder1")) {
                return getRootDocument();
            } else if (uri.toString().endsWith("1")) {
                return getDocument("1");
            } else if (uri.toString().endsWith("2")) {
                return getDocument("2");
            } else if (uri.toString().endsWith("3")) {
                return getDocument("3");
            }
            throw new PluginOperationFailedException("Document not found");
        }

        // For example purposes, open an example Document stream
        public InputStream get(URI uri) {
            // Open this documents content stream from the data source.
            // For example purposes, read a fake stream here.
            return new ByteArrayInputStream(("This is searchable text for document \'" + uri + "\'")
                    .getBytes(StandardCharsets.UTF_8));
        }

        // For example purposes, open an example Document stream
        public InputStream openNamedStream(Document document, String streamName) {
            // Check that requested stream exists on the document
            if (!document.getStreamNames().contains(streamName)) {
                return null;
            }
            // Open the stream from the data source.
            // For example purposes, read a fake stream here.
            return new ByteArrayInputStream(
                    ("This is searchable text for stream \'" + streamName + "\'")
                            .getBytes(StandardCharsets.UTF_8));
        }

        // For example purposes, list all example documents
        public Iterator<Document> list() {
            final List<Document> documents = new ArrayList<>();
            documents.add(getDocument("1"));
            documents.add(getDocument("2"));
            documents.add(getDocument("3"));

            return new StreamingDocumentIterator() {
                final Iterator<Document> docIter = documents.iterator();

                // Computes the next Document to return.
                // When there are no more documents to return, returns endOfDocuments().
                @Override
                protected Document getNextDocument() {
                    if (docIter.hasNext()) {
                        return docIter.next();
                    }
                    return endOfDocuments();
                }
            };
        }

        // Obtain an example document with the given name suffix
        private Document getDocument(String name) {
            HashMap<String, String> contentStreamMetadata = new HashMap<>();
            // Optionally add metadata about this stream (e.g. it's size, etc.)
            contentStreamMetadata.put("ExampleStreamMetadataKey", "ExampleStreamMetadataValue");
            return callback.documentBuilder()
                    .addMetadata("document" + name + "Message", StringDocumentFieldValue.builder()
                            .setString("This is document " + name).build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString("example://folder1/doc" + name).build())
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString("example://folder1/doc" + name).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString("Document " + name).build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }
    }
}

