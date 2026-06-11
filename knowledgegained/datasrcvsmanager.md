While both `DataSource` and `DriverManager` return the same `Connection`, there is a fundamental difference between them.

`DataSource` is an abstraction, which comes in handy when using connection poolers, it allows implementing functionality for connection reuse.

At the same time `DriverManager` is a one time use thing, gives a connection once, and every time goes through a TCP handshake creating a new connection, introducing overhead. In short, `DriverManager` is a hammer, `DataSource` is a drill press.