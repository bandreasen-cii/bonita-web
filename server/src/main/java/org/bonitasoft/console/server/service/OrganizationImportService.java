/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.console.server.service;

import static org.bonitasoft.web.toolkit.client.common.i18n.AbstractI18n.t_;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.console.common.server.utils.BonitaHomeFolderAccessor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.identity.ImportPolicy;
import org.bonitasoft.engine.identity.InvalidOrganizationFileFormatException;
import org.bonitasoft.engine.session.InvalidSessionException;
import org.bonitasoft.web.toolkit.client.common.i18n.AbstractI18n;
import org.bonitasoft.web.toolkit.server.ServiceException;

/**
 * @author Séverin Moussel
 */
public class OrganizationImportService extends ConsoleService {

    public static final String TOKEN = "/organization/import";

    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(OrganizationImportService.class.getName());
    
    /**
     * organization data file
     */
    private static final String FILE_UPLOAD = "organizationDataUpload";

    /**
     * import policy
     */
    private static final String IMPORT_POLICY_PARAM_NAME = "importPolicy";

    @Override
    public Object run() {
        final BonitaHomeFolderAccessor tenantFolder = new BonitaHomeFolderAccessor();
        try {
            final byte[] organizationContent = getOrganizationContent(tenantFolder);
            getIdentityAPI().importOrganizationWithWarnings(new String(organizationContent), getImportPolicy());
        } catch (final InvalidSessionException e) {
            getHttpResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            String message = AbstractI18n.t_("Session expired. Please log in again.");
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, message, e.getMessage());
            }
            throw new ServiceException(TOKEN, message, e);
        } catch (InvalidOrganizationFileFormatException | IllegalArgumentException e) {
            getHttpResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String message = AbstractI18n.t_("Can't import organization. Please check that your file is well-formed.");
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, message, e.getMessage());
            }
            throw new ServiceException(TOKEN, message, e);
        } catch (final Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            throw new ServiceException(TOKEN, AbstractI18n.t_("Can't import organization"), e);
        }
        return "";
    }

    private ImportPolicy getImportPolicy() {
        final String importPolicyAsString = getParameter(IMPORT_POLICY_PARAM_NAME);
        ImportPolicy importPolicy = ImportPolicy.MERGE_DUPLICATES;
        if (importPolicyAsString != null) {
            importPolicy = ImportPolicy.valueOf(importPolicyAsString);
        }
        return importPolicy;
    }

    public byte[] getOrganizationContent(final BonitaHomeFolderAccessor tenantFolder) throws IOException {
        try (InputStream xmlStream = new FileInputStream(tenantFolder.getTempFile(getFileUploadParameter(), getTenantId()))) {
            return IOUtils.toByteArray(xmlStream);
        }
    }


    protected IdentityAPI getIdentityAPI() throws InvalidSessionException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return TenantAPIAccessor.getIdentityAPI(getSession());
    }

    protected long getTenantId() {
        return getSession().getTenantId();
    }

    protected String getFileUploadParameter() {
        return getParameter(FILE_UPLOAD);
    }

}
