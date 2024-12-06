package org.uroran.gui;

import org.uroran.models.SessionData;
import org.uroran.service.SessionDataService;
import org.uroran.service.SshService;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SessionManagerWindow extends JFrame {
    private static final String HOST = "umt.imm.uran.ru";
    private static final int PORT = 22;

    private JTextField sessionNameField;  // Новое поле
    private JTextField usernameField;
    private JTextField privateKeyField;
    private JTextField passphraseField;
    private JList<String> sessionList;

    private final SessionDataService sessionDataService;

    public SessionManagerWindow(SessionDataService sessionDataService) {
        this.sessionDataService = sessionDataService;

        //Создание окна выбора сессии
        setTitle("УРО РАН - Управление сессиями");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new GridLayout(1, 2));

        JPanel createSessionPanel = createSessionPanel();
        JPanel existingSessionsPanel = createExistingSessionPanel();


        add(createSessionPanel);
        add(existingSessionsPanel);

        setLocationRelativeTo(null);
    }

    public JPanel createSessionPanel() {
        JPanel createSessionPanel = new JPanel();
        createSessionPanel.setLayout(new GridLayout(6, 1, 5, 5));
        createSessionPanel.setBorder(BorderFactory.createTitledBorder("Создать новую сессию"));

        sessionNameField = new JTextField(20);
        usernameField = new JTextField(20);
        privateKeyField = new JTextField(20);
        passphraseField = new JTextField(20);

        JButton browseButton = new JButton("Обзор...");
        browseButton.addActionListener(e -> choosePrivateKeyFile());

        JPanel privateKeyPanel = new JPanel(new BorderLayout());
        privateKeyPanel.add(privateKeyField, BorderLayout.CENTER);
        privateKeyPanel.add(browseButton, BorderLayout.EAST);

        createSessionPanel.add(new JLabel("Имя сессии:"));
        createSessionPanel.add(sessionNameField);
        createSessionPanel.add(new JLabel("Имя пользователя:"));
        createSessionPanel.add(usernameField);
        createSessionPanel.add(new JLabel("Приватный ключ (OpenSSH):"));
        createSessionPanel.add(privateKeyPanel);
        createSessionPanel.add(new JLabel("Контрольное слово:"));
        createSessionPanel.add(passphraseField);

        JButton addButton = new JButton("Добавить");
        addButton.addActionListener(e -> addNewSession());

        createSessionPanel.add(addButton);

        return createSessionPanel;
    }

    public JPanel createExistingSessionPanel() {
        JPanel existingSessionsPanel = new JPanel();
        existingSessionsPanel.setLayout(new BorderLayout());
        existingSessionsPanel.setBorder(BorderFactory.createTitledBorder("Сохраненные сессии"));

        String[] savedSessions = sessionDataService.loadSessionDataList().toArray(new String[0]);
        sessionList = new JList<>(savedSessions);
        JScrollPane sessionScrollPane = new JScrollPane(sessionList);

        JButton connectButton = new JButton("Подключить");
        connectButton.addActionListener(e -> connectToSelectedSession());

        JButton deleteButton = new JButton("Удалить");
        deleteButton.addActionListener(e -> deleteSession());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(connectButton);
        buttonPanel.add(deleteButton);

        existingSessionsPanel.add(sessionScrollPane, BorderLayout.CENTER);
        existingSessionsPanel.add(buttonPanel, BorderLayout.SOUTH);

        return existingSessionsPanel;
    }

    /**
     * Выбор пути до приватного ключа
     */
    private void choosePrivateKeyFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            privateKeyField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Добавление новой сессии в список сессий
     */
    private void addNewSession() {
        String sessionName = sessionNameField.getText();
        String username = usernameField.getText();
        String privateKeyPath = privateKeyField.getText();
        String passphrase = passphraseField.getText();

        SessionData newSession = SessionData.builder()
                .name(sessionName)
                .host(HOST)
                .port(PORT)
                .user(username)
                .pathToKey(privateKeyPath)
                .passPhrase(passphrase)
                .build();

        try {
            sessionDataService.saveSessionData(newSession);
            refreshSessionList();
            JOptionPane.showMessageDialog(this, "Сессия успешно добавлена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }

        sessionNameField.setText("");
        usernameField.setText("");
        privateKeyField.setText("");
        passphraseField.setText("");
    }

    /**
     * Подключение к выбранной сессии. Закрывается текущее окно и открывается следующее
     */
    private void connectToSelectedSession() {
        String selectedSession = sessionList.getSelectedValue();

        if (selectedSession == null) {
            JOptionPane.showMessageDialog(this, "Выберите сессию для подключения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SessionData chosenSession = sessionDataService.loadSessionData(selectedSession);

        SshService sshService = new SshService(chosenSession);
        MainWindow window = new MainWindow(sshService);
        window.setVisible(true);

        this.dispose();
    }

    /**
     * Удаление сессии из списка сессий
     */
    private void deleteSession() {
        String selectedSession = sessionList.getSelectedValue();

        if (selectedSession == null) {
            JOptionPane.showMessageDialog(this, "Выберите сессию для удаления.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить сессию \"" + selectedSession + "\"?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            sessionDataService.deleteSession(selectedSession);
            refreshSessionList();
            JOptionPane.showMessageDialog(this, "Сессия успешно удалена.", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Обновление списка сессий
     */
    private void refreshSessionList() {
        String[] updatedSessions = sessionDataService.loadSessionDataList().toArray(new String[0]);
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String session : updatedSessions) {
            model.addElement(session);
        }
        sessionList.setModel(model);
    }
}
