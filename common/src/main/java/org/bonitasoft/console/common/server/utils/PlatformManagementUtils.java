/**
 * Copyright (C) 2016 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

package org.bonitasoft.console.common.server.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bonitasoft.console.common.server.preferences.properties.ConfigurationFilesManager;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.InvalidPlatformCredentialsException;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.util.APITypeManager;

/**
 * @author Baptiste Mesta
 */
public class PlatformManagementUtils {
    
    private static final Logger LOGGER = Logger.getLogger(PlatformManagementUtils.class.getName());

    private final ConfigurationFilesManager configurationFilesManager = ConfigurationFilesManager.getInstance();

    //package local for testing purpose
    boolean isLocal() throws UnknownAPITypeException, ServerAPIException, IOException {
        return ApiAccessType.LOCAL.equals(APITypeManager.getAPIType());
    }

    //package local for testing purpose
    PlatformAPI getPlatformAPI(final PlatformSession platformSession) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return PlatformAPIAccessor.getPlatformAPI(platformSession);
    }

    //package local for testing purpose
    PlatformLoginAPI getPlatformLoginAPI() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return PlatformAPIAccessor.getPlatformLoginAPI();
    }

    PlatformSession platformLogin() throws BonitaException, IOException {
        if (isLocal()) {
            try {
                return localPlatformLogin();
            } catch (final Exception e) {
                throw new ServerAPIException("Unable to login locally", e);
            }
        } else {
            String username = System.getProperty("org.bonitasoft.platform.username");
            String password = System.getProperty("org.bonitasoft.platform.password");
            try {

                return getPlatformLoginAPI().login(username,
                        password);
            } catch (InvalidPlatformCredentialsException e) {
                throw new InvalidPlatformCredentialsException("The portal is not able to login to the engine because " +
                        "system properties org.bonitasoft.platform.username and org.bonitasoft.platform.password are " +
                        "not set correctly to the platform administrator credentials.\n " +
                        "These properties must be set when connecting to a Bonita engine that is not local. " +
                        "If the engine is local, change the connection to LOCAL using the APITypeManager");
            }
        }
    }

    private PlatformSession localPlatformLogin()
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        final Class<?> api = Class.forName("org.bonitasoft.engine.LocalLoginMechanism");
        return (PlatformSession) api.getDeclaredMethod("login").invoke(api.newInstance());
    }

    void platformLogout(final PlatformSession platformSession) throws BonitaException {
        final PlatformLoginAPI platformLoginAPI = getPlatformLoginAPI();
        platformLoginAPI.logout(platformSession);
    }

    private void retrieveTenantsConfiguration(final PlatformAPI platformAPI) throws IOException {
        final Map<Long, Map<String, byte[]>> clientTenantConfigurations = platformAPI.getClientTenantConfigurations();
        for (final Entry<Long, Map<String, byte[]>> tenantConfiguration : clientTenantConfigurations.entrySet()) {
            configurationFilesManager.setTenantConfigurationFiles(tenantConfiguration.getValue(), tenantConfiguration.getKey());
        }
    }

    private void retrievePlatformConfiguration(final PlatformAPI platformAPI) throws IOException {
        final Map<String, byte[]> clientPlatformConfigurations = platformAPI.getClientPlatformConfigurations();
        configurationFilesManager.setPlatformConfigurations(clientPlatformConfigurations);
    }

    public void initializePlatformConfiguration() throws BonitaException, IOException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        retrievePlatformConfiguration(platformAPI);
        retrieveTenantsConfiguration(platformAPI);
        platformLogout(platformSession);
    }

    public void updateConfigurationFile(final long tenantId, final String file, final byte[] content) throws IOException, BonitaException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        platformAPI.updateClientTenantConfigurationFile(tenantId, file, content);
        platformLogout(platformSession);
    }

    public void retrieveTenantsConfiguration() throws BonitaException, IOException {
        final PlatformSession platformSession = platformLogin();
        final PlatformAPI platformAPI = getPlatformAPI(platformSession);
        retrieveTenantsConfiguration(platformAPI);
        platformLogout(platformSession);
    }

    public byte[] getTenantConfiguration(long tenantId, String configurationFileName) {
        final PlatformSession platformSession;
        try {
            platformSession = platformLogin();
            try {
                return getPlatformAPI(platformSession).getClientTenantConfiguration(tenantId, configurationFileName);
            } finally {
                platformLogout(platformSession);
            }
        } catch (BonitaException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Long => tenantId
     * String => configuration file name
     * Properties => content of the configuration file
     */
    public Map<Long, Map<String, Properties>> getTenantConfigurations() throws IOException {
        try {
            final PlatformSession platformSession = platformLogin();
            try {
                Map<Long, Map<String, byte[]>> clientTenantConfigurations = getPlatformAPI(platformSession).getClientTenantConfigurations();
                Map<Long, Map<String, Properties>> clientTenantConfigurationProperties = new HashMap<>();
                for (Entry<Long, Map<String, byte[]>> entry : clientTenantConfigurations.entrySet()) {
                    final Map<String, byte[]> map = entry.getValue();
                    clientTenantConfigurationProperties.put(entry.getKey(),
                            map.entrySet().stream().collect(Collectors.toMap(
                                    Entry::getKey,
                                    v -> ConfigurationFilesManager.getProperties(v.getValue()))));
                }
                return clientTenantConfigurationProperties;
            } finally {
                platformLogout(platformSession);
            }
        } catch (BonitaException e) {
            throw new IOException(e);
        }
    }
    
    public boolean isPlatformAvailable() {
        try {
            final PlatformSession platformSession = platformLogin();
            try {
                PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(platformSession);
                return platformAPI.isNodeStarted();
            } finally {
                platformLogout(platformSession);
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Platform is not available.", e);
            } else if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Platform is not available.");
            }
            return false;
        }
    }
}
