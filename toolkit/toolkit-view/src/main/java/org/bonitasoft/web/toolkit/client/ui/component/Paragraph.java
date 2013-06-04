/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.bonitasoft.web.toolkit.client.ui.component;

import org.bonitasoft.web.toolkit.client.ui.component.core.AbstractComponent;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * @author Séverin Moussel
 * 
 */
public class Paragraph extends Text {

    public Paragraph(final String text, final AbstractComponent... args) {
        super(text, args);
    }

    public Paragraph(final AbstractComponent... args) {
        super("", args);
    }

    public Paragraph(final String text) {
        super(text);
    }

    /**
     * Generate the DOM Element corresponding to the current Text Element.
     */
    @Override
    protected Element makeElement() {
        this.rootElement = DOM.createElement(this.rootTagName);

        makeTextHtml();

        return this.rootElement;
    }

}
