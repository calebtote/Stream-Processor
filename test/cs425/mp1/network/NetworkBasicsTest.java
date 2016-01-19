package cs425.mp1.network;

import cs425.mp1.agent.QueryHandler;
import cs425.mp1.common.QueryProfile;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 */
public class NetworkBasicsTest extends TestCase {
    protected QueryProfile profile = null;
    protected QueryHandler grep = null;


    public void testBasicCommunication() throws Exception {
        // TODO: this should be modified to be a config entry for 'local test host.
        // TODO: this needs to be less hacky in general
        // Local hostname Nodes auto start their servers
        // Sometimes 'localhost' isn't part of the hosts file, so this hack works for me
        // The system thinks this is two nodes, but it's the same host for testing purposes.
        Node listenNode = new Node("MacBookAir");

        Node senderNode = new Node("localhost");
        senderNode.getClient().Start();

        Assert.assertTrue(senderNode.getClient().getConnection().getUpdateThread().isAlive());
        Assert.assertTrue(listenNode.getServer().getConnection().getUpdateThread().isAlive());

        Integer result = senderNode.getClient().SendTCP(listenNode, profile);
        Assert.assertEquals(30, (int) result);
    }

    public void setUp () throws Exception {
        profile = new QueryProfile();
        profile.setFileName("data/samples.log");
        profile.setQuery("suexec");
        grep = QueryHandler.getQueryHandler();
    }
}
