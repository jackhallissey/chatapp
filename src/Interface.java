import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.HashMap;

class MainWindow extends JFrame {
    CardLayout cardLayout;
    JPanel mainPanel;
    HashMap<String, AppPanel> appPanels;

    LaunchPanel launchPanel;
    ServerModePanel serverModePanel;
    ClientModePanel clientModePanel;
    ChatPanel chatPanel;
    ConnectionClosedPanel connectionClosedPanel;
    ErrorPanel errorPanel;

    GridBagConstraints sgbc;

    public MainWindow(String title) {
        super(title);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (App.connection != null && !App.connection.ended) {
                    App.connection.ended = true;
                    App.connection.send("C:END");
                }
            }
        });

        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);
        appPanels = new HashMap<>();

        //Prepare sgbc for later use when adding spacers in panels
        sgbc = new GridBagConstraints();
        sgbc.anchor = GridBagConstraints.NORTH;
        sgbc.gridx = 0;
        sgbc.gridy = 2;
        sgbc.weightx = 1;
        sgbc.weighty = 1;
        sgbc.fill = GridBagConstraints.VERTICAL;
        //sbc.gridy will be set each time
    }

    public void start() {
        //Create panels
        launchPanel = new LaunchPanel();
        mainPanel.add(launchPanel, "launchPanel");
        appPanels.put("launchPanel", launchPanel);

        serverModePanel = new ServerModePanel();
        mainPanel.add(serverModePanel, "serverModePanel");
        appPanels.put("serverModePanel", serverModePanel);

        clientModePanel = new ClientModePanel();
        mainPanel.add(clientModePanel, "clientModePanel");
        appPanels.put("clientModePanel", clientModePanel);

        chatPanel = new ChatPanel();
        mainPanel.add(chatPanel, "chatPanel");
        appPanels.put("chatPanel", chatPanel);

        connectionClosedPanel = new ConnectionClosedPanel();
        mainPanel.add(connectionClosedPanel, "connectionClosedPanel");
        appPanels.put("connectionClosedPanel", connectionClosedPanel);

        errorPanel = new ErrorPanel();
        mainPanel.add(errorPanel, "errorPanel");
        appPanels.put("errorPanel", errorPanel);

        cardLayout.show(mainPanel, "launchPanel");
        setVisible(true);
    }

    public void changePanel(String panelName) {
        if (SwingUtilities.isEventDispatchThread()) {
            appPanels.get(panelName).initialise();
            App.mainWin.cardLayout.show(App.mainWin.mainPanel, panelName);
        } else {
            SwingUtilities.invokeLater(new PanelChange(panelName));
        }
    }
}

class AppPanel extends JPanel {
    GridBagConstraints gbc;

    public AppPanel() {
        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        // gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
    }

    public void addSpacer(int y) {
        App.mainWin.sgbc.gridy = y;
        add(Box.createVerticalGlue(), App.mainWin.sgbc);
    }

    public void initialise() {}
}

class PanelChange implements Runnable, ActionListener {
    // Used to call App.mainWin.changePanel() when changing a panel needs to be done through an action listener for a button, or a runnable for SwingUtilities.invokeLater()

    String panelName;

    public PanelChange(String name) {
        panelName = name;
    }

    @Override
    public void run() {
        // App.mainWin.changePanel() performs this test, but worth doing here as well so this PanelChange object can be reused instead of creating a new one
        if (SwingUtilities.isEventDispatchThread()) {
            App.mainWin.changePanel(panelName);
        } else {
            SwingUtilities.invokeLater(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }
}

class LaunchPanel extends AppPanel {
    private final JLabel welcomeLabel;
    private final JButton serverModeButton;
    private final JButton clientModeButton;

    public LaunchPanel() {
        welcomeLabel = new JLabel("Welcome to ChatApp. Please choose whether to run the application in server or client mode.");

        serverModeButton = new JButton("Server Mode");
        serverModeButton.setFocusable(false);
        serverModeButton.addActionListener(new PanelChange("serverModePanel"));

        clientModeButton = new JButton("Client Mode");
        clientModeButton.setFocusable(false);
        clientModeButton.addActionListener(new PanelChange("clientModePanel"));
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(welcomeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(serverModeButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(clientModeButton, gbc);

        addSpacer(2);
    }
}

class ServerModePanel extends AppPanel {
    private JLabel portLabel;
    private JTextField portField;
    private JButton startButton;
    private JButton backButton;
    private JLabel statusLabel;

    public ServerModePanel() {
        portLabel = new JLabel("Port Number");
        portField = new JTextField(16);
        startButton = new JButton("Start Server");
        backButton = new JButton("Back");
        statusLabel = new JLabel();

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                backButton.setEnabled(false);
                statusLabel.setText("Starting...");

                try {
                    App.connection = new Connection(Integer.parseInt(portField.getText()));
                    App.connection.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportError();
                }
            }
        });

        backButton.addActionListener(new PanelChange("launchPanel"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(startButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(backButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);

        addSpacer(4);
    }

    @Override
    public void initialise() {
        statusLabel.setText("");
        startButton.setEnabled(true);
        backButton.setEnabled(true);
    }

    //Invoked by connector thread
    public void reportStarted() {
        //Report that server has started and is waiting for a connection
        statusLabel.setText("Server started. Waiting for a connection.");
    }

    public void reportError() {
        statusLabel.setText("Failed to start server");
        startButton.setEnabled(true);
        backButton.setEnabled(true);
    }
}

class ClientModePanel extends AppPanel {
    private JLabel ipLabel;
    private JTextField ipField;
    private JLabel portLabel;
    private JTextField portField;
    private JButton connectButton;
    private JButton backButton;
    private JLabel statusLabel;

    public ClientModePanel() {
        ipLabel = new JLabel("IP Address");        
        ipField = new JTextField(16);        
        portLabel = new JLabel("Port Number");        
        portField = new JTextField(16);
        connectButton = new JButton("Connect");
        backButton = new JButton("Back");
        statusLabel = new JLabel();

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectButton.setEnabled(false);
                backButton.setEnabled(false);
                statusLabel.setText("Connecting...");
                
                try {
                    App.connection = new Connection(ipField.getText(), Integer.parseInt(portField.getText()));
                    App.connection.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportError();
                }
            }
        });

        backButton.addActionListener(new PanelChange("launchPanel"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(ipLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(ipField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(connectButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(backButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(statusLabel, gbc);

        addSpacer(5);
    }

    @Override
    public void initialise() {
        statusLabel.setText("");
        connectButton.setEnabled(true);
        backButton.setEnabled(true);
    }

    public void reportError() {
        statusLabel.setText("Connection Failed");
        connectButton.setEnabled(true);
        backButton.setEnabled(true);
    }
}

class ChatPanel extends AppPanel implements Runnable {
    private JLabel connectedLabel;
    private JButton closeButton;
    private JTextField messageField;
    private JButton sendButton;
    private JTextArea messagesArea;

    private int LINELENGTH = 100;

    public ChatPanel() {
        connectedLabel = new JLabel();
        closeButton = new JButton("Close");
        messageField = new JTextField(40);
        sendButton = new JButton("Send");
        messagesArea = new JTextArea(0, 50);
        messagesArea.setBackground(getBackground());

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                App.connection.send("M:" + message);
                display(String.format("You: %s\n", message));
                messageField.setText("");
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                App.connection.send("C:END");
                App.connection.ended = true;
                App.connection.end();
                App.mainWin.changePanel("launchPanel");
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        add(connectedLabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        add(closeButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        add(messageField, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(sendButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        add(messagesArea, gbc);

        addSpacer(3);
    }

    @Override
    public void initialise() {
        String labelText = String.format("Connected to %s on port %d", App.connection.displayName, App.connection.sock.getLocalPort());
        connectedLabel.setText(labelText);
        messagesArea.setText("");
    }

    //Invoked by receiver thread
    @Override
    public void run() {
        //Get messages from receive queue and display them
        if (App.connection.ended) {
            return;
        }

        String message;

        while (!App.connection.recvQueue.isEmpty()) {
            message = App.connection.recvQueue.poll();

            display(String.format("%s: %s\n", App.connection.displayName, message));
        }
    }

    public void display(String message) {
        int length = message.length();

        if (length < LINELENGTH) {
            messagesArea.append(message);
        } else {
            int displayed = 0;
            String segment;

            while (length - displayed > LINELENGTH) {
                segment = message.substring(displayed, displayed + LINELENGTH);
                messagesArea.append(segment + "\n");
                displayed += LINELENGTH;
            }

            if (length - displayed > 0) {
                segment = message.substring(displayed);
                messagesArea.append(segment + "\n");
            }
        }
    }
}

class ConnectionClosedPanel extends AppPanel {
    private JLabel closedLabel;
    private JButton backButton;

    public ConnectionClosedPanel() {
        closedLabel = new JLabel("The connection was closed by the other side.");
        backButton = new JButton("Back");
        
        backButton.addActionListener(new PanelChange("launchPanel"));

        gbc.gridx = 0;

        gbc.gridy = 0;
        add(closedLabel, gbc);

        gbc.gridy = 1;
        add(backButton, gbc);

        addSpacer(2);
    }
}

class ErrorPanel extends AppPanel {
    private JLabel errorLabel;
    private JButton exitButton;

    public ErrorPanel() {
        errorLabel = new JLabel("An error occurred. Please restart the application.");
        exitButton = new JButton("Exit");
        
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });

        gbc.gridx = 0;

        gbc.gridy = 0;
        add(errorLabel, gbc);

        gbc.gridy = 1;
        add(exitButton, gbc);

        addSpacer(2);
    }
}