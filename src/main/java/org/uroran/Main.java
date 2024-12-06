package org.uroran;

import org.uroran.gui.SessionManagerWindow;
import org.uroran.service.SessionDataService;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SessionManagerWindow(SessionDataService.getInstance()).setVisible(true));
    }
}