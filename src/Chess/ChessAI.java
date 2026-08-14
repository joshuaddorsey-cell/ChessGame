package Chess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChessAI {
    private static final int CHECKMATE_SCORE = 1_000_000;

    private final int searchDepth;

    public ChessAI(int searchDepth) {
        this.searchDepth = Math.max(1, searchDepth);
    }

    public ChessMove chooseMove(Board board, PieceColor computerColor) {
        List<ChessMove> legalMoves = board.getLegalMoves(computerColor);

        if (legalMoves.isEmpty()) {
            return null;
        }

        orderMoves(board, legalMoves);

        int bestScore = Integer.MIN_VALUE;
        List<ChessMove> bestMoves = new ArrayList<>();

        for (ChessMove move : legalMoves) {
            Board nextBoard = board.copy();
            nextBoard.movePiece(
                    move.getFromPosition(),
                    move.getToPosition(),
                    computerColor);

            int score = minimax(
                    nextBoard,
                    searchDepth - 1,
                    opposite(computerColor),
                    computerColor,
                    Integer.MIN_VALUE + 1,
                    Integer.MAX_VALUE);

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }

        return bestMoves.get(
                ThreadLocalRandom.current().nextInt(bestMoves.size()));
    }

    private int minimax(
            Board board,
            int depth,
            PieceColor turn,
            PieceColor computerColor,
            int alpha,
            int beta) {

        if (depth == 0) {
            if (board.isInCheck(turn)
                    && board.getLegalMoves(turn).isEmpty()) {

                return turn == computerColor
                        ? -CHECKMATE_SCORE
                        : CHECKMATE_SCORE;
            }

            return evaluate(board, computerColor);
        }

        List<ChessMove> legalMoves = board.getLegalMoves(turn);

        if (legalMoves.isEmpty()) {
            if (board.isInCheck(turn)) {
                return turn == computerColor
                        ? -CHECKMATE_SCORE - depth
                        : CHECKMATE_SCORE + depth;
            }

            return 0;
        }

        orderMoves(board, legalMoves);

        if (turn == computerColor) {
            int bestScore = Integer.MIN_VALUE;

            for (ChessMove move : legalMoves) {
                Board nextBoard = board.copy();
                nextBoard.movePiece(
                        move.getFromPosition(),
                        move.getToPosition(),
                        turn);

                int score = minimax(
                        nextBoard,
                        depth - 1,
                        opposite(turn),
                        computerColor,
                        alpha,
                        beta);

                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, score);

                if (beta <= alpha) {
                    break;
                }
            }

            return bestScore;
        }

        int bestScore = Integer.MAX_VALUE;

        for (ChessMove move : legalMoves) {
            Board nextBoard = board.copy();
            nextBoard.movePiece(
                    move.getFromPosition(),
                    move.getToPosition(),
                    turn);

            int score = minimax(
                    nextBoard,
                    depth - 1,
                    opposite(turn),
                    computerColor,
                    alpha,
                    beta);

            bestScore = Math.min(bestScore, score);
            beta = Math.min(beta, score);

            if (beta <= alpha) {
                break;
            }
        }

        return bestScore;
    }

    private int evaluate(Board board, PieceColor computerColor) {
        int score = 0;

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = board.getPiece(row, column);

                if (piece == null) {
                    continue;
                }

                int pieceScore = pieceValue(piece.getType());
                pieceScore += positionBonus(piece, row, column);

                if (piece.getColor() == computerColor) {
                    score += pieceScore;
                } else {
                    score -= pieceScore;
                }
            }
        }

        if (board.isInCheck(opposite(computerColor))) {
            score += 30;
        }

        if (board.isInCheck(computerColor)) {
            score -= 30;
        }

        return score;
    }

    private int positionBonus(Piece piece, int row, int column) {
        int bonus = 0;

        if (row >= 2 && row <= 5 && column >= 2 && column <= 5) {
            bonus += 8;
        }

        if ((row == 3 || row == 4)
                && (column == 3 || column == 4)) {
            bonus += 8;
        }

        if (piece.getType() == PieceType.PAWN) {
            if (piece.getColor() == PieceColor.WHITE) {
                bonus += (6 - row) * 3;
            } else {
                bonus += (row - 1) * 3;
            }
        }

        if (piece.getType() == PieceType.KING
                && (column == 2 || column == 6)
                && (row == 0 || row == 7)) {

            bonus += 25;
        }

        return bonus;
    }

    private int pieceValue(PieceType type) {
        switch (type) {
            case PAWN:
                return 100;
            case KNIGHT:
                return 320;
            case BISHOP:
                return 330;
            case ROOK:
                return 500;
            case QUEEN:
                return 900;
            case KING:
                return 20_000;
            default:
                return 0;
        }
    }

    private void orderMoves(Board board, List<ChessMove> moves) {
        moves.sort(Comparator.comparingInt(
                (ChessMove move) -> movePriority(board, move))
                .reversed());
    }

    private int movePriority(Board board, ChessMove move) {
        Piece capturedPiece = board.getPiece(
                move.getToRow(),
                move.getToColumn());

        if (capturedPiece == null) {
            return 0;
        }

        return pieceValue(capturedPiece.getType());
    }

    private PieceColor opposite(PieceColor color) {
        return color == PieceColor.WHITE
                ? PieceColor.BLACK
                : PieceColor.WHITE;
    }
}
