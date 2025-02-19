/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.web.toolkit.client.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIIncorrectIdException;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIItemIdMalformedException;
import org.bonitasoft.web.toolkit.client.common.json.JSonSerializer;
import org.bonitasoft.web.toolkit.client.common.json.JsonSerializable;
import org.bonitasoft.web.toolkit.client.data.item.ItemDefinition;
import org.bonitasoft.web.toolkit.client.ui.utils.ListUtils;

/**
 * @author Julien Mege
 */
public class APIID implements JsonSerializable {

    private static final String SEPARATOR = "/";

    private final List<String> ids = new ArrayList<String>();

    private ItemDefinition<?> itemDefinition = null;

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private APIID(final String... id) {
        this(Arrays.asList(id));
    }

    private APIID(final Long... id) {
        for (final Long i : id) {
            this.ids.add(i != null ? String.valueOf(i) : null);
        }
    }

    private APIID(final List<String> ids) {
        // if the id passed is a serialized APIID
        if (ids.size() == 1 && ids.get(0).contains("/")) {
            this.ids.addAll(Arrays.asList(ids.get(0).split("/")));
        } else {
            this.ids.addAll(ids);
        }
    }

    public void setItemDefinition(final ItemDefinition<?> definition) {
        this.itemDefinition = definition;

        final int size = this.itemDefinition.getPrimaryKeys().size();

        if (this.ids.size() < size) {
            if (size == 0) {
                throw new APIException(this.itemDefinition.getClass().getName() + " is missing a valid primaryKey");
            }

            if (size == 1) {
                throw new APIException(
                        "Wrong APIID format for  [" + this.itemDefinition.getClass().getName() + "]." +
                                " This APIID must be a single id.");
            }

            throw new APIException(
                    "Wrong APIID format for  [" + this.itemDefinition.getClass().getName() + "]." +
                            " This APIID must be compound of [" +
                            ListUtils.join(this.itemDefinition.getPrimaryKeys(), ",") +
                            "] in this exact order.");
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // STATIC CONSTRUCTORS
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final APIID makeAPIID(final String... id) {
        if (id == null) {
            return null;
        }
        return makeAPIID(Arrays.asList(id));
    }

    public static final APIID makeAPIID(final Long... ids) {
        if (ids == null || ids.length == 0) {
            return null;
        }

        // If at least one id is not null
        for (final Long id : ids) {
            if (id != null && id > 0L) {
                return new APIID(ids);
            }
        }

        return null;
    }

    public static final APIID makeAPIID(final List<String> ids) {
        if (ids == null || ids.size() == 0) {
            return null;
        }

        // If at least one id is not null
        for (final String id : ids) {
            if (id != null && !id.isEmpty()) {
                return new APIID(ids);
            }
        }

        return null;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GETTERS
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List<String> getIds() {
        return this.ids;
    }

    @Override
    public String toString() {
        String resourceId = "";
        if (this.ids != null && this.ids.size() > 0) {
            for (final String id : this.ids) {
                if (!"".equals(resourceId)) {
                    resourceId = resourceId + SEPARATOR;
                }
                resourceId = resourceId + id;
            }
        }
        return resourceId;
    }

    public Long toLong() {
        if (this.ids.size() > 1) {
            throw new IllegalArgumentException("Can't convert compound ID to long");
        }

        try {
            return Long.parseLong(this.ids.get(0));
        } catch (final NumberFormatException e) {
            throw new APIItemIdMalformedException("APIID", "Can't convert non numeric ID to long");
        }

    }

    /*
     * Retrieve a part of the id with his index.
     * @return this method return a part of the id as a String.
     */
    public final String getPart(final int partIndex) {
        return this.ids.get(partIndex);
    }

    public final Long getPartAsLong(final int partIndex) {
        return Long.parseLong(getPart(partIndex));
    }

    public String getPart(final String attributeName) {
        final int index = this.itemDefinition.getPrimaryKeys().indexOf(attributeName);

        if (index == -1) {
            throw new APIException(attributeName +
                    " is an invalid APIID index. " +
                    "This APIID must be made of " +
                    ListUtils.join(this.itemDefinition.getPrimaryKeys(), ", ") +
                    " in this exact order.");
        }

        return this.ids.get(index);
    }

    public Long getPartAsLong(final String attributeName) {
        String part = getPart(attributeName);
        return part == null ? null : Long.valueOf(part);
    }

    public APIID getPartAsAPIID(final String attributeName) {
        return APIID.makeAPIID(getPart(attributeName));
    }

    public static final List<Long> toLongList(final List<APIID> ids) {
        final List<Long> results = new ArrayList<Long>();
        for (final APIID id : ids) {
            results.add(id.toLong());
        }
        return results;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // EQUALS
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        return toString().equals(obj.toString());
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // JSON
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toJson() {
        return JSonSerializer.serialize(this.ids);
    }

}
