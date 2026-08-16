package Chess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ChessGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color IVORY = new Color(244, 229, 198);
    private static final Color WALNUT = new Color(158, 101, 62);
    private static final Color DEEP_WALNUT = new Color(72, 45, 31);
    private static final Color GOLD = new Color(223, 184, 76);
    private static final Color PANEL_CREAM = new Color(248, 239, 218);
    private static final Color PIECE_COLOR = new Color(38, 27, 21);
    private static final Color LEGAL_MOVE = new Color(181, 201, 128);
    private static final Color LEGAL_CAPTURE = new Color(205, 132, 104);
    private static final Color LAST_MOVE = new Color(213, 174, 91);
    private static final Color LEGAL_DOT = new Color(69, 96, 49);
    private static final String COMPUTER_MODE = "Play Computer";
    private static final String TWO_PLAYER_MODE = "Two Players";
    private static final String EASY = "Easy";
    private static final String MEDIUM = "Medium";
    private static final String HARD = "Hard";
    private static final String WARM_WALNUT = "Warm Walnut";
    private static final String CLASSIC_GREEN = "Classic Green";
    private static final String MIDNIGHT_BLUE = "Midnight Blue";

    private final Board board = new Board();
    private final JButton[][] buttons = new JButton[8][8];
    private final boolean[][] legalDestinations = new boolean[8][8];
    private final List<String> moveHistory = new ArrayList<>();
    private final List<GameSnapshot> undoStack = new ArrayList<>();
    private final List<Piece> capturedByWhite = new ArrayList<>();
    private final List<Piece> capturedByBlack = new ArrayList<>();
    private final Map<String, Integer> positionOccurrences = new HashMap<>();
    private final JPanel boardWithCoordinates =
            new JPanel(new BorderLayout());
    private final JComboBox<String> modeSelector =
            new JComboBox<>(new String[] {
                    COMPUTER_MODE,
                    TWO_PLAYER_MODE
            });
    private final JComboBox<String> difficultySelector =
            new JComboBox<>(new String[] {
                    EASY,
                    MEDIUM,
                    HARD
            });
    private final JTextArea moveHistoryArea = new JTextArea();
    private final JLabel capturedByWhiteLabel = new JLabel();
    private final JLabel capturedByBlackLabel = new JLabel();
    private final JLabel whiteClockLabel = new JLabel();
    private final JLabel blackClockLabel = new JLabel();
    private final Timer clockTimer;

    private final JLabel statusLabel =
            new JLabel(
                    "Your turn - you are White so go jeez",
                    SwingConstants.CENTER);

    private PieceColor currentTurn = PieceColor.WHITE;

    private int selectedRow = -1;
    private int selectedColumn = -1;
    private int lastFromRow = -1;
    private int lastFromColumn = -1;
    private int lastToRow = -1;
    private int lastToColumn = -1;
    private boolean gameOver = false;
    private boolean computerThinking = false;
    private boolean computerMode = true;
    private boolean updatingModeSelector = false;
    private boolean boardFlipped = false;
    private boolean gameStarted = false;
    private int halfMoveClock = 0;
    private int pieceFontSize = 50;
    private int soundVolume = 50;
    private int timeControlMinutes = 10;
    private long whiteTimeMilliseconds = 10 * 60 * 1000L;
    private long blackTimeMilliseconds = 10 * 60 * 1000L;
    private long lastClockTick;
    private String boardTheme = WARM_WALNUT;
    private String gameResult = "*";
    private Color lightSquareColor = IVORY;
    private Color darkSquareColor = WALNUT;
    private Color coordinateBackground = DEEP_WALNUT;
    private Color coordinateForeground = IVORY;
    private SwingWorker<ChessMove, Void> computerWorker;

    public ChessGUI() {
    	setTitle("Josh's Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JButton restartButton = new JButton("Restart");
        restartButton.setFont(new Font("Arial", Font.BOLD, 16));
        restartButton.setBackground(GOLD);
        restartButton.setForeground(DEEP_WALNUT);
        restartButton.addActionListener(event -> requestRestart());

        JButton undoButton = new JButton("Undo");
        undoButton.setFont(new Font("Arial", Font.BOLD, 14));
        undoButton.addActionListener(event -> undoMove());

        JButton saveButton = new JButton("Save Game");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.addActionListener(event -> saveGame());

        JButton loadButton = new JButton("Load Game");
        loadButton.setFont(new Font("Arial", Font.BOLD, 14));
        loadButton.addActionListener(event -> loadGame());

        JButton flipButton = new JButton("Flip Board");
        flipButton.setFont(new Font("Arial", Font.BOLD, 14));
        flipButton.addActionListener(event -> flipBoard());

        JButton settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("Arial", Font.BOLD, 14));
        settingsButton.addActionListener(event -> showSettings());

        JButton exportButton = new JButton("Export PGN");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.addActionListener(event -> exportPgn());

        modeSelector.setFont(new Font("Arial", Font.BOLD, 14));
        modeSelector.addActionListener(event -> {
            if (!updatingModeSelector) {
                boolean requestedComputerMode = COMPUTER_MODE.equals(
                        modeSelector.getSelectedItem());

                if (requestedComputerMode != computerMode
                        && !confirmReplaceCurrentGame(
                                "Changing modes will start a new game.")) {

                    updatingModeSelector = true;
                    modeSelector.setSelectedItem(
                            computerMode ? COMPUTER_MODE : TWO_PLAYER_MODE);
                    updatingModeSelector = false;
                    return;
                }

                computerMode = requestedComputerMode;
                difficultySelector.setEnabled(computerMode);
                restartGame();
            }
        });

        difficultySelector.setSelectedItem(MEDIUM);
        difficultySelector.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(PANEL_CREAM);

        JLabel titleLabel =
                new JLabel(
                        "Josh's Chess Game",
                        SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(DEEP_WALNUT);

        JPanel selectorsPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 4));
        selectorsPanel.setBackground(PANEL_CREAM);
        selectorsPanel.add(whiteClockLabel);
        selectorsPanel.add(new JLabel("Mode:"));
        selectorsPanel.add(modeSelector);
        selectorsPanel.add(new JLabel("Difficulty:"));
        selectorsPanel.add(difficultySelector);
        selectorsPanel.add(blackClockLabel);

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 6, 3));
        actionPanel.setBackground(PANEL_CREAM);
        actionPanel.add(restartButton);
        actionPanel.add(undoButton);
        actionPanel.add(flipButton);
        actionPanel.add(saveButton);
        actionPanel.add(loadButton);
        actionPanel.add(settingsButton);
        actionPanel.add(exportButton);

        JPanel controlsPanel = new JPanel(new GridLayout(2, 1));
        controlsPanel.add(selectorsPanel);
        controlsPanel.add(actionPanel);

        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(controlsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                JButton button = new JButton();

                button.setFont(
                        new Font(
                                "Segoe UI Symbol",
                                Font.PLAIN,
                                pieceFontSize));
                button.setForeground(PIECE_COLOR);
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);

                final int selectedButtonRow = row;
                final int selectedButtonColumn = column;

                button.addActionListener(event ->
                        handleSquareClick(
                                selectedButtonRow,
                                selectedButtonColumn));

                buttons[row][column] = button;
            }
        }

        rebuildBoardOrientation();

        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(DEEP_WALNUT);
        statusLabel.setBackground(PANEL_CREAM);
        statusLabel.setOpaque(true);

        moveHistoryArea.setEditable(false);
        moveHistoryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        moveHistoryArea.setForeground(DEEP_WALNUT);
        moveHistoryArea.setBackground(PANEL_CREAM);
        moveHistoryArea.setMargin(new Insets(8, 8, 8, 8));

        JLabel historyTitle = new JLabel(
                "Move History",
                SwingConstants.CENTER);
        historyTitle.setFont(new Font("Arial", Font.BOLD, 17));
        historyTitle.setForeground(IVORY);
        historyTitle.setBackground(DEEP_WALNUT);
        historyTitle.setOpaque(true);

        capturedByWhiteLabel.setFont(
                new Font("Segoe UI Symbol", Font.PLAIN, 17));
        capturedByBlackLabel.setFont(
                new Font("Segoe UI Symbol", Font.PLAIN, 17));
        capturedByWhiteLabel.setOpaque(true);
        capturedByBlackLabel.setOpaque(true);
        capturedByWhiteLabel.setBackground(PANEL_CREAM);
        capturedByBlackLabel.setBackground(PANEL_CREAM);
        capturedByWhiteLabel.setForeground(DEEP_WALNUT);
        capturedByBlackLabel.setForeground(DEEP_WALNUT);

        JPanel historyHeader = new JPanel(new GridLayout(3, 1));
        historyHeader.add(capturedByWhiteLabel);
        historyHeader.add(capturedByBlackLabel);
        historyHeader.add(historyTitle);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setPreferredSize(new Dimension(210, 0));
        historyPanel.add(historyHeader, BorderLayout.NORTH);
        historyPanel.add(
                new JScrollPane(moveHistoryArea),
                BorderLayout.CENTER);

        add(boardWithCoordinates, BorderLayout.CENTER);
        add(historyPanel, BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);

        configureClockLabel(whiteClockLabel);
        configureClockLabel(blackClockLabel);
        clockTimer = new Timer(200, event -> updateClock());
        clockTimer.start();

        positionOccurrences.put(
                board.getPositionKey(currentTurn),
                1);
        refreshCapturedPieces();
        refreshClocks();
        refreshBoard();

        fitWindowToUsableScreen();
        setResizable(true);
        setVisible(true);
    }

    private void fitWindowToUsableScreen() {
        Rectangle usableScreen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int screenPadding = 12;
        int availableWidth = Math.max(
                1,
                usableScreen.width - screenPadding * 2);
        int availableHeight = Math.max(
                1,
                usableScreen.height - screenPadding * 2);
        int windowWidth = Math.min(930, availableWidth);
        int windowHeight = Math.min(850, availableHeight);

        setSize(windowWidth, windowHeight);
        setLocation(
                usableScreen.x
                        + (usableScreen.width - windowWidth) / 2,
                usableScreen.y
                        + (usableScreen.height - windowHeight) / 2);
    }

    private void rebuildBoardOrientation() {
        JPanel orientedBoard = new JPanel(new GridLayout(8, 8));

        for (int displayRow = 0; displayRow < 8; displayRow++) {
            for (int displayColumn = 0;
                    displayColumn < 8;
                    displayColumn++) {

                int boardRow = boardFlipped
                        ? 7 - displayRow
                        : displayRow;
                int boardColumn = boardFlipped
                        ? 7 - displayColumn
                        : displayColumn;
                orientedBoard.add(buttons[boardRow][boardColumn]);
            }
        }

        JPanel boardRow = new JPanel(new BorderLayout());
        boardRow.add(createRankLabels(), BorderLayout.WEST);
        boardRow.add(orientedBoard, BorderLayout.CENTER);
        boardRow.add(createRankLabels(), BorderLayout.EAST);

        boardWithCoordinates.removeAll();
        boardWithCoordinates.add(createFileLabels(), BorderLayout.NORTH);
        boardWithCoordinates.add(boardRow, BorderLayout.CENTER);
        boardWithCoordinates.add(createFileLabels(), BorderLayout.SOUTH);
        boardWithCoordinates.revalidate();
        boardWithCoordinates.repaint();
    }

    private void flipBoard() {
        boardFlipped = !boardFlipped;
        rebuildBoardOrientation();
        refreshBoard();
    }

    private JPanel createFileLabels() {
        JPanel labels = new JPanel(new GridLayout(1, 8));

        for (int index = 0; index < 8; index++) {
            char file = (char) (
                    boardFlipped ? 'h' - index : 'a' + index);
            labels.add(createCoordinateLabel(String.valueOf(file)));
        }

        JPanel row = new JPanel(new BorderLayout());
        row.add(createCoordinateSpacer(), BorderLayout.WEST);
        row.add(labels, BorderLayout.CENTER);
        row.add(createCoordinateSpacer(), BorderLayout.EAST);
        row.setPreferredSize(new Dimension(0, 26));

        return row;
    }

    private JPanel createRankLabels() {
        JPanel labels = new JPanel(new GridLayout(8, 1));
        labels.setPreferredSize(new Dimension(26, 0));

        for (int index = 0; index < 8; index++) {
            int rank = boardFlipped ? index + 1 : 8 - index;
            labels.add(createCoordinateLabel(String.valueOf(rank)));
        }

        return labels;
    }

    private JPanel createCoordinateSpacer() {
        JPanel spacer = new JPanel();
        spacer.setBackground(coordinateBackground);
        spacer.setPreferredSize(new Dimension(26, 26));
        return spacer;
    }

    private JLabel createCoordinateLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(coordinateForeground);
        label.setBackground(coordinateBackground);
        label.setOpaque(true);
        return label;
    }

    private void requestRestart() {
        if (confirmReplaceCurrentGame(
                "Start a new game and discard the current position?")) {

            restartGame();
        }
    }

    private boolean confirmReplaceCurrentGame(String message) {
        if (!gameStarted && moveHistory.isEmpty()) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirm New Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        return choice == JOptionPane.YES_OPTION;
    }

    private void restartGame() {
        cancelComputerTurn();

        board.reset();

        currentTurn = PieceColor.WHITE;
        selectedRow = -1;
        selectedColumn = -1;
        lastFromRow = -1;
        lastFromColumn = -1;
        lastToRow = -1;
        lastToColumn = -1;
        gameOver = false;
        computerThinking = false;
        gameStarted = false;
        gameResult = "*";
        halfMoveClock = 0;
        moveHistory.clear();
        undoStack.clear();
        capturedByWhite.clear();
        capturedByBlack.clear();
        positionOccurrences.clear();
        positionOccurrences.put(
                board.getPositionKey(currentTurn),
                1);
        whiteTimeMilliseconds = timeControlMinutes * 60_000L;
        blackTimeMilliseconds = timeControlMinutes * 60_000L;
        lastClockTick = System.currentTimeMillis();
        clearLegalDestinations();

        updateGameStatus();
        refreshMoveHistory();
        refreshCapturedPieces();
        refreshClocks();
        refreshBoard();
    }

    private void cancelComputerTurn() {
        if (computerWorker != null) {
            computerWorker.cancel(true);
            computerWorker = null;
        }

        computerThinking = false;
    }

    private GameSnapshot createSnapshot() {
        return new GameSnapshot(
                board.copy(),
                currentTurn,
                lastFromRow,
                lastFromColumn,
                lastToRow,
                lastToColumn,
                moveHistory.size(),
                capturedByWhite.size(),
                capturedByBlack.size(),
                halfMoveClock,
                new HashMap<>(positionOccurrences),
                whiteTimeMilliseconds,
                blackTimeMilliseconds,
                gameStarted,
                gameResult);
    }

    private void undoMove() {
        cancelComputerTurn();

        if (undoStack.isEmpty()) {
            statusLabel.setText("There are no moves to undo.");
            return;
        }

        restoreLastSnapshot();

        if (computerMode
                && currentTurn == PieceColor.BLACK
                && !undoStack.isEmpty()) {

            restoreLastSnapshot();
        }

        selectedRow = -1;
        selectedColumn = -1;
        gameOver = false;
        clearLegalDestinations();
        updateGameStatus();
        refreshMoveHistory();
        refreshCapturedPieces();
        refreshClocks();
        refreshBoard();
    }

    private void restoreLastSnapshot() {
        GameSnapshot snapshot = undoStack.remove(undoStack.size() - 1);
        board.restoreFrom(snapshot.board);
        currentTurn = snapshot.currentTurn;
        lastFromRow = snapshot.lastFromRow;
        lastFromColumn = snapshot.lastFromColumn;
        lastToRow = snapshot.lastToRow;
        lastToColumn = snapshot.lastToColumn;
        halfMoveClock = snapshot.halfMoveClock;
        positionOccurrences.clear();
        positionOccurrences.putAll(snapshot.positionOccurrences);
        whiteTimeMilliseconds = snapshot.whiteTimeMilliseconds;
        blackTimeMilliseconds = snapshot.blackTimeMilliseconds;
        gameStarted = snapshot.gameStarted;
        gameResult = snapshot.gameResult;
        lastClockTick = System.currentTimeMillis();

        while (moveHistory.size() > snapshot.historySize) {
            moveHistory.remove(moveHistory.size() - 1);
        }

        while (capturedByWhite.size() > snapshot.capturedByWhiteSize) {
            capturedByWhite.remove(capturedByWhite.size() - 1);
        }

        while (capturedByBlack.size() > snapshot.capturedByBlackSize) {
            capturedByBlack.remove(capturedByBlack.size() - 1);
        }
    }

    private void showLegalDestinations(int fromRow, int fromColumn) {
        clearLegalDestinations();

        for (ChessMove move : board.getLegalMoves(currentTurn)) {
            if (move.getFromRow() == fromRow
                    && move.getFromColumn() == fromColumn) {

                legalDestinations[move.getToRow()][move.getToColumn()] = true;
            }
        }
    }

    private void clearLegalDestinations() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                legalDestinations[row][column] = false;
            }
        }
    }

    private void setLastMove(ChessMove move) {
        lastFromRow = move.getFromRow();
        lastFromColumn = move.getFromColumn();
        lastToRow = move.getToRow();
        lastToColumn = move.getToColumn();
    }

    private String createMoveNotation(
            Board positionBeforeMove,
            ChessMove move,
            PieceColor movingColor) {

        Piece movingPiece = positionBeforeMove.getPiece(
                move.getFromRow(),
                move.getFromColumn());
        Piece capturedPiece = positionBeforeMove.getPiece(
                move.getToRow(),
                move.getToColumn());

        if (movingPiece == null) {
            return move.toString();
        }

        String notation;

        if (movingPiece.getType() == PieceType.KING
                && Math.abs(
                        move.getToColumn() - move.getFromColumn()) == 2) {

            notation = move.getToColumn() == 6 ? "O-O" : "O-O-O";
        } else {
            boolean capture = capturedPiece != null
                    || (movingPiece.getType() == PieceType.PAWN
                    && move.getFromColumn() != move.getToColumn());

            if (movingPiece.getType() == PieceType.PAWN) {
                StringBuilder pawnMove = new StringBuilder();

                if (capture) {
                    pawnMove.append((char) ('a' + move.getFromColumn()));
                    pawnMove.append('x');
                }

                pawnMove.append(move.getToPosition());

                if (move.getToRow() == 0 || move.getToRow() == 7) {
                    Piece promotedPiece = board.getPiece(
                            move.getToRow(),
                            move.getToColumn());
                    pawnMove.append('=').append(
                            promotedPiece == null
                            ? "Q"
                            : pieceLetter(promotedPiece.getType()));
                }

                notation = pawnMove.toString();
            } else {
                notation = pieceLetter(movingPiece.getType())
                        + (capture ? "x" : "")
                        + move.getToPosition();
            }
        }

        PieceColor nextTurn = opposite(movingColor);

        if (board.isCheckmate(nextTurn)) {
            notation += "#";
        } else if (board.isInCheck(nextTurn)) {
            notation += "+";
        }

        return notation;
    }

    private String pieceLetter(PieceType type) {
        switch (type) {
            case KING:
                return "K";
            case QUEEN:
                return "Q";
            case ROOK:
                return "R";
            case BISHOP:
                return "B";
            case KNIGHT:
                return "N";
            default:
                return "";
        }
    }

    private void refreshMoveHistory() {
        StringBuilder text = new StringBuilder();

        for (int index = 0; index < moveHistory.size(); index += 2) {
            text.append(index / 2 + 1).append(". ");
            text.append(moveHistory.get(index));

            if (index + 1 < moveHistory.size()) {
                text.append("   ").append(moveHistory.get(index + 1));
            }

            text.append(System.lineSeparator());
        }

        moveHistoryArea.setText(text.toString());
        moveHistoryArea.setCaretPosition(moveHistoryArea.getDocument().getLength());
    }

    private int getComputerSearchDepth() {
        String difficulty = (String) difficultySelector.getSelectedItem();

        if (EASY.equals(difficulty)) {
            return 1;
        }

        if (HARD.equals(difficulty)) {
            return 3;
        }

        return 2;
    }

    private PieceType choosePromotionPiece() {
        String[] options = { "Queen", "Rook", "Bishop", "Knight" };
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose the pawn's promotion piece:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 1:
                return PieceType.ROOK;
            case 2:
                return PieceType.BISHOP;
            case 3:
                return PieceType.KNIGHT;
            case 0:
            default:
                return PieceType.QUEEN;
        }
    }

    private Piece getCapturedPiece(Board positionBeforeMove, ChessMove move) {
        Piece capturedPiece = positionBeforeMove.getPiece(
                move.getToRow(),
                move.getToColumn());
        Piece movingPiece = positionBeforeMove.getPiece(
                move.getFromRow(),
                move.getFromColumn());

        if (capturedPiece == null
                && movingPiece != null
                && movingPiece.getType() == PieceType.PAWN
                && move.getFromColumn() != move.getToColumn()) {

            capturedPiece = positionBeforeMove.getPiece(
                    move.getFromRow(),
                    move.getToColumn());
        }

        return capturedPiece;
    }

    private void addCapturedPiece(
            Piece capturedPiece,
            PieceColor capturingColor) {

        if (capturedPiece == null) {
            return;
        }

        if (capturingColor == PieceColor.WHITE) {
            capturedByWhite.add(capturedPiece);
        } else {
            capturedByBlack.add(capturedPiece);
        }
    }

    private void refreshCapturedPieces() {
        capturedByWhiteLabel.setText(
                " White captured: " + capturedPieceSymbols(capturedByWhite));
        capturedByBlackLabel.setText(
                " Black captured: " + capturedPieceSymbols(capturedByBlack));
    }

    private String capturedPieceSymbols(List<Piece> pieces) {
        StringBuilder symbols = new StringBuilder();
        PieceType[] displayOrder = {
                PieceType.QUEEN,
                PieceType.ROOK,
                PieceType.BISHOP,
                PieceType.KNIGHT,
                PieceType.PAWN
        };

        for (PieceType type : displayOrder) {
            int count = 0;
            Piece example = null;

            for (Piece piece : pieces) {
                if (piece.getType() == type) {
                    count++;
                    example = piece;
                }
            }

            if (count > 0 && example != null) {
                symbols.append(getPieceSymbol(example));

                if (count > 1) {
                    symbols.append('\u00d7').append(count);
                }

                symbols.append(' ');
            }
        }

        return symbols.length() == 0 ? "-" : symbols.toString();
    }

    private void updateDrawTracking(Board positionBeforeMove, ChessMove move) {
        Piece movingPiece = positionBeforeMove.getPiece(
                move.getFromRow(),
                move.getFromColumn());
        Piece capturedPiece = getCapturedPiece(positionBeforeMove, move);

        if ((movingPiece != null
                && movingPiece.getType() == PieceType.PAWN)
                || capturedPiece != null) {

            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        String positionKey = board.getPositionKey(currentTurn);
        positionOccurrences.merge(positionKey, 1, Integer::sum);
    }

    private boolean isThreefoldRepetition() {
        return positionOccurrences.getOrDefault(
                board.getPositionKey(currentTurn),
                0) >= 3;
    }

    private void configureClockLabel(JLabel label) {
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        label.setForeground(IVORY);
        label.setBackground(DEEP_WALNUT);
        label.setOpaque(true);
    }

    private void updateClock() {
        long now = System.currentTimeMillis();

        if (!gameStarted || gameOver || timeControlMinutes == 0) {
            lastClockTick = now;
            refreshClocks();
            return;
        }

        long elapsed = Math.max(0, now - lastClockTick);
        lastClockTick = now;

        if (currentTurn == PieceColor.WHITE) {
            whiteTimeMilliseconds -= elapsed;

            if (whiteTimeMilliseconds <= 0) {
                whiteTimeMilliseconds = 0;
                handleTimeout(PieceColor.WHITE);
            }
        } else {
            blackTimeMilliseconds -= elapsed;

            if (blackTimeMilliseconds <= 0) {
                blackTimeMilliseconds = 0;
                handleTimeout(PieceColor.BLACK);
            }
        }

        refreshClocks();
    }

    private void handleTimeout(PieceColor losingColor) {
        gameOver = true;
        cancelComputerTurn();
        PieceColor winner = opposite(losingColor);
        gameResult = winner == PieceColor.WHITE ? "1-0" : "0-1";

        if (computerMode) {
            statusLabel.setText(
                    winner == PieceColor.WHITE
                    ? "TIME! You win."
                    : "TIME! The computer wins.");
        } else {
            statusLabel.setText("TIME! " + winner + " wins.");
        }

        SoundPlayer.play(SoundPlayer.Effect.GAME_OVER, soundVolume);
    }

    private void refreshClocks() {
        if (timeControlMinutes == 0) {
            whiteClockLabel.setText(" White --:-- ");
            blackClockLabel.setText(" Black --:-- ");
            whiteClockLabel.setBackground(DEEP_WALNUT);
            blackClockLabel.setBackground(DEEP_WALNUT);
            whiteClockLabel.setForeground(IVORY);
            blackClockLabel.setForeground(IVORY);
            return;
        }

        whiteClockLabel.setText(
                " White " + formatClock(whiteTimeMilliseconds) + " ");
        blackClockLabel.setText(
                " Black " + formatClock(blackTimeMilliseconds) + " ");
        whiteClockLabel.setBackground(
                currentTurn == PieceColor.WHITE && gameStarted && !gameOver
                ? GOLD
                : DEEP_WALNUT);
        blackClockLabel.setBackground(
                currentTurn == PieceColor.BLACK && gameStarted && !gameOver
                ? GOLD
                : DEEP_WALNUT);
        whiteClockLabel.setForeground(
                currentTurn == PieceColor.WHITE && gameStarted && !gameOver
                ? DEEP_WALNUT
                : IVORY);
        blackClockLabel.setForeground(
                currentTurn == PieceColor.BLACK && gameStarted && !gameOver
                ? DEEP_WALNUT
                : IVORY);
    }

    private String formatClock(long milliseconds) {
        long totalSeconds = Math.max(0, milliseconds + 999) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void playMoveSound(boolean capture) {
        if (gameOver) {
            SoundPlayer.play(SoundPlayer.Effect.GAME_OVER, soundVolume);
        } else if (board.isInCheck(currentTurn)) {
            SoundPlayer.play(SoundPlayer.Effect.CHECK, soundVolume);
        } else if (capture) {
            SoundPlayer.play(SoundPlayer.Effect.CAPTURE, soundVolume);
        } else {
            SoundPlayer.play(SoundPlayer.Effect.MOVE, soundVolume);
        }
    }

    private void showSettings() {
        JComboBox<String> themeChoice = new JComboBox<>(new String[] {
                WARM_WALNUT,
                CLASSIC_GREEN,
                MIDNIGHT_BLUE
        });
        themeChoice.setSelectedItem(boardTheme);

        JComboBox<String> pieceSizeChoice = new JComboBox<>(new String[] {
                "Small",
                "Medium",
                "Large"
        });
        pieceSizeChoice.setSelectedItem(
                pieceFontSize <= 44
                ? "Small"
                : pieceFontSize >= 56 ? "Large" : "Medium");

        JComboBox<String> clockChoice = new JComboBox<>(new String[] {
                "Off",
                "5 minutes",
                "10 minutes",
                "15 minutes"
        });
        clockChoice.setSelectedItem(clockSettingName(timeControlMinutes));

        JComboBox<String> volumeChoice = new JComboBox<>(new String[] {
                "Off",
                "Low",
                "Medium",
                "High"
        });
        volumeChoice.setSelectedItem(volumeSettingName(soundVolume));

        JPanel settingsPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        settingsPanel.add(new JLabel("Board theme:"));
        settingsPanel.add(themeChoice);
        settingsPanel.add(new JLabel("Piece size:"));
        settingsPanel.add(pieceSizeChoice);
        settingsPanel.add(new JLabel("Chess clock:"));
        settingsPanel.add(clockChoice);
        settingsPanel.add(new JLabel("Sound volume:"));
        settingsPanel.add(volumeChoice);

        int choice = JOptionPane.showConfirmDialog(
                this,
                settingsPanel,
                "Game Settings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        int requestedClock = clockMinutesFromSetting(
                (String) clockChoice.getSelectedItem());

        if (requestedClock != timeControlMinutes
                && !confirmReplaceCurrentGame(
                        "Changing the clock will start a new game.")) {

            return;
        }

        boardTheme = (String) themeChoice.getSelectedItem();
        pieceFontSize = pieceSizeFromSetting(
                (String) pieceSizeChoice.getSelectedItem());
        soundVolume = volumeFromSetting(
                (String) volumeChoice.getSelectedItem());
        boolean clockChanged = requestedClock != timeControlMinutes;
        timeControlMinutes = requestedClock;

        applyTheme();
        applyVisualSettings();

        if (clockChanged) {
            restartGame();
        } else {
            refreshClocks();
            refreshBoard();
        }
    }

    private void applyTheme() {
        if (CLASSIC_GREEN.equals(boardTheme)) {
            lightSquareColor = new Color(238, 238, 210);
            darkSquareColor = new Color(118, 150, 86);
            coordinateBackground = new Color(61, 86, 48);
            coordinateForeground = new Color(244, 244, 224);
        } else if (MIDNIGHT_BLUE.equals(boardTheme)) {
            lightSquareColor = new Color(202, 216, 230);
            darkSquareColor = new Color(79, 103, 137);
            coordinateBackground = new Color(34, 48, 69);
            coordinateForeground = new Color(230, 238, 247);
        } else {
            boardTheme = WARM_WALNUT;
            lightSquareColor = IVORY;
            darkSquareColor = WALNUT;
            coordinateBackground = DEEP_WALNUT;
            coordinateForeground = IVORY;
        }
    }

    private void applyVisualSettings() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                buttons[row][column].setFont(new Font(
                        "Segoe UI Symbol",
                        Font.PLAIN,
                        pieceFontSize));
            }
        }

        rebuildBoardOrientation();
        refreshBoard();
    }

    private int pieceSizeFromSetting(String setting) {
        if ("Small".equals(setting)) {
            return 44;
        }

        if ("Large".equals(setting)) {
            return 56;
        }

        return 50;
    }

    private String clockSettingName(int minutes) {
        return minutes == 0 ? "Off" : minutes + " minutes";
    }

    private int clockMinutesFromSetting(String setting) {
        if ("5 minutes".equals(setting)) {
            return 5;
        }

        if ("10 minutes".equals(setting)) {
            return 10;
        }

        if ("15 minutes".equals(setting)) {
            return 15;
        }

        return 0;
    }

    private String volumeSettingName(int volume) {
        if (volume <= 0) {
            return "Off";
        }

        if (volume <= 30) {
            return "Low";
        }

        if (volume >= 75) {
            return "High";
        }

        return "Medium";
    }

    private int volumeFromSetting(String setting) {
        if ("Low".equals(setting)) {
            return 25;
        }

        if ("Medium".equals(setting)) {
            return 50;
        }

        if ("High".equals(setting)) {
            return 85;
        }

        return 0;
    }

    private void exportPgn() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Portable Game Notation (*.pgn)",
                "pgn"));
        fileChooser.setSelectedFile(new File(
                System.getProperty("user.home"),
                "JoshChessGame.pgn"));

        if (fileChooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File pgnFile = addFileExtension(
                fileChooser.getSelectedFile(),
                ".pgn");

        if (pgnFile.exists()) {
            int overwriteChoice = JOptionPane.showConfirmDialog(
                    this,
                    "Replace the existing PGN file?",
                    "Confirm Export",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (overwriteChoice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                pgnFile.toPath(),
                StandardCharsets.UTF_8)) {

            writer.write(buildPgn());
            statusLabel.setText("PGN exported as " + pgnFile.getName() + ".");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The PGN could not be exported:\n"
                    + exception.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildPgn() {
        String date = LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String blackPlayer = computerMode
                ? "Computer (" + difficultySelector.getSelectedItem() + ")"
                : "Player 2";
        StringBuilder pgn = new StringBuilder();
        pgn.append("[Event \"Josh's Chess Game\"]\n");
        pgn.append("[Site \"Local\"]\n");
        pgn.append("[Date \"").append(date).append("\"]\n");
        pgn.append("[Round \"-\"]\n");
        pgn.append("[White \"Player 1\"]\n");
        pgn.append("[Black \"").append(blackPlayer).append("\"]\n");
        pgn.append("[Result \"").append(gameResult).append("\"]\n\n");

        for (int index = 0; index < moveHistory.size(); index += 2) {
            pgn.append(index / 2 + 1).append(". ");
            pgn.append(moveHistory.get(index));

            if (index + 1 < moveHistory.size()) {
                pgn.append(' ').append(moveHistory.get(index + 1));
            }

            pgn.append(' ');
        }

        pgn.append(gameResult).append(System.lineSeparator());
        return pgn.toString();
    }

    private File addFileExtension(File file, String extension) {
        if (file.getName().toLowerCase().endsWith(extension)) {
            return file;
        }

        return new File(file.getParentFile(), file.getName() + extension);
    }

    private void saveGame() {
        JFileChooser fileChooser = createSaveFileChooser();
        int choice = fileChooser.showSaveDialog(this);

        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveFile = addSaveExtension(fileChooser.getSelectedFile());

        if (saveFile.exists()) {
            int overwriteChoice = JOptionPane.showConfirmDialog(
                    this,
                    "Replace the existing saved game?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (overwriteChoice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        updateClock();
        Properties properties = new Properties();
        properties.setProperty("save.version", "3");
        properties.setProperty("game.turn", currentTurn.name());
        properties.setProperty(
                "game.mode",
                computerMode ? COMPUTER_MODE : TWO_PLAYER_MODE);
        properties.setProperty(
                "game.difficulty",
                String.valueOf(difficultySelector.getSelectedItem()));
        properties.setProperty("game.result", gameResult);
        properties.setProperty(
                "game.halfMoveClock",
                String.valueOf(halfMoveClock));
        properties.setProperty(
                "game.started",
                String.valueOf(gameStarted));
        properties.setProperty("settings.theme", boardTheme);
        properties.setProperty(
                "settings.pieceSize",
                String.valueOf(pieceFontSize));
        properties.setProperty(
                "settings.soundVolume",
                String.valueOf(soundVolume));
        properties.setProperty(
                "settings.timeControlMinutes",
                String.valueOf(timeControlMinutes));
        properties.setProperty(
                "clock.whiteMilliseconds",
                String.valueOf(whiteTimeMilliseconds));
        properties.setProperty(
                "clock.blackMilliseconds",
                String.valueOf(blackTimeMilliseconds));
        properties.setProperty(
                "ui.boardFlipped",
                String.valueOf(boardFlipped));
        properties.setProperty("ui.lastFromRow", String.valueOf(lastFromRow));
        properties.setProperty(
                "ui.lastFromColumn",
                String.valueOf(lastFromColumn));
        properties.setProperty("ui.lastToRow", String.valueOf(lastToRow));
        properties.setProperty(
                "ui.lastToColumn",
                String.valueOf(lastToColumn));
        properties.setProperty(
                "history.count",
                String.valueOf(moveHistory.size()));

        for (int index = 0; index < moveHistory.size(); index++) {
            properties.setProperty(
                    "history." + index,
                    moveHistory.get(index));
        }

        saveCapturedPieces(properties, "captured.white", capturedByWhite);
        saveCapturedPieces(properties, "captured.black", capturedByBlack);
        properties.setProperty(
                "repetition.count",
                String.valueOf(positionOccurrences.size()));

        int repetitionIndex = 0;

        for (Map.Entry<String, Integer> entry
                : positionOccurrences.entrySet()) {

            properties.setProperty(
                    "repetition." + repetitionIndex + ".key",
                    entry.getKey());
            properties.setProperty(
                    "repetition." + repetitionIndex + ".value",
                    String.valueOf(entry.getValue()));
            repetitionIndex++;
        }

        board.saveState(properties);

        try (FileOutputStream output = new FileOutputStream(saveFile)) {
            properties.store(output, "Josh's Chess Game save file");
            statusLabel.setText(
                    "Game saved as " + saveFile.getName() + ".");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The game could not be saved:\n"
                    + exception.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGame() {
        JFileChooser fileChooser = createSaveFileChooser();
        int choice = fileChooser.showOpenDialog(this);

        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        if (!confirmReplaceCurrentGame(
                "Loading a saved game will replace the current game.")) {

            return;
        }

        File saveFile = fileChooser.getSelectedFile();
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(saveFile)) {
            properties.load(input);

            String saveVersion = properties.getProperty("save.version");

            if (!"1".equals(saveVersion)
                    && !"2".equals(saveVersion)
                    && !"3".equals(saveVersion)) {
                throw new IllegalArgumentException(
                        "This saved-game version is not supported.");
            }

            String loadedTurnValue = properties.getProperty("game.turn");

            if (loadedTurnValue == null) {
                throw new IllegalArgumentException(
                        "The saved game does not specify whose turn it is.");
            }

            PieceColor loadedTurn = PieceColor.valueOf(loadedTurnValue);
            String loadedMode = properties.getProperty("game.mode");

            if (!COMPUTER_MODE.equals(loadedMode)
                    && !TWO_PLAYER_MODE.equals(loadedMode)) {
                throw new IllegalArgumentException(
                        "The saved game has an invalid play mode.");
            }

            String loadedDifficulty = MEDIUM;
            List<String> loadedHistory = new ArrayList<>();
            int loadedLastFromRow = -1;
            int loadedLastFromColumn = -1;
            int loadedLastToRow = -1;
            int loadedLastToColumn = -1;
            String loadedTheme = WARM_WALNUT;
            int loadedPieceSize = 50;
            int loadedSoundVolume = 50;
            int loadedTimeControl = 10;
            long loadedWhiteTime = 10 * 60 * 1000L;
            long loadedBlackTime = 10 * 60 * 1000L;
            boolean loadedGameStarted = false;
            boolean loadedBoardFlipped = false;
            int loadedHalfMoveClock = 0;
            String loadedGameResult = "*";
            List<Piece> loadedCapturedByWhite = new ArrayList<>();
            List<Piece> loadedCapturedByBlack = new ArrayList<>();
            Map<String, Integer> loadedPositionOccurrences = new HashMap<>();

            if ("2".equals(saveVersion) || "3".equals(saveVersion)) {
                loadedDifficulty = properties.getProperty(
                        "game.difficulty",
                        MEDIUM);

                if (!EASY.equals(loadedDifficulty)
                        && !MEDIUM.equals(loadedDifficulty)
                        && !HARD.equals(loadedDifficulty)) {

                    throw new IllegalArgumentException(
                            "The saved game has an invalid difficulty.");
                }

                int historyCount = readSavedInt(
                        properties,
                        "history.count",
                        0,
                        10_000,
                        0);

                for (int index = 0; index < historyCount; index++) {
                    String move = properties.getProperty("history." + index);

                    if (move == null) {
                        throw new IllegalArgumentException(
                                "The saved move history is incomplete.");
                    }

                    loadedHistory.add(move);
                }

                loadedLastFromRow = readSavedInt(
                        properties, "ui.lastFromRow", -1, 7, -1);
                loadedLastFromColumn = readSavedInt(
                        properties, "ui.lastFromColumn", -1, 7, -1);
                loadedLastToRow = readSavedInt(
                        properties, "ui.lastToRow", -1, 7, -1);
                loadedLastToColumn = readSavedInt(
                        properties, "ui.lastToColumn", -1, 7, -1);
            }

            if ("3".equals(saveVersion)) {
                loadedTheme = properties.getProperty(
                        "settings.theme",
                        WARM_WALNUT);

                if (!WARM_WALNUT.equals(loadedTheme)
                        && !CLASSIC_GREEN.equals(loadedTheme)
                        && !MIDNIGHT_BLUE.equals(loadedTheme)) {

                    throw new IllegalArgumentException(
                            "The saved game has an invalid board theme.");
                }

                loadedPieceSize = readSavedInt(
                        properties, "settings.pieceSize", 40, 60, 50);
                loadedSoundVolume = readSavedInt(
                        properties, "settings.soundVolume", 0, 100, 50);
                loadedTimeControl = readSavedInt(
                        properties,
                        "settings.timeControlMinutes",
                        0,
                        60,
                        10);

                if (loadedTimeControl != 0
                        && loadedTimeControl != 5
                        && loadedTimeControl != 10
                        && loadedTimeControl != 15) {

                    throw new IllegalArgumentException(
                            "The saved game has an invalid clock setting.");
                }
                loadedWhiteTime = readSavedLong(
                        properties,
                        "clock.whiteMilliseconds",
                        0,
                        60 * 60 * 1000L,
                        loadedTimeControl * 60_000L);
                loadedBlackTime = readSavedLong(
                        properties,
                        "clock.blackMilliseconds",
                        0,
                        60 * 60 * 1000L,
                        loadedTimeControl * 60_000L);
                loadedGameStarted = Boolean.parseBoolean(
                        properties.getProperty("game.started", "false"));
                loadedBoardFlipped = Boolean.parseBoolean(
                        properties.getProperty("ui.boardFlipped", "false"));
                loadedHalfMoveClock = readSavedInt(
                        properties, "game.halfMoveClock", 0, 100_000, 0);
                loadedGameResult = properties.getProperty(
                        "game.result",
                        "*");
                loadedCapturedByWhite = loadCapturedPieces(
                        properties,
                        "captured.white");
                loadedCapturedByBlack = loadCapturedPieces(
                        properties,
                        "captured.black");

                int repetitionCount = readSavedInt(
                        properties,
                        "repetition.count",
                        0,
                        100_000,
                        0);

                for (int index = 0; index < repetitionCount; index++) {
                    String key = properties.getProperty(
                            "repetition." + index + ".key");
                    int value = readSavedInt(
                            properties,
                            "repetition." + index + ".value",
                            1,
                            100_000,
                            1);

                    if (key == null) {
                        throw new IllegalArgumentException(
                                "The saved repetition history is incomplete.");
                    }

                    loadedPositionOccurrences.put(key, value);
                }
            } else {
                loadedGameStarted = !loadedHistory.isEmpty();
            }

            Board validationBoard = new Board();
            validationBoard.loadState(properties);

            cancelComputerTurn();
            board.loadState(properties);
            currentTurn = loadedTurn;
            computerMode = COMPUTER_MODE.equals(loadedMode);
            selectedRow = -1;
            selectedColumn = -1;
            lastFromRow = loadedLastFromRow;
            lastFromColumn = loadedLastFromColumn;
            lastToRow = loadedLastToRow;
            lastToColumn = loadedLastToColumn;
            gameOver = false;
            boardTheme = loadedTheme;
            pieceFontSize = loadedPieceSize;
            soundVolume = loadedSoundVolume;
            timeControlMinutes = loadedTimeControl;
            whiteTimeMilliseconds = loadedWhiteTime;
            blackTimeMilliseconds = loadedBlackTime;
            gameStarted = loadedGameStarted;
            boardFlipped = loadedBoardFlipped;
            halfMoveClock = loadedHalfMoveClock;
            gameResult = loadedGameResult;
            lastClockTick = System.currentTimeMillis();
            moveHistory.clear();
            moveHistory.addAll(loadedHistory);
            capturedByWhite.clear();
            capturedByWhite.addAll(loadedCapturedByWhite);
            capturedByBlack.clear();
            capturedByBlack.addAll(loadedCapturedByBlack);
            positionOccurrences.clear();
            positionOccurrences.putAll(loadedPositionOccurrences);

            if (positionOccurrences.isEmpty()) {
                positionOccurrences.put(
                        board.getPositionKey(currentTurn),
                        1);
            }

            undoStack.clear();
            clearLegalDestinations();

            updatingModeSelector = true;
            modeSelector.setSelectedItem(loadedMode);
            updatingModeSelector = false;
            difficultySelector.setSelectedItem(loadedDifficulty);
            difficultySelector.setEnabled(computerMode);

            applyTheme();
            applyVisualSettings();
            updateGameStatus();
            refreshMoveHistory();
            refreshCapturedPieces();
            refreshClocks();
            refreshBoard();

            if (!gameOver
                    && computerMode
                    && currentTurn == PieceColor.BLACK) {
                startComputerTurn();
            }
        } catch (IOException | IllegalArgumentException exception) {
            updatingModeSelector = false;
            JOptionPane.showMessageDialog(
                    this,
                    "The game could not be loaded:\n"
                    + exception.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser createSaveFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Josh's Chess saved games (*.jchess)",
                "jchess"));
        fileChooser.setSelectedFile(new File(
                System.getProperty("user.home"),
                "JoshChessGame.jchess"));
        return fileChooser;
    }

    private File addSaveExtension(File file) {
        if (file.getName().toLowerCase().endsWith(".jchess")) {
            return file;
        }

        return new File(file.getParentFile(), file.getName() + ".jchess");
    }

    private int readSavedInt(
            Properties properties,
            String key,
            int minimum,
            int maximum,
            int defaultValue) {

        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            int number = Integer.parseInt(value);

            if (number < minimum || number > maximum) {
                throw new IllegalArgumentException(
                        "The saved game contains an invalid " + key + ".");
            }

            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "The saved game contains an invalid " + key + ".",
                    exception);
        }
    }

    private long readSavedLong(
            Properties properties,
            String key,
            long minimum,
            long maximum,
            long defaultValue) {

        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            long number = Long.parseLong(value);

            if (number < minimum || number > maximum) {
                throw new IllegalArgumentException(
                        "The saved game contains an invalid " + key + ".");
            }

            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "The saved game contains an invalid " + key + ".",
                    exception);
        }
    }

    private void saveCapturedPieces(
            Properties properties,
            String prefix,
            List<Piece> pieces) {

        properties.setProperty(prefix + ".count", String.valueOf(pieces.size()));

        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            properties.setProperty(
                    prefix + "." + index + ".type",
                    piece.getType().name());
            properties.setProperty(
                    prefix + "." + index + ".color",
                    piece.getColor().name());
        }
    }

    private List<Piece> loadCapturedPieces(
            Properties properties,
            String prefix) {

        int count = readSavedInt(
                properties,
                prefix + ".count",
                0,
                30,
                0);
        List<Piece> pieces = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            String typeValue = properties.getProperty(
                    prefix + "." + index + ".type");
            String colorValue = properties.getProperty(
                    prefix + "." + index + ".color");

            if (typeValue == null || colorValue == null) {
                throw new IllegalArgumentException(
                        "The saved captured-piece list is incomplete.");
            }

            pieces.add(new Piece(
                    PieceType.valueOf(typeValue),
                    PieceColor.valueOf(colorValue)));
        }

        return pieces;
    }

    private void handleSquareClick(int row, int column) {
        if (gameOver
                || computerThinking
                || (computerMode
                && currentTurn != PieceColor.WHITE)) {
            return;
        }

        Piece clickedPiece = board.getPiece(row, column);

        if (selectedRow == -1) {
            if (clickedPiece == null) {
                statusLabel.setText("Select one of your White pieces.");
                return;
            }

            if (clickedPiece.getColor() != currentTurn) {
                if (computerMode) {
                    statusLabel.setText(
                            "That piece belongs to the computer.");
                } else {
                    statusLabel.setText(
                            "It is " + currentTurn + "'s turn.");
                }
                return;
            }

            if (!gameStarted) {
                gameStarted = true;
                lastClockTick = System.currentTimeMillis();
                refreshClocks();
            }

            selectedRow = row;
            selectedColumn = column;
            showLegalDestinations(row, column);

            statusLabel.setText(
                    "Piece selected. Choose its destination.");

            refreshBoard();
            return;
        }

        // Clicking another friendly piece selects that piece instead.
        if (clickedPiece != null
                && clickedPiece.getColor() == currentTurn) {
            selectedRow = row;
            selectedColumn = column;
            showLegalDestinations(row, column);
            refreshBoard();
            return;
        }

        String from = convertToChessPosition(
                selectedRow, selectedColumn);

        String to = convertToChessPosition(row, column);

        ChessMove move = new ChessMove(
                selectedRow,
                selectedColumn,
                row,
                column);
        PieceColor movingColor = currentTurn;
        Piece movingPiece = board.getPiece(selectedRow, selectedColumn);
        PieceType promotionType = PieceType.QUEEN;

        if (movingPiece != null
                && movingPiece.getType() == PieceType.PAWN
                && (row == 0 || row == 7)) {

            promotionType = choosePromotionPiece();
        }

        updateClock();
        GameSnapshot snapshot = createSnapshot();
        Piece capturedPiece = getCapturedPiece(snapshot.board, move);

        if (board.movePiece(from, to, currentTurn, promotionType)) {
            undoStack.add(snapshot);
            addCapturedPiece(capturedPiece, movingColor);
            moveHistory.add(createMoveNotation(
                    snapshot.board,
                    move,
                    movingColor));
            setLastMove(move);

            currentTurn = opposite(currentTurn);
            gameStarted = true;
            lastClockTick = System.currentTimeMillis();
            updateDrawTracking(snapshot.board, move);
            selectedRow = -1;
            selectedColumn = -1;
            clearLegalDestinations();

            updateGameStatus();
            refreshMoveHistory();
            refreshCapturedPieces();
            refreshClocks();
            refreshBoard();
            playMoveSound(capturedPiece != null);

            if (!gameOver
                    && computerMode
                    && currentTurn == PieceColor.BLACK) {
                startComputerTurn();
            }

            return;
        }

        statusLabel.setText("That move is not legal.");

        selectedRow = -1;
        selectedColumn = -1;
        clearLegalDestinations();

        refreshBoard();
    }

    private void startComputerTurn() {
        if (!computerMode
                || gameOver
                || currentTurn != PieceColor.BLACK) {
            return;
        }

        computerThinking = true;
        statusLabel.setText("Computer is thinking...");

        Board boardSnapshot = board.copy();
        int searchDepth = getComputerSearchDepth();

        computerWorker = new SwingWorker<ChessMove, Void>() {
            @Override
            protected ChessMove doInBackground() {
                ChessAI computerForTurn = new ChessAI(searchDepth);
                return computerForTurn.chooseMove(
                        boardSnapshot,
                        PieceColor.BLACK);
            }

            @Override
            protected void done() {
                if (computerWorker != this || isCancelled()) {
                    return;
                }

                try {
                    ChessMove move = get();

                    if (move == null) {
                        updateGameStatus();
                        return;
                    }

                    updateClock();
                    GameSnapshot snapshot = createSnapshot();
                    Piece capturedPiece = getCapturedPiece(
                            snapshot.board,
                            move);
                    boolean moveMade = board.movePiece(
                            move.getFromPosition(),
                            move.getToPosition(),
                            PieceColor.BLACK);

                    if (!moveMade) {
                        statusLabel.setText(
                                "The computer could not make its move. Restart the game.");
                        return;
                    }

                    undoStack.add(snapshot);
                    addCapturedPiece(capturedPiece, PieceColor.BLACK);
                    moveHistory.add(createMoveNotation(
                            snapshot.board,
                            move,
                            PieceColor.BLACK));
                    setLastMove(move);

                    currentTurn = PieceColor.WHITE;
                    gameStarted = true;
                    lastClockTick = System.currentTimeMillis();
                    updateDrawTracking(snapshot.board, move);
                    selectedRow = -1;
                    selectedColumn = -1;
                    clearLegalDestinations();

                    updateGameStatus();
                    refreshMoveHistory();
                    refreshCapturedPieces();
                    refreshClocks();
                    playMoveSound(capturedPiece != null);

                    if (!gameOver && !board.isInCheck(PieceColor.WHITE)) {
                        statusLabel.setText(
                                "Computer played " + move + ". Your turn.");
                    }
                } catch (CancellationException exception) {
                    // Restarting the game cancels the old computer turn.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    statusLabel.setText(
                            "The computer encountered an error. Restart the game.");
                    exception.getCause().printStackTrace();
                } finally {
                    if (computerWorker == this) {
                        computerThinking = false;
                        computerWorker = null;
                    }

                    refreshBoard();
                }
            }
        };

        computerWorker.execute();
    }

    private void updateGameStatus() {
        if (board.isCheckmate(currentTurn)) {
            gameOver = true;
            PieceColor winner = opposite(currentTurn);
            gameResult = winner == PieceColor.WHITE ? "1-0" : "0-1";

            if (!computerMode) {
                statusLabel.setText(
                        "CHECKMATE! Bitch "
                        + winner
                        + " wins!");
            } else if (currentTurn == PieceColor.BLACK) {
                statusLabel.setText("CHECKMATE! You win! Nice Job homie");
            } else {
                statusLabel.setText("CHECKMATE! The computer wins.  You suck.");
            }

        } else if (board.isStalemate(currentTurn)) {
            gameOver = true;
            gameResult = "1/2-1/2";
            statusLabel.setText("STALEMATE! The game is a draw. You guys suck");

        } else if (board.isInsufficientMaterial()) {
            gameOver = true;
            gameResult = "1/2-1/2";
            statusLabel.setText(
                    "DRAW! There is insufficient material to checkmate.");

        } else if (halfMoveClock >= 100) {
            gameOver = true;
            gameResult = "1/2-1/2";
            statusLabel.setText("DRAW! Fifty moves without a pawn move or capture.");

        } else if (isThreefoldRepetition()) {
            gameOver = true;
            gameResult = "1/2-1/2";
            statusLabel.setText("DRAW! The same position occurred three times.");

        } else if (board.isInCheck(currentTurn)) {
            gameResult = "*";
            if (!computerMode) {
                statusLabel.setText(currentTurn + " is in check!");
            } else if (currentTurn == PieceColor.WHITE) {
                statusLabel.setText("You are in check!");
            } else {
                statusLabel.setText("The computer is in check! be careful");
            }

        } else if (!computerMode) {
            gameResult = "*";
            statusLabel.setText(currentTurn + "'s turn");
        } else if (currentTurn == PieceColor.WHITE) {
            gameResult = "*";
            statusLabel.setText("Your turn - you are White");
        } else {
            gameResult = "*";
            statusLabel.setText("Computer's turn");
        }
    }

    private PieceColor opposite(PieceColor color) {
        return color == PieceColor.WHITE
                ? PieceColor.BLACK
                : PieceColor.WHITE;
    }

    private String convertToChessPosition(int row, int column) {
        char file = (char) ('a' + column);
        int rank = 8 - row;

        return "" + file + rank;
    }

    private void refreshBoard() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                JButton button = buttons[row][column];
                Piece piece = board.getPiece(row, column);

                if (piece == null) {
                    if (legalDestinations[row][column]) {
                        button.setText("\u2022");
                        button.setForeground(LEGAL_DOT);
                    } else {
                        button.setText("");
                        button.setForeground(PIECE_COLOR);
                    }
                } else {
                    button.setText(getPieceSymbol(piece));
                    button.setForeground(PIECE_COLOR);
                }

                if (row == selectedRow
                        && column == selectedColumn) {
                    button.setBackground(GOLD);
                } else if (legalDestinations[row][column]) {
                    if (piece == null) {
                        button.setBackground(LEGAL_MOVE);
                    } else {
                        button.setBackground(LEGAL_CAPTURE);
                    }
                } else if ((row == lastFromRow
                        && column == lastFromColumn)
                        || (row == lastToRow
                        && column == lastToColumn)) {

                    button.setBackground(LAST_MOVE);
                } else if ((row + column) % 2 == 0) {
                    button.setBackground(lightSquareColor);
                } else {
                    button.setBackground(darkSquareColor);
                }
            }
        }
    }

    private String getPieceSymbol(Piece piece) {
        if (piece.getColor() == PieceColor.WHITE) {
            switch (piece.getType()) {
                case KING:
                    return "\u2654";
                case QUEEN:
                    return "\u2655";
                case ROOK:
                    return "\u2656";
                case BISHOP:
                    return "\u2657";
                case KNIGHT:
                    return "\u2658";
                case PAWN:
                    return "\u2659";
            }
        } else {
            switch (piece.getType()) {
                case KING:
                    return "\u265A";
                case QUEEN:
                    return "\u265B";
                case ROOK:
                    return "\u265C";
                case BISHOP:
                    return "\u265D";
                case KNIGHT:
                    return "\u265E";
                case PAWN:
                    return "\u265F";
            }
        }

        return "";
    }

    private static final class GameSnapshot {
        private final Board board;
        private final PieceColor currentTurn;
        private final int lastFromRow;
        private final int lastFromColumn;
        private final int lastToRow;
        private final int lastToColumn;
        private final int historySize;
        private final int capturedByWhiteSize;
        private final int capturedByBlackSize;
        private final int halfMoveClock;
        private final Map<String, Integer> positionOccurrences;
        private final long whiteTimeMilliseconds;
        private final long blackTimeMilliseconds;
        private final boolean gameStarted;
        private final String gameResult;

        private GameSnapshot(
                Board board,
                PieceColor currentTurn,
                int lastFromRow,
                int lastFromColumn,
                int lastToRow,
                int lastToColumn,
                int historySize,
                int capturedByWhiteSize,
                int capturedByBlackSize,
                int halfMoveClock,
                Map<String, Integer> positionOccurrences,
                long whiteTimeMilliseconds,
                long blackTimeMilliseconds,
                boolean gameStarted,
                String gameResult) {

            this.board = board;
            this.currentTurn = currentTurn;
            this.lastFromRow = lastFromRow;
            this.lastFromColumn = lastFromColumn;
            this.lastToRow = lastToRow;
            this.lastToColumn = lastToColumn;
            this.historySize = historySize;
            this.capturedByWhiteSize = capturedByWhiteSize;
            this.capturedByBlackSize = capturedByBlackSize;
            this.halfMoveClock = halfMoveClock;
            this.positionOccurrences = positionOccurrences;
            this.whiteTimeMilliseconds = whiteTimeMilliseconds;
            this.blackTimeMilliseconds = blackTimeMilliseconds;
            this.gameStarted = gameStarted;
            this.gameResult = gameResult;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}
