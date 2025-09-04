import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;

public class MusicApp extends JFrame {
    private Connection conn;
    private DefaultListModel<String> songListModel;
    private java.util.List<String> songPaths;
    private JLabel nowPlayingLabel;

    public MusicApp() {
        setTitle("My Music App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        songListModel = new DefaultListModel<>();
        JList<String> songList = new JList<>(songListModel);
        JScrollPane scrollPane = new JScrollPane(songList);

        JButton uploadButton = new JButton("Upload Song");
        JButton playButton = new JButton("Play");
        JButton shuffleButton = new JButton("Shuffle");
        JButton aboutButton = new JButton("About");

        nowPlayingLabel = new JLabel("Now Playing: None");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(uploadButton);
        buttonPanel.add(playButton);
        buttonPanel.add(shuffleButton);
        buttonPanel.add(aboutButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(nowPlayingLabel, BorderLayout.NORTH);

        initDatabase();
        loadSongs();

        // Upload button logic
        uploadButton.addActionListener(e -> uploadSong());

        playButton.addActionListener(e -> {
            String selected = songList.getSelectedValue();
            if (selected != null) {
                playSong(selected);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a song to play.");
            }
        });

        shuffleButton.addActionListener(e -> shuffleSongs());

        aboutButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Music App\nCreated by ...\nEnjoy your music!")
        );
    }

    private void initDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:music.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS songs (id INTEGER PRIMARY KEY, name TEXT, path TEXT)");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSongs() {
        songPaths = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name, path FROM songs");
            while (rs.next()) {
                songListModel.addElement(rs.getString("name"));
                songPaths.add(rs.getString("path"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void uploadSong() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio Files", "mp3", "wav"));
        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String songName = selectedFile.getName();
            String musicDir = "music";

            File dir = new File(musicDir);
            if (!dir.exists()) dir.mkdir();

            File destFile = new File(dir, songName);
            try {
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO songs (name, path) VALUES (?, ?)");
                pstmt.setString(1, songName);
                pstmt.setString(2, destFile.getAbsolutePath());
                pstmt.executeUpdate();
                pstmt.close();

                songListModel.addElement(songName);
                songPaths.add(destFile.getAbsolutePath());

                JOptionPane.showMessageDialog(this, "Song uploaded successfully!");
            } catch (IOException | SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to upload song.");
            }
        }
    }

    private void playSong(String songName) {
        try {
            int index = songListModel.indexOf(songName);
            if (index >= 0) {
                String path = songPaths.get(index);
                nowPlayingLabel.setText("Now Playing: " + songName);

                // Play with system default player
                Desktop.getDesktop().open(new File(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shuffleSongs() {
        Collections.shuffle(songPaths);
        songListModel.clear();
        for (String path : songPaths) {
            songListModel.addElement(new File(path).getName());
        }
        JOptionPane.showMessageDialog(this, "Songs shuffled!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MusicApp().setVisible(true));
    }
}
