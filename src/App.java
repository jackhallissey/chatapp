import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class App {
    public static MainWindow mainWin;

    public static Connection connection;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainWin = new MainWindow("ChatApp");
                mainWin.start();
            }
        });
    }
}

class Connection extends Thread {
    public String ip;
    public int port;
    public String displayName;

    public ConcurrentLinkedQueue<String> recvQueue;
    public ServerSocket serverSock;
    public Socket sock;

    private InputStreamReader in;
    private BufferedReader bf;

    private PrintWriter pr;

    public boolean ended;

    public Connection(String i, int p) {
        //Client mode
        ip = i;
        port = p;
    }

    public Connection(int p) {
        //Server mode
        ip = null;
        port = p;
    }

    @Override
    public void run() {
        //Establish connection
        try {
            establish();
        } catch (Exception e) {
            e.printStackTrace();
            if (ip == null) {
                //Server mode
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        App.mainWin.serverModePanel.reportError();
                    }
                });
            } else {
                //Client mode
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        App.mainWin.clientModePanel.reportError();
                    }
                });
            }
            return;
        }

        //Receive messages
        try {
            String invalidMessage = "Invalid message format";
            
            String message;
            char type;
            String body;

            while (!ended) {
                message = bf.readLine();

                if (message == null) {
                    if (ended) {
                        break;
                    } else {
                        throw new ParseException(invalidMessage, 0);
                    }
                }

                type = message.charAt(0);
                body = message.substring(2);

                switch (type) {
                    case 'M':
                        recvQueue.add(body);

                        //Get the AWT event dispatching thread to display the messages in the receive queue
                        SwingUtilities.invokeLater(App.mainWin.chatPanel);

                        break;
                    case 'C':
                        switch (body) {
                            case "END":
                                if (!ended) {
                                    ended = true;
                                    App.mainWin.changePanel("connectionClosedPanel");
                                }
                                break;
                            default:
                                throw new ParseException(invalidMessage, 2);
                        }
                        break;
                    default:
                        throw new ParseException(invalidMessage, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!ended) {
                App.mainWin.changePanel("errorPanel");
            }
        }

        //End connection
        end();
    }

    public void establish() throws IOException {
        if (ip == null) {
            //Server mode
            serverSock = new ServerSocket(port);

            //Report that server has started and is waiting for a connection
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    App.mainWin.serverModePanel.reportStarted();
                }
            });

            sock = serverSock.accept();
        } else {
            //Client mode
            sock = new Socket(ip, port);
        }

        displayName = sock.getRemoteSocketAddress().toString().replaceFirst("/", "");

        recvQueue = new ConcurrentLinkedQueue<>();

        in = new InputStreamReader(sock.getInputStream());
        bf = new BufferedReader(in);

        pr = new PrintWriter(App.connection.sock.getOutputStream());

        App.mainWin.changePanel("chatPanel");
    }

    public void end() {
        ended = true;

        try {
            if (!sock.isClosed()) {
                sock.close();
            }
        } catch (Exception e) {}

        try {
            if (serverSock != null && !serverSock.isClosed()) {
                serverSock.close();
            }
        } catch (Exception e) {}
    }

    public void send(String message) {
        // Called by the AWT thread
        if (!ended || message.equals("C:END")) {
            try {
                if (message != null) {
                    pr.println(message);
                    pr.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
                App.mainWin.changePanel("errorPanel");                
                end();
            }
        }
    }
}