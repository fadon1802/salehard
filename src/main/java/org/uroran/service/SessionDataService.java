package org.uroran.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.uroran.models.SessionData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервисный класс для работы с сохраненными сессиями.
 */
public class SessionDataService {
    private static final Path SESSION_LIST_FILE = Paths.get("src/main/resources/sessions.yaml");
    private static final Path SESSIONS_DIRECTORY = Paths.get("src/main/resources/sessions");
    private final YAMLMapper yamlMapper;

    private static final SessionDataService INSTANCE = new SessionDataService();

    public static SessionDataService getInstance() {
        return INSTANCE;
    }

    public SessionDataService() {
        this.yamlMapper = new YAMLMapper();
        try {
            if (!Files.exists(SESSIONS_DIRECTORY)) {
                Files.createDirectories(SESSIONS_DIRECTORY);
            }
            if (!Files.exists(SESSION_LIST_FILE)) {
                Files.createFile(SESSION_LIST_FILE);
                yamlMapper.writeValue(SESSION_LIST_FILE.toFile(), new ArrayList<String>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при инициализации");
        }
    }

    /**
     * Сохранение сессии.
     */
    public void saveSessionData(SessionData session) {
        Path sessionFile = SESSIONS_DIRECTORY.resolve("session_" + session.getName() + ".yaml");

        try {
            yamlMapper.writeValue(sessionFile.toFile(), session);

            List<String> sessionList = loadSessionDataList();
            if (!sessionList.contains(session.getName())) {
                sessionList.add(session.getName());
                yamlMapper.writeValue(SESSION_LIST_FILE.toFile(), sessionList);
            }
        } catch (IOException ignored) {}
    }

    /**
     * Получение сессии.
     */
    public SessionData loadSessionData(String sessionName) {
        Path sessionDataFile = SESSIONS_DIRECTORY.resolve("session_" + sessionName + ".yaml");

        try {
            if (Files.exists(sessionDataFile)) {
                return yamlMapper.readValue(sessionDataFile.toFile(), SessionData.class);
            }
        } catch (IOException ignored) {}

        throw new RuntimeException("Сессия " + sessionName + " не существует");
    }

    /**
     * Удаление сессии.
     */
    public void deleteSession(String sessionName) {
        Path sessionFile = SESSIONS_DIRECTORY.resolve("session_" + sessionName + ".yaml");

        try {
            Files.deleteIfExists(sessionFile);

            List<String> sessionList = loadSessionDataList();

            if (sessionList.contains(sessionName)) {
                sessionList.remove(sessionName);
                yamlMapper.writeValue(SESSION_LIST_FILE.toFile(), sessionList);
            }
        } catch (IOException ignored) {
            throw new RuntimeException("Сессия " + sessionName + " не была найдена в списке сессий");
        }
    }

    /**
     * Получение имен всех сессий.
     */
    public List<String> loadSessionDataList() {
        try {
            return yamlMapper.readValue(SESSION_LIST_FILE.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
