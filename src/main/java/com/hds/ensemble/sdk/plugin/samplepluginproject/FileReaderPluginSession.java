/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hds.ensemble.sdk.plugin.samplepluginproject;

import com.hds.ensemble.sdk.exception.PluginOperationFailedException;
import com.hds.ensemble.sdk.model.Document;
import com.hds.ensemble.sdk.model.StandardFields;
import com.hds.ensemble.sdk.model.StreamingDocumentIterator;
import com.hds.ensemble.sdk.model.StringDocumentFieldValue;
import com.hds.ensemble.sdk.plugin.PluginCallback;
import com.hds.ensemble.sdk.plugin.PluginSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class FileReaderPluginSession implements PluginSession {

    PluginCallback callback;
    String dir ; 

    FileReaderPluginSession(PluginCallback callback,String dir ) {
        // Can initiate a connection to a remote server here, and utilize this
        // session across API calls.
        this.callback = callback;
        this.dir = dir; 
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
        
        String str = uri.toString().substring(8) ;
        System.out.println("String = " + str); 
        return getDocument(str) ;
        /*if (uri.toString().endsWith("folder1")) {
            return getRootDocument();
        } else if (uri.toString().endsWith("1")) {
            return getDocument("1");
        } else if (uri.toString().endsWith("2")) {
            return getDocument("2");
        } else if (uri.toString().endsWith("3")) {
            return getDocument("3");
        }
        throw new PluginOperationFailedException("Document not found");*/
    }

    // For example purposes, open an example Document stream
    public InputStream get(URI uri) {
        // Open this documents content stream from the data source.
        // For example purposes, read a fake stream here.
        return new ByteArrayInputStream(("This is searchable text for document \'" + uri + "\'")
                .getBytes(StandardCharsets.UTF_8));
    }

    // For example purposes, open an example Document stream
    public InputStream openNamedStream(Document document, String streamName) throws IOException {
        // Check that requested stream exists on the document
        if (!document.getStreamNames().contains(streamName)) {
            return null;
        }
        // Open the stream from the data source.
        // For example purposes, read a fake stream here.
        File f = new File(streamName) ;
        return new ByteArrayInputStream(FileUtils.readFileToByteArray(f)) ;
        /*return new ByteArrayInputStream(
                ("This is searchable text for stream \'" + streamName + "\'")
                        .getBytes(StandardCharsets.UTF_8)); */
    }

    // For example purposes, list all example documents
    public Iterator<Document> list() {
        final List<Document> documents = new ArrayList<>();
        final List<String> fileNames = printFilenames("C:\\temp\\Agreements_Dump") ;
        
        fileNames.forEach((fileName) -> {
            System.out.println("FILE NAME = " + fileName); 
            documents.add(getDocument(fileName));
        });

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

    public static List<String> printFilenames(String sDir) {
        List<String> fileList = new ArrayList<>();
        System.out.println("DIR = " + sDir); 
        try {
            Files.find(Paths.get(sDir), 999, (p, bfa) -> bfa.isRegularFile()).forEach((fileName) -> fileList.add(fileName.toAbsolutePath().toString().replace("\\","/")));
        } catch (IOException ex) {
            System.out.println("Exception Occured. " + ex); 
        }
        System.out.println("FILE LIST SIZE  = " + fileList.size());
        return fileList;
    }

    // Obtain an example document with the given name suffix
    private Document getDocument(String name) {
        HashMap<String, String> contentStreamMetadata = new HashMap<>();
        // Optionally add metadata about this stream (e.g. it's size, etc.)
        contentStreamMetadata.put("streamMetaData_Path", name);
        return callback.documentBuilder()
                .addMetadata("document " + name + " Message", StringDocumentFieldValue.builder()
                        .setString("This is document " + name).build())
                .addMetadata(StandardFields.ID, StringDocumentFieldValue.builder()
                        .setString(name).build())
                .addMetadata(StandardFields.URI, StringDocumentFieldValue.builder()
                        .setString("file:///"+name).build())
                .addMetadata(StandardFields.DISPLAY_NAME, StringDocumentFieldValue.builder()
                        .setString("Document " + name).build())
                .addMetadata(StandardFields.VERSION, StringDocumentFieldValue.builder()
                        .setString("1.0").build())
                .setStreamMetadata(StandardFields.CONTENT, contentStreamMetadata )
                .build();
    }
}
