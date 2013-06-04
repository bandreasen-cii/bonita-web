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
package org.bonitasoft.console.client.admin.process.view.section.entitymapping;

import static org.bonitasoft.web.toolkit.client.common.i18n.AbstractI18n._;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.console.client.admin.process.view.section.entitymapping.action.ShowAddUserToActorPageAction;
import org.bonitasoft.console.client.model.bpm.process.ActorItem;
import org.bonitasoft.console.client.model.bpm.process.ActorMemberItem;
import org.bonitasoft.console.client.model.identity.MemberType;
import org.bonitasoft.console.common.client.FilterKey;
import org.bonitasoft.web.toolkit.client.common.texttemplate.Arg;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.bonitasoft.web.toolkit.client.ui.action.HistoryBackAction;
import org.bonitasoft.web.toolkit.client.ui.component.button.ButtonAction;
import org.bonitasoft.web.toolkit.client.ui.component.table.ItemTable;
import org.bonitasoft.web.toolkit.client.ui.component.table.Table;

/**
 * User List page
 * 
 * @author Julien Mege
 */
public class ListProcessActorUserPage extends ListProcessActorMemberPage {

    public static final String TOKEN = "ListProcessActorUser";

    @Override
    public void defineTitle(final ActorItem actor) {
        setTitle(_("Edit users of actor %actor_name%", new Arg("actor_name", getDisplayName(actor))));
    }

    @Override
    public void buildView(final ActorItem actor) {
        addBody(userTable(actor));
    }

    private ItemTable userTable(final ActorItem actor) {
        final String actorId = actor.getId().toString();
        final Map<String, String> params = builItemTableParameters(actor, actorId);
        return userItemTable(actor, actorId, params);
    }

    private Map<String, String> builItemTableParameters(final ActorItem actor, final String actorId) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("id", actorId);
        params.put(ActorMemberItem.ATTRIBUTE_ACTOR_ID, actor.getId().toString());
        params.put(DeleteActorMemberPage.PARAM_REDIRECT_TOKEN, TOKEN);
        params.put(FilterKey.PROCESS_ID.name(), this.getParameter(FilterKey.PROCESS_ID.name()));
        return params;
    }

    private ItemTable userItemTable(final ActorItem actor, final String actorId, final Map<String, String> params) {
        return buildItemTable(actor.getId(), MemberType.USER, params)
                .addAction(new ButtonAction("btn-add-user", _("Add a user"), "", new ShowAddUserToActorPageAction(actorId)))
                .addAction(new ButtonAction("btn-close", _("Close"), "", new HistoryBackAction()))
                .setView(Table.VIEW_TYPE.FORM);
    }

    public static Map<String, String> buildPageParameters(final APIID actorId) {
        return Collections.singletonMap(ActorMemberItem.ATTRIBUTE_ACTOR_ID, actorId.toString());
    }

    @Override
    public String defineToken() {
        return TOKEN;
    }
}
