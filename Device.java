import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import javax.swing.Timer;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.*;

import java.awt.event.ActionListener;

import Packets.Packet;

class Device implements ActionListener{

    String myMACAddress;
    InetAddress myIPAddress;
    volatile static HashMap<InetAddress, String> ARPTable;
    Socket s;
    int port;
    int switchPort;
    ReentrantLock lock = new ReentrantLock();
    Object ob = new Object();
    DatagramSocket ds;
    int RouterPort;
    ArrayList<String> messages;

    JFrame f;
    JPanel p1, p2, p3, p4;
    JPanel MainPanel1, MainPanel2, MainPanel3;
    JButton connect, send;
    JTextField tf1, tf2, tf3, tf4, tf5;
    JDialog d;
    static DefaultTableModel dtm = new DefaultTableModel();
    JTable t1;
    JLabel l4;
    JPanel jp1;
    JPanel inner;
    JTextArea textArea;
    JScrollPane js;

    public static Object[][] getTable() {

        Object o[][] = new Object[10][2];

        int i = 0;
        for (Map.Entry<InetAddress, String> entry : ARPTable.entrySet()) {

            o[i][0] = entry.getKey();
            o[i][1] = entry.getValue();
            i++;
        }

        return o;
    }

    public static void updateTable() {

        Object[][] ob = Device.getTable();

        for (int j = dtm.getRowCount() - 1; j >= 0; j--)
        {
            dtm.removeRow(j);
        }
        
        for(int temp=0;temp<ob.length;temp++)
        {
            dtm.addRow(ob[temp]);
        }
    }

    Device() {
        
        t1 = new JTable(dtm);
        String col[] = {"IP Address", "MAC address"};
        dtm.setColumnIdentifiers(col);
        
        JScrollPane jp2 = new JScrollPane(t1);
        jp2.setMaximumSize(new Dimension(1200,150));
        t1.setRowHeight(20);
        messages = new ArrayList<String>();
        textArea = new JTextArea();
        textArea.setEditable(false);
		f = new JFrame("Device");
		f.setSize(500, 400);
		f.setLayout(null);
		p1 = new JPanel();
		JLabel l1 = new JLabel("My Port");
        l1.setBounds(50, 50, 100, 30);
		tf1 = new JTextField(5);
        tf1.setBounds(250, 50, 140, 30);
		
		f.add(l1); f.add(tf1);
		
		p2 = new JPanel();
		JLabel l2 = new JLabel("My MAC Address");
        l2.setBounds(50, 100, 100, 30);
		tf2 = new JTextField(14);
        tf2.setBounds(250, 100, 140, 30);

		f.add(l2); f.add(tf2);
		
		p3 = new JPanel();
		JLabel l3 = new JLabel("DHCP Port");
        l3.setBounds(50, 150, 100, 30);
		tf3 = new JTextField(5);
        tf3.setBounds(250, 150, 140, 30);

        f.add(l3); f.add(tf3);
        send = new JButton("Send Message");
		
		connect = new JButton("Connect");
		connect.setBounds(170, 220, 100, 40);
		connect.addActionListener(this);
		
		f.add(connect);
		
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		
		

        ARPTable = new HashMap<InetAddress, String>();

        try {
            
            //System.out.print("My Port> ");
            //DataInputStream d = new DataInputStream(System.in);
            //port = Integer.parseInt(d.readLine());

            //ds = new DatagramSocket(port);
			ds = new DatagramSocket(null);
        } catch (Exception e) {
            //TODO: handle exception
            System.out.println(e);
        }
    }

    class Execute extends Thread {

        public void run() {

            try {
                String msg1 = "connect";
				msg1 += myMACAddress;
				DatagramPacket dp = new DatagramPacket(msg1.getBytes(), msg1.length(), InetAddress.getByName("255.255.255.255"), RouterPort);
				ds.send(dp);
				System.out.println("Connectde");
				byte[] b = new byte[20];
				DatagramPacket dp1 = new DatagramPacket(b, b.length);
				ds.receive(dp1);

				System.out.println(new String(dp1.getData()));
				
				myIPAddress = InetAddress.getByName(new String(dp1.getData()));
					
				
				
				JOptionPane.showMessageDialog(f, "MY IP is " +  new String(dp1.getData()), "Exception", JOptionPane.INFORMATION_MESSAGE);
                d.setTitle(myIPAddress.getHostAddress());

                ARPTimer timer = new ARPTimer();
                timer.start();

				System.out.println("My IP is    " + myIPAddress.getHostAddress());

				msg1 = "";

				byte[] b1 = new byte[20];
				DatagramPacket dp2 = new DatagramPacket(b1, b1.length);
				ds.receive(dp2);

				System.out.println("Connected to Switch port " + new String(dp2.getData()));

                
                SwingWorker swingWorker = new SwingWorker() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        // TODO Auto-generated method stub

                        while (true) {

                            Thread.sleep(45000);
                            Socket sock = new Socket("localhost", RouterPort);
                            DataOutputStream dout = new DataOutputStream(sock.getOutputStream());
                            dout.writeUTF(myMACAddress);
                            sock.close();
                            System.out.println("Renewed!!!!");  
                        }
                        
                    }

                    
                };

                swingWorker.execute();
                
                
				String tempstr = new String(dp2.getData());
				tempstr = tempstr.trim();
				System.out.println("**************************\n" + tempstr);
				switchPort = Integer.parseInt(tempstr);
				s = new Socket("127.0.0.1", switchPort);

				DataOutputStream dout = new DataOutputStream(s.getOutputStream());
				String str = String.valueOf(port);
				dout.writeUTF(str);

                
				RecieverThread r = new RecieverThread(port);
				r.start();

                r.join();
                

            } catch (Exception e) {
                //TODO: handle exception
            }
        }
    }

    class SendMessage extends Thread {

        public void run() {

            try {
                String destMAC = "";
                

					//System.out.print("Enter message >");
					//String msg = din.readLine();
                    String msg = tf4.getText().trim();

					if (msg.equals("print ARP")) {

						for (Map.Entry<InetAddress, String> entry : ARPTable.entrySet()) {

							System.out.println(entry.getKey().getHostAddress() + "\t" + entry.getValue());
						}
						//continue;
					}

					System.out.println(msg);

					System.out.print("Enter dest IP>");
					//String destIP = din.readLine();
                    String destIP = tf5.getText().trim();

					if (! ARPTable.containsKey(InetAddress.getByName(destIP))) {

						Packet ef = new Packet(myMACAddress, "FF:FF:FF:FF:FF", myIPAddress, InetAddress.getByName(destIP), port);
						ObjectOutputStream oos;
						OutputStream o;
						o = s.getOutputStream();
						oos = new ObjectOutputStream(o);

						oos.writeObject(ef);
						InputStream in = s.getInputStream();
			
						ObjectInputStream ois = new ObjectInputStream(in);

						Packet et = (Packet) ois.readObject();
						System.out.println("Dest device MAC is " + et.srcMAC);
						ARPTable.put(InetAddress.getByName(destIP), et.srcMAC);
                        Device.updateTable();
						destMAC = et.srcMAC;

					} else {

						destMAC = ARPTable.get(InetAddress.getByName(destIP));
					}

					Packet send = new Packet(myMACAddress, destMAC, myIPAddress, InetAddress.getByName(destIP), msg, port);
					ObjectOutputStream oos;
					OutputStream o;
					o = s.getOutputStream();
					oos = new ObjectOutputStream(o);

					oos.writeObject(send);

                
            } catch (Exception e) {
                //TODO: handle exception
            }
        }
    }
	
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == connect) {
			
			try {

				//DataInputStream din = new DataInputStream(System.in);
				port = Integer.parseInt(tf1.getText().trim());
				myMACAddress = tf2.getText().trim();
				RouterPort = Integer.parseInt(tf3.getText().trim());
				ds.bind(new InetSocketAddress(port));

                d = new JDialog(f, "Device");
                d.setLayout(null);

                JLabel l4 = new JLabel("Enter Message");
                l4.setBounds(50, 20, 100, 20);

                tf4 = new JTextField(20);
                tf4.setBounds(250, 20, 150, 20);

                d.add(l4); d.add(tf4);

                JLabel l5 = new JLabel("Enter Dest IP");
                l5.setBounds(50, 50, 100, 20);

                tf5 = new JTextField(20);
                tf5.setBounds(250, 50, 150, 20);

                d.add(l5); d.add(tf5);

                send.setBounds(170, 100, 140, 20);
                d.add(send);
                send.addActionListener(this);

                JLabel l6 = new JLabel("Local ARP Table");
                l6.setBounds(170, 130, 170, 20);

                d.add(l6);

                t1.setBounds(0, 160, 500, 80);
                d.add(t1);

                Device.updateTable();
                
                JLabel l7 = new JLabel("Received Messages");
                l7.setBounds(170, 300, 160, 20);
                d.add(l7);

                JPanel pp = new JPanel();
                pp.setLayout(new GridLayout(1, 1));
                pp.setBounds(0, 300, 500, 200);
                //textArea.setBounds(0, 300, 500, 200);
                //js.add(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                js = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                pp.add(js);
                d.add(pp);



                d.setSize(500, 500);
                d.setVisible(true);

                Execute execute = new Execute();
                execute.start();


			} catch (Exception E) {
				
				JOptionPane.showMessageDialog(f, E, "Exception", JOptionPane.ERROR_MESSAGE);
			}
		}

        if (e.getSource() == send) {

            SendMessage sender = new SendMessage();
            sender.start();
        }
	}

    class ARPTimer extends Thread {

        public void run() {

            try {

                while (true) {

                    Thread.sleep(50000);
                    if (! ARPTable.isEmpty()) {

                        synchronized(ob) {

                            ARPTable.clear();
                            Device.updateTable();
                        }
                    }
                    
                }

            } catch (Exception e) {
                //TODO: handle exception
            }
        }
    }

    public void startDevice() throws Exception {
        

    }

    public static void main(String[] args) throws Exception {

        Device d = new Device();
        //d.startDevice();
        
    }


    class RecieverThread extends Thread {

        int port;
        

        RecieverThread(int port) {

            this.port = port;
            
        }

        public synchronized void run() {

            try {

                while (true) {

                    ServerSocket ss = new ServerSocket(port);
                    Socket s = ss.accept();

                    System.out.println("Some message recieved....");
    
                    InputStream in = s.getInputStream();
    
                    ObjectInputStream ois = new ObjectInputStream(in);
    
                    Packet et = (Packet) ois.readObject();
                    
                    if (et.destMAC.equals("FF:FF:FF:FF:FF")) {
    
                        if (et.destIP.getHostAddress().equals(myIPAddress.getHostAddress())) {
    
                            Packet ef1 = new Packet(myMACAddress, et.srcMAC, myIPAddress, et.srcIP, port);
    
                            //Socket switchSocket = new Socket("127.0.0.1", switchPort);

                            ObjectOutputStream oos;
                            OutputStream o;
                            o = s.getOutputStream();
                            oos = new ObjectOutputStream(o);
                            System.out.println("Writting");
                            oos.writeObject(ef1);
                            System.out.println("ARP Broadcast responded");


                            
                        } else {

                            ss.close();
                        }
                    } else {
                        
                        //JOptionPane.showMessageDialog(f, "Msg from " + et.srcIP.getHostAddress()  + "\nMsg is " + et.msg, "Exception", JOptionPane.ERROR_MESSAGE);
                        
                        String recvMsg = "Msg from " + et.srcIP.getHostAddress()  + "\nMsg is " + et.msg;
                        
                        messages.add(recvMsg);

                        String str1 = "";

                        for (int i = messages.size() - 1; i >=0; i--) {

                            str1 += messages.get(i) + "\n\n";
                        }

                        textArea.setText("");

                        textArea.setText(str1);

                        System.out.println("Msg from " + et.srcIP.getHostAddress()  + "\nMsg is " + et.msg);
                    }

                    ss.close();
    
                }

            } catch (Exception E) {

                
            }
        }
    }
}