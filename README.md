# Simulating DHCP and ARP protocols using Java

DHCP and ARP simulation.

DHCP server will listen on port no 2002.

Switch class is implemented with 4 ports listening on 3001, 3002, 3003, 3004

Clients connecting to DHCP server to obtain the IP address will have a port no and a MAC address (Dummy MAC address in this case, since it is a simulation).

DHCP server will have 4 addresses namely
1) 192.168.0.1
2) 192.168.0.2
3) 192.168.0.3
4) 192.168.0.4

Client will be preconfigured with a dummy MAC address and will connect to the DHCP server by sending UDP Packet with the message “Connect” + its MAC address
DHCP server will have lease time for every IP address it assigns.

After DHCP server assigns IP address to the client, each device will be assigned to another port. (This project simulates Switch like element using JAVA)
There will be MAC table in Switch class, which will keep record of which port number is associated with MAC address of the client.
Each Client will have a Local ARP cache table, which will consist of IP addresses and their corresponding MAC address.
This Local ARP cache table will have timeout of 40 seconds.

If the destination IP address is not found in the local ARP cache, destination MAC address will be set to FF:FF:FF:FF:FF:FF and while the switch receiving this, it will broadcast the message to all the client connected to that switch.
Corresponding ARP reply will be sent to the sender, if that client resides in that network.
