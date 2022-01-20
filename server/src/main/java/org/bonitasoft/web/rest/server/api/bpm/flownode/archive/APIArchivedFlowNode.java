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
package org.bonitasoft.web.rest.server.api.bpm.flownode.archive;

import java.util.List;

import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedFlowNodeDefinition;
import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedFlowNodeItem;
import org.bonitasoft.web.rest.server.api.bpm.flownode.AbstractAPIFlowNode;
import org.bonitasoft.web.rest.server.datastore.bpm.flownode.archive.ArchivedFlowNodeDatastore;

/**
 * @author Séverin Moussel
 *
 */
public class APIArchivedFlowNode extends AbstractAPIFlowNode<ArchivedFlowNodeItem> {

    @Override
    protected ArchivedFlowNodeDefinition defineItemDefinition() {
        return new ArchivedFlowNodeDefinition();
    }

    @Override
    protected ArchivedFlowNodeDatastore defineDefaultDatastore() {
        return new ArchivedFlowNodeDatastore(getEngineSession(), ArchivedFlowNodeDefinition.TOKEN);
    }

    @Override
    protected List<String> defineReadOnlyAttributes() {
        final List<String> attributes = super.defineReadOnlyAttributes();

        attributes.add(ArchivedFlowNodeItem.ATTRIBUTE_ARCHIVED_DATE);

        return attributes;
    }

}
