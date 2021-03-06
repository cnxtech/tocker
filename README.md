Welcome to Tocker

This project is designed to help the development process when using Docker containers by removing most of the boilerplate required for spinning up and shutting down Docker containers.

# Creating a Docker instance

```
this.instance = DockerInstance
                .fromImage("redis")
                .mappingPorts(PortMap.of(6379, 6380))
                .withContainerName("my-redis-container")
                .build()
```

This will create a Docker instance for the latest version of Redis and bind the Redis default port of 6379 to the local host on port 6380, meaning you can access Redis on port 6380 on your host.  Note that the container will not be started, you've just created the instance.

When running from within an IDE you need to ensure the Docker environment variables are set and exposed in order for the DockerClient to run correctly.

# Starting/Stopping an instance

To start an instance:

```
instance.run()
```

To stop an instance:

```
instance.stop()
```

Starting a container is pretty much an asychronous operation- control will be returned once the start operation has been accepted, but whatever your container is running might not yet be available. For example, mysql on my MBP takes 11 seconds to start.  During tests you will more than likely not want to return from the `run()` call until the service is actually ready to receive requests.  The `run()` method is overloaded to accept an `AliveStrategy` which defines a single method that will block until ready.  For Redis this strategy is a `retrying` strategy that attempts to connect to Redis.

```
instance.run(AliveStrategies.retrying(() -> redisIsAcceptingConnections(), 10, 100);

private Boolean redisIsAcceptingConnections() {
	try {
		new Jedis(instance.host(), 6380);
        } catch (JedisException ex) {
		return false;	
	}
	return true;
}
```

This will run the `redisIsAcceptingConnections` function every 100 milliseconds for a maximum of 10 times before giving up.  As soon as Redis is ready, the `run()` method will return control.

# Fetching host information

When running Docker on a Windows or Mac you will be making use of boot2docker or docker-machine, both of which mean that you cannot access the container on localhost.  Because of this a Docker instance will return the host you need to connect to.  Make sure you always use this rather than hardcoding an IP address which may change between machines.

# Recommendations

Don't tear down the containers at the end of tests! You will incurr severe costs in terms of time for running your tests.  Containers are lightweight so can be left running during the development process.  Each test can start the container and if it is already alive you should not notice any difference to running a local instance of that infrastructure.  When starting a container the `AliveStrategy` is first consulted to see if anything actually has to happen.  With the Redis example above, once the container is running your tests run at the speed of unit tests.

# Downloading
## Maven
```
<dependency>
   <groupId>com.shazam.tocker</groupId>
   <artifactId>tocker-core</artifactId>
   <version>0.0.9</version>
</dependency>
```

## Gradle
```
com.shazam.tocker:tocker-core:0.0.9
```

# Building
tocker is built using the Gradle wrapper and uses Spock for tests

```
$ ./gradlew test
```