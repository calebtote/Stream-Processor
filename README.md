####Concept
This started as a project for a course in distributed systems during my masters program. The basic idea is based in part off Apache Storm and Heron.

The design has some core classes that need to be discussed before we get into how it is implemented and works in practice.

######Coordinator
The `Coordinator` class is much like a Topology Master in Apache Storm, just a bit watered down. The `Coordinator` assigns available workers to utilities (think, bolts), and sends an initialization RPC letting each host know who the `Coordinator` is, and which utilities have been assigned to them.

######AppBase / UtilBase
These two base classes make up our "topology". Every application is derived from `AppBase`. Applications are straightforward in that they simply define a set of utilities to be applied to the dataset. Utilities are derived from `UtilBase`, and are executed in the order they are added. Utilities are `Callable` objects, which override a thread-safe `call()` method.

######WokerController
Every worker in the system has a `WorkerController` which acts as a local coordination class for the worker host. This class helps coordinate which application is active, which utility is assigned to this worker, who the coordinator is, who the next hop is, and where our GateKeeper is.

######GateKeeper
The `GateKeeper` at each worker is the entry and exit point of all streams of data. Stream units of work are enqueued into the `GateKeeper`, which applies the assigned utilities to the stream. Once the utility has been applied to the stream, the `GateKeeper` forwards the result to the next worker for further processing.

![diagram](http://i.imgur.com/cMyx7XT.png)

######General Flow / Idea
Suppose you have a massive set of data, perhaps a 100GB store of tweets from around the world. Your goal is to find all tweets that satisfy all of the following conditions:  (1) The tweet contains `#PresidentialDebate`, (2) The tweet is from someone with  `>5,000` followers, and (3) The tweet has been re-tweeted `+500` times.

Once proper utilities are defined (see: `UtilBase` and it's derivations), here's a high-level view of what this system does:
- The `Coordinator` initializes the topology, and assigns utilities to each available `Worker`
- The `Coordinator` starts streaming in the dataset, and connects the stream to the endpoint of the next `Worker` in the chain
- Supposing the utilities were assigned in order (optional), the first `Worker` will analyze the stream as it flows through it, and remove all tweets that do not contain `#PresidentialDebate`. It will then connect the stream output to the next `Worker` in the chain.
- The next worker will analyze the stream as it flows through it, and remove all tweets who were not from users with more than `5,000` followers. It will then connect the stream output to the next `Worker` in the chain.
- The final worker will analyze the stream as it flows through it, and remove all tweets that haven't been re-tweeted more than `500` times.
- Finally, the last `Worker` in the chain will return the results to the `Coordinator` for summarization of the results.

Much of the above is fully customizable, based on configuration settings, utility definitions and assignments, and how you want your summarization of data to appear. Additionally, `Workers` do not explicitly have to work in serial, but can be adjusted fairly easily (through code changes) to have a single stream source as the input to both `Worker`s, simultaneously, with a consolidation node that merges the results. This isn't fully implemented, but the framework doesn't need much modification.

Note also that these aren't distinct opperations, but are in fact happening in parallel. To put it another way, the `Coordinator` could be accumulating results from the last `Worker` in the chain, _while it's still streaming in data_ from the source. Indeed, the stream _flows_ through the topology.

#####File needed to run the project:
- UX.jar                  -- main jar containing project compiled code<br>
- config.properties       -- properties file containing the configuration parameters of the system<br>
- kryonet-2.21-all.jar    -- network lib for managing connections and serializations<br>
<br>

#####Configuration properties:<br>
[see the config.properties for details]<br>
<br>

#####Running the application:<br>
- From the command prompt, run the following:   java -jar UX.jar<br>
