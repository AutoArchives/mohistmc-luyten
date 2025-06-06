package com.mohistmc.mowudecomp;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.OneDarkTheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starter, the main class
 */
public class MoWuDecomp {

    private static final AtomicReference<MainWindow> mainWindowRef = new AtomicReference<>();
    private static final Queue<File> pendingFiles = new ConcurrentLinkedQueue<>();
    private static ServerSocket lockSocket = null;

    public static final String VERSION;

    static {
        String version = MoWuDecomp.class.getPackage().getImplementationVersion();
        VERSION = (version == null ? "DEV" : version);
    }

    public static void main(final String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // for TotalCommander External Viewer setting:
        // javaw -jar "c:\Program Files\MoWuDecomp\luyten.jar"
        // (TC will not complain about temporary file when opening .class from
        // .zip or .jar)
        final File fileFromCommandLine = getFileFromCommandLine(args);

        SwingUtilities.invokeLater(() -> {
            LafManager.install(new OneDarkTheme());
            if (!mainWindowRef.compareAndSet(null, new MainWindow(fileFromCommandLine))) {
                // Already set - so add the files to open
                openFileInInstance(fileFromCommandLine);
            }
            processPendingFiles();
            mainWindowRef.get().setVisible(true);
        });
    }

    // Private function which processes all pending files - synchronized on the
    // list of pending files
    public static void processPendingFiles() {
        final MainWindow mainWindow = mainWindowRef.get();
        if (mainWindow != null) {
            for (File f : pendingFiles) {
                mainWindow.loadNewFile(f);
            }
            pendingFiles.clear();
        }
    }

    // Function which opens the given file in the instance, if it's running -
    // and if not, it processes the files
    public static void openFileInInstance(File fileToOpen) {
        synchronized (pendingFiles) {
            if (fileToOpen != null) {
                pendingFiles.add(fileToOpen);
            }
        }
        processPendingFiles();
    }

    // Function which opens the given file in the instance, if it's running -
    // and if not, it processes the files
    public static void addToPendingFiles(File fileToOpen) {
        if (fileToOpen != null) {
            pendingFiles.offer(fileToOpen);
        }
    }

    // Function which exits the application if it's running
    public static void quitInstance() {
        final MainWindow mainWindow = mainWindowRef.get();
        if (mainWindow != null) {
            mainWindow.onExitMenu();
        }
    }

    public static File getFileFromCommandLine(String... args) {
        try {
            if (args.length > 0) {
                return new File(args[0]).getCanonicalFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method allows for users to copy the stacktrace for reporting any issues.
     * Add Cool Hyperlink Enhanced for mouse users.
     *
     * @param message
     * @param e
     */
    public static void showExceptionDialog(String message, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stacktrace = sw.toString();
        try {
            sw.close();
            pw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        System.out.println(stacktrace);

        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        if (message.contains("\n")) {
            for (String s : message.split("\n")) {
                pane.add(new JLabel(s));
            }
        } else {
            pane.add(new JLabel(message));
        }
        pane.add(new JLabel(" \n")); // Whitespace
        final JTextArea exception = new JTextArea(25, 100);
        exception.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        exception.setText(stacktrace);
        exception.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    new JPopupMenu() {
                        {
                            JMenuItem menuitem = new JMenuItem("Select All");
                            menuitem.addActionListener(event -> {
                                exception.requestFocus();
                                exception.selectAll();
                            });
                            this.add(menuitem);
                            menuitem = new JMenuItem("Copy");
                            menuitem.addActionListener(new DefaultEditorKit.CopyAction());
                            this.add(menuitem);
                        }

                        private static final long serialVersionUID = 562054483562666832L;
                    }.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        JScrollPane scroll = new JScrollPane(exception);
        scroll.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Stacktrace"),
                new BevelBorder(BevelBorder.LOWERED)));
        pane.add(scroll);
        final String issue = "https://github.com/MohistMC/MoWuDecomp/issues";
        final JLabel link = new JLabel("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URI(issue));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<HTML>Submit to <FONT color=\"#00aa99\"><U>" + issue + "</U></FONT></HTML>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                link.setText("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
            }
        });
        pane.add(link);
        JOptionPane.showMessageDialog(null, pane, "Error!", JOptionPane.ERROR_MESSAGE);
    }

}
