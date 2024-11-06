import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

class PlayerScore implements Serializable {
    String name;
    int score;
    long time;

    public PlayerScore(String name, int score) {
        this.name = name;
        this.score = score;
        this.time = System.currentTimeMillis();
    }
}

class GameSettings {
    public static int gameSpeed = 1;
    public static float customSpeed = 1.0f;
    public static int aiDifficulty = 1;
    public static int skinType = 0;
    public static boolean isFullscreen = false;
    public static int maxPoints = 5;
    public static boolean isInfinite = false;
    public static String player1Name = "";
    public static String player2Name = "";
    public static ArrayList<PlayerScore> leaderboard = new ArrayList<PlayerScore>();

    public static final Color[][] SKINS = {
            {new Color(200, 200, 255), new Color(255, 100, 100)},
            {new Color(100, 255, 100), new Color(255, 255, 100)},
            {new Color(255, 100, 255), new Color(255, 150, 0)},
            {new Color(255, 255, 255), new Color(100, 100, 100)}
    };

    @SuppressWarnings("unchecked")
    public static void loadLeaderboard() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("leaderboard.dat"))) {
            leaderboard = (ArrayList<PlayerScore>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            leaderboard = new ArrayList<PlayerScore>();
        }
    }

    public static void saveLeaderboard() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("leaderboard.dat"))) {
            out.writeObject(leaderboard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addScore(String name, int score) {
        leaderboard.add(new PlayerScore(name, score));
        Collections.sort(leaderboard, (a, b) -> b.score - a.score);
        if (leaderboard.size() > 10) {
            leaderboard = new ArrayList<PlayerScore>(leaderboard.subList(0, 10));
        }
        saveLeaderboard();
    }
}

class FullScreenUtil {
    private static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    public static void toggleFullscreen(Window window) {
        if (GameSettings.isFullscreen) {
            device.setFullScreenWindow(null);
            GameSettings.isFullscreen = false;
            window.setSize(800, 600);
        } else {
            device.setFullScreenWindow(window);
            GameSettings.isFullscreen = true;
        }
    }

    public static void applyFullscreenState(Window window) {
        if (GameSettings.isFullscreen) {
            device.setFullScreenWindow(window);
        } else {
            window.setSize(800, 600);
        }
    }

    public static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
}

class NameInputDialog extends JDialog {
    private String playerName = "";
    private boolean confirmed = false;

    public NameInputDialog(Frame owner, String title) {
        super(owner, title, true);
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        JTextField nameField = new JTextField(20);
        JButton confirmButton = new JButton("Confirmer");

        panel.add(new JLabel("Entrez votre nom:"));
        panel.add(nameField);
        panel.add(confirmButton);

        confirmButton.addActionListener(e -> {
            if (!nameField.getText().trim().isEmpty()) {
                playerName = nameField.getText().trim();
                confirmed = true;
                dispose();
            }
        });

        nameField.addActionListener(e -> confirmButton.doClick());

        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}

class SpeedInputDialog extends JDialog {
    private float speedValue = 1.0f;
    private boolean confirmed = false;

    public SpeedInputDialog(Frame owner) {
        super(owner, "Vitesse personnalisée", true);
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        JSlider speedSlider = new JSlider(1, 50, 10);
        JLabel speedLabel = new JLabel("Vitesse: 1.0x", SwingConstants.CENTER);
        JButton confirmButton = new JButton("Confirmer");

        speedSlider.addChangeListener(e -> {
            float value = speedSlider.getValue() / 10.0f;
            speedLabel.setText(String.format("Vitesse: %.1fx", value));
            speedValue = value;
        });

        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        panel.add(new JLabel("Ajustez la vitesse:", SwingConstants.CENTER));
        panel.add(speedSlider);
        panel.add(speedLabel);
        panel.add(confirmButton);

        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(owner);
    }

    public float getSpeedValue() {
        return speedValue;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}

class LeaderboardDialog extends JDialog {
    public LeaderboardDialog(Frame owner) {
        super(owner, "Classement", true);
        setLayout(new BorderLayout(10, 10));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Top 10 meilleurs scores:"));
        panel.add(Box.createVerticalStrut(10));

        for (int i = 0; i < GameSettings.leaderboard.size(); i++) {
            PlayerScore score = GameSettings.leaderboard.get(i);
            JLabel scoreLabel = new JLabel(String.format("%d. %s - %d points",
                    i + 1, score.name, score.score));
            panel.add(scoreLabel);
            panel.add(Box.createVerticalStrut(5));
        }

        JButton closeButton = new JButton("Fermer");
        closeButton.addActionListener(e -> dispose());

        add(panel, BorderLayout.CENTER);
        add(closeButton, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
class MenuPong extends JFrame {
    private JPanel menuPanel;
    private int selectedOption = 0;
    private String[] mainOptions = {"Jouer", "Parametres", "Classement", "Quitter"};
    private boolean inSettings = false;
    private int selectedSetting = 0;
    private String[] settingsOptions = {"Vitesse: ", "Vitesse Personnalisee", "Difficulte IA: ", "Skin: ", "Plein ecran: ", "Retour"};
    private Color backgroundColor = new Color(0, 0, 40);
    private Color textColor = new Color(200, 200, 255);
    private Color selectedColor = new Color(255, 100, 100);

    public MenuPong() {
        GameSettings.loadLeaderboard();
        setTitle("Pong - Menu");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        menuPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(backgroundColor);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                g2d.setColor(textColor);
                g2d.drawString("PONG", centerX - 80, centerY - 150);

                if (!inSettings) {
                    drawMainMenu(g2d, centerX, centerY);
                } else {
                    drawSettingsMenu(g2d, centerX, centerY);
                }

                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                g2d.setColor(textColor);
                String currentSpeed = GameSettings.customSpeed > 0 ?
                        String.format("%.1fx", GameSettings.customSpeed) :
                        "x" + GameSettings.gameSpeed;
                g2d.drawString("Vitesse actuelle: " + currentSpeed, 10, getHeight() - 40);
                g2d.drawString("F11 = Plein ecran", getWidth() - 150, getHeight() - 40);
            }
        };

        menuPanel.setFocusable(true);
        menuPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    FullScreenUtil.toggleFullscreen(MenuPong.this);
                } else if (!inSettings) {
                    handleMainMenuInput(e);
                } else {
                    handleSettingsMenuInput(e);
                }
                menuPanel.repaint();
            }
        });

        add(menuPanel);
        FullScreenUtil.applyFullscreenState(this);
        setVisible(true);
    }

    private void drawMainMenu(Graphics2D g2d, int centerX, int centerY) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        for(int i = 0; i < mainOptions.length; i++) {
            g2d.setColor(i == selectedOption ? selectedColor : textColor);
            g2d.drawString(mainOptions[i], centerX - 60, centerY + i * 60);
        }
    }

    private void drawSettingsMenu(Graphics2D g2d, int centerX, int centerY) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        for(int i = 0; i < settingsOptions.length; i++) {
            g2d.setColor(i == selectedSetting ? selectedColor : textColor);
            String value = "";
            switch(i) {
                case 0: value = "x" + GameSettings.gameSpeed; break;
                case 1: value = String.format("%.1fx", GameSettings.customSpeed); break;
                case 2: value = getAIDifficultyString(); break;
                case 3: value = getSkinString(); break;
                case 4: value = GameSettings.isFullscreen ? "Oui" : "Non"; break;
            }
            g2d.drawString(settingsOptions[i] + value, centerX - 150, centerY + i * 60);
        }
    }

    private String getAIDifficultyString() {
        switch(GameSettings.aiDifficulty) {
            case 0: return "Facile";
            case 1: return "Normal";
            case 2: return "Difficile";
            default: return "Normal";
        }
    }

    private String getSkinString() {
        switch(GameSettings.skinType) {
            case 0: return "Bleu/Rouge";
            case 1: return "Vert/Jaune";
            case 2: return "Rose/Orange";
            case 3: return "Blanc/Gris";
            default: return "Bleu/Rouge";
        }
    }

    private void handleMainMenuInput(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selectedOption = (selectedOption - 1 + mainOptions.length) % mainOptions.length;
                break;
            case KeyEvent.VK_DOWN:
                selectedOption = (selectedOption + 1) % mainOptions.length;
                break;
            case KeyEvent.VK_ENTER:
                handleMainMenuSelection();
                break;
        }
    }

    private void handleMainMenuSelection() {
        switch(selectedOption) {
            case 0:
                new GameMenu();
                dispose();
                break;
            case 1:
                inSettings = true;
                selectedOption = 0;
                break;
            case 2:
                new LeaderboardDialog(this).setVisible(true);
                break;
            case 3:
                System.exit(0);
                break;
        }
    }

    private void handleSettingsMenuInput(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selectedSetting = (selectedSetting - 1 + settingsOptions.length) % settingsOptions.length;
                break;
            case KeyEvent.VK_DOWN:
                selectedSetting = (selectedSetting + 1) % settingsOptions.length;
                break;
            case KeyEvent.VK_LEFT:
                updateSetting(-1);
                break;
            case KeyEvent.VK_RIGHT:
                updateSetting(1);
                break;
            case KeyEvent.VK_ENTER:
                if (selectedSetting == 1) {
                    SpeedInputDialog dialog = new SpeedInputDialog(this);
                    dialog.setVisible(true);
                    if (dialog.isConfirmed()) {
                        GameSettings.customSpeed = dialog.getSpeedValue();
                        GameSettings.gameSpeed = 0;
                    }
                } else if (selectedSetting == settingsOptions.length - 1) {
                    inSettings = false;
                }
                break;
            case KeyEvent.VK_ESCAPE:
                inSettings = false;
                break;
        }
    }

    private void updateSetting(int direction) {
        switch(selectedSetting) {
            case 0:
                GameSettings.gameSpeed = Math.max(1, Math.min(3, GameSettings.gameSpeed + direction));
                GameSettings.customSpeed = 0;
                break;
            case 2:
                GameSettings.aiDifficulty = Math.max(0, Math.min(2, GameSettings.aiDifficulty + direction));
                break;
            case 3:
                GameSettings.skinType = (GameSettings.skinType + direction + GameSettings.SKINS.length) % GameSettings.SKINS.length;
                break;
            case 4:
                if (direction != 0) {
                    FullScreenUtil.toggleFullscreen(this);
                }
                break;
        }
    }
}
class GameMenu extends JFrame {
    private int selectedMode = 0;
    private int selectedScore = 0;
    private boolean choosingScore = false;
    private String[] modes = {"1 vs 1", "1 vs CPU", "CPU vs CPU", "Retour"};
    private Color backgroundColor = new Color(0, 0, 40);
    private Color textColor = new Color(200, 200, 255);
    private Color selectedColor = new Color(255, 100, 100);

    public GameMenu() {
        setTitle("Pong - Selection du mode");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(backgroundColor);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                g2d.setFont(new Font("Arial", Font.BOLD, 50));
                g2d.setColor(textColor);

                if (!choosingScore) {
                    g2d.drawString("MODE DE JEU", centerX - 150, centerY - 150);
                    drawModeMenu(g2d, centerX, centerY);
                } else {
                    g2d.drawString("POINTS MAXIMUM", centerX - 200, centerY - 150);
                    drawScoreMenu(g2d, centerX, centerY);
                }
            }

            private void drawModeMenu(Graphics2D g2d, int centerX, int centerY) {
                g2d.setFont(new Font("Arial", Font.PLAIN, 30));
                for(int i = 0; i < modes.length; i++) {
                    g2d.setColor(i == selectedMode ? selectedColor : textColor);
                    g2d.drawString(modes[i], centerX - 60, centerY + i * 60);
                }
            }

            private void drawScoreMenu(Graphics2D g2d, int centerX, int centerY) {
                g2d.setFont(new Font("Arial", Font.PLAIN, 30));
                String[] scoreOptions = {"5 points", "10 points", "15 points", "20 points", "30 points",
                        "50 points", "100 points", "Personnalise", "Infini", "Retour"};
                for(int i = 0; i < scoreOptions.length; i++) {
                    g2d.setColor(i == selectedScore ? selectedColor : textColor);
                    g2d.drawString(scoreOptions[i], centerX - 100, centerY + i * 40);
                }
            }
        };

        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    FullScreenUtil.toggleFullscreen(GameMenu.this);
                } else if (!choosingScore) {
                    handleModeInput(e);
                } else {
                    handleScoreInput(e);
                }
                panel.repaint();
            }
        });

        add(panel);
        FullScreenUtil.applyFullscreenState(this);
        setVisible(true);
    }

    private void handleModeInput(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selectedMode = (selectedMode - 1 + modes.length) % modes.length;
                break;
            case KeyEvent.VK_DOWN:
                selectedMode = (selectedMode + 1) % modes.length;
                break;
            case KeyEvent.VK_ENTER:
                if(selectedMode == modes.length - 1) {
                    new MenuPong();
                    dispose();
                } else {
                    choosingScore = true;
                }
                break;
            case KeyEvent.VK_ESCAPE:
                new MenuPong();
                dispose();
                break;
        }
    }

    private void handleScoreInput(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selectedScore = (selectedScore - 1 + 10) % 10;
                break;
            case KeyEvent.VK_DOWN:
                selectedScore = (selectedScore + 1) % 10;
                break;
            case KeyEvent.VK_ENTER:
                handleScoreSelection();
                break;
            case KeyEvent.VK_ESCAPE:
                choosingScore = false;
                break;
        }
    }

    private void handleScoreSelection() {
        switch(selectedScore) {
            case 0: setPointsAndStart(5); break;
            case 1: setPointsAndStart(10); break;
            case 2: setPointsAndStart(15); break;
            case 3: setPointsAndStart(20); break;
            case 4: setPointsAndStart(30); break;
            case 5: setPointsAndStart(50); break;
            case 6: setPointsAndStart(100); break;
            case 7:
                String input = JOptionPane.showInputDialog(this,
                        "Entrez le nombre de points (1-999):", "Points personnalisés",
                        JOptionPane.PLAIN_MESSAGE);
                try {
                    int points = Integer.parseInt(input);
                    if (points > 0 && points < 1000) {
                        setPointsAndStart(points);
                    }
                } catch (NumberFormatException ex) {}
                break;
            case 8:
                GameSettings.isInfinite = true;
                startGame();
                break;
            case 9:
                choosingScore = false;
                break;
        }
    }

    private void setPointsAndStart(int points) {
        GameSettings.maxPoints = points;
        GameSettings.isInfinite = false;
        startGame();
    }

    private void startGame() {
        if (selectedMode == 0) {
            NameInputDialog p1Dialog = new NameInputDialog(this, "Joueur 1");
            p1Dialog.setVisible(true);
            if (p1Dialog.isConfirmed()) {
                GameSettings.player1Name = p1Dialog.getPlayerName();
                NameInputDialog p2Dialog = new NameInputDialog(this, "Joueur 2");
                p2Dialog.setVisible(true);
                if (p2Dialog.isConfirmed()) {
                    GameSettings.player2Name = p2Dialog.getPlayerName();
                    new Pong(selectedMode);
                    dispose();
                }
            }
        } else if (selectedMode == 1) {
            NameInputDialog dialog = new NameInputDialog(this, "Joueur");
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                GameSettings.player1Name = dialog.getPlayerName();
                GameSettings.player2Name = "CPU";
                new Pong(selectedMode);
                dispose();
            }
        } else {
            GameSettings.player1Name = "CPU 1";
            GameSettings.player2Name = "CPU 2";
            new Pong(selectedMode);
            dispose();
        }
    }
}
public class Pong extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_HEIGHT = 90;
    private static final int BALL_SIZE = 15;
    private static final int INITIAL_BALL_SPEED = 5;

    private int gameMode;
    private int paddle1Y = HEIGHT/2 - PADDLE_HEIGHT/2;
    private int paddle2Y = HEIGHT/2 - PADDLE_HEIGHT/2;
    private int ballX = WIDTH/2 - BALL_SIZE/2;
    private int ballY = HEIGHT/2 - BALL_SIZE/2;
    private float ballXSpeed = INITIAL_BALL_SPEED;
    private float ballYSpeed = INITIAL_BALL_SPEED;
    private int score1 = 0;
    private int score2 = 0;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean zPressed = false;
    private boolean sPressed = false;

    private Color backgroundColor = new Color(0, 0, 40);
    private Color lineColor = new Color(70, 70, 120);

    public Pong(int mode) {
        this.gameMode = mode;
        setTitle("Pong");
        setSize(WIDTH, HEIGHT);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(Color.BLACK);

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_UP: upPressed = true; break;
                    case KeyEvent.VK_DOWN: downPressed = true; break;
                    case KeyEvent.VK_Z: zPressed = true; break;
                    case KeyEvent.VK_S: sPressed = true; break;
                    case KeyEvent.VK_ESCAPE: returnToMenu(); break;
                    case KeyEvent.VK_F11: FullScreenUtil.toggleFullscreen(Pong.this); break;
                }
            }

            public void keyReleased(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_UP: upPressed = false; break;
                    case KeyEvent.VK_DOWN: downPressed = false; break;
                    case KeyEvent.VK_Z: zPressed = false; break;
                    case KeyEvent.VK_S: sPressed = false; break;
                }
            }
        });

        int delay = GameSettings.customSpeed > 0 ?
                (int)(16 / GameSettings.customSpeed) :
                16 / GameSettings.gameSpeed;

        javax.swing.Timer timer = new javax.swing.Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
                repaint();
            }
        });
        timer.start();

        FullScreenUtil.applyFullscreenState(this);
        setVisible(true);
    }

    private void returnToMenu() {
        dispose();
        new MenuPong();
    }

    private void update() {
        float speedMultiplier = GameSettings.customSpeed > 0 ?
                GameSettings.customSpeed : GameSettings.gameSpeed;

        float currentPaddleSpeed = 6 * speedMultiplier;

        if(gameMode != 2) {
            if (zPressed && paddle1Y > 0) paddle1Y -= currentPaddleSpeed;
            if (sPressed && paddle1Y < HEIGHT - PADDLE_HEIGHT) paddle1Y += currentPaddleSpeed;
        }
        if(gameMode == 0) {
            if (upPressed && paddle2Y > 0) paddle2Y -= currentPaddleSpeed;
            if (downPressed && paddle2Y < HEIGHT - PADDLE_HEIGHT) paddle2Y += currentPaddleSpeed;
        }

        updateCPU(speedMultiplier);

        ballX += ballXSpeed * speedMultiplier;
        ballY += ballYSpeed * speedMultiplier;

        checkCollisions();
        checkScore();
    }

    private void updateCPU(float speedMultiplier) {
        if(gameMode == 1 || gameMode == 2) {
            float cpuSpeed = (5 + (GameSettings.aiDifficulty * 2)) * speedMultiplier;
            int targetY2 = predictBallY(WIDTH - PADDLE_WIDTH);
            if(Math.abs(paddle2Y + PADDLE_HEIGHT/2 - targetY2) > cpuSpeed) {
                if(paddle2Y + PADDLE_HEIGHT/2 < targetY2) paddle2Y += cpuSpeed;
                if(paddle2Y + PADDLE_HEIGHT/2 > targetY2) paddle2Y -= cpuSpeed;
            }
        }
        if(gameMode == 2) {
            float cpuSpeed = (5 + (GameSettings.aiDifficulty * 2)) * speedMultiplier;
            int targetY1 = predictBallY(PADDLE_WIDTH);
            if(Math.abs(paddle1Y + PADDLE_HEIGHT/2 - targetY1) > cpuSpeed) {
                if(paddle1Y + PADDLE_HEIGHT/2 < targetY1) paddle1Y += cpuSpeed;
                if(paddle1Y + PADDLE_HEIGHT/2 > targetY1) paddle1Y -= cpuSpeed;
            }
        }
    }

    private int predictBallY(int x) {
        float time = (x - ballX) / ballXSpeed;
        float predictedY = ballY + (ballYSpeed * time);
        int bounces = (int)((predictedY) / HEIGHT);
        if(bounces % 2 == 0) {
            predictedY = predictedY % HEIGHT;
        } else {
            predictedY = HEIGHT - (predictedY % HEIGHT);
        }
        return (int)predictedY;
    }

    private void checkCollisions() {
        if (ballX <= PADDLE_WIDTH && ballY + BALL_SIZE >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT) {
            float angle = ((ballY + BALL_SIZE/2) - (paddle1Y + PADDLE_HEIGHT/2)) / (PADDLE_HEIGHT/2) * 1.0f;
            ballXSpeed = Math.abs(ballXSpeed) + 0.2f;
            ballYSpeed = INITIAL_BALL_SPEED * angle;
            ballX = PADDLE_WIDTH + 1;
        }

        if (ballX >= WIDTH - PADDLE_WIDTH - BALL_SIZE && ballY + BALL_SIZE >= paddle2Y && ballY <= paddle2Y + PADDLE_HEIGHT) {
            float angle = ((ballY + BALL_SIZE/2) - (paddle2Y + PADDLE_HEIGHT/2)) / (PADDLE_HEIGHT/2) * 1.0f;
            ballXSpeed = -(Math.abs(ballXSpeed) + 0.2f);
            ballYSpeed = INITIAL_BALL_SPEED * angle;
            ballX = WIDTH - PADDLE_WIDTH - BALL_SIZE - 1;
        }

        if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
            ballYSpeed = -ballYSpeed;
            ballY = ballY <= 0 ? 0 : HEIGHT - BALL_SIZE;
        }

        if (Math.abs(ballXSpeed) > 15) {
            ballXSpeed = 15 * Math.signum(ballXSpeed);
        }
        if (Math.abs(ballYSpeed) > 10) {
            ballYSpeed = 10 * Math.signum(ballYSpeed);
        }
    }

    private void checkScore() {
        if (ballX <= 0) {
            score2++;
            if (!GameSettings.isInfinite && score2 >= GameSettings.maxPoints) {
                gameOver(GameSettings.player2Name);
            } else {
                resetBall();
            }
        }
        if (ballX >= WIDTH - BALL_SIZE) {
            score1++;
            if (!GameSettings.isInfinite && score1 >= GameSettings.maxPoints) {
                gameOver(GameSettings.player1Name);
            } else {
                resetBall();
            }
        }
    }

    private void gameOver(String winner) {
        if (!GameSettings.player1Name.startsWith("CPU")) {
            GameSettings.addScore(GameSettings.player1Name, score1);
        }
        if (!GameSettings.player2Name.startsWith("CPU")) {
            GameSettings.addScore(GameSettings.player2Name, score2);
        }

        int choice = JOptionPane.showOptionDialog(
                this,
                winner + " gagne!\nScore final: " + score1 + " - " + score2,
                "Fin de partie",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Rejouer", "Menu principal"},
                "Menu principal"
        );

        if (choice == 0) {
            new GameMenu();
        } else {
            new MenuPong();
        }
        dispose();
    }

    private void resetBall() {
        ballX = WIDTH/2 - BALL_SIZE/2;
        ballY = HEIGHT/2 - BALL_SIZE/2;
        ballXSpeed = INITIAL_BALL_SPEED * (new Random().nextBoolean() ? 1 : -1);
        ballYSpeed = INITIAL_BALL_SPEED * (new Random().nextBoolean() ? 1 : -1);
    }

    public void paint(Graphics g) {
        Dimension size = getSize();
        Image buffer = createImage(size.width, size.height);
        Graphics2D bufferGraphics = (Graphics2D)buffer.getGraphics();

        bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float scaleX = size.width / (float)WIDTH;
        float scaleY = size.height / (float)HEIGHT;
        float scale = Math.min(scaleX, scaleY);

        int offsetX = (int)((size.width - WIDTH * scale) / 2);
        int offsetY = (int)((size.height - HEIGHT * scale) / 2);

        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, size.width, size.height);

        bufferGraphics.translate(offsetX, offsetY);
        bufferGraphics.scale(scale, scale);

        bufferGraphics.setColor(backgroundColor);
        bufferGraphics.fillRect(0, 0, WIDTH, HEIGHT);

        bufferGraphics.setColor(lineColor);
        for(int y = 0; y < HEIGHT; y += 30) {
            bufferGraphics.fillRoundRect(WIDTH/2 - 2, y, 4, 15, 2, 2);
        }

        bufferGraphics.setColor(GameSettings.SKINS[GameSettings.skinType][0]);
        bufferGraphics.fillRoundRect(0, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, 8, 8);
        bufferGraphics.fillRoundRect(WIDTH - PADDLE_WIDTH, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, 8, 8);

        bufferGraphics.setColor(GameSettings.SKINS[GameSettings.skinType][1]);
        bufferGraphics.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);

        bufferGraphics.setFont(new Font("Arial", Font.BOLD, 50));
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.drawString(String.valueOf(score1), WIDTH/4, 60);
        bufferGraphics.drawString(String.valueOf(score2), 3*WIDTH/4, 60);

        bufferGraphics.setFont(new Font("Arial", Font.PLAIN, 20));
        bufferGraphics.drawString(GameSettings.player1Name, 20, 30);
        bufferGraphics.drawString(GameSettings.player2Name, WIDTH - 150, 30);

        String scoreInfo = !GameSettings.isInfinite ?
                "Score maximum: " + GameSettings.maxPoints :
                "Mode infini";
        bufferGraphics.drawString(scoreInfo, WIDTH/2 - 80, 30);

        g.drawImage(buffer, 0, 0, null);
    }

    public static void main(String[] args) {
        new MenuPong();
    }
}