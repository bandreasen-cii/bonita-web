/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.web.rest.server.datastore.bpm.flownode.archive;

import org.apache.commons.lang3.StringUtils;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedHumanTaskItem;
import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedTaskItem;
import org.bonitasoft.web.rest.model.bpm.flownode.HumanTaskItem;
import org.bonitasoft.web.rest.server.datastore.bpm.flownode.archive.converter.ArchivedActivitySearchDescriptorConverter;
import org.bonitasoft.web.rest.server.datastore.bpm.flownode.archive.converter.ArchivedHumanTaskSearchDescriptorConverter;
import org.bonitasoft.web.rest.server.datastore.utils.SearchOptionsCreator;
import org.bonitasoft.web.rest.server.framework.search.ItemSearchResult;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemNotFoundException;
import org.bonitasoft.web.toolkit.client.common.util.MapUtil;
import org.bonitasoft.web.toolkit.client.data.APIID;

import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * @author Séverin Moussel
 */
public abstract class AbstractArchivedHumanTaskDatastore<CONSOLE_ITEM extends ArchivedHumanTaskItem, ENGINE_ITEM extends ArchivedHumanTaskInstance>
        extends AbstractArchivedTaskDatastore<CONSOLE_ITEM, ENGINE_ITEM> {

    public AbstractArchivedHumanTaskDatastore(final APISession engineSession, String token) {
        super(engineSession, token);
    }

    /**
     * Fill a console item using the engine item passed.
     *
     * @param result The console item to fill
     * @param item   The engine item to use for filling
     * @return This method returns the result parameter passed.
     */
    public static ArchivedHumanTaskItem fillConsoleItem(final ArchivedHumanTaskItem result, final ArchivedHumanTaskInstance item) {
        ArchivedTaskDatastore.fillConsoleItem(result, item);

        result.setActorId(APIID.makeAPIID(item.getActorId()));
        result.setAssignedId(APIID.makeAPIID(item.getAssigneeId()));
        result.setAssignedDate(item.getClaimedDate());
        result.setPriority(item.getPriority() != null ? item.getPriority().toString().toLowerCase() : null);
        result.setDueDate(item.getExpectedEndDate());

        return result;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // C.R.U.D.S
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ItemSearchResult<CONSOLE_ITEM> search(final int page, final int resultsByPage, final String search, final String orders,
                                                 final Map<String, String> filters) {
        try {
            // can't use the ArchivedFlowNodeSearchDescriptorConverter to map web filter to engine ones since
            // the supervisor id filter isn't handle in engine but is a specific method
            String supervisorIdString = filters.remove(HumanTaskItem.FILTER_SUPERVISOR_ID);
            final SearchOptionsCreator creator = makeSearchOptionCreator(page, resultsByPage, search, orders, filters);
            if (StringUtils.isNotBlank(supervisorIdString)) {
                filters.put(HumanTaskItem.FILTER_SUPERVISOR_ID, supervisorIdString);
            }

            final SearchResult<ENGINE_ITEM> results = runSearch(creator, filters);

            return new ItemSearchResult<CONSOLE_ITEM>(
                    page,
                    resultsByPage,
                    results.getCount(),
                    convertEngineToConsoleItemsList(results.getResult()));

        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SearchResult<ENGINE_ITEM> runSearch(final SearchOptionsCreator creator,
                                                  final Map<String, String> filters) {
        try {
            final SearchResult<ENGINE_ITEM> result;
            if (!MapUtil.isBlank(filters, ArchivedHumanTaskItem.FILTER_SUPERVISOR_ID)) {
                result = (SearchResult<ENGINE_ITEM>) getProcessAPI().searchArchivedHumanTasksSupervisedBy(
                        MapUtil.getValueAsLong(filters, ArchivedTaskItem.FILTER_SUPERVISOR_ID),
                        creator.create());
            } else if (!MapUtil.isBlank(filters, ArchivedHumanTaskItem.FILTER_TEAM_MANAGER_ID)) {
                result = (SearchResult<ENGINE_ITEM>) getProcessAPI().searchArchivedHumanTasksManagedBy(
                        MapUtil.getValueAsLong(filters, ArchivedHumanTaskItem.FILTER_TEAM_MANAGER_ID),
                        creator.create());
            } else {
                result = (SearchResult<ENGINE_ITEM>) getProcessAPI().searchArchivedHumanTasks(
                        creator.create());
            }

            return result;
        } catch (final Exception e) {
            throw new APIException(e);
        }
    }

    @Override
    protected ArchivedActivitySearchDescriptorConverter getSearchDescriptorConverter() {
        return new ArchivedHumanTaskSearchDescriptorConverter();
    }


}
