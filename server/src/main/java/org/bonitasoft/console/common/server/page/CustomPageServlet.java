/**
 * Copyright (C) 2014 BonitaSoft S.A.
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
package org.bonitasoft.console.common.server.page;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bonitasoft.console.common.server.page.extension.PageResourceProviderImpl;
import org.bonitasoft.console.common.server.utils.BonitaHomeFolderAccessor;
import org.bonitasoft.console.common.server.utils.SessionUtil;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.livingapps.ApplicationModelFactory;

public class CustomPageServlet extends HttpServlet {

    public static final String APP_TOKEN_PARAM = "appToken";
    /**
     * uuid
     */
    private static final long serialVersionUID = -5410859017103815654L;
    /**
     * Logger
     */
    private static Logger LOGGER = Logger.getLogger(CustomPageServlet.class.getName());
    protected ResourceRenderer resourceRenderer = new ResourceRenderer();

    protected PageRenderer pageRenderer = new PageRenderer(resourceRenderer);

    protected BonitaHomeFolderAccessor bonitaHomeFolderAccessor = new BonitaHomeFolderAccessor();

    protected CustomPageRequestModifier customPageRequestModifier = new CustomPageRequestModifier();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        /*
         * Check if requested URL is missing last slash, like "custom-page/page-name".
         * If missing, redirect to "custom-page/page-name/"
         */
        if (isPageUrlWithoutFinalSlash(request)) {
            customPageRequestModifier.redirectToValidPageUrl(request, response);
            return;
        }

        final String appToken = request.getParameter(APP_TOKEN_PARAM);
        final HttpSession session = request.getSession();
        final APISession apiSession = (APISession) session.getAttribute(SessionUtil.API_SESSION_PARAM_KEY);

        final List<String> pathSegments = resourceRenderer.getPathSegments(request.getPathInfo());
        if (pathSegments.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The name of the page is required.");
            return;
        }
        final String pageName = pathSegments.get(0);

        try {

            if (isAuthorized(apiSession, appToken, pageName)) {
                if (isPageRequest(pathSegments)) {
                    pageRenderer.displayCustomPage(request, response, apiSession, pageName);
                } else {
                    final File resourceFile = getResourceFile(request.getPathInfo(), pageName, apiSession);
                    pageRenderer.ensurePageFolderIsPresent(apiSession, pageRenderer.getPageResourceProvider(pageName, apiSession.getTenantId()));
                    resourceRenderer.renderFile(request, response, resourceFile, apiSession);
                }
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User not Authorized");
                return;
            }
        } catch (final Exception e) {
            handleException(pageName, e);
        }

    }

    private boolean isPageRequest(final List<String> pathSegments) {
        if (pathSegments.size() == 1) {
            return true;
        } else if (pathSegments.size() == 2) {
            return isAnIndexSegment(pathSegments.get(1));
        }
        return false;
    }

    private boolean isAnIndexSegment(final String segment) {
        return segment.equalsIgnoreCase(CustomPageService.PAGE_INDEX_FILENAME) || segment.equalsIgnoreCase(CustomPageService.PAGE_CONTROLLER_FILENAME)
                || segment.equalsIgnoreCase(CustomPageService.PAGE_INDEX_NAME);
    }

    private boolean isPageUrlWithoutFinalSlash(final HttpServletRequest request) {
        return request.getPathInfo().matches("/[^/]+");
    }

    private File getResourceFile(final String resourcePath, final String pageName, final APISession apiSession) throws IOException, BonitaException {
        final PageResourceProviderImpl pageResourceProvider = pageRenderer.getPageResourceProvider(pageName, apiSession.getTenantId());
        final File resourceFile = new File(pageResourceProvider.getPageDirectory(), CustomPageService.RESOURCES_PROPERTY + File.separator
                + getResourcePathWithoutPageName(resourcePath, pageName));

        if (!bonitaHomeFolderAccessor.isInFolder(resourceFile, pageResourceProvider.getPageDirectory())) {
            throw new BonitaException("Unauthorized access to the file " + resourcePath);
        }
        return resourceFile;
    }

    private String getResourcePathWithoutPageName(final String resourcePath, final String pageName) {
        //resource path match "/pagename/resourcefolder/filename"
        return resourcePath.substring(pageName.length() + 2);
    }

    private boolean isAuthorized(final APISession apiSession, final String appToken, final String pageName) throws BonitaException {
        //Technical user should be authorized in order for the custom pages to be displayed in his profile
        return apiSession.isTechnicalUser() 
                || getCustomPageAuthorizationsHelper(apiSession).isPageAuthorized(appToken, pageName);
    }

    private void handleException(final String pageName, final Exception e) throws ServletException {
        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.log(Level.WARNING, "Error while trying to render the custom page " + pageName, e);
        }
        throw new ServletException(e.getMessage());
    }

    protected CustomPageAuthorizationsHelper getCustomPageAuthorizationsHelper(final APISession apiSession) throws BonitaHomeNotSetException,
            ServerAPIException, UnknownAPITypeException {
        return new CustomPageAuthorizationsHelper(apiSession,
                TenantAPIAccessor.getLivingApplicationAPI(apiSession), TenantAPIAccessor.getCustomPageAPI(apiSession), new ApplicationModelFactory(
                TenantAPIAccessor.getLivingApplicationAPI(apiSession),
                TenantAPIAccessor.getCustomPageAPI(apiSession),
                TenantAPIAccessor.getProfileAPI(apiSession)));
    }
}
