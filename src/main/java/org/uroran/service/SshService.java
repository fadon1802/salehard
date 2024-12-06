package org.uroran.service;

import com.jcraft.jsch.*;
import org.uroran.models.SessionData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SshService {
    private Session session;
    public ChannelShell channelShell;
    private InputStream inputStream;
    private OutputStream outputStream;

    private final SessionData sessionData;

    public SshService(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    /**
     * Метод для создания сессии
     */
    public void connect() throws JSchException, IOException {
        JSch jsch = new JSch();

        jsch.addIdentity(sessionData.getPathToKey(), sessionData.getPassPhrase());
        session = jsch.getSession(sessionData.getUser(), sessionData.getHost(), sessionData.getPort());
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        channelShell = (ChannelShell) session.openChannel("shell");

        inputStream = channelShell.getInputStream();
        outputStream = channelShell.getOutputStream();

        channelShell.connect();
    }

    /**
     * Метод для отправки команды и получения ответа
     */
    public String sendCommand(String command) throws Exception {
        if (channelShell == null || !channelShell.isConnected()) {
            throw new IllegalStateException("Shell channel is not connected.");
        }

        outputStream.write((command + "\n").getBytes());
        outputStream.flush();

        Thread.sleep(500);
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[2048];
        while (inputStream.available() > 0) {
            int bytesRead = inputStream.read(buffer);
            output.append(new String(buffer, 0, bytesRead));
        }

        return output.toString();
    }

    /**
     * Метод для загрузки файла на сервер
     */
    public void uploadFile(String localFilePath, String remoteFilePath) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.put(localFilePath, remoteFilePath);
        channelSftp.disconnect();
    }

    /**
     * Метод для скачивания файла с сервера
     */
    public void downloadFile(String remoteFilePath, String localFilePath) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.get(remoteFilePath, localFilePath);
        channelSftp.disconnect();
    }

    /**
     * Метод для получения списка файлов в указанной директории на сервере.
     */
    public List<String> listFiles(String remoteDirectory) throws JSchException, SftpException {
        List<String> fileNames = new ArrayList<>();
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            Vector<ChannelSftp.LsEntry> files = channelSftp.ls(channelSftp.pwd());
            for (ChannelSftp.LsEntry entry : files) {
                String filename = entry.getFilename();
                //if (filename.startsWith(".")) continue;
                fileNames.add(filename);
            }
        } finally {
            channelSftp.disconnect();
        }

        return fileNames;
    }

    public String getCurrentRemoteDirectory() throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            return channelSftp.pwd(); // Возвращает текущую директорию
        } finally {
            channelSftp.disconnect();
        }
    }



    /**
     * Метод для разъединения сессии
     */
    public void disconnect() {
        if (channelShell != null && channelShell.isConnected()) {
            channelShell.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
