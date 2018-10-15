import java.io.*; // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries

class Worker0 extends Thread { // Class definition
    Socket sock; // Class member, socket, local to Worker.
    Worker0 (Socket s) {sock = s;} // Constructor, assign arg s to local sock
    public void run()
    {
        // Get I/O streams in/out from the socket:
        PrintStream out = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));
            out = new PrintStream(sock.getOutputStream());
            // Note that this branch might not execute when expected:
            try {
                String name;
                name = in.readLine ();
                System.out.println("Looking up " + name);
                printRemoteAddress(name, out);
            } catch (IOException x) {
                System.out.println("Server read error");
                x.printStackTrace ();
            }
            sock.close(); // close this connection, but not the server;
        } catch (IOException ioe) {System.out.println(ioe);}
    }

    static void printRemoteAddress (String name, PrintStream out) {
        try {
            out.println("Looking up " + name + "...");
            InetAddress machine = InetAddress.getByName (name);
            out.println("Host name : " + machine.getHostName ()); // To client...
        } catch(UnknownHostException ex) {
            out.println ("Failed in atempt to look up " + name);
        }
    }
}

class Server0 {

    public static void main(String a[]) throws IOException {
        int q_len = 6; /* Not intersting. Number of requests for OpSys to queue */
        int port = 4710;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println
                ("Clark Elliott's Inet server 1.8 starting up, listening at port 4710.\n");
        while (true) {
            sock = servsock.accept(); // wait for the next client connection
            new Worker0(sock).start(); // Spawn worker to handle it
        }
    }
}

class Worker1 extends Thread { // Class definition
    Socket sock; // Class member, socket, local to Worker.
    Worker1 (Socket s) {sock = s;} // Constructor, assign arg s to local sock
    public void run(){
        // Get I/O streams in/out from the socket:
        PrintStream out = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));
            out = new PrintStream(sock.getOutputStream());
            // Note that this branch might not execute when expected:
            try {
                String name;
                name = in.readLine ();
                System.out.println("Looking up " + name);
                printRemoteAddress(name, out);
            } catch (IOException x) {
                System.out.println("Server read error");
                x.printStackTrace ();
            }
            sock.close(); // close this connection, but not the server;
        } catch (IOException ioe) {System.out.println(ioe);}
    }

    static void printRemoteAddress (String name, PrintStream out) {
        try {
            out.println("Looking up " + name + "...");
            InetAddress machine = InetAddress.getByName (name);
            out.println("Host name : " + machine.getHostName ()); // To client...
        } catch(UnknownHostException ex) {
            out.println ("Failed in atempt to look up " + name);
        }
    }
}

class Server1 {

    public static void main(String a[]) throws IOException {
        int q_len = 6; /* Not intersting. Number of requests for OpSys to queue */
        int port = 4820;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println
                ("Clark Elliott's Inet server 1.8 starting up, listening at port 4820.\n");
        while (true) {
            sock = servsock.accept(); // wait for the next client connection
            new Worker1(sock).start(); // Spawn worker to handle it
        }
    }
}

class Worker2 extends Thread { // Class definition
    Socket sock; // Class member, socket, local to Worker.
    Worker2 (Socket s) {sock = s;} // Constructor, assign arg s to local sock
    public void run(){
        // Get I/O streams in/out from the socket:
        PrintStream out = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));
            out = new PrintStream(sock.getOutputStream());
            // Note that this branch might not execute when expected:
            try {
                String name;
                name = in.readLine ();
                System.out.println("Looking up " + name);
                printRemoteAddress(name, out);
            } catch (IOException x) {
                System.out.println("Server read error");
                x.printStackTrace ();
            }
            sock.close(); // close this connection, but not the server;
        } catch (IOException ioe) {System.out.println(ioe);}
    }

    static void printRemoteAddress (String name, PrintStream out) {
        try {
            out.println("Looking up " + name + "...");
            InetAddress machine = InetAddress.getByName (name);
            out.println("Host name : " + machine.getHostName ()); // To client...
        } catch(UnknownHostException ex) {
            out.println ("Failed in atempt to look up " + name);
        }
    }
}

class Server2 {

    public static void main(String a[]) throws IOException {
        int q_len = 6; /* Not intersting. Number of requests for OpSys to queue */
        int port = 4820;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println
                ("Clark Elliott's Inet server 1.8 starting up, listening at port 4820.\n");
        while (true) {
            sock = servsock.accept(); // wait for the next client connection
            new Worker2(sock).start(); // Spawn worker to handle it
        }
    }
}

class InetClient{
    public static void main (String args[]) {
        String serverName;
        if (args.length < 1) serverName = "localhost";
        else serverName = args[0];

        System.out.println("Clark Elliott's Inet Client, 1.8.\n");
        System.out.println("Using server: " + serverName + ", Port: 1565");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String name;
            do {
                System.out.print
                        ("Enter a hostname or an IP address, (quit) to end: ");
                System.out.flush ();
                name = in.readLine ();
                if (name.indexOf("quit") < 0)
                    getRemoteAddress(name, serverName);
            } while (name.indexOf("quit") < 0);
            System.out.println ("Cancelled by user request.");
        } catch (IOException x) {x.printStackTrace ();}
    }

    static void getRemoteAddress (String name, String serverName){
        Socket sock;
        BufferedReader fromServer;
        PrintStream toServer;
        String textFromServer;

        try{
            /* Open our connection to server port, choose your own port number.. */
            sock = new Socket(serverName, 1565);

            // Create filter I/O streams for the socket:
            fromServer =
                    new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServer = new PrintStream(sock.getOutputStream());
            // Send machine name or IP address to server:
            toServer.println(name); toServer.flush();

            // Read two or three lines of response from the server,
            // and block while synchronously waiting:
            for (int i = 1; i <=3; i++){
                textFromServer = fromServer.readLine();
                if (textFromServer != null) System.out.println(textFromServer);
            }
            sock.close();
        } catch (IOException x) {
            System.out.println ("Socket error.");
            x.printStackTrace ();
        }
    }
}
