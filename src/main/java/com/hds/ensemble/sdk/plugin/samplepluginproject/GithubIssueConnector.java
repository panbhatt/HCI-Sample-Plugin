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
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class GithubIssueConnector implements ConnectorPlugin, DocumentActionProcessor {

    private static final Logger log = LogManager.getLogger(GithubIssueConnector.class);

    private static final String PLUGIN_NAME = "com.hds.ensemble.sdk.plugin.github.GithubIssueConnector";
    private static final String DISPLAY_NAME = "GITHUB ISSUE Crawler";
    private static final String DESCRIPTION = "GITHUB ISSUE CRAWLER - Creating documents from all the issues for a particular REPO.";
    private static final String LONG_DESCRIPTION = "GITHUB Issue crawler - navigating through all the issuesl for a specific GITHUB Repository. ";

    private static final String SUBCATEGORY_NAME = "Connector Plugin";

    private final PluginCallback callback;
    private final PluginConfig pluginConfig;

    private final static String GITHUB_URL = "GITHUB_URL";
    private final static String GITHUB_TOKEN = "GITHUB_TOKEN";

    private GHRepository repo;

    private ArrayList<Document> documentList;

    public static final ConfigProperty.Builder GITHUB_REPOSITORY_URL_PROPERTY = new ConfigProperty.Builder().setName(GITHUB_URL)
            .setValue("<username>/<repo>").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("Enter GITHUB Repository URL")
            .setUserVisibleDescription("This URL entered would be considered for crawling.");

    public static final ConfigProperty.Builder GITHUB_TOKEN_PROPERTY = new ConfigProperty.Builder().setName(GITHUB_TOKEN)
            .setValue("<adasdfasdasdfasdfadfadsasdsd>").setType(PropertyType.TEXT).setRequired(true)
            .setUserVisibleName("GITHUB OAUTH TOKEN")
            .setUserVisibleDescription("This OAUTH TOKEN would be used to read the Github Details.");

    private static List<ConfigProperty.Builder> url_input_Properties = new ArrayList<>();

    static {
        url_input_Properties.add(GITHUB_REPOSITORY_URL_PROPERTY);
        url_input_Properties.add(GITHUB_TOKEN_PROPERTY);

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

    public GithubIssueConnector() {
        this.pluginConfig = null;
        this.callback = null;
        //basic log4j configuration
        BasicConfigurator.configure();
    }

    // Constructor for configured plugin instances to be used in workflows
    private GithubIssueConnector(PluginConfig config, PluginCallback callback) throws ConfigurationException {
        this.pluginConfig = config;
        this.callback = callback;

        Builder configBuild = PluginConfig.builder(pluginConfig);
        DEFAULT_CONFIG = configBuild.build();
    }

    @Override
    public void validateConfig(PluginConfig config) throws ConfigurationException {
        Config.validateConfig(getDefaultConfig(), config);

        if (config.getPropertyValue(GITHUB_REPOSITORY_URL_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: Github URL format should be <username>/<reponame>");
        }
        if (config.getPropertyValue(GITHUB_TOKEN_PROPERTY.getName()) == null) {
            throw new ConfigurationException("Missing Property: GIthub Token");
        }

    }

    @Override
    public GithubIssueConnector build(PluginConfig config, PluginCallback callback) throws ConfigurationException {
        validateConfig(config);

        // This method is used as a factory to create a configured instance of
        // this connector
        return new GithubIssueConnector(config, callback);
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
    public String getLongDescription() {
        // A user-visible local description string used to document the behavior of this plugin
        // Uses "markdown" syntax described here: https://daringfireball.net/projects/markdown/syntax
        return LONG_DESCRIPTION;
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

        /* try {

           Iterable<Issue> issueIterator = repo.issues().iterate(new HashMap<String, String>());
            for (Issue issue : issueIterator) {
                myDocument = myPluginSession.getDocument(issue);
                documentList.add(myDocument);
            }
        } catch (Exception e) {
            throw new PluginOperationFailedException("An error was thrown while attempting to evaluate the URL: " + e);
        }*/
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

        String url = pluginConfig.getProperty(GITHUB_URL).getValue();
        String token = pluginConfig.getProperty(GITHUB_TOKEN).getValue();
        try {
            GitHub github = GitHub.connectUsingOAuth(token);
            this.repo = github.getRepository(url);

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            throw new ConfigurationException("UNABLE TO GET REPO -> " + ioEx.getMessage());
        }

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
        GHRepository repo;

        MyPluginSession(PluginCallback callback) {
            this.callback = callback;
            try {

                GitHub github = GitHub.connectUsingOAuth(pluginConfig.getProperty(GITHUB_TOKEN).getValue());
                this.repo = github.getRepository(pluginConfig.getProperty(GITHUB_URL).getValue());
                log.info("Successfully Connected to GITHUB URL " + this.repo.getHtmlUrl().toString());

            } catch (IOException ex) {
                ex.printStackTrace();

            }
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

            /*String url = DEFAULT_CONFIG.getPropertyValue(GITHUB_URL);
            String token = DEFAULT_CONFIG.getPropertyValue(GITHUB_TOKEN);*/
            System.out.println(repo);
            String url = repo.getHtmlUrl().toString();

            return callback.documentBuilder()
                    .setIsContainer(true)
                    .addMetadata("rootMessage", StringDocumentFieldValue.builder()
                            .setString("This is a REPO ROOT FOLDER").build())
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
                    .addMetadata("GIT_USER", StringDocumentFieldValue.builder()
                            .setString("username").build())
                    .addMetadata("GIT_LABELS", StringDocumentFieldValue.builder()
                            .setString("labels").build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }

        public Iterator<Document> list(PluginSession session) throws PluginOperationFailedException {

            documentList = new ArrayList<Document>();

            getDocuments(session, repo.getHtmlUrl());
            log.info("TOTAL ISSUES = " + documentList.size());

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

        public void getDocuments(PluginSession session, URL url)
                throws PluginOperationFailedException {

            MyPluginSession myPluginSession = getMyPluginSession(session);
            try {
                List<GHIssue> issues;

                issues = repo.getIssues(GHIssueState.OPEN);

                issues.forEach((issue) -> {
                    try {
                        Document myDocument = getDocument(issue);
                        documentList.add(myDocument);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        log.error("Error occured, while processing issue no : " + issue.getNumber());

                    }
                }
                );
            } catch (IOException ex) {
                ex.printStackTrace();
                log.error("An IO Exception haso occured while getting the list of issues. ");
            }

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
        private Document getDocument(String uri) {
            HashMap<String, String> contentStreamMetadata = new HashMap<>();
            // Optionally gdd metadata about this stream (e.g. it's size, etc.)
            contentStreamMetadata.put("ExampleStreamMetadataKey", "ExampleStreamMetadataValue");

            return callback.documentBuilder()
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString(uri).build())
                    .addMetadata("URI: " + uri, StringDocumentFieldValue.builder()
                            .setString("This is document for URI " + uri).build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString(uri).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString(uri).build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }

        private Document getDocument(GHIssue issue) throws IOException {
            HashMap<String, String> contentStreamMetadata = new HashMap<>();
            // Optionally gdd metadata about this stream (e.g. it's size, etc.)
            contentStreamMetadata.put("ExampleStreamMetadataKey", "ExampleStreamMetadataValue");
            String issueUrl = issue.getUrl().toString();
            log.info(" PROCESSING -> " + issueUrl);

            return callback.documentBuilder()
                    .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                            .setString(issueUrl).build())
                    .addMetadata("URI: " + issueUrl, StringDocumentFieldValue.builder()
                            .setString("This is document for URI " + issueUrl).build())
                    .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                            .setString(issueUrl).build())
                    .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                            .setString(issue.getTitle()).build())
                    .addMetadata("GIT_USER", StringDocumentFieldValue.builder()
                            .setString(issue.getUser().getName()).build())
                    .addMetadata("GIT_LABELS", StringDocumentFieldValue.builder()
                            .setString(StringUtils.join(issue.getLabels(), " - ")).build())
                    .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                            .setString("1").build())
                    .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata)
                    .build();
        }

    }
}
