package org.bonitasoft.web.rest.server.api.bpm.cases;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.web.rest.server.ResourceFinder;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.ServerResource;

public class ArchivedCaseVariableResourceFinder extends ResourceFinder {

    @Override
    public ServerResource create(Request request, Response response) {
        final ProcessAPI processAPI = getProcessAPI(request);
        return new ArchivedCaseVariableResource(processAPI);
    }

}