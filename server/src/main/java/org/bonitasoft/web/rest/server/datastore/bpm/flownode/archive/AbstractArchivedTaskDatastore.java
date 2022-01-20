/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.web.rest.server.datastore.bpm.flownode.archive;

import org.bonitasoft.engine.bpm.flownode.ArchivedTaskInstance;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedTaskItem;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemNotFoundException;
import org.bonitasoft.web.toolkit.client.data.APIID;

/**
 * @author Séverin Moussel
 */
public abstract class AbstractArchivedTaskDatastore<CONSOLE_ITEM extends ArchivedTaskItem, ENGINE_ITEM extends ArchivedTaskInstance>
        extends AbstractArchivedActivityDatastore<CONSOLE_ITEM, ENGINE_ITEM> {

    public AbstractArchivedTaskDatastore(final APISession engineSession, String token) {
        super(engineSession, token);
    }

    @Override
    protected ENGINE_ITEM runGet(final APIID id) {
        try {
            return super.runGet(id);
        } catch (ClassCastException e) {
            throw new APIItemNotFoundException(this.token, id);
        }
    }

}
