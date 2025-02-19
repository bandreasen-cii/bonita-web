package org.bonitasoft.console.common.server.login.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bonitasoft.console.common.server.auth.AuthenticationFailedException;
import org.bonitasoft.console.common.server.auth.AuthenticationManager;
import org.bonitasoft.console.common.server.auth.AuthenticationManagerNotFoundException;
import org.bonitasoft.console.common.server.login.LoginFailedException;
import org.bonitasoft.console.common.server.login.LoginManager;
import org.bonitasoft.console.common.server.utils.SessionUtil;
import org.bonitasoft.engine.exception.TenantStatusException;
import org.bonitasoft.engine.session.APISession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


/**
 * Created by Vincent Elcrin
 * Date: 10/09/13
 * Time: 15:18
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginServletTest {

    @Mock
    HttpServletRequest req;

    @Mock
    HttpServletResponse resp;

    @Mock
    HttpSession httpSession;

    @Mock
    APISession apiSession;
    
    @Before
    public void setup() {
        doReturn("application/x-www-form-urlencoded").when(req).getContentType();
    }
    
    @Test
    public void testPasswordIsDroppedWhenParameterIsLast() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("?username=walter.bates&password=bpm");

        assertThat(cleanQueryString, is("?username=walter.bates"));
    }

    @Test
    public void testPasswordIsDroppedWhenParameterIsBeforeHash() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("?username=walter.bates&password=bpm#hash");

        assertThat(cleanQueryString, is("?username=walter.bates#hash"));
    }

    @Test
    public void testUrlIsDroppedWhenParameterIsFirstAndBeforeHash() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("?username=walter.bates&password=bpm#hash");

        assertThat(cleanQueryString, is("?username=walter.bates#hash"));
    }

    @Test
    public void testUrlStayTheSameIfNoPasswordArePresent() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("?param=value#dhash");

        assertThat(cleanQueryString, is("?param=value#dhash"));
    }

    @Test
    public void testPasswordIsDroppedEvenIfQueryMarkUpIsntThere() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("password=bpm#dhash1&dhash2");

        assertThat(cleanQueryString, is("#dhash1&dhash2"));
    }

    @Test
    public void testUrlStayEmptyIfParameterIsEmpty() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanQueryString = servlet.dropPassword("");

        assertThat(cleanQueryString, is(""));
    }

    @Test
    public void testDropPasswordOnRealUrl() throws Exception {
        final LoginServlet servlet = new LoginServlet();

        final String cleanUrl = servlet
            .dropPassword(
                "?username=walter.bates&password=bpm&redirectUrl=http%3A%2F%2Flocalhost%3A8080%2Fbonita%2Fapps%2FappDirectoryBonita%3Flocale%3Den%23form%3DPool-\n"
                    +
                    "-1.0%24entry%26process%3D8506394779365952706%26mode%3Dapp");

        assertThat(cleanUrl,
            is("?username=walter.bates&redirectUrl=http%3A%2F%2Flocalhost%3A8080%2Fbonita%2Fapps%2FappDirectoryBonita%3Flocale%3Den%23form%3DPool-\n" +
                "-1.0%24entry%26process%3D8506394779365952706%26mode%3Dapp"));
    }

    @Test
    public void testDoGetShouldDropPassowrdWhenLoggingQueryString() throws Exception {
        final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        logger.setLevel(Level.FINEST);

        //given
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("query string").when(req).getQueryString();
        doNothing().when(servlet).doPost(req, resp);

        //when
        servlet.doGet(req, resp);

        //then
        verify(req).getQueryString();
        verify(servlet).dropPassword(anyString());
    }

    @Test
    public void testDoGetShouldNotLogQueryString() throws Exception {
        final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        logger.setLevel(Level.INFO);

        //given
        final LoginServlet servlet = spy(new LoginServlet());
        doNothing().when(servlet).doPost(req, resp);

        //when
        servlet.doGet(req, resp);

        //then
        verify(req, never()).getQueryString();
    }

    @Test
    public void testDoPostShouldNotUseQueryString() throws Exception {
        final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        logger.setLevel(Level.FINEST);

        //given
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(true).when(apiSession).isTechnicalUser();
        doReturn(null).when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doNothing().when(servlet).doLogin(req, resp);

        //when
        servlet.doPost(req, resp);

        //then
        verify(req, never()).getQueryString();
    }

    @Test
    public void should_send_error_401_when_login_with_wrong_credentials_and_no_redirect() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doThrow(new LoginFailedException("")).when(servlet).doLogin(req, resp);

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
    
    @Test
    public void should_send_error_415_when_login_with_wrong_content_type() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("application/json").when(req).getContentType();

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }
    
    @Test
    public void should_login_with_no_content_type() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        doReturn(null).when(req).getContentType();
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);

        servlet.doPost(req, resp);

        verify(loginManager).login(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
    
    @Test
    public void should_login_with_content_type_containing_charset() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        doReturn("application/x-www-form-urlencoded; charset=UTF-8").when(req).getContentType();
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);

        servlet.doPost(req, resp);

        verify(loginManager).login(req, resp);
        verify(resp, never()).setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }
    
    @Test
    public void should_not_redirect_after_login_when_redirect_parameter_is_set_to_false_in_request() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        doReturn("false").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doReturn("anyurl").when(req).getParameter(AuthenticationManager.REDIRECT_URL);
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);

        servlet.doPost(req, resp);
        
        verify(resp, never()).sendRedirect(anyString());
    }
    
    @Test
    public void should_not_redirect_after_login_when_user_has_no_profile() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        final ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doReturn("anyurl").when(req).getParameter(AuthenticationManager.REDIRECT_URL);
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doReturn(servletContext).when(servlet).getServletContext();
        doReturn(requestDispatcher).when(servletContext).getRequestDispatcher(anyString());
        doNothing().when(loginManager).login(req, resp);
        doReturn(false).when(servlet).hasProfile(apiSession);

        servlet.doPost(req, resp);
        
        verify(resp, never()).sendRedirect(anyString());
        verify(servletContext).getRequestDispatcher(AuthenticationManager.LOGIN_PAGE);
        verify(requestDispatcher).forward(req, resp);
    }
    
    @Test
    public void should_redirect_after_login_when_redirect_url_is_set_in_request() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        doReturn("anyurl").when(req).getParameter(AuthenticationManager.REDIRECT_URL);
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);
        doReturn(true).when(servlet).hasProfile(apiSession);
        
        servlet.doPost(req, resp);

        verify(resp).sendRedirect(startsWith("anyurl"));
    }
    
    @Test
    public void should_redirect_after_login_when_redirect_parameter_is_set_to_true_in_request() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        final LoginManager loginManager = mock(LoginManager.class);
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);
        doReturn(true).when(servlet).hasProfile(apiSession);

        servlet.doPost(req, resp);

        verify(resp).sendRedirect(startsWith(AuthenticationManager.DEFAULT_DIRECT_URL));
    }

    @Test(expected = ServletException.class)
    public void should_send_servlet_exception_when_login_with_wrong_credentials_and_redirect() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doThrow(new LoginFailedException("")).when(servlet).doLogin(req, resp);

        servlet.doPost(req, resp);
    }

    @Test(expected = ServletException.class)
    public void should_throw_tenant_status_exception() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doThrow(new TenantStatusException("")).when(servlet).doLogin(req, resp);

        servlet.doPost(req, resp);
    }

    @Test(expected = ServletException.class)
    public void should_return_servlet_exception_when_throw_authentication_failed_exception() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doThrow(new AuthenticationFailedException("")).when(servlet).doLogin(req, resp);

        servlet.doPost(req, resp);
    }

    @Test(expected = ServletException.class)
    public void should_return_servlet_exception_when_authentication_manager_not_found_exception() throws Exception {
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn("true").when(req).getParameter(AuthenticationManager.REDIRECT_AFTER_LOGIN_PARAM_NAME);
        doThrow(new AuthenticationManagerNotFoundException("")).when(servlet).doLogin(req, resp);

        servlet.doPost(req, resp);
    }

    @Test(expected = LoginFailedException.class)
    public void testTenantInMaintenance() throws Exception {
        final LoginManager loginManager = mock(LoginManager.class);
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn(loginManager).when(servlet).getLoginManager();
        
        final TenantStatusException tenantInMaintenanceException = new TenantStatusException("Tenant is in pause, unable to log in with other user than the technical user.");
        doThrow(tenantInMaintenanceException).when(loginManager).login(req, resp);

        try {
            servlet.doLogin(req, resp);
        } finally {
            verify(req, times(1)).setAttribute(LoginServlet.TENANT_IN_MAINTENACE_MESSAGE, LoginServlet.TENANT_IN_MAINTENACE_MESSAGE);
        }
    }

    @Test
    public void testTenantNotInMaintenance() throws Exception {
        final LoginManager loginManager = mock(LoginManager.class);
        final LoginServlet servlet = spy(new LoginServlet());
        doReturn(httpSession).when(req).getSession();
        doReturn(apiSession).when(httpSession).getAttribute(SessionUtil.API_SESSION_PARAM_KEY);
        doReturn(loginManager).when(servlet).getLoginManager();
        doNothing().when(loginManager).login(req, resp);
        
        servlet.doLogin(req, resp);
        
        verify(req, times(0)).setAttribute(LoginServlet.TENANT_IN_MAINTENACE_MESSAGE, LoginServlet.TENANT_IN_MAINTENACE_MESSAGE);
        verify(loginManager).login(req, resp);
    }

}
