import sun.management.ConnectorAddressLink;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;

public class Main {
//	static final String URL = "service:jmx:rmi:///jndi/rmi://:1099/jmxrmi";
	static final String PID = "21843";

	static class ClientListener implements NotificationListener {
		public void handleNotification(Notification notification, Object handback) {
			echo("\nReceived notification:");
			echo("\tClassName: " + notification.getClass().getName());
			echo("\tSource: " + notification.getSource());
			echo("\tType: " + notification.getType());
			echo("\tMessage: " + notification.getMessage());

			if (notification instanceof AttributeChangeNotification) {
				AttributeChangeNotification acn = (AttributeChangeNotification) notification;
				echo("\tAttributeName: " + acn.getAttributeName());
				echo("\tAttributeType: " + acn.getAttributeType());
				echo("\tNewValue: " + acn.getNewValue());
				echo("\tOldValue: " + acn.getOldValue());
			}
		}
	}

	public static void main(String[] args) {
		try {
//			JMXServiceURL url = new JMXServiceURL(URL);
			String address = ConnectorAddressLink.importFrom(Integer.parseInt(PID));
			JMXServiceURL url = new JMXServiceURL(address);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			ClientListener listener = new ClientListener();

			echo("\nGet an MBeanServerConnection");
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
			waitForEnterPressed();

			echo("\nDomains: ");
			String domains[] = mbsc.getDomains();
			Arrays.sort(domains);
			for (String domain: domains) {
				echo("\tDomain = " + domain);
			}
			waitForEnterPressed();

			List<ObjectName> clients = new ArrayList<ObjectName>();
			String clientConnectorPattern = "org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=mqtt,connectionViewType=clientId,connectionName=";

			// Query MBean names
			echo("\nQuery MBeanServer Mbeans: ");
			Set<ObjectName> names = new TreeSet<ObjectName>(mbsc.queryNames(null, null));
			for (ObjectName name: names) {
				echo("\tObjectName = " + name);
				if (name.toString().startsWith(clientConnectorPattern)) {
					clients.add(name);
				}
			}
			waitForEnterPressed();

			//  Perform operations
			echo("\n Total mqtt clients: " + clients.size());
			waitForEnterPressed();
			for (ObjectName name: clients) {
				ClientConnector clientConnectorProxy = JMX.newMBeanProxy(mbsc, name, ClientConnector.class, true);
				echo("\n Stop " + name.toString());
				clientConnectorProxy.stop();
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void echo(String msg) {
		System.out.println(msg);
	}

	private static void sleep(int milis) {
		try {
			Thread.sleep(milis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void waitForEnterPressed() {
		try {
			echo("\nPress <Enter> to continue..");
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
