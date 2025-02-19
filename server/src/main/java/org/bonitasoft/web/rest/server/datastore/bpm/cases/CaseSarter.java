/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * 
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
package org.bonitasoft.web.rest.server.datastore.bpm.cases;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.bonitasoft.engine.bpm.data.DataDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.web.rest.model.bpm.cases.CaseItem;
import org.bonitasoft.web.rest.server.datastore.utils.VariableMapper;
import org.bonitasoft.web.rest.server.datastore.utils.VariablesMapper;
import org.bonitasoft.web.rest.server.engineclient.CaseEngineClient;
import org.bonitasoft.web.rest.server.engineclient.ProcessEngineClient;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.i18n.T_;
import org.bonitasoft.web.toolkit.client.common.texttemplate.Arg;
import org.bonitasoft.web.toolkit.client.common.util.StringUtil;

public class CaseSarter {

    private final CaseItem caseItem;

    private final CaseEngineClient caseEngineClient;

    private final ProcessEngineClient processEngineClient;

    private final Long processId;

    private final Long delegateUserId;

    public CaseSarter(final CaseItem caseItem, final CaseEngineClient caseEngineClient, final ProcessEngineClient processEngineClient) {
        this.caseItem = caseItem;
        processId = caseItem.getProcessId().toLong();
        if (caseItem.hasAttribute(CaseItem.ATTRIBUTE_STARTED_BY_SUBSTITUTE_USER_ID) && caseItem.getStartedBySubstituteUserId() != null) {
            delegateUserId = caseItem.getStartedBySubstituteUserId().toLong();
        } else {
            delegateUserId = -1L;
        }
        this.caseEngineClient = caseEngineClient;
        this.processEngineClient = processEngineClient;
    }

    public CaseItem start() {
        final HashMap<String, Serializable> variables = getVariables(caseItem);
        if (variables.isEmpty()) {
            return startCase();
        } else {
            return startCaseWithVariables(variables);
        }
    }

    private HashMap<String, Serializable> getVariables(final CaseItem caseItem) {
        final String jsonVariables = caseItem.getAttributeValue(CaseItem.ATTRIBUTE_VARIABLES);
        if (StringUtil.isBlank(jsonVariables)) {
            return new HashMap<String, Serializable>();
        }
        return buildVariablesMap(jsonVariables);
    }

    private HashMap<String, Serializable> buildVariablesMap(final String jsonValue) {
        final List<DataDefinition> dataDefinitions = processEngineClient.getProcessDataDefinitions(processId);

        final HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        for (final VariableMapper var : VariablesMapper.fromJson(jsonValue).getVariables()) {
            final DataDefinition data = getDataDefinitionByName(var.getName(), dataDefinitions);
            map.put(var.getName(), var.getSerializableValue(data.getClassName()));
        }
        return map;
    }

    private CaseItem startCaseWithVariables(final HashMap<String, Serializable> variables) {
        final ProcessInstance processInstance = caseEngineClient.start(delegateUserId, processId, variables);
        return new CaseItemConverter().convert(processInstance);
    }

    private CaseItem startCase() {
        final ProcessInstance processInstance = caseEngineClient.start(delegateUserId, processId);
        return new CaseItemConverter().convert(processInstance);
    }

    private DataDefinition getDataDefinitionByName(final String dataName, final List<DataDefinition> dataDefinitions) {
        for (final DataDefinition dataDefinition : dataDefinitions) {
            if (dataDefinition.getName().equals(dataName)) {
                return dataDefinition;
            }
        }
        throw new APIException(new T_("Data definition %dataName% doesn't exists for process %processId%", new Arg("dataName", dataName), new Arg("processId",
                caseItem.getProcessId())));
    }
}
