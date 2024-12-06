package org.uroran.service;

import com.jcraft.jsch.*;
import org.uroran.models.SessionData;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SftpService {
    private static final String BASE_REMOTE_DIRECTORY = "/misc/home/";
    private Session session;
    private final SessionData sessionData;
    private ChannelSftp channelSftp;

    public SftpService(SessionData data) {
        this.sessionData = data;
    }
    public void connect() throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(sessionData.getPathToKey(), sessionData.getPassPhrase());

        session = jsch.getSession(sessionData.getUser(), sessionData.getHost(), sessionData.getPort());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        channelSftp = (ChannelSftp) channel;
    }

    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void uploadFile(Path localFile) throws SftpException {
        String remotePath = BASE_REMOTE_DIRECTORY + sessionData.getUser() + "/";
        channelSftp.cd(remotePath);
        channelSftp.put(localFile.toString(), localFile.getFileName().toString());
        System.out.println("Файл " + localFile + " загружен в " + remotePath);
    }

    public void downloadFile(String remoteUser, String remoteFileName, Path localDirectory) throws SftpException {
        String remotePath = BASE_REMOTE_DIRECTORY + remoteUser + "/" + remoteFileName;
        Path localFile = localDirectory.resolve(remoteFileName);

        try (OutputStream outputStream = Files.newOutputStream(localFile)) {
            channelSftp.get(remotePath, outputStream);
        } catch (Exception e) {
            throw new SftpException(1, "Ошибка при скачивании файла", e);
        }

        System.out.println("File " + remoteFileName + " downloaded to " + localDirectory);
    }

    public void deleteFile(String remoteFileName) throws SftpException {
        String remotePath = BASE_REMOTE_DIRECTORY + sessionData.getUser() + "/" + remoteFileName;
        channelSftp.rm(remotePath);
        System.out.println("Файл " + remoteFileName + " удален из " + remotePath);
    }

    public void listFiles() throws SftpException {
        String remotePath = BASE_REMOTE_DIRECTORY + sessionData.getUser() + "/_scratch/";
        channelSftp.ls(remotePath).forEach(item -> {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) item;
            System.out.println(entry.getFilename());
        });
    }
}
