package game;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Game {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window=new JFrame("Santa: Last Christmas");
            GamePanel gamePanel=new GamePanel();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(1270,720);window.setResizable(false);window.add(gamePanel);
            window.setLocationRelativeTo(null);window.setVisible(true);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { gamePanel.stopGame(); }
            });
            gamePanel.startGame();
        });
    }
}
