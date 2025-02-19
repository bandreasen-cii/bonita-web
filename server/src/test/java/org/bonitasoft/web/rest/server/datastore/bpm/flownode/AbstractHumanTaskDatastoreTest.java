package org.bonitasoft.web.rest.server.datastore.bpm.flownode;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.impl.APISessionImpl;
import org.bonitasoft.web.rest.model.bpm.flownode.ActivityItem;
import org.bonitasoft.web.rest.model.bpm.flownode.HumanTaskItem;
import org.bonitasoft.web.rest.server.datastore.converter.ActivityAttributeConverter;
import org.bonitasoft.web.rest.server.datastore.utils.Sort;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class AbstractHumanTaskDatastoreTest {

    @Mock
    private ProcessAPI processAPI;
    private APISession session = new APISessionImpl(55L, new Date(), 5000, "john", 44L, "default", 1L);
    private AbstractHumanTaskDatastore<HumanTaskItem, HumanTaskInstance> datastore = new AbstractHumanTaskDatastore<HumanTaskItem, HumanTaskInstance>(session) {
        @Override
        protected ProcessAPI getProcessAPI() {
            return processAPI;
        }
    };

    @Test
    public void should_use_searchAssignedAndPendingHumanTasks_when_filtering_ready_state() throws SearchException {
        datastore.runSearch(new SearchOptionsBuilder(1, 100), Collections.singletonMap("state", "ready"));

        verify(processAPI).searchAssignedAndPendingHumanTasks(any());
    }

    @Test
    public void should_use_searchHumanTaskInstances_when_there_is_no_particular_filter() throws SearchException {
        datastore.runSearch(new SearchOptionsBuilder(1, 100), Collections.singletonMap("tpye", "toto"));

        verify(processAPI).searchHumanTaskInstances(any());
    }

    @Test
    public void should_SearchOptionBuilderConvertSortParameter() throws SearchException {
        SearchOptionsBuilder builder = datastore.makeSearchOptionBuilder(0, 10, null, ActivityItem.ATTRIBUTE_ROOT_CASE_ID + " " +  Order.DESC, new HashMap<String, String>());

        Assert.assertEquals(builder.done().getSorts().size(),1);
        Assert.assertEquals(builder.done().getSorts().get(0).getField(), ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID);
        Assert.assertEquals(builder.done().getSorts().get(0).getOrder(), Order.DESC);
    }


}