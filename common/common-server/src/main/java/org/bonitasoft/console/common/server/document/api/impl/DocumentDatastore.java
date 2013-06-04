/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.common.server.document.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;

import org.bonitasoft.console.common.client.ViewType;
import org.bonitasoft.console.common.client.document.model.ArchivedDocumentItem;
import org.bonitasoft.console.common.client.document.model.DocumentItem;
import org.bonitasoft.console.common.server.preferences.properties.PropertiesFactory;
import org.bonitasoft.console.common.server.utils.DateUtil;
import org.bonitasoft.console.common.server.utils.DocumentUtil;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.document.ArchivedDocument;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentAttachmentException;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.NotFoundException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.InvalidSessionException;

/**
 * Document data store
 * 
 * @author Yongtao Guo
 * 
 */
public class DocumentDatastore {

    private final APISession apiSession;

    public final static String CREATE_NEW_VERSION_DOCUMENT = "AddNewVersionDocument";

    public final static String CREATE_NEW_DOCUMENT = "AddNewDocument";

    /**
     * Default constructor.
     */
    public DocumentDatastore(final APISession apiSession) {
        this.apiSession = apiSession;
    }

    public SearchResult<Document> searchDocuments(final long userId, final String viewType, final SearchOptionsBuilder builder) throws InvalidSessionException,
            BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, SearchException, NotFoundException, ServletException {
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        SearchResult<Document> result = null;
        switch (ViewType.valueOf(viewType)) {
            case ADMINISTRATOR:
                result = processAPI.searchDocuments(builder.done());
            case USER:
                result = processAPI.searchDocumentsSupervisedBy(userId, builder.done());
                break;
            case TEAM_MANAGER:
                result = processAPI.searchDocuments(builder.done());
                break;
            case PROCESS_OWNER:
                result = processAPI.searchDocumentsSupervisedBy(userId, builder.done());
                break;
            default:
                throw new ServletException("Invalid view type.");

        }

        return result;
    }

    public SearchResult<ArchivedDocument> searchArchivedDocuments(final long userId, final String viewType, final SearchOptionsBuilder builder)
            throws InvalidSessionException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, SearchException, NotFoundException,
            ServletException {
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        SearchResult<ArchivedDocument> result = null;
        switch (ViewType.valueOf(viewType)) {
            case ADMINISTRATOR:
            case USER:
            case TEAM_MANAGER:
                result = processAPI.searchArchivedDocuments(builder.done());
                break;
            case PROCESS_OWNER:
                result = processAPI.searchArchivedDocumentsSupervisedBy(userId, builder.done());
                break;
            default:
                throw new ServletException("Invalid view type.");

        }
        return result;
    }

    public DocumentItem createDocument(final long processInstanceId, final String documentName, final String documentCreationType, final String path) 
                throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, DocumentException, IOException, ProcessInstanceNotFoundException, DocumentAttachmentException, InvalidSessionException, ProcessDefinitionNotFoundException, RetrieveException {

        DocumentItem item = new DocumentItem();
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        String fileName = null;
        String mimeType = null;
        byte[] fileContent = null;
        final File theSourceFile = new File(path);
        if (theSourceFile.exists()) {
            final long maxSize = PropertiesFactory.getConsoleProperties(this.apiSession.getTenantId()).getMaxSize();
            if (theSourceFile.length() > maxSize * 1048576) {
                final String errorMessage = "This document is exceeded " + maxSize + "Mo";
                throw new DocumentException(errorMessage);
            }
            fileContent = DocumentUtil.getArrayByteFromFile(theSourceFile);
            if (theSourceFile.isFile()) {
                fileName = theSourceFile.getName();
                final FileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
                mimeType = mimetypesFileTypeMap.getContentType(theSourceFile);
            }
        }
        // Attach a new document to a case
        if (CREATE_NEW_DOCUMENT.equals(documentCreationType)) {
            final Document document = processAPI.attachDocument(processInstanceId, documentName, fileName, mimeType, fileContent);
            item = mapToDocumentItem(document);
        } else if (CREATE_NEW_VERSION_DOCUMENT.equals(documentCreationType)) {
            final Document document = processAPI.attachNewDocumentVersion(processInstanceId, documentName, fileName, mimeType, fileContent);
            item = mapToDocumentItem(document);
        }
        return item;
    }

    public DocumentItem createDocumentFromUrl(final long processInstanceId, final String documentName, final String documentCreationType, final String path)
            throws InvalidSessionException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, ProcessInstanceNotFoundException,
            DocumentAttachmentException, IOException, RetrieveException, ProcessDefinitionNotFoundException {

        DocumentItem item = new DocumentItem();
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        final String fileName = DocumentUtil.getFileNameFromUrl(path);
        final String mimeType = DocumentUtil.getMimeTypeFromUrl(path);
        if (fileName != null && mimeType != null) {
            // Attach a new document to a case
            if (CREATE_NEW_DOCUMENT.equals(documentCreationType)) {
                final Document document = processAPI.attachDocument(processInstanceId, documentName, fileName, mimeType, path);
                item = mapToDocumentItem(document);
            } else if (CREATE_NEW_VERSION_DOCUMENT.equals(documentCreationType)) {
                final Document document = processAPI.attachNewDocumentVersion(processInstanceId, documentName, fileName, mimeType, path);
                item = mapToDocumentItem(document);
            }
        }

        return item;
    }

    public DocumentItem mapToDocumentItem(final Document document) throws InvalidSessionException, BonitaHomeNotSetException, ServerAPIException,
            UnknownAPITypeException, ProcessDefinitionNotFoundException, RetrieveException {

        if (document == null) {
            throw new IllegalArgumentException("The document must be not null!");
        }
        DocumentItem item = new DocumentItem();
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        ProcessInstance processInstance;
        String caseName = "";
        String processDisplayName = "";
        String version = "";
        try {
            processInstance = processAPI.getProcessInstance(document.getProcessInstanceId());
            caseName = processInstance.getName();
            final ProcessDeploymentInfo processDeploymentInfo = processAPI.getProcessDeploymentInfo(processInstance.getProcessDefinitionId());
            processDisplayName = processDeploymentInfo.getDisplayName();
            version = processDeploymentInfo.getVersion();
        } catch (final ProcessInstanceNotFoundException e) {
            item = buildDocumentItem(caseName, processDisplayName, version, document);
            return item;
        }

        item = buildDocumentItem(caseName, processDisplayName, version, document);
        return item;

    }

    private DocumentItem buildDocumentItem(final String caseName, final String processDisplayName, final String version, final Document document) {
        final DocumentItem item = new DocumentItem();
        item.setDocumentId(String.valueOf(document.getId()));
        item.setCaseId(String.valueOf(document.getProcessInstanceId()));
        item.setDocumentName(document.getName());
        item.setDocumentAuthor(document.getAuthor());
        item.setDocumentFileName(document.getContentFileName());
        item.setDocumentCreationDate(DateUtil.parseDate(document.getCreationDate()));
        item.setDocumentMIMEType(document.getContentMimeType());
        item.setDocumentHasContent(String.valueOf(document.hasContent()));
        item.setDocumentStorageId(document.getContentStorageId());
        item.setDocumentURL(document.getUrl());
        item.setProcessDisplayName(processDisplayName);
        item.setProcessVersion(version);
        item.setCaseName(caseName);
        return item;
    }

    public ArchivedDocumentItem mapToArchivedDocumentItem(final ArchivedDocument document) throws InvalidSessionException, BonitaHomeNotSetException,
            ServerAPIException, UnknownAPITypeException, RetrieveException, ProcessDefinitionNotFoundException {

        if (document == null) {
            throw new IllegalArgumentException("The document must be not null!");
        }
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(this.apiSession);
        ArchivedDocumentItem item = new ArchivedDocumentItem();
        String caseName = "";
        String processDisplayName = "";
        String version = "";

        List<ArchivedProcessInstance> archivedCaseList;
        try {
            archivedCaseList = processAPI.getArchivedProcessInstances(document.getProcessInstanceId(), 0, 1);
            final ArchivedProcessInstance processInstance = archivedCaseList.get(0);
            caseName = processInstance.getName();
            final ProcessDeploymentInfo processDeploymentInfo = processAPI.getProcessDeploymentInfo(processInstance.getProcessDefinitionId());
            processDisplayName = processDeploymentInfo.getDisplayName();
            version = processDeploymentInfo.getVersion();
        } catch (final NotFoundException e) {
            item = buildArchivedDocumentItem(caseName, processDisplayName, version, document);
            return item;
        }

        item = buildArchivedDocumentItem(caseName, processDisplayName, version, document);
        return item;

    }

    private ArchivedDocumentItem buildArchivedDocumentItem(final String caseName, final String processDisplayName, final String version,
            final ArchivedDocument document) {
        final ArchivedDocumentItem item = new ArchivedDocumentItem();
        item.setDocumentId(String.valueOf(document.getSourceObjectId()));
        item.setDocumentSourceObjectId(String.valueOf(document.getSourceObjectId()));
        item.setCaseId(String.valueOf(document.getProcessInstanceId()));
        item.setDocumentName(document.getName());
        item.setDocumentAuthor(document.getDocumentAuthor());
        item.setDocumentFileName(document.getDocumentContentFileName());
        item.setDocumentCreationDate(DateUtil.parseDate(document.getDocumentCreationDate()));
        item.setDocumentMIMEType(document.getDocumentContentMimeType());
        item.setDocumentHasContent(String.valueOf(document.getDocumentHasContent()));
        item.setDocumentStorageId(document.getContentStorageId());
        item.setDocumentURL(document.getDocumentURL());
        item.setProcessDisplayName(processDisplayName);
        item.setProcessVersion(version);
        item.setCaseName(caseName);
        item.setArchivedDate(DateUtil.parseDate(document.getArchiveDate()));
        return item;
    }

}
