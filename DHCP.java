import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.*;


import Packets.Packet;

class DHCP implements ActionListener{

	
    DatagramSocket ds;
    Object ob = new Object();
    volatile Stack<InetAddress> IpPool;
    volatile Stack<Integer> PortPool;
    volatile HashMap<String, InetAddress> assignedDevices;
    volatile Stack<InetAddress> assignedIP;
    volatile HashMap<Integer, Integer> assignedPort;

    volatile HashMap<InetAddress, String> IPTable;
    volatile HashMap<String, Boolean> RenewedList;
    volatile Switch s;
    JFrame f1;
    JButton b1, b2, exit;
    JLabel l1, l2;
    JPanel p1, p2;
    DefaultTableModel dtm1=new DefaultTableModel();
    DefaultTableModel dtm2 = new DefaultTableModel();
	JTable t1, t2;
    

    DHCP() {
		
        f1 = new JFrame("Server");
        p1 = new JPanel();

        f1.setLayout(new GridLayout(2, 1));
        f1.setSize(500,500);
        f1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        l1 = new JLabel("List of IP addresses assigned to Device's MAC address");
        JPanel jp1 = new JPanel(); jp1.setLayout(new FlowLayout());
        jp1.add(l1);
        p1.setLayout(new GridLayout(2, 1));
        p1.add(jp1);
        t1 = new JTable(dtm1);
        String[] col1 = {"IP", "MAC"};
        dtm1.setColumnIdentifiers(col1);
        JScrollPane jspane=new JScrollPane(t1);
		//jspane.setBorder(new EmptyBorder());
		jspane.setMaximumSize(new Dimension(500,150));
        t1.setRowHeight(20);
        p1.add(jspane);
        f1.add(p1);

        p2 = new JPanel();
        p2.setLayout(new GridLayout(2, 1));
        JPanel jp3 = new JPanel(); jp3.setLayout(new FlowLayout());
        l2 = new JLabel("Switch's port - MAC addresses");
        jp3.add(l2);
        t2 = new JTable(dtm2);
        String[] col2 = {"MAC", "Switch's Port #"};
        t2 = new JTable(dtm2);
        dtm2.setColumnIdentifiers(col2);
        JScrollPane jp2 = new JScrollPane(t2);
        jp2.setMaximumSize(new Dimension(1200,150));
        t2.setRowHeight(20);
        p2.add(jp3);
        p2.add(jp2);
        f1.add(p2);

        f1.pack();
        f1.setVisible(true);
        
        try {

            ds = new DatagramSocket(2002);
            IpPool = new Stack<InetAddress>();
            assignedDevices = new HashMap<String, InetAddress>();
            assignedIP = new Stack<InetAddress>();
            assignedPort = new HashMap<Integer, Integer>();
            IPTable = new HashMap<InetAddress, String>();
            PortPool = new Stack<Integer>();
            RenewedList = new HashMap<String, Boolean>();

            IpPool.push(InetAddress.getByName("192.168.0.1"));
            IpPool.push(InetAddress.getByName("192.168.0.2"));
            IpPool.push(InetAddress.getByName("192.168.0.3"));
            IpPool.push(InetAddress.getByName("192.168.0.4"));

            PortPool.push(3001);
            PortPool.push(3002);
            PortPool.push(3003);
            PortPool.push(3004);

        } catch (Exception E) {

            System.out.println(E);
        }
    }

    public Object[][] getTable1() {

        Object o[][] = new Object[4][2];

        int i = 0;
        for (Map.Entry<String,InetAddress> entry : assignedDevices.entrySet()) {
    
            o[i][0] = entry.getValue();
            o[i][1] = entry.getKey();
            i++;
        }

        return o;
    }

    public void updateTable() {

        Object [][] ob=this.getTable1();
										
        for(int j=dtm1.getRowCount()-1;j>=0;j--)
        {
            dtm1.removeRow(j);
        }
        
        for(int temp=0;temp<ob.length;temp++)
        {
            dtm1.addRow(ob[temp]);
        }
    }

    public void startRouter() throws Exception {

        this.updateTable();
        Reciever r[] = new Reciever[4];
        for (int i = 0; i < 4; i++) {

            r[i] = new Reciever(this);
            r[i].start();
        }


        Lease l = new Lease();
        l.start();

        s = new Switch();
        s.startSwitch();

    }

    public static void main(String[] args) throws Exception {

        DHCP r = new DHCP();
        r.startRouter();
    }

    class Lease extends Thread {

        public void run() {

            try {

                while (true) {
                    ServerSocket ss = new ServerSocket(2002);
                    System.out.println("Lease Part working");
                    Socket s = ss.accept();

                    DataInputStream din = new DataInputStream(s.getInputStream());
                    String str = din.readUTF();

                    System.out.println("Lease Client connected " + str);

                    synchronized(ob) {

                        RenewedList.put(str, true);
                        System.out.println(RenewedList.get(str));
                    }

                    s.close();
                    ss.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class Reciever extends Thread {

        //DHCP Server
        DHCP r;

        Reciever(DHCP r) {

            this.r = r;
        }

        public void run() {

            try {
                System.out.println("Running");
                byte[] b = new byte[1024];

                    DatagramPacket dp = new DatagramPacket(b, b.length);
                    ds.receive(dp);

                    String msg = new String(dp.getData());

                    System.out.println(msg + dp.getPort());

                    if (msg.startsWith("connect")) {

                        String mac = "MAC";
                        DatagramPacket dp1 = new DatagramPacket(mac.getBytes(), mac.length(), dp.getAddress(), dp.getPort());
                        //mac = msg.substring(8, 26);

                        System.out.println(mac);
                        InetAddress temp; int tempPort;
                        String devMac = msg.substring(7, 24).trim();
                        if (! assignedDevices.containsKey(devMac)) {


                            
                            synchronized (ob) {

                                temp = IpPool.pop();
                                assignedDevices.put(devMac, temp);
                                tempPort = PortPool.pop();
                                assignedPort.put(tempPort, dp.getPort());
                                IPTable.put(temp, new String(dp.getData()));
                                byte[] addr = temp.getHostAddress().getBytes();
                                DatagramPacket IPPacket = new DatagramPacket(addr, addr.length, dp.getAddress(), dp.getPort());
                                ds.send(IPPacket);
                                System.out.println(devMac + " assigned with " + temp.getHostAddress());
                                assignedIP.push(temp);

                                //ds.receive(dp);
                                System.out.println("Recieved");
                                String s = String.valueOf(tempPort);
                                byte[] portbyte = s.getBytes();
                                DatagramPacket PortPacket = new DatagramPacket(portbyte, portbyte.length, dp.getAddress(), dp.getPort());
                                ds.send(PortPacket);
                                System.out.println(devMac+ " assigned with " + temp.getHostAddress() + " with Switch port : " + tempPort);


                                RenewedList.put(devMac, false);
                            }
                            r.updateTable();

                            while (true) {

                                System.out.println("Thread sleeping");
                                Thread.sleep(60000);


                                System.out.println(devMac +  " Renewal status " + RenewedList.get(devMac));
                                if (RenewedList.get(devMac) != true) {

                                    synchronized (ob) {
        
                                        System.out.println("Client left");
                                        IpPool.push(temp);
                                        PortPool.push(tempPort);
                                        s.DestPort.remove(tempPort);
                                        s.rDestPort.remove(dp.getPort());
                                        System.out.println(temp.getHostName() + " lease expired");
                                        assignedDevices.remove(devMac);

                                        Iterator it = IpPool.iterator();

                                        while (it.hasNext()) {
                                            System.out.println(it.next());
                                        }
                                        for (Map.Entry<String,InetAddress> entry : assignedDevices.entrySet()) {
    
                                            System.out.println(entry.getValue() + " " + entry.getKey());
                                        }
                                        r.updateTable();
                                        break; 
                                    } 
                                } else {

                                    System.out.println("Client renewed");
                                    r.updateTable();
                                    synchronized (ob) {
                                        RenewedList.put(devMac, false);
                                    }
                                }

                        } 
                        
                    } 
                    else {

                            InetAddress temp1 = assignedDevices.get(devMac);
                            byte[] addr = temp1.getHostAddress().getBytes();
                            DatagramPacket IPPacket = new DatagramPacket(addr, addr.length, dp.getAddress(), dp.getPort());
                            ds.send(IPPacket);
                            System.out.println(devMac + " assigned with " + temp1.getHostAddress());

                            int tempPort1;
                            synchronized(ob) {

                                tempPort1 = PortPool.pop();
                            }

                            String s1 = String.valueOf(tempPort1);
                            byte[] portbyte = s1.getBytes();
                            DatagramPacket PortPacket = new DatagramPacket(portbyte, portbyte.length, dp.getAddress(), dp.getPort());
                            ds.send(PortPacket);
                            System.out.println(devMac+ " assigned with " + temp1.getHostAddress() + " with Switch port : " + tempPort1);


                            RenewedList.put(devMac, false);

                            while (true) {

                                System.out.println("Thread sleeping");
                                Thread.sleep(60000);


                                System.out.println(devMac +  " Renewal status " + RenewedList.get(devMac));
                                if (RenewedList.get(devMac) != true) {

                                    synchronized (ob) {
        
                                        System.out.println("Client left");
                                        IpPool.push(temp1);
                                        PortPool.push(tempPort1);
                                        s.DestPort.remove(tempPort1);
                                        s.rDestPort.remove(dp.getPort());
                                        System.out.println(temp1.getHostName() + " lease expired");
                                        assignedDevices.remove(devMac);

                                        Iterator it = IpPool.iterator();

                                        while (it.hasNext()) {
                                            System.out.println(it.next());
                                        }
                                        for (Map.Entry<String,InetAddress> entry : assignedDevices.entrySet()) {
    
                                            System.out.println(entry.getValue() + " " + entry.getKey());
                                        }
                                        r.updateTable();
                                        break; 
                                    } 
                                } else {

                                    System.out.println("Client renewed");
                                    r.updateTable();
                                    synchronized (ob) {
                                        RenewedList.put(devMac, false);
                                    }
                                }
                    }}}
                this.run();

            } catch (Exception E) {

                System.out.println(E + "mE");
            }
        }
    }

    class Switch {

        //Send messages using ARP

        public int ports[];
        Object ob = new Object();
        volatile public HashMap<String, Integer> MACTable; // MAC Addr and Port no
        volatile public HashMap<Integer, String> PortTable; // Portn no and MAC addr
        public ServerSocket ss[];
        ReentrantLock lock = new ReentrantLock();
        volatile public Packet globalFrame;
                
    
        volatile public HashMap<Integer, Integer> DestPort; //Switch Port and Device port
        volatile public HashMap<Integer, Integer> rDestPort; //Device port and Switch Port

        public Object[][] getTable2() {

            Object o[][] = new Object[4][2];
    
            int i = 0;
            for (Map.Entry<Integer, String> entry : PortTable.entrySet()) {
        
                o[i][0] = entry.getValue();
                o[i][1] = entry.getKey();
                i++;
            }
    
            return o;
        }

        public void updateTable2() {

            Object [][] ob=this.getTable2();
										
            for(int j=dtm2.getRowCount()-1;j>=0;j--)
            {
                dtm2.removeRow(j);
            }
            
            for(int temp=0;temp<ob.length;temp++)
            {
                dtm2.addRow(ob[temp]);
            }
        }

        Switch() {
    
    
            try {
                
                ports = new int[4];
                int startPort = 3001;
                ss = new ServerSocket[4];
        
                for (int i = 0; i < 4; i++) {
        
                    ports[i] = startPort;
                    ss[i] = new ServerSocket(startPort);
                    startPort ++;
                    
                }
        
                MACTable = new HashMap<String, Integer>();
                PortTable = new HashMap<Integer, String>();
                DestPort = new HashMap<Integer, Integer>();
                rDestPort = new HashMap<Integer, Integer>();
        
            } catch (Exception e) {
                //TODO: handle exception
            }
    
        }
    
        public void startSwitch() throws Exception{
            
            this.updateTable2();
            RecieverThread r[] = new RecieverThread[4];
            for (int i = 0; i < 4; i++) {
    
                r[i] = new RecieverThread(ports[i], ss[i], this);
                r[i].start();
                System.out.println("Starting port no " + ports[i]);
            }
    
            for (int i = 0; i < 4; i++) {
    
                r[i].join();
                System.out.println("Joined");
            }
        }
    
        class RecieverThread extends Thread {
    
            int portno;
            ServerSocket ss;
            Socket s;
            Switch sh;
    
            RecieverThread(int portno, ServerSocket ss, Switch s) {
    
                this.portno = portno;
                this.ss = ss;
                this.sh = s;
    
                try {
    
    
                }catch(Exception E) {
    
                    System.out.println(E);
                }
            }
    
            public synchronized void run() {
    
                try {
                    
                    System.out.println("Listening on " + portno);
                    s = ss.accept();
                    System.out.println("Client connected in portno " + portno);
                    DataInputStream din = new DataInputStream(s.getInputStream());
                    int p = Integer.parseInt(din.readUTF());
    
                    synchronized(ob) {
    
                        DestPort.put(portno, p);
                        rDestPort.put(p, portno);
                    }

                    sh.updateTable2();

                    while (true) {
    
                        InputStream in = s.getInputStream();
                        ObjectInputStream ois = new ObjectInputStream(in);
                        System.out.println("\"x\"");
                        globalFrame = (Packet) ois.readObject();
                        Packet ef = globalFrame;
                        
    
                        System.out.println("Connected with device " + ef.srcIP.getHostAddress());
                        System.out.println("MAC entries");
    
                        for (Map.Entry<String,Integer> entry : MACTable.entrySet()) {
    
                            System.out.println("MAC : " + entry.getKey() + "\tPort : " + entry.getValue());
                        }
                        sh.updateTable2();
                        
    
                        if (ef.destMAC.equals("FF:FF:FF:FF:FF")) {
    
                            if (! MACTable.containsKey(ef.srcMAC)) {
    
                                synchronized(ob) {

                                    MACTable.put(ef.srcMAC, portno);
                                    PortTable.put(portno, ef.srcMAC);
                                    sh.updateTable2();

                                }
    
                            }
    
                            for (int i = 0; i < 4; i++) {
    
                                if (! rDestPort.containsKey(ef.srcPort))
                                    continue;
                                if (ports[i] != rDestPort.get(ef.srcPort)) {
                                    
                                    System.out.println("Check" + ports[i]);
                                    if (! PortTable.containsKey(ports[i])) {
    
                                        System.out.println("if");
                                        try {
    
                                            SenderThread st = new SenderThread(DestPort.get(ports[i]), ef, ss);
                                            st.start();
    
                                            Thread.sleep(400);
    
                                            if (st.isAlive()) {
    
                                                st.s.close();
                                                st.interrupt();
                                                continue;
                                            }
    
                                            st.join(500);
                                            if (globalFrame.srcPort == ef.srcPort)
                                                continue;

                                            if (! MACTable.containsKey(globalFrame.srcMAC)) {
    
                                                System.out.println(globalFrame.srcPort);
                                                synchronized(ob) {

                                                    int switchPort = 0;
    
                                                    for (Map.Entry<Integer,Integer> entry : DestPort.entrySet()) {
    
                                                        if (entry.getValue() == globalFrame.srcPort) {
    
                                                            switchPort = entry.getKey();
                                                            break;
                                                        }
                                                    }
    
                                                   
                                                    MACTable.put(globalFrame.srcMAC, switchPort);
                                                    PortTable.put(switchPort, globalFrame.srcMAC);
                                                    sh.updateTable2();

                                                    for (Map.Entry<String,Integer> entry : MACTable.entrySet()) {
    
                                                        System.out.println("MAC : " + entry.getKey() + "\tPort : " + entry.getValue());
                                                    }
                                                    sh.updateTable2();
                                                }
                    
                                            }
    
                                            Packet send = new Packet(globalFrame.srcMAC, ef.srcMAC, ef.destIP, ef.srcIP, portno);
                                            OutputStream o = s.getOutputStream();
                                            ObjectOutputStream oos = new ObjectOutputStream(o);
                                            System.out.println("Broadcast query completed!");
                                            oos.writeObject(send);
    
                                            break;
                                        } catch (NullPointerException NE) {
                                            
                                            System.out.println("Continue : " + ports[i]);

                                        }
                                        
                                    } else {
                                        System.out.println("Yes " + ports[i]);
    
                                        SenderThread st = new SenderThread(DestPort.get(ports[i]), globalFrame, ss);
                                        st.start();
                                        Thread.sleep(500);
                                        st.s.close();
                                        st.interrupt();
    
                                        if (globalFrame.srcPort == ef.srcPort)
                                            continue;
    
                                        OutputStream o = s.getOutputStream();
                                        ObjectOutputStream oos = new ObjectOutputStream(o);
                                        System.out.println("Broadcast query completed! srcMAC & destMAC is " + globalFrame.srcMAC + globalFrame.destMAC);
                                        Packet temp = new Packet(globalFrame.srcMAC, globalFrame.destMAC, globalFrame.srcIP, globalFrame.destIP, globalFrame.srcPort);
                                        oos.writeObject(temp);
                                        break;
                                    }
    
                                }
                            }
                            
                        } else {
                            //transmit the packet to dest device
    
                            System.out.println("transmit the packet to dest device");
                            Packet sendFrame = globalFrame;
                            System.out.println("Trying to send \"" + sendFrame.msg + "\" to " + sendFrame.destMAC);
                            for (Map.Entry<String,Integer> entry : MACTable.entrySet()) {
    
                               if (entry.getKey().equals(sendFrame.destMAC)) {
    
                                    int tempDestPort = DestPort.get(entry.getValue());
                                    System.out.println("Sending " + sendFrame.msg + " to " + tempDestPort);
                                    Socket s1 = new Socket("127.0.0.1", tempDestPort);
                                    
                                    OutputStream o = s1.getOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(o);
                                    oos.writeObject(sendFrame);
                                    System.out.println("Sent Successfully!!!!!");
                                    s1.close();
                                    break;
                               }
                            }
    
                        }
                    }
                    
                } catch (Exception E) {
    
                    System.out.print(E + "  ME!!!!");
                    
                    synchronized (ob) {

                        PortPool.push(portno);
                        MACTable.clear();
                        PortTable.clear();
                    }
                    sh.updateTable2();
                    RecieverThread re = new RecieverThread(portno, ss, sh);
                    re.start();
                    try {
                        re.join();
                    } catch (Exception e) {

                        System.out.println("Nested Try Catch");
                    }
                }
            }
        }
    
        class SenderThread extends Thread {
    
            int portno;
            Packet ef;
            public ServerSocket ss;
            public Socket s;
    
            SenderThread(int portno, Packet ef, ServerSocket ss) {
    
                this.portno = portno;
                this.ef = ef;
                this.ss = ss;
            }
    
            public synchronized void run() {
    
                try {
    
                    s = new Socket("127.0.0.1", portno);
    
                    Packet et = new Packet(ef.srcMAC, ef.destMAC, ef.srcIP, ef.destIP, portno);
    
                    OutputStream o = s.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(o);
    
                    oos.writeObject(et);
                    System.out.println("Broadcast sent!");
                    InputStream in = s.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(in);
    
                    System.out.println("Waiting");
    
                    
                    synchronized (ob) {
                        globalFrame = (Packet) ois.readObject();
                        System.out.println("Broadcast response" + globalFrame.srcPort);
                        System.out.println(globalFrame.srcMAC + "\t" + globalFrame.destMAC + "\t" + globalFrame.srcIP.getHostAddress() + "\t" + globalFrame.destIP.getHostAddress());
                        System.out.println("Sender Thread exits");
                    }
                    
                    System.out.println("Sender Thread exits");
                    s.close();
                    
                }
                catch (Exception E) {
    
                    System.out.print(E);
                }
            }
        }
    }
}

