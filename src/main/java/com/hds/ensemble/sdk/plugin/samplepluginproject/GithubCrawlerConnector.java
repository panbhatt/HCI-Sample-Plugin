/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hds.ensemble.sdk.plugin.samplepluginproject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
import com.hds.ensemble.sdk.plugin.PluginConfig.Builder;
import com.hds.ensemble.sdk.plugin.PluginSession;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;

public class GithubCrawlerConnector implements ConnectorPlugin, DocumentActionProcessor {

    private static final Logger log = LogManager.getLogger(GithubCrawlerConnector.class);

    private static final String PLUGIN_NAME = "com.hds.panbhatt.github.GithubCrawlerConnector";
    private static final String DISPLAY_NAME = "GITHUB Crawler";
    private static final String DESCRIPTION = "URL Crawler Data Connection";

    private static final String SUBCATEGORY_NAME = "Connector Plugin";

    private final PluginCallback callback;
    private final PluginConfig pluginConfig;

    private final static String GITHUB_URL = "GITHUB_URL";
    private final static String GITHUB_TOKEN = "GITHUB_TOKEN";

    private Github github;
    private Repo repo;
    private ArrayList<Document> documentList;

    public static final ConfigProperty.Builder GITHUB_REPOSITORY_URL_PROPERTY = new ConfigProperty.Builder().setName(GITHUB_URL)
            .setValue("Enter GITHUB URL for crawling").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("Enter GITHUB Repository URL")
            .setUserVisibleDescription("This URL entered would be considered for crawling.");

    public static final ConfigProperty.Builder GITHUB_TOKEN_PROPERTY = new ConfigProperty.Builder().setName(GITHUB_TOKEN)
            .setValue("Enter GITHUB OAUTH TOKEN").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("Enter Github OAUTH Token")
            .setUserVisibleDescription("This OAUTH TOKEN would be used to read the Github Details.");

    /* public static final ConfigProperty.Builder URL_EXCLUSION_REGEX_PROPERTY = new ConfigProperty.Builder().setName("exRegEx")
            .setValue("Enter regular expression for URLs to exclude in crawl").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("Enter RegEx Exclude")
            .setUserVisibleDescription("This regular expression will be used for evaluating URLs for crawl exclusion.");

    public static final ConfigProperty.Builder MAX_DEPTH_PROPERTY = new ConfigProperty.Builder().setName("MaxDepth")
            .setValue("Enter depth of directory search").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("Enter Max Depth")
            .setUserVisibleDescription("This will determine the maximum directory depth searched during the crawl.");*/
    private static List<ConfigProperty.Builder> url_input_Properties = new ArrayList<>();

    static {
        url_input_Properties.add(GITHUB_REPOSITORY_URL_PROPERTY);
        url_input_Properties.add(GITHUB_TOKEN_PROPERTY);
        /* url_input_Properties.add(URL_EXCLUSION_REGEX_PROPERTY);
        url_input_Properties.add(MAX_DEPTH_PROPERTY);*/
    }

    public static final ConfigPropertyGroup.Builder URL_GROUP_PROPERTY = new ConfigPropertyGroup.Builder("URL Path",
            null).setType(PropertyGroupType.DEFAULT).setConfigProperties(url_input_Properties);

    public static PluginConfig DEFAULT_CONFIG = PluginConfig.builder().addGroup(URL_GROUP_PROPERTY).build();

    // public PluginConfig DEFAULT_CONFIG = PluginConfig.builder().build();
    private static final ConfigProperty PROPERTY_ACTION = new ConfigProperty.Builder().setName("URL Path")
            .setValue("Action url crawling").setRequired(true).setUserVisibleName("URL Path")
            .setUserVisibleDescription("This URL would be used for crawling").setType(PropertyType.TEXT).build();

    private static final String ACTION_GROUP_CONFIG_NAME = "Action Config Settings";
    private static final PluginConfig ACTION_CONFIG = PluginConfig.builder()
            .setGroup(new ConfigPropertyGroup.Builder().setName(ACTION_GROUP_CONFIG_NAME)
                    .setConfigProperties(Collections.singletonList(new ConfigProperty.Builder(PROPERTY_ACTION))))
            .build();

    private static final String ACTION_BUILDER_NAME = "Action Builder";

    private static final Action ACTION_BUILDER = Action.builder().name(ACTION_BUILDER_NAME)
            .description("URL crawler connector").config(ACTION_CONFIG)
            .types(EnumSet.of(ActionType.OUTPUT, ActionType.STAGE)).build();

    public GithubCrawlerConnector() {
        this.pluginConfig = null;
        this.callback = null;
        //basic log4j configuration
        BasicConfigurator.configure();
    }

    // Constructor for configured plugin instances to be used in workflows
    private GithubCrawlerConnector(PluginConfig config, PluginCallback callback) throws ConfigurationException {
        this.pluginConfig = config;
        this.callback = callback;

        Builder configBuild = PluginConfig.builder(pluginConfig);
        DEFAULT_CONFIG = configBuild.build();
    }

    @Override
    public void validateConfig(PluginConfig config) throws ConfigurationException {
        Config.validateConfig(getDefaultConfig(), config);

        if (config.getPropertyValue(GITHUB_REPOSITORY_URL_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: Github URL");
        }
        if (config.getPropertyValue(GITHUB_TOKEN_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: GIthub Token");
        }
        /*  if (config.getPropertyValue(URL_EXCLUSION_REGEX_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: Inclusion RegEx");
        }
        if (config.getPropertyValue(MAX_DEPTH_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: Max Depth");
        }*/
    }

    @Override
    public GithubCrawlerConnector build(PluginConfig config, PluginCallback callback) throws ConfigurationException {
        validateConfig(config);

        // This method is used as a factory to create a configured instance of
        // this connector
        return new GithubCrawlerConnector(config, callback);
    }

    @Override
    public String getName() {
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
    public PluginConfig getDefaultConfig() {
        // This method is used to specify a default plugin configuration.
        return DEFAULT_CONFIG;
    }

    private MyPluginSession getMyPluginSession(PluginSession session) {
        if (!(session instanceof MyPluginSession)) {
            throw new PluginOperationRuntimeException("PluginSession is not an instance of MyPluginSession", null);
        }
        return (MyPluginSession) session;
    }

    @Override
    public PluginSession startSession() {
        return new MyPluginSession(callback);
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public Integer getPort() {
        return null;
    }

    @Override
    public Document root(PluginSession session) throws ConfigurationException {
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
        //We are not going to use startDocument in any way here
        try {
            return myPluginSession.list(session);
        } catch (PluginOperationFailedException e) {
            log.error("A plugin operation failed: " + e);
        } // Only 1 container to list
        return null;
    }

    @Override
    public Document getMetadata(PluginSession session, URI uri)
            throws PluginOperationFailedException {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.getMetadata(uri);
    }

    public void getDocuments(PluginSession session, URI uri)
            throws PluginOperationFailedException {
        MyPluginSession myPluginSession = getMyPluginSession(session);

        URL url = null;
        Document myDocument = null;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new PluginOperationFailedException("The provided configuration URL is not acceptable: " + e);
        }

        try {

            Iterable<Issue> issueIterator = repo.issues().iterate(new HashMap<String, String>());
            for (Issue issue : issueIterator) {
                myDocument = myPluginSession.getDocument(issue);
                documentList.add(myDocument);
            }

        } catch (Exception e) {
            throw new PluginOperationFailedException("An error was thrown while attempting to evaluate the URL: " + e);
        }
    }

    @Override
    public InputStream get(PluginSession session, URI uri) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.get(uri);
    }

    @Override
    public InputStream openNamedStream(PluginSession session, Document doc, String streamName) {
        MyPluginSession myPluginSession = getMyPluginSession(session);
        return myPluginSession.openNamedStream(doc, streamName);
    }

    @Override
    public ConnectorPluginCategory getCategory() {
        return ConnectorPluginCategory.CUSTOM;
    }

    @Override
    public String getSubCategory() {
        return SUBCATEGORY_NAME;
    }

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.CRAWL_LIST;
    }

    @Override
    public DocumentPagedResults getChanges(PluginSession pluginSession, String eventToken)
            throws ConfigurationException, PluginOperationFailedException {
        throw new PluginOperationFailedException("Operation not supported");
    }

    @Override
    public void test(PluginSession pluginSession) throws ConfigurationException, PluginOperationFailedException {
        // Write Logic here to test things. 
    }

    @Override
    public boolean supports(ConnectorOptionalMethod connectorOptionalMethod) {
        boolean supports = false;
        switch (connectorOptionalMethod) {
            case ROOT:
            case LIST_CONTAINERS:
            case LIST:
                supports = true;
            default:
                break;
        }
        return supports;
    }

    @Override
    public Action getAction(String name) throws ConfigurationException {
        switch (name) {
            case ACTION_BUILDER_NAME:
                return ACTION_BUILDER;
            default:
                throw new ConfigurationException(name + " not a valid action name");
        }
    }

    @Override
    public List<Action> listActions(Boolean all) {
        List<Action> actions = new ArrayList<>();
        actions.add(ACTION_BUILDER);
        return actions;
    }

    @Override
    public Iterator<Document> executeAction(PluginSession session, Action action, List<Document> documents) {
        List<Document> docs = new ArrayList<>();
        documents.forEach(d -> {
            DocumentBuilder builder = callback.documentBuilder().copy(d);
            switch (action.getName()) {
                case ACTION_BUILDER_NAME:
                    builder.setMetadata("Action", StringDocumentFieldValue.builder().setString("execute-action").build());
                    break;
            }
            docs.add(builder.build());
        });
        return docs.iterator();
    }

    @Override
    public void flush(PluginSession session) {
    }

    private class MyPluginSession implements PluginSession {

        PluginCallback callback;

        MyPluginSession(PluginCallback callback) {
            this.callback = callback;
        }

        @Override
        public void close() {
        }

        /*
		 * Our first step is to get the root document from the initial URL that was provided to us
		 * We evaluate our URL.  If good, we build a document with some basic metadata
		 * For our purposes, we want nothing more than a list of URLs.  So we will create documents with only the URI attribute
		 * -URI
         */
        public Document getRootDocument() throws ConfigurationException {
            //Take the instantiated URL list that was given
            //String url = DEFAULT_CONFIG.getPropertyValue(GITHUB_URL);

            String url = DEFAULT_CONFIG.getPropertyValue(GITHUB_URL);
            String token = DEFAULT_CONFIG.getPropertyValue(GITHUB_TOKEN);
            System.out.println("TOKEN = " + token);
            github = new RtGithub(token);
            repo = github.repos().get(new Coordinates.Simple("jcabi", "jcabi-github"));

            return callback.documentBuilder()
                    .setIsContainer(true)
                    .addMetadata("rootMessage", StringDocumentFieldValue.builder()
                            .setString("This is a REPO PLUGIN").build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString(url).build())
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString(url).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString("GITHUB REPO").build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .build();
        }

        public Iterator<Document> listContainers() {
            return new StreamingDocumentIterator() {

                @Override
                protected Document getNextDocument() {
                    return endOfDocuments();
                }
            };
        }

        public InputStream get(URI uri) {
            return openNamedStream(null, uri.toString());
        }

        public InputStream openNamedStream(Document document, String streamName) {
            return new ByteArrayInputStream(
                    ("This is searchable text for stream \'" + streamName + "\'").getBytes(StandardCharsets.UTF_8));
        }

        // Part of the plugin testing that will occur is to 
        public Document getMetadata(URI uri) throws PluginOperationFailedException {
            HashMap<String, String> contentStreamMetadata = new HashMap<>();
            return callback.documentBuilder()
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString(uri.toString()).build())
                    .addMetadata("URI: " + uri, StringDocumentFieldValue.builder()
                            .setString("This is document for URI " + uri).build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString("//folder1/doc/" + uri).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString(uri.toString()).build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }

        public Iterator<Document> list(PluginSession session) throws PluginOperationFailedException {

            String url = DEFAULT_CONFIG.getPropertyValue(GITHUB_URL);
            String token = DEFAULT_CONFIG.getPropertyValue(GITHUB_TOKEN);
            URI uri = null;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new PluginOperationFailedException("The provided configuration URL is not acceptable: " + e);
            }

            documentList = new ArrayList<Document>();
            getDocuments(session, uri);

            return new StreamingDocumentIterator() {
                Iterator<Document> docIter = documentList.iterator();

                @Override
                protected Document getNextDocument() {
                    if (docIter.hasNext()) {
                        return docIter.next();
                    }
                    return endOfDocuments();
                }
            };
        }

        /*Obtain a document with the given URI
         * We are simply building a list of document with a single attribute of their URL
         * We let a stage take this URL and evaluate it further for content
         * 
         * However, we have these additional fields of testing in HCI.  Even for a simple data connector it wants to
         * test the connector by looking at it's ID, version, display name, description and even content. 
         * So we have mocked up some here for each of our "URL" documents.
         * 
         * I have not implemented any functionality around things like incrementing the version number
         * because I don't see a need for it yet.  We would only reach one version of each of these URLs.
         * If they change, then it is not the same URL, so it's a new document
         */
        private Document getDocument(Issue issue) {
            HashMap<String, String> contentStreamMetadata = new HashMap<>();
            // Optionally add metadata about this stream (e.g. it's size, etc.)
            contentStreamMetadata.put("ExampleStreamMetadataKey", "ExampleStreamMetadataValue");
            String uri = GITHUB_URL + "/issues/" + issue.number();
            final StringBuilder labels = new StringBuilder();
            issue.labels().iterate().forEach((label) -> {
                labels.append(label.name()).append(" ");
            });
            return callback.documentBuilder()
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString(uri).build())
                    .addMetadata("URI: " + uri, StringDocumentFieldValue.builder()
                            .setString("This is document for URI " + uri).build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString("//folder1/doc/" + uri).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString(uri).build())
                    .addMetadata("HCI_Labels", StringDocumentFieldValue.builder()
                            .setString(labels.toString()).build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }

    }
}
