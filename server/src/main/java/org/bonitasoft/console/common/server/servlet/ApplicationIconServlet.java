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
package org.bonitasoft.console.common.server.servlet;

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.api.ApplicationAPI;
import org.bonitasoft.engine.business.application.Application;
import org.bonitasoft.engine.business.application.ApplicationNotFoundException;
import org.bonitasoft.engine.business.application.ApplicationUpdater;
import org.bonitasoft.engine.business.application.Icon;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.NotFoundException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemNotFoundException;
import org.bonitasoft.web.toolkit.client.common.exception.http.ServerException;
import org.bonitasoft.web.toolkit.client.data.APIID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class ApplicationIconServlet extends IconServlet {

    @Override
    protected Optional<IconContent> retrieveIcon(Long iconId, APISession apiSession) {
        ApplicationAPI applicationApi = getApplicationApi(apiSession);
        try {
            Icon icon = applicationApi.getIconOfApplication(iconId);
            if (icon != null) {
                return Optional.of(new IconContent(icon.getContent(), icon.getMimeType()));
            } else {
                return Optional.empty();
            }
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    protected void deleteIcon(Long entityId, APISession apiSession, HttpServletRequest request, HttpServletResponse response) throws ServerException {
        ApplicationAPI applicationApi = getApplicationApi(apiSession);
        ApplicationUpdater updater = new ApplicationUpdater();
        updater.setIcon(null, null);
        try {
            applicationApi.updateApplication(entityId, updater);
        } catch (ApplicationNotFoundException e) {
            throw new APIItemNotFoundException(Application.class.getName(), APIID.makeAPIID(entityId));
        } catch (UpdateException | AlreadyExistsException e) {
            throw new APIException(e);
        }
    }

    ApplicationAPI getApplicationApi(APISession apiSession) {
        return new APIClient(apiSession).getApplicationAPI();
    }

}
