package org.uroran.gui;

import com.jcraft.jsch.JSchException;
import org.uroran.service.SessionDataService;
import org.uroran.service.SshService;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow extends JFrame {
    private JTextField commandInputField;

    private JTextArea sftpLogArea;
    private StyledDocument document;

    private final SshService sshService;

    public MainWindow(SshService sshService) {
        this.sshService = sshService;

        try {
            sshService.connect();
            Thread.sleep(1000);
        } catch (IOException | JSchException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        setTitle("Удаленный терминал");
        setSize(1000, 600);
        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sshService.disconnect();
                dispose();
                System.exit(1);
            }
        });

        // Основной контейнер
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        // Основной разделитель (левая и правая часть)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(500);
        container.add(mainSplitPane, BorderLayout.CENTER);

        // SSH терминал
        JPanel sshPanel = createSshPanel();
        mainSplitPane.setLeftComponent(sshPanel);

        // Разделитель для SFTP и графиков
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setDividerLocation(300);

        // SFTP
        JPanel sftpPanel = createSftpPanel();
        sftpPanel.setBackground(Color.LIGHT_GRAY);
        rightSplitPane.setTopComponent(sftpPanel);


        // Графики
        JPanel graphPanel = new JPanel();
        graphPanel.setBackground(Color.LIGHT_GRAY);
        graphPanel.add(new JLabel("Здесь будут графики"));

        // Добавляем панели в правый разделитель
        rightSplitPane.setTopComponent(sftpPanel);
        rightSplitPane.setBottomComponent(graphPanel);

        // Устанавливаем правую часть в основной разделитель
        mainSplitPane.setRightComponent(rightSplitPane);
    }

    /**
     * Метод для создания SSH панели
     */
    private JPanel createSshPanel() {
        JPanel sshPanel = new JPanel(new BorderLayout());

        JTextPane terminalOutputPane = new JTextPane();
        terminalOutputPane.setEditable(false);
        terminalOutputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        document = terminalOutputPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(terminalOutputPane);
        sshPanel.add(scrollPane, BorderLayout.CENTER);

        commandInputField = new JTextField();
        commandInputField.addActionListener(e -> sendCommand());
        sshPanel.add(commandInputField, BorderLayout.SOUTH);

        return sshPanel;
    }

    private JPanel createSftpPanel() {
        JPanel sftpPanel = new JPanel(new BorderLayout());

        // Поле для вывода логов SFTP
        sftpLogArea = new JTextArea();
        sftpLogArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(sftpLogArea);
        sftpPanel.add(logScrollPane, BorderLayout.CENTER);

        // Панель с кнопками загрузки и скачивания
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton uploadButton = new JButton("Загрузить файл");
        JButton downloadButton = new JButton("Скачать файл");

        uploadButton.addActionListener(e -> uploadFile(sftpLogArea));
        downloadButton.addActionListener(e -> downloadFile(sftpLogArea));

        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        sftpPanel.add(buttonPanel, BorderLayout.SOUTH);

        return sftpPanel;
    }

    private JTextArea getSftpLogArea() {
        return sftpLogArea;
    }

    /**
     * Метод для загрузки файла на сервер
     */
    private void uploadFile(JTextArea sftpLogArea) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String remotePath = JOptionPane.showInputDialog(this, "Введите путь для сохранения файла на сервере:");

            try {
                sshService.uploadFile(selectedFile.getAbsolutePath(), remotePath);
                sftpLogArea.append("Файл " + selectedFile.getName() + " успешно загружен на сервер в " + remotePath + "\n");
            } catch (Exception e) {
                sftpLogArea.append("Ошибка при загрузке файла: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Метод для скачивания файла с сервера
     */
    private void downloadFile(JTextArea sftpLogArea) {
        String remotePath = JOptionPane.showInputDialog(this, "Введите путь к файлу на сервере:");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            String localPath = new File(selectedDir, new File(remotePath).getName()).getAbsolutePath();

            try {
                sshService.downloadFile(remotePath, localPath);
                sftpLogArea.append("Файл " + remotePath + " успешно скачан в " + localPath + "\n");
            } catch (Exception e) {
                sftpLogArea.append("Ошибка при скачивании файла: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Создаем меню-бар с кнопками "Настройки", "SFTP" и "Закрыть сессию".
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Меню "Настройки"
        JMenu settingsMenu = new JMenu("Настройки");
        JMenuItem settingsPlaceholder = new JMenuItem("Пусто");
        settingsPlaceholder.setEnabled(false);
        settingsMenu.add(settingsPlaceholder);
        menuBar.add(settingsMenu);

        // Меню "SFTP"
        JMenu sftpMenu = new JMenu("SFTP");
        JMenuItem uploadItem = new JMenuItem("Загрузить");
        JMenuItem downloadItem = new JMenuItem("Скачать");
        // Добавляем действия для SFTP-заглушек
        sftpMenu.add(uploadItem);
        sftpMenu.add(downloadItem);
        menuBar.add(sftpMenu);

        // Кнопка "Закрыть сессию"
        JMenuItem closeSessionItem = new JMenuItem("Закрыть сессию");
        closeSessionItem.addActionListener(e -> {
            sshService.disconnect();
            new SessionManagerWindow(SessionDataService.getInstance()).setVisible(true);
            dispose();
        });
        menuBar.add(closeSessionItem);

        return menuBar;
    }

    /**
     * Метод для отправки команды из GUI по SSH и вывода ответа
     */
    private void sendCommand() {
        String command = commandInputField.getText().trim();
        if (command.equals("ls")) {
            listFilesInSftp();
        }

        String output;
        try {
            output = sshService.sendCommand(command);
        } catch (Exception ignored) {
            output = "Failed";
        }

        appendColoredText(">>> " + command + "\n", Color.BLUE);
        appendAnsiColoredText(output);
        commandInputField.setText("");
    }

    private void listFilesInSftp() {
        System.out.println("listFilesInSftp called");

        try {
            String currentDirectory = sshService.getCurrentRemoteDirectory();
            StringBuilder fileList = new StringBuilder("Текущая директория: " + currentDirectory + "\nСписок файлов:\n");
            for (String fileName : sshService.listFiles(currentDirectory)) { // "." — текущая директория
                fileList.append(fileName).append("\n");
            }
            sftpLogArea.setText(fileList.toString());
        } catch (Exception e) {
            sftpLogArea.append("Ошибка получения списка файлов: " + e.getMessage() + "\n");
        }
    }

    /**
     * Метод для добавления текста с указанным цветом (GPT, криво)
     */
    private void appendColoredText(String text, Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для парсинга и отображения текста с ANSI-кодами (GPT, вроде работает)
     */
    private void appendAnsiColoredText(String text) {
        // Регулярное выражение для ANSI-кодов
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+;?)*m");
        Matcher matcher = ansiPattern.matcher(text);
        int lastEnd = 0;

        SimpleAttributeSet attributes = new SimpleAttributeSet();

        while (matcher.find()) {
            // Добавляем текст до ANSI-кода
            if (matcher.start() > lastEnd) {
                try {
                    document.insertString(document.getLength(), text.substring(lastEnd, matcher.start()), attributes);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            // Обновляем стиль текста в зависимости от ANSI-кода
            String ansiCode = matcher.group();
            updateAttributesForAnsiCode(ansiCode, attributes);

            lastEnd = matcher.end();
        }

        // Добавляем оставшийся текст
        if (lastEnd < text.length()) {
            try {
                document.insertString(document.getLength(), text.substring(lastEnd), attributes);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Метод для получения цвета по ANSI коду (GPT, криво)
     */
    private void updateAttributesForAnsiCode(String ansiCode, SimpleAttributeSet attributes) {
        if (ansiCode.contains("31")) {
            StyleConstants.setForeground(attributes, Color.RED);
        } else if (ansiCode.contains("32")) {
            StyleConstants.setForeground(attributes, Color.GREEN);
        } else if (ansiCode.contains("33")) {
            StyleConstants.setForeground(attributes, Color.YELLOW);
        } else if (ansiCode.contains("34")) {
            StyleConstants.setForeground(attributes, Color.BLUE);
        } else if (ansiCode.contains("35")) {
            StyleConstants.setForeground(attributes, Color.MAGENTA);
        } else if (ansiCode.contains("36")) {
            StyleConstants.setForeground(attributes, Color.CYAN);
        } else if (ansiCode.contains("0")) {
            StyleConstants.setForeground(attributes, Color.BLACK);
        }
    }

}