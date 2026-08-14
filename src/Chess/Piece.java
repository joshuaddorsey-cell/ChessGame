

package Chess;

public class Piece {
    private final PieceType type;
    private final PieceColor color;

    public Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }

    @Override
    public String toString() {
        String symbol;

        switch (type) {
            case KING:
                symbol = "K";
                break;
            case QUEEN:
                symbol = "Q";
                break;
            case ROOK:
                symbol = "R";
                break;
            case BISHOP:
                symbol = "B";
                break;
            case KNIGHT:
                symbol = "N";
                break;
            case PAWN:
                symbol = "P";
                break;
            default:
                symbol = "?";
        }

        return color == PieceColor.WHITE
                ? symbol
                : symbol.toLowerCase();
    }
}