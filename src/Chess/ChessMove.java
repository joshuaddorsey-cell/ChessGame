package Chess;

public final class ChessMove {
    private final int fromRow;
    private final int fromColumn;
    private final int toRow;
    private final int toColumn;

    public ChessMove(
            int fromRow,
            int fromColumn,
            int toRow,
            int toColumn) {

        this.fromRow = fromRow;
        this.fromColumn = fromColumn;
        this.toRow = toRow;
        this.toColumn = toColumn;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getFromColumn() {
        return fromColumn;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToColumn() {
        return toColumn;
    }

    public String getFromPosition() {
        return toChessPosition(fromRow, fromColumn);
    }

    public String getToPosition() {
        return toChessPosition(toRow, toColumn);
    }

    private String toChessPosition(int row, int column) {
        char file = (char) ('a' + column);
        int rank = 8 - row;
        return "" + file + rank;
    }

    @Override
    public String toString() {
        return getFromPosition() + "-" + getToPosition();
    }
}
