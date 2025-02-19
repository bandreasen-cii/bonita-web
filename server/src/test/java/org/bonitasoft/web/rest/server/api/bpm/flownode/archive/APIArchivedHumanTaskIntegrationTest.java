package org.bonitasoft.web.rest.server.api.bpm.flownode.archive;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.*;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.test.toolkit.bpm.TestProcess;
import org.bonitasoft.test.toolkit.bpm.TestProcessFactory;
import org.bonitasoft.test.toolkit.organization.TestUser;
import org.bonitasoft.test.toolkit.organization.TestUserFactory;
import org.bonitasoft.web.rest.model.bpm.flownode.ArchivedHumanTaskItem;
import org.bonitasoft.web.rest.model.bpm.flownode.HumanTaskItem;
import org.bonitasoft.web.rest.server.AbstractConsoleTest;
import org.bonitasoft.web.rest.server.WaitUntil;
import org.bonitasoft.web.rest.server.framework.search.ItemSearchResult;
import org.bonitasoft.web.toolkit.client.common.texttemplate.Arg;
import org.bonitasoft.web.toolkit.client.common.util.MapUtil;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.web.toolkit.client.data.APIID.makeAPIID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * FIXME to be refactored using much more test-toolkit
 */
public class APIArchivedHumanTaskIntegrationTest extends AbstractConsoleTest {

    private APIArchivedHumanTask apiArchivedHumanTask;

    @Override
    public void consoleTestSetUp() throws Exception {
        apiArchivedHumanTask = new APIArchivedHumanTask();
        apiArchivedHumanTask.setCaller(getAPICaller(getInitiator().getSession(), "API/bpm/archivedHumanTask"));
    }

    @Override
    protected TestUser getInitiator() {
        return TestUserFactory.getJohnCarpenter();
    }

    private HumanTaskInstance initArchivedHumanTaskInstance() throws Exception {
        final TestProcess defaultHumanTaskProcess = TestProcessFactory.getDefaultHumanTaskProcess();
        defaultHumanTaskProcess.addActor(getInitiator());
        final ProcessInstance processInstance = defaultHumanTaskProcess.startCase(getInitiator()).getProcessInstance();

        waitPendingHumanTask(processInstance.getId());

        // Retrieve a humanTaskInstance
        final HumanTaskInstance humanTaskInstance = getProcessAPI().getPendingHumanTaskInstances(getInitiator().getId(), 0, 10, null).get(0);
        getProcessAPI().assignUserTask(humanTaskInstance.getId(), getInitiator().getId());

        waitAssignedHumanTask();

        getProcessAPI().executeFlowNode(humanTaskInstance.getId());

        waitArchivedActivityInstance(processInstance.getId());

        return humanTaskInstance;
    }

    private ProcessAPI getProcessAPI() throws Exception {
        return TenantAPIAccessor.getProcessAPI(getInitiator().getSession());
    }

    private ArrayList<String> getProcessIdDeploy() {
        final ArrayList<String> deploys = new ArrayList<String>();
        deploys.add(HumanTaskItem.ATTRIBUTE_PROCESS_ID);
        return deploys;
    }

    private HashMap<String, String> getNameFilter(final HumanTaskInstance humanTaskInstance) {
        final HashMap<String, String> filters = new HashMap<String, String>();
        filters.put(ArchivedHumanTaskItem.ATTRIBUTE_NAME, humanTaskInstance.getName());
        return filters;
    }

    private Map<String, String> buildArchivedHumanTaskStateCompletedForCaseIdFilter(final APIID caseId) {
        return MapUtil.asMap(new Arg(ArchivedHumanTaskItem.ATTRIBUTE_CASE_ID, caseId));
    }

    /**
     * Wait the process contain PendingHumanTaskInstance
     */
    private void waitPendingHumanTask(final long processInstanceId) throws Exception {

        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10);
        searchOptionsBuilder.filter(HumanTaskInstanceSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceId);

        Assert.assertTrue("no pending task instances are found", new WaitUntil(50, 3000) {

            @Override
            protected boolean check() throws Exception {
                return getProcessAPI().searchPendingTasksForUser(getInitiator().getId(), searchOptionsBuilder.done()).getCount() >= 1;
            }
        }.waitUntil());
    }

    private void waitAssignedHumanTask() throws Exception {
        Assert.assertTrue("Human task hasnt been assign", new WaitUntil(50, 3000) {

            @Override
            protected boolean check() throws Exception {
                return getProcessAPI().getAssignedHumanTaskInstances(getInitiator().getId(), 0, 10,
                        ActivityInstanceCriterion.DEFAULT).size() >= 1;
            }
        }.waitUntil());
    }

    /**
     * Wait the process contain ArchivedHumanTaskInstance
     */
    private void waitArchivedActivityInstance(final long processInstanceId) throws Exception {
        Assert.assertTrue("no archived task instances are found", new WaitUntil(50, 3000) {

            @Override
            protected boolean check() throws Exception {
                final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10);
                searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId);
                return getProcessAPI().searchArchivedActivities(searchOptionsBuilder.done()).getCount() >= 1L;
            }
        }.waitUntil());
    }

    @Test
    public void testGetArchivedHumanTask() throws Exception {
        final HumanTaskInstance humanTaskInstance = initArchivedHumanTaskInstance();
        final ArrayList<String> deploys = getProcessIdDeploy();



        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10);
        searchOptionsBuilder.filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, humanTaskInstance.getRootContainerId());
        final ArchivedActivityInstance archivedActivityInstance = getProcessAPI().searchArchivedActivities(searchOptionsBuilder.done()).getResult().get(0);

        final ArchivedHumanTaskItem archivedHumanTaskItem =
                apiArchivedHumanTask.runGet(makeAPIID(archivedActivityInstance.getId()), deploys, new ArrayList<String>());

        assertEquals("Can't get the good archivedTaskItem", archivedHumanTaskItem.getName(), humanTaskInstance.getName());
    }

    @Test
    public void testSearchArchivedHumanTask() throws Exception {
        final HumanTaskInstance humanTaskInstance = initArchivedHumanTaskInstance();
        final ArrayList<String> deploys = getProcessIdDeploy();
        final HashMap<String, String> filters = getNameFilter(humanTaskInstance);

        final ArchivedHumanTaskItem archivedHumanTaskItem = apiArchivedHumanTask.runSearch(0, 1, null, null,
                filters, deploys, new ArrayList<String>()).getResults().get(0);

        assertNotNull("Can't find the good archivedTaskItem", archivedHumanTaskItem);
    }

    @Test
    public void testGetDatastore() {
        assertNotNull("Can't get the Datastore", apiArchivedHumanTask.getDefaultDatastore());
    }

    @Test
    public void archivedHumanTasksCanBeSortedByReachedStateDate() throws Exception {
        shouldSearchArchivedHumaTaskWithOrder(ArchivedHumanTaskItem.ATTRIBUTE_REACHED_STATE_DATE + " DESC");
    }

    @Test
    public void testSearchWithDefaultOrder() throws Exception {
        shouldSearchArchivedHumaTaskWithOrder(apiArchivedHumanTask.defineDefaultSearchOrder());

    }

    private void shouldSearchArchivedHumaTaskWithOrder(final String orders) throws Exception {
        //given
        final HumanTaskInstance humanTaskInstance = initArchivedHumanTaskInstance();
        final ArrayList<String> deploys = getProcessIdDeploy();
        final HashMap<String, String> filters = getNameFilter(humanTaskInstance);

        //when
        final ItemSearchResult<ArchivedHumanTaskItem> search = apiArchivedHumanTask.runSearch(0, 1, null, orders, filters, null, null);

        //then
        assertThat(search.getResults()).as("should get results").isNotEmpty();
    }

}
