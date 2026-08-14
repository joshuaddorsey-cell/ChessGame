package Chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Board {
    private final Piece[][] squares;
    private boolean whiteKingMoved;
    private boolean blackKingMoved;

    private boolean whiteLeftRookMoved;
    private boolean whiteRightRookMoved;

    private boolean blackLeftRookMoved;
    private boolean blackRightRookMoved;
    private int enPassantRow = -1;
    private int enPassantColumn = -1;

    public Board() {
        this(true);
    }

    private Board(boolean setUpStartingPosition) {
        squares = new Piece[8][8];

        if (setUpStartingPosition) {
            setUpPieces();
        }
    }

    
    
    private void setUpPieces() {
        // Black pieces
        squares[0][0] = new Piece(PieceType.ROOK, PieceColor.BLACK);
        squares[0][1] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        squares[0][2] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        squares[0][3] = new Piece(PieceType.QUEEN, PieceColor.BLACK);
        squares[0][4] = new Piece(PieceType.KING, PieceColor.BLACK);
        squares[0][5] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        squares[0][6] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        squares[0][7] = new Piece(PieceType.ROOK, PieceColor.BLACK);

        for (int column = 0; column < 8; column++) {
            squares[1][column] =
                    new Piece(PieceType.PAWN, PieceColor.BLACK);
        }

        // White pieces
        squares[7][0] = new Piece(PieceType.ROOK, PieceColor.WHITE);
        squares[7][1] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
        squares[7][2] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
        squares[7][3] = new Piece(PieceType.QUEEN, PieceColor.WHITE);
        squares[7][4] = new Piece(PieceType.KING, PieceColor.WHITE);
        squares[7][5] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
        squares[7][6] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
        squares[7][7] = new Piece(PieceType.ROOK, PieceColor.WHITE);

        for (int column = 0; column < 8; column++) {
            squares[6][column] =
                    new Piece(PieceType.PAWN, PieceColor.WHITE);
        }
    }

    public void display() {
        System.out.println("  a b c d e f g h");

        for (int row = 0; row < 8; row++) {
            System.out.print(8 - row + " ");

            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column];

                if (piece == null) {
                    System.out.print(". ");
                } else {
                    System.out.print(piece + " ");
                }
            }

            System.out.println(8 - row);
        }

        System.out.println("  a b c d e f g h");
    }
    public boolean movePiece(
            String from,
            String to,
            PieceColor currentTurn) {

        return movePiece(
                from,
                to,
                currentTurn,
                PieceType.QUEEN);
    }

    public boolean movePiece(
            String from,
            String to,
            PieceColor currentTurn,
            PieceType promotionType) {

        from = from.toLowerCase();
        to = to.toLowerCase();

        if (!isValidSquare(from) || !isValidSquare(to)) {
            return false;
        }

        int fromColumn = from.charAt(0) - 'a';
        int fromRow = 8 - (from.charAt(1) - '0');

        int toColumn = to.charAt(0) - 'a';
        int toRow = 8 - (to.charAt(1) - '0');

        Piece movingPiece = squares[fromRow][fromColumn];
        Piece destinationPiece = squares[toRow][toColumn];

        if (movingPiece == null
                || movingPiece.getColor() != currentTurn) {
            return false;
        }

        if (destinationPiece != null
                && destinationPiece.getColor() == currentTurn) {
            return false;
        }

        // Handle castling.
        if (isCastlingMove(
                movingPiece,
                fromRow,
                fromColumn,
                toRow,
                toColumn)) {

            if (!canCastle(
                    movingPiece,
                    fromRow,
                    fromColumn,
                    toRow,
                    toColumn)) {
                return false;
            }

            int rookFromColumn =
                    toColumn == 6 ? 7 : 0;

            int rookToColumn =
                    toColumn == 6 ? 5 : 3;

            Piece rook = squares[fromRow][rookFromColumn];

            squares[toRow][toColumn] = movingPiece;
            squares[fromRow][fromColumn] = null;

            squares[fromRow][rookToColumn] = rook;
            squares[fromRow][rookFromColumn] = null;

            updateCastlingHistory(
                    movingPiece,
                    fromRow,
                    fromColumn);

            updateEnPassantTarget(
                    movingPiece,
                    fromRow,
                    fromColumn,
                    toRow,
                    toColumn);

            return true;
        }

        boolean enPassantCapture =
                isEnPassantCapture(
                        movingPiece,
                        fromRow,
                        fromColumn,
                        toRow,
                        toColumn);

        if (!enPassantCapture
                && !isValidMovement(
                        movingPiece,
                        fromRow,
                        fromColumn,
                        toRow,
                        toColumn)) {
            return false;
        }

        Piece capturedEnPassantPawn = null;

        if (enPassantCapture) {
            capturedEnPassantPawn =
                    squares[fromRow][toColumn];

            squares[fromRow][toColumn] = null;
        }

        // Temporarily make the move.
        squares[toRow][toColumn] = movingPiece;
        squares[fromRow][fromColumn] = null;

        // Undo a move that leaves the king in check.
        if (isKingInCheck(currentTurn)) {
            squares[fromRow][fromColumn] = movingPiece;
            squares[toRow][toColumn] = destinationPiece;

            if (enPassantCapture) {
                squares[fromRow][toColumn] =
                        capturedEnPassantPawn;
            }

            return false;
        }

        updateCastlingHistory(
                movingPiece,
                fromRow,
                fromColumn);

        updateEnPassantTarget(
                movingPiece,
                fromRow,
                fromColumn,
                toRow,
                toColumn);

        // Automatic queen promotion.
        if (movingPiece.getType() == PieceType.PAWN
                && (toRow == 0 || toRow == 7)) {

            PieceType selectedPromotion = promotionType;

            if (selectedPromotion == null
                    || selectedPromotion == PieceType.KING
                    || selectedPromotion == PieceType.PAWN) {

                selectedPromotion = PieceType.QUEEN;
            }

            squares[toRow][toColumn] =
                    new Piece(
                            selectedPromotion,
                            movingPiece.getColor());
        }

        return true;
    }

    private boolean isValidSquare(String position) {
        if (position == null || position.length() != 2) {
            return false;
        }

        char column = position.charAt(0);
        char row = position.charAt(1);

        return column >= 'a' && column <= 'h'
                && row >= '1' && row <= '8';
    }
    private boolean isValidMovement(
            Piece piece,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        int rowDifference = toRow - fromRow;
        int columnDifference = toColumn - fromColumn;

        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(
                        piece,
                        fromRow,
                        fromColumn,
                        toRow,
                        toColumn);

            case KNIGHT:
                return (Math.abs(rowDifference) == 2
                        && Math.abs(columnDifference) == 1)
                        || (Math.abs(rowDifference) == 1
                        && Math.abs(columnDifference) == 2);

            case BISHOP:
                return Math.abs(rowDifference)
                        == Math.abs(columnDifference)
                        && isPathClear(
                                fromRow,
                                fromColumn,
                                toRow,
                                toColumn);

            case ROOK:
                return (rowDifference == 0
                        || columnDifference == 0)
                        && isPathClear(
                                fromRow,
                                fromColumn,
                                toRow,
                                toColumn);

            case QUEEN:
                boolean straight =
                        rowDifference == 0
                        || columnDifference == 0;

                boolean diagonal =
                        Math.abs(rowDifference)
                        == Math.abs(columnDifference);

                return (straight || diagonal)
                        && isPathClear(
                                fromRow,
                                fromColumn,
                                toRow,
                                toColumn);

            case KING:
                return Math.abs(rowDifference) <= 1
                        && Math.abs(columnDifference) <= 1
                        && (rowDifference != 0
                        || columnDifference != 0);

            default:
                return false;
        }
    }
    private boolean isValidPawnMove(
            Piece pawn,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        int direction;

        if (pawn.getColor() == PieceColor.WHITE) {
            direction = -1;
        } else {
            direction = 1;
        }

        int startingRow;

        if (pawn.getColor() == PieceColor.WHITE) {
            startingRow = 6;
        } else {
            startingRow = 1;
        }

        int rowDifference = toRow - fromRow;
        int columnDifference = toColumn - fromColumn;
        Piece destination = squares[toRow][toColumn];

        // Move forward one square.
        if (columnDifference == 0
                && rowDifference == direction
                && destination == null) {
            return true;
        }

        // Move forward two squares from the starting position.
        if (columnDifference == 0
                && fromRow == startingRow
                && rowDifference == 2 * direction
                && destination == null
                && squares[fromRow + direction][fromColumn] == null) {
            return true;
        }

        // Capture diagonally.
        return Math.abs(columnDifference) == 1
                && rowDifference == direction
                && destination != null
                && destination.getColor() != pawn.getColor();
    }
    
    private boolean isPathClear(
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        int rowStep = Integer.compare(toRow, fromRow);
        int columnStep = Integer.compare(toColumn, fromColumn);

        int currentRow = fromRow + rowStep;
        int currentColumn = fromColumn + columnStep;

        while (currentRow != toRow
                || currentColumn != toColumn) {

            if (squares[currentRow][currentColumn] != null) {
                return false;
            }

            currentRow += rowStep;
            currentColumn += columnStep;
        }

        return true;
    }
    public Piece getPiece(int row, int column) {
        return squares[row][column];
    }

    public String getPositionKey(PieceColor turn) {
        StringBuilder key = new StringBuilder(90);

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                key.append(pieceToSaveCode(squares[row][column]));
            }
        }

        key.append('|').append(turn.name());
        key.append('|').append(whiteKingMoved);
        key.append('|').append(blackKingMoved);
        key.append('|').append(whiteLeftRookMoved);
        key.append('|').append(whiteRightRookMoved);
        key.append('|').append(blackLeftRookMoved);
        key.append('|').append(blackRightRookMoved);
        key.append('|').append(enPassantRow);
        key.append('|').append(enPassantColumn);

        return key.toString();
    }

    public boolean isInsufficientMaterial() {
        int knightCount = 0;
        int bishopCount = 0;
        int firstBishopSquareColor = -1;
        boolean allBishopsUseSameColor = true;

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column];

                if (piece == null || piece.getType() == PieceType.KING) {
                    continue;
                }

                if (piece.getType() == PieceType.KNIGHT) {
                    knightCount++;
                } else if (piece.getType() == PieceType.BISHOP) {
                    bishopCount++;
                    int squareColor = (row + column) % 2;

                    if (firstBishopSquareColor == -1) {
                        firstBishopSquareColor = squareColor;
                    } else if (squareColor != firstBishopSquareColor) {
                        allBishopsUseSameColor = false;
                    }
                } else {
                    return false;
                }
            }
        }

        int minorPieceCount = knightCount + bishopCount;

        if (minorPieceCount <= 1) {
            return true;
        }

        return knightCount == 0
                && bishopCount > 0
                && allBishopsUseSameColor;
    }

    public Board copy() {
        Board copy = new Board(false);

        for (int row = 0; row < 8; row++) {
            System.arraycopy(squares[row], 0, copy.squares[row], 0, 8);
        }

        copy.whiteKingMoved = whiteKingMoved;
        copy.blackKingMoved = blackKingMoved;
        copy.whiteLeftRookMoved = whiteLeftRookMoved;
        copy.whiteRightRookMoved = whiteRightRookMoved;
        copy.blackLeftRookMoved = blackLeftRookMoved;
        copy.blackRightRookMoved = blackRightRookMoved;
        copy.enPassantRow = enPassantRow;
        copy.enPassantColumn = enPassantColumn;

        return copy;
    }

    public void restoreFrom(Board source) {
        for (int row = 0; row < 8; row++) {
            System.arraycopy(source.squares[row], 0, squares[row], 0, 8);
        }

        whiteKingMoved = source.whiteKingMoved;
        blackKingMoved = source.blackKingMoved;
        whiteLeftRookMoved = source.whiteLeftRookMoved;
        whiteRightRookMoved = source.whiteRightRookMoved;
        blackLeftRookMoved = source.blackLeftRookMoved;
        blackRightRookMoved = source.blackRightRookMoved;
        enPassantRow = source.enPassantRow;
        enPassantColumn = source.enPassantColumn;
    }

    public void saveState(Properties properties) {
        StringBuilder position = new StringBuilder(64);

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                position.append(pieceToSaveCode(squares[row][column]));
            }
        }

        properties.setProperty("board.position", position.toString());
        properties.setProperty(
                "board.whiteKingMoved",
                String.valueOf(whiteKingMoved));
        properties.setProperty(
                "board.blackKingMoved",
                String.valueOf(blackKingMoved));
        properties.setProperty(
                "board.whiteLeftRookMoved",
                String.valueOf(whiteLeftRookMoved));
        properties.setProperty(
                "board.whiteRightRookMoved",
                String.valueOf(whiteRightRookMoved));
        properties.setProperty(
                "board.blackLeftRookMoved",
                String.valueOf(blackLeftRookMoved));
        properties.setProperty(
                "board.blackRightRookMoved",
                String.valueOf(blackRightRookMoved));
        properties.setProperty(
                "board.enPassantRow",
                String.valueOf(enPassantRow));
        properties.setProperty(
                "board.enPassantColumn",
                String.valueOf(enPassantColumn));
    }

    public void loadState(Properties properties) {
        String position = properties.getProperty("board.position");

        if (position == null || position.length() != 64) {
            throw new IllegalArgumentException(
                    "The saved board position is invalid.");
        }

        Piece[][] loadedSquares = new Piece[8][8];
        int whiteKings = 0;
        int blackKings = 0;

        for (int index = 0; index < position.length(); index++) {
            Piece piece = pieceFromSaveCode(position.charAt(index));
            int row = index / 8;
            int column = index % 8;
            loadedSquares[row][column] = piece;

            if (piece != null && piece.getType() == PieceType.KING) {
                if (piece.getColor() == PieceColor.WHITE) {
                    whiteKings++;
                } else {
                    blackKings++;
                }
            }
        }

        if (whiteKings != 1 || blackKings != 1) {
            throw new IllegalArgumentException(
                    "The saved game must contain both kings.");
        }

        boolean loadedWhiteKingMoved =
                readBoolean(properties, "board.whiteKingMoved");
        boolean loadedBlackKingMoved =
                readBoolean(properties, "board.blackKingMoved");
        boolean loadedWhiteLeftRookMoved =
                readBoolean(properties, "board.whiteLeftRookMoved");
        boolean loadedWhiteRightRookMoved =
                readBoolean(properties, "board.whiteRightRookMoved");
        boolean loadedBlackLeftRookMoved =
                readBoolean(properties, "board.blackLeftRookMoved");
        boolean loadedBlackRightRookMoved =
                readBoolean(properties, "board.blackRightRookMoved");
        int loadedEnPassantRow =
                readBoardCoordinate(properties, "board.enPassantRow");
        int loadedEnPassantColumn =
                readBoardCoordinate(properties, "board.enPassantColumn");

        for (int row = 0; row < 8; row++) {
            System.arraycopy(loadedSquares[row], 0, squares[row], 0, 8);
        }

        whiteKingMoved = loadedWhiteKingMoved;
        blackKingMoved = loadedBlackKingMoved;
        whiteLeftRookMoved = loadedWhiteLeftRookMoved;
        whiteRightRookMoved = loadedWhiteRightRookMoved;
        blackLeftRookMoved = loadedBlackLeftRookMoved;
        blackRightRookMoved = loadedBlackRightRookMoved;
        enPassantRow = loadedEnPassantRow;
        enPassantColumn = loadedEnPassantColumn;
    }

    private char pieceToSaveCode(Piece piece) {
        if (piece == null) {
            return '.';
        }

        char code;

        switch (piece.getType()) {
            case KING:
                code = 'K';
                break;
            case QUEEN:
                code = 'Q';
                break;
            case ROOK:
                code = 'R';
                break;
            case BISHOP:
                code = 'B';
                break;
            case KNIGHT:
                code = 'N';
                break;
            case PAWN:
                code = 'P';
                break;
            default:
                throw new IllegalArgumentException("Unknown piece type.");
        }

        if (piece.getColor() == PieceColor.BLACK) {
            return Character.toLowerCase(code);
        }

        return code;
    }

    private Piece pieceFromSaveCode(char code) {
        if (code == '.') {
            return null;
        }

        PieceColor color = Character.isUpperCase(code)
                ? PieceColor.WHITE
                : PieceColor.BLACK;

        PieceType type;

        switch (Character.toUpperCase(code)) {
            case 'K':
                type = PieceType.KING;
                break;
            case 'Q':
                type = PieceType.QUEEN;
                break;
            case 'R':
                type = PieceType.ROOK;
                break;
            case 'B':
                type = PieceType.BISHOP;
                break;
            case 'N':
                type = PieceType.KNIGHT;
                break;
            case 'P':
                type = PieceType.PAWN;
                break;
            default:
                throw new IllegalArgumentException(
                        "The saved game contains an unknown piece.");
        }

        return new Piece(type, color);
    }

    private boolean readBoolean(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(
                    "The saved game contains an invalid value for " + key + ".");
        }

        return Boolean.parseBoolean(value);
    }

    private int readBoardCoordinate(Properties properties, String key) {
        String value = properties.getProperty(key);

        try {
            int coordinate = Integer.parseInt(value);

            if (coordinate < -1 || coordinate > 7) {
                throw new IllegalArgumentException();
            }

            return coordinate;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "The saved game contains an invalid value for " + key + ".",
                    exception);
        }
    }

    public List<ChessMove> getLegalMoves(PieceColor color) {
        List<ChessMove> legalMoves = new ArrayList<>();

        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromColumn = 0; fromColumn < 8; fromColumn++) {
                Piece piece = squares[fromRow][fromColumn];

                if (piece == null || piece.getColor() != color) {
                    continue;
                }

                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toColumn = 0; toColumn < 8; toColumn++) {
                        if (isPossibleLegalMove(
                                color,
                                fromRow,
                                fromColumn,
                                toRow,
                                toColumn)) {

                            legalMoves.add(new ChessMove(
                                    fromRow,
                                    fromColumn,
                                    toRow,
                                    toColumn));
                        }
                    }
                }
            }
        }

        return legalMoves;
    }

    public boolean isInCheck(PieceColor color) {
        return isKingInCheck(color);
    }

    private boolean isKingInCheck(PieceColor kingColor) {
        int kingRow = -1;
        int kingColumn = -1;

        // Find the king.
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column];

                if (piece != null
                        && piece.getColor() == kingColor
                        && piece.getType() == PieceType.KING) {
                    kingRow = row;
                    kingColumn = column;
                }
            }
        }

        if (kingRow == -1) {
            return false;
        }

        // See whether an opposing piece can attack the king.
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column];

                if (piece != null
                        && piece.getColor() != kingColor
                        && isValidMovement(
                                piece,
                                row,
                                column,
                                kingRow,
                                kingColumn)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public boolean isCheckmate(PieceColor color) {
        return isKingInCheck(color) && !hasAnyLegalMove(color);
    }
    public boolean isStalemate(PieceColor color) {
        return !isKingInCheck(color) && !hasAnyLegalMove(color);
    }

    private boolean hasAnyLegalMove(PieceColor color) {
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromColumn = 0;
                    fromColumn < 8;
                    fromColumn++) {

                Piece piece = squares[fromRow][fromColumn];

                if (piece == null || piece.getColor() != color) {
                    continue;
                }

                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toColumn = 0;
                            toColumn < 8;
                            toColumn++) {

                        if (isPossibleLegalMove(
                                color,
                                fromRow,
                                fromColumn,
                                toRow,
                                toColumn)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isPossibleLegalMove(
            PieceColor color,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        Piece movingPiece = squares[fromRow][fromColumn];
        Piece destinationPiece = squares[toRow][toColumn];

        if (destinationPiece != null
                && destinationPiece.getColor() == color) {
            return false;
        }

        if (isCastlingMove(
                movingPiece,
                fromRow,
                fromColumn,
                toRow,
                toColumn)) {

            return canCastle(
                    movingPiece,
                    fromRow,
                    fromColumn,
                    toRow,
                    toColumn);
        }

        boolean enPassantCapture =
                isEnPassantCapture(
                        movingPiece,
                        fromRow,
                        fromColumn,
                        toRow,
                        toColumn);

        if (!enPassantCapture
                && !isValidMovement(
                        movingPiece,
                        fromRow,
                        fromColumn,
                        toRow,
                        toColumn)) {
            return false;
        }

        Piece capturedEnPassantPawn = null;

        if (enPassantCapture) {
            capturedEnPassantPawn =
                    squares[fromRow][toColumn];

            squares[fromRow][toColumn] = null;
        }

        squares[toRow][toColumn] = movingPiece;
        squares[fromRow][fromColumn] = null;

        boolean kingIsSafe = !isKingInCheck(color);

        squares[fromRow][fromColumn] = movingPiece;
        squares[toRow][toColumn] = destinationPiece;

        if (enPassantCapture) {
            squares[fromRow][toColumn] =
                    capturedEnPassantPawn;
        }

        return kingIsSafe;
    }
    public void reset() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                squares[row][column] = null;
            }
        }
        whiteKingMoved = false;
        blackKingMoved = false;

        whiteLeftRookMoved = false;
        whiteRightRookMoved = false;

        blackLeftRookMoved = false;
        blackRightRookMoved = false;
        enPassantRow = -1;
        enPassantColumn = -1;
        setUpPieces();
    }
    
    private boolean isCastlingMove(
            Piece piece,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        return piece.getType() == PieceType.KING
                && fromRow == toRow
                && Math.abs(toColumn - fromColumn) == 2;
    }
    private boolean canCastle(
            Piece king,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        PieceColor color = king.getColor();
        int homeRow;

        if (color == PieceColor.WHITE) {
            homeRow = 7;

            if (whiteKingMoved) {
                return false;
            }
        } else {
            homeRow = 0;

            if (blackKingMoved) {
                return false;
            }
        }

        if (fromRow != homeRow
                || toRow != homeRow
                || fromColumn != 4
                || (toColumn != 2 && toColumn != 6)) {
            return false;
        }

        boolean kingSide = toColumn == 6;
        int rookColumn = kingSide ? 7 : 0;
        boolean rookMoved;

        if (color == PieceColor.WHITE) {
            rookMoved = kingSide
                    ? whiteRightRookMoved
                    : whiteLeftRookMoved;
        } else {
            rookMoved = kingSide
                    ? blackRightRookMoved
                    : blackLeftRookMoved;
        }

        if (rookMoved) {
            return false;
        }

        Piece rook = squares[homeRow][rookColumn];

        if (rook == null
                || rook.getType() != PieceType.ROOK
                || rook.getColor() != color) {
            return false;
        }

        int direction = kingSide ? 1 : -1;

        // Every square between the king and rook must be empty.
        for (int column = fromColumn + direction;
                column != rookColumn;
                column += direction) {

            if (squares[homeRow][column] != null) {
                return false;
            }
        }

        // A player cannot castle while in check.
        if (isKingInCheck(color)) {
            return false;
        }

        int middleColumn = fromColumn + direction;

        // The king cannot pass through or finish on an attacked square.
        if (!isKingSafeAfterTemporaryMove(
                color,
                fromRow,
                fromColumn,
                homeRow,
                middleColumn)) {
            return false;
        }

        return isKingSafeAfterTemporaryMove(
                color,
                fromRow,
                fromColumn,
                homeRow,
                toColumn);
    }
    private boolean isKingSafeAfterTemporaryMove(
            PieceColor color,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        Piece king = squares[fromRow][fromColumn];
        Piece destinationPiece = squares[toRow][toColumn];

        squares[toRow][toColumn] = king;
        squares[fromRow][fromColumn] = null;

        boolean safe = !isKingInCheck(color);

        squares[fromRow][fromColumn] = king;
        squares[toRow][toColumn] = destinationPiece;

        return safe;
    }
    private boolean isEnPassantCapture(
            Piece movingPiece,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        if (movingPiece.getType() != PieceType.PAWN) {
            return false;
        }

        int direction;

        if (movingPiece.getColor() == PieceColor.WHITE) {
            direction = -1;
        } else {
            direction = 1;
        }

        if (toRow - fromRow != direction
                || Math.abs(toColumn - fromColumn) != 1
                || toRow != enPassantRow
                || toColumn != enPassantColumn
                || squares[toRow][toColumn] != null) {
            return false;
        }

        Piece capturedPawn = squares[fromRow][toColumn];

        return capturedPawn != null
                && capturedPawn.getType() == PieceType.PAWN
                && capturedPawn.getColor()
                        != movingPiece.getColor();
    }
    
    private void updateEnPassantTarget(
            Piece movingPiece,
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        enPassantRow = -1;
        enPassantColumn = -1;

        if (movingPiece.getType() == PieceType.PAWN
                && fromColumn == toColumn
                && Math.abs(toRow - fromRow) == 2) {

            enPassantRow = (fromRow + toRow) / 2;
            enPassantColumn = fromColumn;
        }
    }
    private void updateCastlingHistory(
            Piece movingPiece,
            int fromRow,
            int fromColumn) {

        if (movingPiece.getType() == PieceType.KING) {
            if (movingPiece.getColor() == PieceColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }

        if (movingPiece.getType() == PieceType.ROOK) {
            if (movingPiece.getColor() == PieceColor.WHITE
                    && fromRow == 7) {

                if (fromColumn == 0) {
                    whiteLeftRookMoved = true;
                } else if (fromColumn == 7) {
                    whiteRightRookMoved = true;
                }
            }

            if (movingPiece.getColor() == PieceColor.BLACK
                    && fromRow == 0) {

                if (fromColumn == 0) {
                    blackLeftRookMoved = true;
                } else if (fromColumn == 7) {
                    blackRightRookMoved = true;
                }
            }
        }
    }
    
}
