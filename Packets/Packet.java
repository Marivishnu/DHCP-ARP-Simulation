package Packets;
import java.io.Serializable;
import java.net.InetAddress;

public class Packet implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public String srcMAC;
    public String destMAC;
    public String msg;
    public InetAddress srcIP;
    public InetAddress destIP;
    public int srcPort;

    public Packet() {

        srcMAC = destMAC = "";
    }

    public Packet(String srcMAC, String destMAC, InetAddress srcAddress, InetAddress destAddress, int srcPort) {

        this.srcMAC = srcMAC;
        this.destMAC = destMAC;
        this.srcIP = srcAddress;
        this.destIP = destAddress;
        this.srcPort = srcPort;
    }

    public Packet(String srcMAC, String destMAC, InetAddress srcAddress, InetAddress destAddress, String msg, int srcPort) {

        this.srcMAC = srcMAC;
        this.destMAC = destMAC;
        this.msg = msg;
        this.srcIP = srcAddress;
        this.destIP = destAddress;
        this.srcPort = srcPort;
    }
}
