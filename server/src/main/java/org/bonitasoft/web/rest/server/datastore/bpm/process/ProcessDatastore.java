/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.web.rest.server.datastore.bpm.process;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.console.common.server.page.CustomPageService;
import org.bonitasoft.console.common.server.utils.BPMEngineException;
import org.bonitasoft.console.common.server.utils.BonitaHomeFolderAccessor;
import org.bonitasoft.console.common.server.utils.FormsResourcesUtils;
import org.bonitasoft.console.common.server.utils.PlatformManagementUtils;
import org.bonitasoft.console.common.server.utils.UnauthorizedFolderException;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoUpdater;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.model.bpm.process.ProcessItem;
import org.bonitasoft.web.rest.server.datastore.CommonDatastore;
import org.bonitasoft.web.rest.server.datastore.bpm.process.helper.ProcessItemConverter;
import org.bonitasoft.web.rest.server.datastore.bpm.process.helper.SearchProcessHelper;
import org.bonitasoft.web.rest.server.engineclient.EngineAPIAccessor;
import org.bonitasoft.web.rest.server.engineclient.EngineClientFactory;
import org.bonitasoft.web.rest.server.engineclient.ProcessEngineClient;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasAdd;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasDelete;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasGet;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasSearch;
import org.bonitasoft.web.rest.server.framework.api.DatastoreHasUpdate;
import org.bonitasoft.web.rest.server.framework.search.ItemSearchResult;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIForbiddenException;
import org.bonitasoft.web.toolkit.client.data.APIID;

/**
 * Process data store
 *
 * @author Vincent Elcrin
 */
public class ProcessDatastore extends CommonDatastore<ProcessItem, ProcessDeploymentInfo> implements
        DatastoreHasAdd<ProcessItem>,
        DatastoreHasUpdate<ProcessItem>,
        DatastoreHasGet<ProcessItem>,
        DatastoreHasSearch<ProcessItem>,
        DatastoreHasDelete {

    private static final Logger logger = Logger.getLogger(ProcessDatastore.class.getName());

    /**
     * process file
     */
    private static final String FILE_UPLOAD = "fileupload";

    private static final int DELETE_PAGES_BUNCH_SIZE = 100;

    public ProcessDatastore(final APISession engineSession) {
        super(engineSession);
    }

    @Override
    public ProcessItem add(final ProcessItem process) {
        final ProcessEngineClient engineClient = getProcessEngineClient();

        File processFile;
        try {
            processFile = getTenantFolder().getTempFile(process.getAttributes().get(FILE_UPLOAD), getEngineSession().getTenantId());
        } catch (final UnauthorizedFolderException e) {
            throw new APIForbiddenException(e.getMessage());
        } catch (final IOException e) {
            throw new APIException(e);
        }

        final BusinessArchive businessArchive = readBusinessArchive(processFile);
        final ProcessDefinition deployedArchive = engineClient.deploy(businessArchive);
        final ProcessDeploymentInfo processDeploymentInfo = engineClient.getProcessDeploymentInfo(deployedArchive.getId());

        try {
            FormsResourcesUtils.retrieveApplicationFiles(
                    getEngineSession(),
                    processDeploymentInfo.getProcessId(),
                    processDeploymentInfo.getDeploymentDate());
        } catch (final IOException e) {
            throw new APIException("", e);
        } catch (final ProcessDefinitionNotFoundException e) {
            throw new APIException("", e);
        } catch (final BPMEngineException e) {
            throw new APIException("", e);
        }

        return convertEngineToConsoleItem(processDeploymentInfo);
    }

    protected BonitaHomeFolderAccessor getTenantFolder() {
        final BonitaHomeFolderAccessor tenantFolder = new BonitaHomeFolderAccessor();
        return tenantFolder;
    }

    /*
     * Overridden in SP
     */
    protected BusinessArchive readBusinessArchive(final File file) {
        try {
            return BusinessArchiveFactory.readBusinessArchive(file);
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @Override
    public ProcessItem update(final APIID id, final Map<String, String> attributes) {
        final ProcessDeploymentInfoUpdater updater = new ProcessDeploymentInfoUpdater();

        final ProcessEngineClient engineClient = getProcessEngineClient();
        if (attributes.containsKey(ProcessItem.ATTRIBUTE_DISPLAY_DESCRIPTION)) {
            updater.setDisplayDescription(attributes.get(ProcessItem.ATTRIBUTE_DISPLAY_DESCRIPTION));
        }

        if (attributes.containsKey(ProcessItem.ATTRIBUTE_DISPLAY_NAME)) {
            updater.setDisplayName(attributes.get(ProcessItem.ATTRIBUTE_DISPLAY_NAME));
        }

        // specific engine methods
        if (attributes.containsKey(ProcessItem.ATTRIBUTE_ACTIVATION_STATE)) {
            changeProcessState(engineClient, id.toLong(), attributes.get(ProcessItem.ATTRIBUTE_ACTIVATION_STATE));
        }

        if (!updater.getFields().isEmpty()) {
            final ProcessDeploymentInfo processDeploymentInfo = engineClient.updateProcessDeploymentInfo(id.toLong(), updater);
            return convertEngineToConsoleItem(processDeploymentInfo);
        } else {
            return convertEngineToConsoleItem(engineClient.getProcessDeploymentInfo(id.toLong()));
        }
    }

    private void changeProcessState(final ProcessEngineClient engineClient, final Long processId, final String state) {
        if (ProcessItem.VALUE_ACTIVATION_STATE_DISABLED.equals(state)) {
            engineClient.disableProcess(processId);
        } else if (ProcessItem.VALUE_ACTIVATION_STATE_ENABLED.equals(state)) {
            engineClient.enableProcess(processId);
        }
    }

    protected PlatformManagementUtils getPlatformManagementUtils() {
        return new PlatformManagementUtils();
    }

    @Override
    public ProcessItem get(final APIID id) {
        final ProcessEngineClient engineClient = getProcessEngineClient();
        final ProcessDeploymentInfo processDeploymentInfo = engineClient.getProcessDeploymentInfo(id.toLong());
        return convertEngineToConsoleItem(processDeploymentInfo);
    }

    @Override
    public void delete(final List<APIID> ids) {
        for (final APIID id : ids) {
            removeProcessPagesFromHome(id);
        }
        final ProcessEngineClient engineClient = getProcessEngineClient();
        engineClient.deleteDisabledProcesses(APIID.toLongList(ids));
    }

    protected void removeProcessPagesFromHome(final APIID id) {
        try {
            int startIndex = 0;
            int count = 0;
            do {
                final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(startIndex, DELETE_PAGES_BUNCH_SIZE);
                searchOptionsBuilder.filter(PageSearchDescriptor.PROCESS_DEFINITION_ID, id.toLong());
                final SearchResult<Page> result = getPageAPI().searchPages(searchOptionsBuilder.done());
                if (count == 0) {
                    count = (int) result.getCount();
                }
                startIndex = startIndex + result.getResult().size();
                for (final Page page : result.getResult()) {
                    getCustomPageService().removePageLocally(getEngineSession(), page);
                }
            } while (startIndex < count);
        } catch (final Exception e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Error when deleting pages for process with ID " + id, e);
            }
        }
    }

    protected CustomPageService getCustomPageService() {
        return new CustomPageService();
    }

    protected PageAPI getPageAPI() {
        try {
            return TenantAPIAccessor.getCustomPageAPI(getEngineSession());
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @Override
    public ItemSearchResult<ProcessItem> search(final int page, final int resultsByPage, final String search, final String orders,
            final Map<String, String> filters) {
        final ProcessEngineClient engineClient = getProcessEngineClient();
        return new SearchProcessHelper(engineClient, getEngineSession().getTenantId()).search(page, resultsByPage, search, orders, filters);
    }

    @Override
    protected ProcessItem convertEngineToConsoleItem(final ProcessDeploymentInfo item) {
        if (item != null) {
            return new ProcessItemConverter(getProcessEngineClient().getProcessApi(), getEngineSession().getTenantId()).convert(item);
        }
        return null;
    }

    protected ProcessEngineClient getProcessEngineClient() {
        return getEngineClientFactory().createProcessEngineClient();
    }

    private EngineClientFactory getEngineClientFactory() {
        return new EngineClientFactory(new EngineAPIAccessor(getEngineSession()));
    }
}
