package cs425.mp1.agent;

import cs425.mp1.common.QueryProfile;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by Ahmed on 9/2/2015.
 */
public class QueryHandlerTest extends TestCase {

    protected QueryProfile profile = null;
    protected QueryHandler grep = null;

    public void testHandleQueryBasic() throws Exception {

        grep.handleQuery(profile, "index");
        Assert.assertEquals(("02:49:35 127.0.0.1 GET /index.html 200" + "\n"), grep.getQueryResults().getResults());

        grep.handleQuery(profile, "200");
        Assert.assertEquals(5, grep.getQueryResults().getResultsCount());

    }

    public void testHandleQueryIgnoreCase() throws Exception {

        profile.setIgnoreCase(true);
        grep.handleQuery(profile, "get");
        Assert.assertEquals(7, grep.getQueryResults().getResultsCount());


        profile.setIgnoreCase(false);
        grep.handleQuery(profile, "get");
        Assert.assertEquals(0, grep.getQueryResults().getResultsCount());
    }

    public void testHandleQueryRegularExpression() throws Exception {

        profile.setIsRegExpression(true);
        grep.handleQuery(profile, "^04");
        Assert.assertEquals(1, grep.getQueryResults().getResultsCount());

        profile.setIsRegExpression(false);
        grep.handleQuery(profile, "04");
        Assert.assertEquals(3, grep.getQueryResults().getResultsCount());

    }



    public void setUp () throws Exception {
        profile = new QueryProfile();
        profile.setFileName("samples/sample.log");
        grep = QueryHandler.getQueryHandler();

    }
}