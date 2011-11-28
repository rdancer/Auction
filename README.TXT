README.TXT -- Basic information about this project

Solution of the summative assignment in the Networks submodule of the Computer
Systems II module, given in Michælmas 2011.  This all at the Durham University,
Durham, UK.

Unless stated otherwise, all files are Copyright © 2011 Jan Minář
<rdancer@rdancer.org>.  All rights reserved.

The deliverables of this assignment are:

(1) Source code for the client ("bidder") and server ("auctioneer") programs
(2) 3-page report in MS-Word format


USAGE

There are two programs:

    (1) Client.java -- the client program
    (2) Server.java -- the server program


Run them like this:

    java Client <HOST> <PORT> <CLIENT_ID>
    java Server <PORT>

You can run many clients in parallell connecting to the same HOST:PORT, i.e. to
the same server.  Multiple clients can share one CLIENT_ID.
