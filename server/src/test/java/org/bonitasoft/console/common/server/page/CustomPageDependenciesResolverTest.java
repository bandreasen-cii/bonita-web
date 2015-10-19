/**
 * Copyright (C) 2015 BonitaSoft S.A.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bonitasoft.console.common.server.preferences.constants.WebBonitaConstantsUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CustomPageDependenciesResolverTest {

    @Mock
    private WebBonitaConstantsUtils webBonitaConstantsUtils;
    @Mock
    private PageResourceProvider pageResourceProvider;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void should_throw_an_IllegalStateException_when_accessing_tmp_folder_before_resolving_libraries() throws Exception {
        final CustomPageDependenciesResolver resolver = newCustomPageDependenciesResolver();

        expectedException.expect(IllegalStateException.class);

        resolver.getTempFolder();
    }

    @Test
    public void should_resolve_dependencies_content() throws Exception {
        final CustomPageDependenciesResolver resolver = newCustomPageDependenciesResolver();
        when(pageResourceProvider.getPageDirectory()).thenReturn(testPageFolder());
        when(pageResourceProvider.getPageName()).thenReturn("myCustomPage");

        final Map<String, byte[]> dependenciesContent = resolver.resolveCustomPageDependencies();

        assertThat(dependenciesContent).containsKeys("resource.properties",
                "bdm-client.jar",
                "bdm-dao.jar",
                "javassist-1.18.1-GA.jar",
                "util.jar");
        assertThat(CustomPageDependenciesResolver.PAGES_LIB_TMPDIR).containsKey("myCustomPage");
        final File cachedTmpFoler = CustomPageDependenciesResolver.PAGES_LIB_TMPDIR.get("myCustomPage");
        assertThat(cachedTmpFoler.exists()).isTrue();
        assertThat(resolver.getTempFolder()).isEqualTo(cachedTmpFoler);
    }

    @Test
    public void should_delete_temporary_lib_foler() throws Exception {
        final CustomPageDependenciesResolver resolver = newCustomPageDependenciesResolver();
        when(pageResourceProvider.getPageDirectory()).thenReturn(testPageFolder());
        when(pageResourceProvider.getPageName()).thenReturn("myCustomPage");
        resolver.resolveCustomPageDependencies();

        assertThat(CustomPageDependenciesResolver.PAGES_LIB_TMPDIR).containsKey("myCustomPage");
        assertThat(CustomPageDependenciesResolver.PAGES_LIB_TMPDIR.get("myCustomPage").exists()).isTrue();

        final File tempFolder = CustomPageDependenciesResolver.removePageLibTempFolder("myCustomPage");

        assertThat(CustomPageDependenciesResolver.PAGES_LIB_TMPDIR).doesNotContainKey("myCustomPage");
        assertThat(tempFolder.exists()).isFalse();
    }

    @Test
    public void should_resolve_dependencies_return_an_empty_map_if_no_lib_folder_is_found_in_custom_page() throws Exception {
        final CustomPageDependenciesResolver resolver = newCustomPageDependenciesResolver();
        when(pageResourceProvider.getPageDirectory()).thenReturn(null);

        final Map<String, byte[]> dependenciesContent = resolver.resolveCustomPageDependencies();

        assertThat(dependenciesContent).isEmpty();
    }

    private File testPageFolder() {
        return new File(CustomPageDependenciesResolverTest.class.getResource("/ARootPageFolder").getFile());
    }

    private CustomPageDependenciesResolver newCustomPageDependenciesResolver() throws IOException {
        when(webBonitaConstantsUtils.getTempFolder()).thenReturn(tmpFolder.newFolder());
        return new CustomPageDependenciesResolver(pageResourceProvider,webBonitaConstantsUtils);
    }

}