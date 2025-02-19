/*
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

package org.bonitasoft.console.common.server.utils;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.bonitasoft.console.common.server.i18n.I18n;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Created by Vincent Elcrin
 * Date: 10/10/13
 * Time: 10:25
 */
public class LocaleUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        HashMap<String, String> availableLocales;
        availableLocales = new HashMap<>();
        availableLocales.put("en", "English");
        availableLocales.put("fr", "Français");
        availableLocales.put("es", "Español");
        availableLocales.put("pt_BR", "Português (Brasil)");
        availableLocales.put("ja", "日本語");

        I18n i18n = mock(I18n.class);
        I18n.setInstance(i18n);
        Mockito.when(i18n.getAvailableLocalesFor(anyString())).thenReturn(availableLocales);
    }

    @After
    public void cleanUp() {
        I18n.setInstance(null);
    }

    @Test
    public void testLocalCanBeRetrieveFromCookie() {
        Cookie[] cookies = {
                new Cookie("aCookie", "value"),
                new Cookie(LocaleUtils.LOCALE_COOKIE_NAME, "fr"),
                new Cookie("aNullCookie", null),
        };
        doReturn(cookies).when(request).getCookies();

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("fr", locale);
    }

    @Test
    public void testANullCookieResultsWithBrowserLocale() {
        doReturn(Locale.CANADA_FRENCH).when(request).getLocale();

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals(Locale.FRENCH.toString(), locale);
    }

    @Test
    public void testANullCookieAndBrowserLocaleResultsWithDefaultLocale() {

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("en", locale);
    }

    @Test
    public void testWeCanRetrieveLocaleFromCookie() {
        Cookie[] cookies = {
                new Cookie(LocaleUtils.LOCALE_COOKIE_NAME, "en_US"),
        };
        doReturn(cookies).when(request).getCookies();

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("en", locale);
    }

    @Test
    public void testWeCanRetrieveLocaleFromRequest() {
        Cookie[] cookies = {
                new Cookie(LocaleUtils.LOCALE_COOKIE_NAME, "en_US"),
        };
        doReturn(cookies).when(request).getCookies();

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("en", locale);
    }

    @Test
    public void testAnInvalidCookieResultsWithDefaultLocale() {
        Cookie[] cookies = {
                new Cookie(LocaleUtils.LOCALE_COOKIE_NAME, "weirdvalue"),
        };
        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("en", locale);
    }

    @Test
    public void testAnInvalidLocaleInRequestResultsWithDefaultLocale() {
        doReturn("weirdvalue").when(request).getParameter(LocaleUtils.LOCALE_PARAM);

        String locale = LocaleUtils.getUserLocaleAsString(request);

        assertEquals("en", locale);
    }
}
