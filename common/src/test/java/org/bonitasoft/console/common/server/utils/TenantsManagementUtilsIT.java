package org.bonitasoft.console.common.server.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.console.common.server.utils.TenantsManagementUtils.isDefaultTenantPaused;

import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.TenantAdministrationAPI;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.test.toolkit.AbstractJUnitTest;
import org.bonitasoft.test.toolkit.organization.TestToolkitCtx;
import org.bonitasoft.test.toolkit.organization.TestUser;
import org.bonitasoft.test.toolkit.organization.TestUserFactory;
import org.junit.Test;

public class TenantsManagementUtilsIT  extends AbstractJUnitTest {


    @Override
    protected TestToolkitCtx getContext() {
        return TestToolkitCtx.getInstance();
    }

    @Override
    protected TestUser getInitiator() {
        return TestUserFactory.getJohnCarpenter();
    }

    @Override
    protected void testSetUp() throws Exception {
        new PlatformManagementUtils().initializePlatformConfiguration();
    }

    @Override
    protected void testTearDown() throws Exception {
    }


    @Test
    public void isDefaultTenantPausedShouldTellIfTenantIsPaused() throws Exception {

        //tenant is not paused
        assertThat(isDefaultTenantPaused()).isFalse();

        pauseTenant();
        assertThat(isDefaultTenantPaused()).isTrue();
        resumeTenant();
    }

    private TenantAdministrationAPI loginAndGetTenantAdministrationAPI() throws Exception {
        final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        APISession technicalUserSession = loginAPI.login(TenantsManagementUtils.getTechnicalUserUsername(),
                TenantsManagementUtils.getTechnicalUserPassword());
        return TenantAPIAccessor.getTenantAdministrationAPI(technicalUserSession);
    }

    private void pauseTenant() throws Exception {
        TenantAdministrationAPI tenantAdministrationAPI = loginAndGetTenantAdministrationAPI();
        tenantAdministrationAPI.pause();
    }

    private void resumeTenant() throws Exception {
        TenantAdministrationAPI tenantAdministrationAPI = loginAndGetTenantAdministrationAPI();
        tenantAdministrationAPI.resume();
    }

}