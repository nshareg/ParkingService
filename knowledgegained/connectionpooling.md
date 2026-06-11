As in large scale systems, we can get an unboundedly large number of requests to the server, and in that case we have two main ways to go:

1. Create a new connection for each request, but such a naive solution comes with huge overhead of creating a connection, which can load the application even more.
2. The other one is reusing the same connection, this one will also be catastrophic, as all other requests will be forced to sit and wait until the other one finishes.

So the solution comes intuitively if we have used executor pools before, now the same pooling will be done with connections. At the start we create, for example, 20 connections, which will be reused across requests, so we will not be forced to create a new connection each time. After each request, if the correct number of active connections was chosen initially, we will always have an active and unbusy connection ready to serve.

#### Overhead

The overhead of the aforementioned solution is the case when we have rare connections to the database, creating a pool of connections will be expensive at startup and will not provide any benefit in the future.