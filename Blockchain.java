/**
 * Copyright 2018 Joshua Glepko All rights reserved
 */

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.sql.Time;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/*
Ports and servers
Because we will have multiple participating processes running on the same
machine we will need flexibilty to avoid port conflicts. For each process:
Port 4710+process number receives public keys (4710, 4711, 4712)
Port 4820+process number receives unverified blocks (4820, 4821, 4822)
Port 4930+process number receives updated blockchains (4930, 4931, 4932)
Other ports at your discretion, but please use the same scheme: base+process
number.
 */
abstract class WorkerThread extends Thread
{
    private Socket socket;
    private int processNumber;
    public ArrayList portList;

    public WorkerThread(int processNumber, Socket socket)
    {
        this.socket = socket;
        this.processNumber = processNumber;
        this.portList = portList;
    }

    public void multiCast() throws Exception
    {
        System.out.println("Multicast method has been called.");
        int portPlusID = 0;

        for (int i = 0; i < 3; i++)
        {
            portPlusID = Blockchain.PUBLIC_KEY_PORT_BASE + i;
            System.out.println("\nCurrent port being sent key is: " + portPlusID);
            boolean a = true;
            Socket keySock;
            while (a) {
                try {
                    keySock = new Socket("localhost", portPlusID);
                    a = false;
                    PrintStream out = new PrintStream(keySock.getOutputStream());
                    String publicKeyBase64 = Blockchain.getConvertedPublicKey(Blockchain.publicKey);
                    out.println(portPlusID + " " + publicKeyBase64);
                    keySock.close();
                } catch (Exception e) {

                }
            }

        }
        Thread.sleep(1000);
        String[] fileArgs = new String[1];
        fileArgs[0] = String.valueOf(3); // Broadcast dummy blocks first
        String XMLblock = Blockchain.blockInputE(fileArgs);
        for (int i = 0; i < 3; i++)
        {// Send out unverified blocks to each process
            portPlusID = Blockchain.UNVERIFIED_BLOCKS_PORT_BASE + i;
            System.out.println("\nCurrent port being sent dummy block is: " + portPlusID);
            Socket dbSock = new Socket("localhost", portPlusID);
            PrintStream out = new PrintStream(dbSock.getOutputStream());

            //System.out.println(XMLblock);
            out.println(XMLblock);
            out.flush();
            dbSock.close();
            //out.close();
        }

        Thread.sleep(1000);
        //String[] fileArgs = new String[1];
        fileArgs[0] = String.valueOf(Blockchain.processID); // Broadcast dummy blocks first
        XMLblock = Blockchain.blockInputE(fileArgs);
        for (int i = 0; i < 3; i++)
        {// Send out unverified blocks to each process
            portPlusID = Blockchain.UNVERIFIED_BLOCKS_PORT_BASE + i;
            System.out.println("\nCurrent port being sent unverified block is: " + portPlusID);
            Socket ubSock = new Socket("localhost", portPlusID);
            PrintStream out = new PrintStream(ubSock.getOutputStream());

            //System.out.println(XMLblock);
            out.println(XMLblock);
            out.flush();
            ubSock.close();
            //out.close();
        }
    }

    public Socket getSocket()
    {
        return socket;
    }

    public int getProcessNumber() {
        return processNumber;
    }
}

class PublicKeyServer implements Runnable
{
    public static final String SEND_KEYS  = "sendKeys";
    private int processNumber;
    private boolean keepLooping;
    private int port;

    public PublicKeyServer(int processNumber, int port)
    {
        this.processNumber = processNumber;
        this.keepLooping = true;
        this.port = port;
    }

    public void run()
    {
        Socket sock;
        ServerSocket servsock = null;
        try {
            servsock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println
                ("PublicKeyServer for process:" + processNumber + " up, listening at port " + port + ".\n");
        while (true) {
            try {
                sock = servsock.accept(); // wait for the next client connection
                new PublicKeyWorker(processNumber, sock).start(); // Spawn worker to handle it
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort()
    {
        return this.port;
    }
}

class PublicKeyWorker extends WorkerThread { // Class definition
    PublicKeyWorker(int processNumber, Socket s) {
        super (processNumber, s);
    } // Constructor, assign arg s to local sock

    public void run() {
        // Get I/O streams in/out from the socket:
        try (
                BufferedReader in = new BufferedReader
                        (new InputStreamReader(getSocket().getInputStream()));
        ){
            // read in the first line to see the message type
            String msg = in.readLine();
            System.out.println("Got key: " + msg + "\n");

            // store key and pid in hashmap
            String tmpArray[] = msg.split(" ");
            String tmpPID = tmpArray[0];
            Integer pid = Integer.parseInt(tmpPID);
            String publickKey = Blockchain.getDecodedPublicKey(tmpArray[1]);
            Blockchain.publicKeyHMap.put(pid, publickKey);
            String tmp = "";
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

class BlockchainServer implements Runnable
{
    private int processNumber;
    private boolean keepLooping;
    private int port;

    public BlockchainServer(int processNumber, int port)
    {
        this.processNumber = processNumber;
        this.keepLooping = true;
        this.port = port;
    }
    public void run()
    {
        Socket sock;
        ServerSocket servsock = null;
        try {
            servsock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println
                ("\nBlockchainServer for process:" + processNumber + " up, listening at port " + port + ".");
        while (true) {
            try {
                sock = servsock.accept(); // wait for the next client connection
                new BlockchainWorker(processNumber ,sock).start(); // Spawn worker to handle it
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public int getPort()
    {
        return this.port;
    }
}
class BlockchainWorker extends WorkerThread
{ // Class definition
    BlockchainWorker(int processNumber,Socket s)
    {
        super(processNumber, s);
    } // Constructor, assign arg s to local sock

    public void run() {
        // Get I/O streams in/out from the socket:
        PrintStream out = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader
                    (new InputStreamReader(getSocket().getInputStream()));
            try {
                String blockData = "";
                String block;
                BlockRecord newBlock;
                while ((block = in.readLine()) != null)
                {
                    blockData += block;
                }

                blockData = blockData.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");

                String[] blockRecordStrings = blockData.split("<blockRecord>");

                /*System.out.println("Put in Blockchain: " + blockData);
                Blockchain.blockchain = blockData; // check for winner still*/

                if (Blockchain.blockchainList.size() >= blockRecordStrings.length - 1) {
                    return;
                }

                Blockchain.blockchainList.clear();

                for (int i=1; i < blockRecordStrings.length; i++) {
                    String blockString = ("<blockRecord>" + blockRecordStrings[i]).replace("</BlockLedger>", "");
                    newBlock = (BlockRecord) Blockchain.unmarshallXML(blockString);
                    Blockchain.blockchainList.add(newBlock);
                }

                System.out.println("New Blockchain List:");
                System.out.println(Blockchain.getBlockchainXMLString());

            } catch (IOException | JAXBException x) {
                System.out.println("Server read error");
                x.printStackTrace();
            }
            getSocket().close(); // close this connection, but not the server;
        } catch (IOException x) {
            System.out.println(x);
        }
    }
}

class UnverifiedBlockServer implements Runnable
{
    private int processNumber;
    private boolean keepLooping;
    private int port;
    BlockingQueue<String> queue;

    public UnverifiedBlockServer(int processNumber, int port, BlockingQueue<String> queue)
    {
        this.processNumber = processNumber;
        this.keepLooping = true;
        this.port = port;
        this.queue = queue;
    }
    public void run()
    {
        Socket sock;
        ServerSocket servsock = null;
        try {
            servsock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("UnverifiedBlockServer for process:" + processNumber + " up, listening at port " + port + ".\n");
        while (true) {
            try {
                sock = servsock.accept(); // wait for the next client connection
                new UnverifiedBlockWorker(processNumber, sock, queue).start(); // Spawn worker to handle it
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public int getPort()
    {
        return this.port;
    }
}
class UnverifiedBlockWorker extends WorkerThread
{ // Class definition
    BlockingQueue<String> queue;
    UnverifiedBlockWorker(int processNumber, Socket s, BlockingQueue<String> queue) {
        super(processNumber ,s);
        this.queue = queue;
    } // Constructor, assign arg s to local sock

    public void run() {
        // Get I/O streams in/out from the socket:
        BufferedReader in = null;
        try {
            in = new BufferedReader
                    (new InputStreamReader(getSocket().getInputStream()));
            // Note that this branch might not execute when expected:
            try {
                String msg;
                StringBuilder blockString = new StringBuilder();

                List<String> blockStrings = new ArrayList<>();
                while ((msg = in.readLine()) != null)
                {
                    if (!(msg.equals("<BlockLedger>") || msg.equals("</BlockLedger") || msg.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")))
                        blockString.append(msg);
                    if (msg.equals("</blockRecord>")) {
                        blockStrings.add(blockString.toString());
                        blockString.delete(0, blockString.length());
                    }
                }
                //String recordTmp = blockString.toString();

                for (String item : blockStrings) {
                    queue.put(item);
                }
                //queue.put(recordTmp);
            } catch (IOException | InterruptedException x) {
                System.out.println("Server read error");
            }
            getSocket().close(); // close this connection, but not the server;
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}

// Unmarshall in consumer only if I need to.
class UnverifiedBlockConsumer implements Runnable {
    BlockingQueue<String> queue;
    int PID;
    UnverifiedBlockConsumer(BlockingQueue<String> queue){
        this.queue = queue; // Constructor binds our prioirty queue to the local variable.
    }

    public void run(){
        String record;
        PrintStream toServer;
        Socket sock;
        String verifiedBlock;

        System.out.println("Starting the Unverified Block Priority Queue Consumer thread.\n");
        try{
            while(true){ // Consume from the incoming queue. Do the work to verify. Mulitcast new blockchain
                record = queue.take(); // Will blocked-wait on empty queue
                System.out.println("Consumer got unverified: " + record);

                // Unmarshall the XML to a BlockRecord object
                BlockRecord block = Blockchain.unmarshallXML(record);

                // Ordindarily we would do real work here, based on the incoming data.
                int j; // Here we fake doing some work (That is, here we could cheat, so not ACTUAL work...)
                for(int i=0; i< 100; i++){ // put a limit on the fake work for this example
                    j = ThreadLocalRandom.current().nextInt(0,10);
                    try{Thread.sleep(500);}catch(Exception e){e.printStackTrace();}
                    if (j < 3) break; // <- how hard our fake work is; about 1.5 seconds.
                }

                // Check to make sure queue doesn't already contain block by it's id
                String tmpArray[] = record.split("    ");
                String blockID = tmpArray[1];

                //if (!Blockchain.blockchain.contains(block.getABlockID()))
                if (!Blockchain.idExistsInBlockchain(block.getABlockID()))
                { // Crude, but excludes most duplicates.
                    /*verifiedBlock = "[" + record + " verified by P" + Blockchain.processID + " at time "
                            + Integer.toString(ThreadLocalRandom.current().nextInt(100,1000)) + "]\n";
                    System.out.println(verifiedBlock);*/

                    block.setAVerificationProcessID(String.valueOf(Blockchain.processID));
                    if (Blockchain.blockchainList.size() > 0) {
                        block.setPreviousHash(Blockchain.blockchainList.get(Blockchain.blockchainList.size() - 1).getABlockID());
                    }

                    String tempblockchain = record + Blockchain.blockchain; // add the verified block to the chain
                    Blockchain.blockchainList.add(block);
                    String newBlockChainXML = Blockchain.getBlockchainXMLString();
                    /*for(int z=0; z < 3; z++)
                    { // send to each process in group, including us:
                        sock = new Socket("localhost", Blockchain.UPDATED_BLOCKCHAINS_PORT_BASE + z);
                        toServer = new PrintStream(sock.getOutputStream());
                        toServer.println(tempblockchain); toServer.flush(); // make the multicast
                        sock.close();
                    }*/

                    for(int z=0; z < 3; z++)
                    { // send to each process in group, including us:
                        sock = new Socket("localhost", Blockchain.UPDATED_BLOCKCHAINS_PORT_BASE + z);
                        toServer = new PrintStream(sock.getOutputStream());
                        toServer.println(newBlockChainXML); toServer.flush(); // make the multicast
                        sock.close();
                    }
                    Thread.sleep(1500); // For the example, wait for our blockchain to be updated before processing a new block
                }
            }
        }catch (Exception e) {System.out.println(e);}
    }
}

class SendKeyMsgWorker extends WorkerThread { // Class definition

    SendKeyMsgWorker(int processNumber, Socket blockchainSocket) {
        super(processNumber, blockchainSocket);
    } // Constructor, assign arg s to local sock

    public void run()  {
        // multicast our send key message to all of the processes
        try {
            this.multiCast();
        } catch (Exception e) {
            System.err.println("SendKeyMsgWorker exception occurred while calling multicast: " + e);
        }
    }
}

@XmlRootElement
class BlockRecord
{
    /* Examples of block fields: */
    String SHA256String;
    String SignedSHA256;
    String BlockID;
    String VerificationProcessID;
    String CreatingProcess;
    String PreviousHash;
    String Fname;
    String Lname;
    String SSNum;
    String DOB;
    String Diag;
    String Treat;
    String Rx;

  /* Examples of accessors for the BlockRecord fields. Note that the XML tools sort the fields alphabetically
     by name of accessors, so A=header, F=Indentification, G=Medical: */

    public String getASHA256String() {return SHA256String;}
    @XmlElement
    public void setASHA256String(String SH){this.SHA256String = SH;}

    public String getASignedSHA256() {return SignedSHA256;}
    @XmlElement
    public void setASignedSHA256(String SH){this.SignedSHA256 = SH;}

    public String getACreatingProcess() {return CreatingProcess;}
    @XmlElement
    public void setACreatingProcess(String CP){this.CreatingProcess = CP;}

    public String getAVerificationProcessID() {return VerificationProcessID;}
    @XmlElement
    public void setAVerificationProcessID(String VID){this.VerificationProcessID = VID;}

    public String getABlockID() {return BlockID;}
    @XmlElement
    public void setABlockID(String BID){this.BlockID = BID;}

    public String getPreviousHash() {return PreviousHash;}
    @XmlElement
    public void setPreviousHash(String prevHash){this.PreviousHash = prevHash;}

    public String getFSSNum() {return SSNum;}
    @XmlElement
    public void setFSSNum(String SS){this.SSNum = SS;}

    public String getFFname() {return Fname;}
    @XmlElement
    public void setFFname(String FN){this.Fname = FN;}

    public String getFLname() {return Lname;}
    @XmlElement
    public void setFLname(String LN){this.Lname = LN;}

    public String getFDOB() {return DOB;}
    @XmlElement
    public void setFDOB(String DOB){this.DOB = DOB;}

    public String getGDiag() {return Diag;}
    @XmlElement
    public void setGDiag(String D){this.Diag = D;}

    public String getGTreat() {return Treat;}
    @XmlElement
    public void setGTreat(String D){this.Treat = D;}

    public String getGRx() {return Rx;}
    @XmlElement
    public void setGRx(String D){this.Rx = D;}
}

public class Blockchain
{
    public static final int PUBLIC_KEY_PORT_BASE = 4710;
    public static final int UNVERIFIED_BLOCKS_PORT_BASE = 4820;
    public static final int UPDATED_BLOCKCHAINS_PORT_BASE = 4930;

    public static int processID;
    public UnverifiedBlockServer unverifiedBlockServer;
    public BlockchainServer blockchainServer;
    public PublicKeyServer publicKeyServer;
    public static PublicKey publicKey;
    public static PrivateKey privateKey;
    public static final ArrayList<PublicKey> publicKeyList = new ArrayList<PublicKey>(3);
    public static final ArrayList<Integer> publicKeyPortList = new ArrayList<>(3);
    public static BlockingQueue<String> queue = new PriorityBlockingQueue<>();
    public static Iterator<String> it = queue.iterator();
    public static String blockchain = "";
    public static List<BlockRecord> blockchainList = Collections.synchronizedList(new ArrayList<BlockRecord>());
    public static final HashMap<Integer, String> publicKeyHMap = new HashMap<>();
    public static JAXBContext jaxbContext;
    private static final String XMLHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    static { try { jaxbContext = JAXBContext.newInstance(BlockRecord.class); }
    catch (JAXBException e) { e.printStackTrace(); } }

    /**
     * Creates a blockchain process for the given {@code proccesID}
     * @param processID the process id
     * @throws Exception if cannot create key pair
     */
    public Blockchain(int processID) throws Exception
    {
        // save the process id for this blockchain process
        this.processID = processID;
        // first generate a public/private key pair
        this.generateKeyPair(LocalDate.now().toEpochDay());
        // initialize our servers here
        this.publicKeyServer = new PublicKeyServer(
                processID,
                PUBLIC_KEY_PORT_BASE + processID);
        this.unverifiedBlockServer = new UnverifiedBlockServer(processID,
                UNVERIFIED_BLOCKS_PORT_BASE + processID, queue);
        this.blockchainServer = new BlockchainServer(
                processID,
                UPDATED_BLOCKCHAINS_PORT_BASE + processID);

        loadPortListHelper();
        //loadDummyBlock();
    }

    public void run()
    {
        /*
        Initialization
        Using the start script, start your servers in the order P0, P1, P2
        When P2 starts, it also triggers the multicast of public keys, and
        starts the whole system running.
        All processes start with the same initial one-block (dummy entry)
        form of the blockchain.
        After all public keys have been established read the data file for
        this process.
        Create unverified blocks from the data and using XML as the external
        data format, multicast each unverified block in turn to all other processes.
         */
        // Start our servers
        // if its process 2 we'll send public key data to all other servers
        (new Thread(this.publicKeyServer)).start();
        (new Thread(this.unverifiedBlockServer)).start();
        (new Thread(this.blockchainServer)).start();
        try{Thread.sleep(100);}catch(Exception e) {}
        new Thread(new UnverifiedBlockConsumer(queue)).start();
        // if this is process 2 then multicast a send key message to start everything up
        // using a sendkeyworker propably just put in a wait
        if (processID == 2)
        {
            // Add a short delay
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            // create and run the send key worker
            Socket blockchainSocket = new Socket();
            (new SendKeyMsgWorker(processID, blockchainSocket)).start();
        }
    }

    public void generateKeyPair(long seed) throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        rng.setSeed(seed);
        keyGenerator.initialize(1024, rng);

        KeyPair keyPair = keyGenerator.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    public static String getConvertedPublicKey(PublicKey key)
    {
        byte[] publicKeyEncoded = Blockchain.publicKey.getEncoded();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyEncoded);

        return publicKeyBase64;
    }

    public static String getDecodedPublicKey(String key)
    {
        byte[] decodeData = Base64.getDecoder().decode(key);
        String decodedPublicKey = new String(decodeData);

        return decodedPublicKey;
    }

    public void loadPortListHelper()
    {
        publicKeyPortList.add(this.publicKeyServer.getPort());
    }

//    public void loadDummyBlock() throws Exception {
//        String[] fileArgs = new String[1];
//        fileArgs[0] = "3";
//        String db = blockInputE(fileArgs);
//        bcList.add(db);
//    }

    public static String getBlockUUID()
    {
        UUID blockID = UUID.randomUUID();
        String stringID = UUID.randomUUID().toString();
        return stringID;
    }

    public static String getTimestamp()
    {
        Date date = new Date();
        String T1 = String.format("%1$s %2$tF.%2$tT", "", date);
        String TimeStampString = T1 + "." + processID + "\n";
        return TimeStampString;
    }

    public static BlockRecord unmarshallXML(String stringXML) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        StringReader reader = new StringReader(stringXML);
        BlockRecord blockRecord = (BlockRecord) jaxbUnmarshaller.unmarshal(reader);
        return blockRecord;
    }

    public static String getBlockchainXMLString() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            StringWriter sw = new StringWriter();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            for (BlockRecord block : Blockchain.blockchainList) {
                jaxbMarshaller.marshal(block, sw);
            }


            String fullBlock = sw.toString();
            String cleanBlock = fullBlock.replace(XMLHeader, "");
            String XMLBlock = "\n<BlockLedger>" + cleanBlock + "</BlockLedger>";

            return XMLBlock;
        } catch (Exception e) {e.printStackTrace();}
        return "";
    }

    public static Boolean idExistsInBlockchain(String uid) {
        for (BlockRecord block : Blockchain.blockchainList) {
            if (block.getABlockID().contentEquals(uid)) {
                return true;
            }
        }

        return false;
    }

    public static String blockInputE(String[] args) throws Exception
    {
        String FILENAME;

        /* Token indexes for input: */
        int iFNAME = 0;
        int iLNAME = 1;
        int iDOB = 2;
        int iSSNUM = 3;
        int iDIAG = 4;
        int iTREAT = 5;
        int iRX = 6;

        /* CDE: Process numbers and port numbers to be used: */
        int pnum;
        int UnverifiedBlockPort;
        int BlockChainPort;
        String fullBlock = "";
        String XMLBlock = "";

        /* CDE If you want to trigger bragging rights functionality... */
        if (args.length > 1) System.out.println("Special functionality is present \n");

        if (args.length < 1) pnum = 0;
        else if (args[0].equals("0")) pnum = 0;
        else if (args[0].equals("1")) pnum = 1;
        else if (args[0].equals("2")) pnum = 2;
        else if (args[0].equals("3")) pnum = 3; // 3 for dummy block
        else pnum = 0; /* Default for badly formed argument */

        UnverifiedBlockPort = 4710 + pnum;
        BlockChainPort = 4820 + pnum;

//        System.out.println("Process number: " + pnum + " Ports: " + UnverifiedBlockPort + " " +
//                BlockChainPort + "\n");

        switch(pnum){
            case 1: FILENAME = "BlockInput1.txt"; break;
            case 2: FILENAME = "BlockInput2.txt"; break;
            case 3: FILENAME = "DummyBlock.txt"; break;
            default: FILENAME= "BlockInput0.txt"; break;
        }

//        System.out.println("Using input file: " + FILENAME);

        try
        {
            try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/src/" + FILENAME))) {
                String[] tokens = new String[10];
                String stringXML;
                String InputLineStr;
                String suuid;
                UUID idA;

                BlockRecord[] blockArray = new BlockRecord[20];

                //JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                StringWriter sw = new StringWriter();

                // CDE Make the output pretty printed:
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                int n = 0;

                while ((InputLineStr = br.readLine()) != null) {
                    blockArray[n] = new BlockRecord();

                    blockArray[n].setASHA256String("SHA string goes here...");
                    blockArray[n].setASignedSHA256("Signed SHA string goes here...");

                    /* CDE: Generate a unique blockID. This would also be signed by creating process: */
                    idA = UUID.randomUUID();
                    suuid = new String(UUID.randomUUID().toString());
                    blockArray[n].setABlockID(suuid);
                    blockArray[n].setACreatingProcess("Process" + Integer.toString(pnum));
                    blockArray[n].setAVerificationProcessID("To be set later...");
                    /* CDE put the file data into the block record: */
                    tokens = InputLineStr.split(" +"); // Tokenize the input
                    blockArray[n].setFSSNum(tokens[iSSNUM]);
                    blockArray[n].setFFname(tokens[iFNAME]);
                    blockArray[n].setFLname(tokens[iLNAME]);
                    blockArray[n].setFDOB(tokens[iDOB]);
                    blockArray[n].setGDiag(tokens[iDIAG]);
                    blockArray[n].setGTreat(tokens[iTREAT]);
                    blockArray[n].setGRx(tokens[iRX]);
                    n++;
                }
                //System.out.println(n + " records read.");
                //System.out.println("Names from input:");
//                for(int i=0; i < n; i++){
//                    System.out.println("  " + blockArray[i].getFFname() + " " +
//                            blockArray[i].getFLname());
//                }
//                System.out.println("\n");

                stringXML = sw.toString();
                for(int i=0; i < n; i++){
                    jaxbMarshaller.marshal(blockArray[i], sw);
                }
                fullBlock = sw.toString();
                //System.out.println(fullBlock);
                String XMLHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
                String cleanBlock = fullBlock.replace(XMLHeader, "");
                // Show the string of concatenated, individual XML blocks:
                XMLBlock = XMLHeader + "\n<BlockLedger>" + cleanBlock + "</BlockLedger>";

            } catch (IOException e) {e.printStackTrace();}
        } catch (Exception e) {e.printStackTrace();}
        return XMLBlock;
    }

    // He gives us main to use in the BlockH.java
    // The input utility program is the BlockInputE file
    public static void main(String args[])
    {
        int processID;

        if (args.length < 1)
        {
            processID = 0;
        }
        else
        {
            processID = Integer.parseInt(args[0]);
        }

        try
        {
            System.out.println("Blockchain framework launched.");
            System.out.println("Using processID " + processID + "\n");
            Blockchain b = new Blockchain(processID);
            b.run();
        }
        catch(Exception e)
        {
            System.err.println("Blockchain initialization exception occurred: " + e);
        }
    }
}