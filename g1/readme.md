# Run Program

mvn clean compile exec:java -Dexec.mainClass=Tester

# Program

Trying to achieve fault tolerance for the server.
2 Clients, 2 Servers, clients make that same request to both servers, wait the first response and keep running.

# Conclusions

Program should work fine with 1 server multiple clients.
Could cause issues for multiple servers 1 client if messages we're not ordered, but since the client waits for an answer, that is not the case.

If multiple clientes and multiple servers are running, it's not possible to keep consistency if the clients just wait for the first response and the servers don't comunicate among themselves, that is because the client requests may reach the servers in different order, and, in that case they may produce a diferent response, the state becomes inconsistent.(Ex: client 1 wthdraws 100€, client 2 withdraws 50€, both servers have 100€, server 1 gets -100,-50, responds true,false, server 2 gets -50,-100, respondes true,false, both clients may receive the positive response, in that case, client 1 has 100€, client 2 has 50€, server 1 has 0€, server 2 has 50€, this state is clearly corrupt)
