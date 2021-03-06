package org.alm;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.alm.model.*;
import org.apache.commons.lang.StringUtils;

public final class Dao
{
    private Dao()
    {
    }

    /**
     * @return null if authenticated.<br> a url to authenticate against if not authenticated.
     * @throws Exception
     */
    public static String isAuthenticated() throws Exception
    {
        String isAuthenticatedUrl = "qcbin/rest/is-authenticated";

        try
        {
            connector().get(isAuthenticatedUrl, Response.class, null, null);

            return null;
        }
        catch (ResponseException ex)
        {
            Response response = ex.response();

            if (response.getStatus() != Status.UNAUTHORIZED.getStatusCode())
            {
                throw ex;
            }

            String authPoint = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);

            if (StringUtils.isNotBlank(authPoint))
            {
                authPoint = authPoint.split("=")[1].replace("\"", "");
                authPoint += "/authenticate";

                return authPoint;
            }

            throw new Exception("Invalid authentication point");
        }
    }

    /**
     * Client sends a valid Basic Authorization header to the authentication point
     * and server set cookies on client.
     *
     * @param authenticationPoint to authenticate at
     * @param username
     * @param password
     * @throws Exception
     */
    public static void login(String authenticationPoint, String username, String password) throws Exception
    {
        connector().get(authenticationPoint, Response.class, RestConnector.createBasicAuthHeader(username, password), null);
    }

    /**
     * Make a call to is-authenticated resource to obtain authenticationPoint and do login.
     *
     * @param username
     * @param password
     * @throws Exception
     */
    public static void login(String username, String password) throws Exception
    {
        String authenticationPoint = isAuthenticated();

        if (authenticationPoint != null)
        {
            URI uri = new URI(authenticationPoint);

            login(uri.getPath(), username, password);
        }
    }

    /**
     * Close session on server and clean session cookies on client
     *
     * @throws Exception
     */
    public static void logout() throws Exception
    {
        String logoutUrl = "qcbin/authentication-point/logout";

        connector().get(logoutUrl, Response.class, null, null);
    }

    /**
     * Read the test entity with the specified ID
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static Test readTest(String id) throws Exception
    {
        String testUrl = connector().buildEntityUrl("test", id);

        return connector().get(testUrl, Test.class, null, null);
    }

    /**
     * Read the test set entity with the specified ID
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static TestSet readTestSet(String id) throws Exception
    {
        String testSetUrl = connector().buildEntityUrl("test-set", id);

        return connector().get(testSetUrl, TestSet.class, null, null);
    }

    /**
     * Read the test instance set entity with the specified ID
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static TestInstance readTestInstance(String id) throws Exception
    {
        String testInstanceUrl = connector().buildEntityUrl("test-instance", id);

        return connector().get(testInstanceUrl, TestInstance.class, null, null);
    }

    /**
     * Read the test instance entities wuth the specified testSetId
     *
     * @param testSetId
     * @return
     * @throws Exception
     */
    public static TestInstances readTestInstances(String testSetId) throws Exception
    {
        String testInstancesUrl = connector().buildEntityCollectionUrl("test-instance");

        Map<String, String> criteria = new HashMap<String, String>();
        criteria.put("query", "{cycle-id[" + testSetId + "]}");

        return connector().get(testInstancesUrl, TestInstances.class, null, criteria);
    }

    /**
     * Create an attachment for run entity
     *
     * @param runId
     * @param fileName to use on serverside
     * @param fileData content of file
     * @return the xml of the metadata on the created attachment
     * @throws Exception
     */
    public static Attachment createRunAttachment(String runId, String fileName, byte[] fileData) throws Exception
    {
        String attachmentsUrl =  connector().buildEntityUrl("run", runId) + "/attachments";

        return createAttachment(attachmentsUrl, fileName, fileData);
    }

    /**
     * Create an attachment for run step entity
     *
     * @param runStepId
     * @param fileName to use on serverside
     * @param fileData content of file
     * @return the xml of the metadata on the created attachment
     * @throws Exception
     */
    public static Attachment createRunStepAttachment(String runStepId, String fileName, byte[] fileData) throws Exception
    {
        String attachmentsUrl =  connector().buildEntityUrl("run-step", runStepId) + "/attachments";

        return createAttachment(attachmentsUrl, fileName, fileData);
    }

    /**
     * Create run entity
     *
     * @param run
     * @return
     * @throws Exception
     */
    public static Run createRun(Run run) throws Exception
    {
        String runsUrl =  connector().buildEntityCollectionUrl("run");

        return connector().post(runsUrl, Run.class, null, null, run);
    }

    /**
     * Update run entity
     *
     * @param run
     * @return
     * @throws Exception
     */
    public static Run updateRun(Run run) throws Exception
    {
        String runUrl =  connector().buildEntityUrl("run", run.id());

        run.clearBeforeUpdate();

        return connector().put(runUrl, Run.class, null, null, run);
    }

    /**
     * Read a collection of run-steps of the specified run
     *
     * @param runId
     * @return
     * @throws Exception
     */
    public static RunSteps readRunSteps(String runId) throws Exception
    {
        String runStepsUrl =  connector().buildEntityUrl("run", runId) + "/run-steps";

        return connector().get(runStepsUrl, RunSteps.class, null, null);
    }

    /**
     * Create a new run step
     *
     * @param runStep
     * @return
     * @throws Exception
     */
    public static RunStep createRunStep(RunStep runStep) throws Exception
    {
        String runStepsUrl = connector().buildEntityUrl("run", runStep.runId()) + "/run-steps";

        return connector().post(runStepsUrl, RunStep.class, null, null, runStep);
    }

    /**
     * Update a run step
     *
     * @param runStep
     * @return
     * @throws Exception
     */
    public static RunStep updateRunStep(RunStep runStep) throws Exception
    {
        String runStepUrl = connector().buildEntityUrl("run", runStep.runId()) + "/run-steps/" + runStep.id();

        runStep.clearBeforeUpdate();

        return connector().put(runStepUrl, RunStep.class, null, null, runStep);
    }

    /**
     * Gets an instance of RestConnector
     *
     * @return
     */
    private static RestConnector connector()
    {
        return RestConnector.instance();
    }

    /**
     * Create attachment
     *
     * @param entityUrl url of entity to attach the file to
     * @param fileName to use on serverside
     * @param payload content of file
     * @return the xml of the metadata on the created attachment
     * @throws Exception
     */
    private static Attachment createAttachment(String entityUrl, String fileName, byte[] fileData) throws Exception
    {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
        headers.add("Slug", fileName);

        return connector().post(entityUrl, Attachment.class, headers, null, fileData, "application/octet-stream");
    }
}
