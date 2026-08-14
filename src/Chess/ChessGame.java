package Chess;

import java.util.Scanner;

public class ChessGame {

    public static void main(String[] args) {
        Board board = new Board();
        PieceColor currentTurn = PieceColor.WHITE;

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                board.display();

                System.out.print(
                        currentTurn
                        + "'s turn. Enter a move (e2 e4), or quit: ");

                String from = scanner.next();

                if (from.equalsIgnoreCase("quit")) {
                    break;
                }

                String to = scanner.next();

                if (board.movePiece(from, to, currentTurn)) {
                    if (currentTurn == PieceColor.WHITE) {
                        currentTurn = PieceColor.BLACK;
                    } else {
                        currentTurn = PieceColor.WHITE;
                    }
                } else {
                    System.out.println(
                            "Invalid move. Try again.");
                }
            }
        }

        System.out.println("Game ended.");
    }
}