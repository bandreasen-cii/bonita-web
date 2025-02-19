package org.bonitasoft.console.common.server.auth.impl.standard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import javax.servlet.http.Cookie;

import org.bonitasoft.console.common.server.login.HttpServletRequestAccessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class StandardAuthenticationManagerImplTest {

    private MockHttpServletRequest request;
    private HttpServletRequestAccessor requestAccessor;

    private StandardAuthenticationManagerImpl standardLoginManagerImpl = spy(new StandardAuthenticationManagerImpl());


    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        request.setContextPath("bonita");
        request.setParameter("tenant", "1");
        doReturn(2L).when(standardLoginManagerImpl).getDefaultTenantId();

        requestAccessor = new HttpServletRequestAccessor(request);
    }

    @Test
    public void testGetSimpleLoginpageURL() throws Exception {
        String redirectUrl = "%2Fapps%2FappDirectoryBonita";

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, redirectUrl);

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?tenant=1&redirectUrl=%2Fapps%2FappDirectoryBonita");
    }

    @Test
    public void testGetLoginpageURLWithLocale() throws Exception {
        String redirectUrl = "%2Fapps%2FappDirectoryBonita";
        request.setParameter("_l", "es");

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, redirectUrl);

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?tenant=1&_l=es&redirectUrl=%2Fapps%2FappDirectoryBonita");
    }

    @Test
    public void testGetLoginpageURLFromPortal() throws Exception {
        String redirectUrl = "%2Fapps%2FappDirectoryBonita";
        request.setServletPath("/portal/");

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, redirectUrl);

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?tenant=1&redirectUrl=%2Fapps%2FappDirectoryBonita");
    }

    @Test
    public void should_add_tenant_parameter_contained_in_request_params() throws Exception {
        request.setParameter("tenant", "4");
        request.setCookies(new Cookie("bonita.tenant", "123"));
        String redirectUrl = "%2Fapps%2FappDirectoryBonita";

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, redirectUrl);

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?tenant=4&redirectUrl=%2Fapps%2FappDirectoryBonita");
    }

    @Test
    public void should_add_tenant_parameter_from_cookie_if_not_in_request() throws Exception {
        request.removeAllParameters();
        request.setCookies(new Cookie("bonita.tenant", "123"));
        String redirectUrl = "%2Fapps%2FappDirectoryBonita";

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, redirectUrl);

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?tenant=123&redirectUrl=%2Fapps%2FappDirectoryBonita");
    }

    @Test
    public void should_not_add_tenantid_when_it_is_default_one() throws Exception {
        when(standardLoginManagerImpl.getDefaultTenantId()).thenReturn(123L);
        request.setParameter("tenant", "123");

        String loginURL = standardLoginManagerImpl.getLoginPageURL(requestAccessor, "%2Fapps%2FappDirectoryBonita");

        assertThat(loginURL).isEqualToIgnoringCase("bonita/login.jsp?redirectUrl=%2Fapps%2FappDirectoryBonita");
    }
}
