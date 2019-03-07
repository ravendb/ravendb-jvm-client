package net.ravendb.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// to execute:
// exec:java -Dexec.mainClass="net.ravendb.client.NetworkIssuesSimulator" -Dexec.classpathScope="test"
public class NetworkIssuesSimulator {

    private String localAddress;

    private enum State {
        OK, INTER_BREAK, TWO_NODES_DOWN
    }

    private String[] nodes = new String[] { "127.0.1.1", "127.0.1.2", "127.0.1.3", "127.0.1.4", "127.0.1.5" };
    private int httpPort = 8080;
    private int tcpPort = 38888;
    private Random random = new Random();

    private State currentState = State.OK;

    private Runnable healCluster = null;

    public static void main(String[] args) {
        String localAddress = args.length > 0 ? args[0] : "127.0.0.1";
        System.out.println("Using [" + localAddress + "] as local address");

        NetworkIssuesSimulator simulator = new NetworkIssuesSimulator();
        simulator.start(localAddress);
    }

    public void start(String localAddress) {
        this.localAddress = localAddress;
        System.out.println("Started...");
        System.out.println("Nodes = " + String.join(", ", nodes));
        System.out.println("Ports: HTTP = " + httpPort + ", TCP = " + tcpPort);


        while (true) {
            randomWait();

            State nextState = null;

            switch (currentState) {
                case OK:
                    nextState = random.nextDouble() > 0.5 ? State.INTER_BREAK : State.TWO_NODES_DOWN;
                    break;
                case INTER_BREAK:
                    nextState = State.OK;
                    break;
                case TWO_NODES_DOWN:
                    nextState = State.OK;
                    break;
            }

            System.out.println("Going from [" + currentState + "] -> [" + nextState + "]");

            switch (nextState) {
                case OK:
                    healCluster(currentState);
                    break;
                case INTER_BREAK:
                    startInterBreak(currentState);
                    break;
                case TWO_NODES_DOWN:
                    disconnectTreeNodes(currentState);
                    break;
            }

            System.out.println("Current STATE = " + nextState);
            currentState = nextState;
        }
    }

    private void healCluster(State previousState) {
        System.out.println("Heal cluster");
        healCluster.run();
        healCluster = null;
    }

    private void startInterBreak(State previousState) {
        System.out.println("Start inter break");

        List<String> groupA = new ArrayList<>();
        List<String> groupB = new ArrayList<>();
        divideNodes(groupA, groupB);

        final List<String> revert = new ArrayList<>();

        for (String a: groupA) {
            for (String b: groupB) {
                // block outgoing traffic from A (any port) -> B (http port)
                String cmd = null;
                cmd = "iptables -A OUTPUT -j DROP -s " + a + " -d " + b + " --dport " + httpPort;
                executeShell(cmd);
                revert.add(cmd);

                // block outgoing traffic from A (any port) -> B (tcp port)
                cmd = "iptables -A OUTPUT -j DROP -s " + a + " -d " + b + " --dport " + tcpPort;
                executeShell(cmd);
                revert.add(cmd);

                // block incoming traffic from B (any port) -> A (http port)
                cmd = "iptables -A INPUT -j DROP -s " + b + " -d " + a + " --dport " + httpPort;
                executeShell(cmd);
                revert.add(cmd);

                // block incoming traffic from B (any port) -> A (tcp port)
                cmd = "iptables -A INPUT -j DROP -s " + b + " -d " + a + " --dport " + tcpPort;
                executeShell(cmd);
                revert.add(cmd);
            }
        }

        healCluster = () -> {
            for (String cmd : revert) {
                executeShell(cmd.replaceAll("-A", "-D"));
            }
        };
    }

    private void disconnectTreeNodes(State previousState) {
        System.out.println("disconnect 3 nodes");

        List<String> groupA = new ArrayList<>();
        List<String> groupB = new ArrayList<>();
        divideNodes(groupA, groupB);


        final List<String> revert = new ArrayList<>();
        String cmd = null;
        // group B should have 3 nodes
        for (String nodeToReject : groupB) {
            cmd = "iptables -A OUTPUT -j DROP -s " + localAddress + " -d " + nodeToReject + " --dport " + httpPort;
            executeShell(cmd);
            revert.add(cmd);
        }

        healCluster = () -> {
            for (String c : revert) {
                executeShell(c.replaceAll("-A", "-D"));
            }
        };
    }

    private void divideNodes(List<String> groupA, List<String> groupB) {
        String[] clone = nodes.clone();
        List<String> list = new ArrayList<>(Arrays.asList(clone));

        int i = random.nextInt(list.size());
        groupB.add(list.remove(i));

        i = random.nextInt(list.size());
        groupB.add(list.remove(i));

        groupA.addAll(list);
    }

    private void executeShell(String command) {
        System.out.println("EXEC: " + command);
        try {
            Process exec = Runtime.getRuntime().exec(command);
            exec.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void randomWait() {
        int wait = random.nextInt(10000) + 10000;
        System.out.println("Sleeping for: " + wait);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
