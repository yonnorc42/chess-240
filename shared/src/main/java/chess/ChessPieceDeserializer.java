package chess;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ChessPieceDeserializer implements JsonDeserializer<ChessPiece> {
    @Override
    public ChessPiece deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        ChessGame.TeamColor color = context.deserialize(obj.get("pieceColor"), ChessGame.TeamColor.class);
        ChessPiece.PieceType type = context.deserialize(obj.get("type"), ChessPiece.PieceType.class);
        ChessPiece piece = new ChessPiece(color, type);
        if (obj.has("hasMoved") && obj.get("hasMoved").getAsBoolean()) {
            piece.setMoved();
        }
        return piece;
    }
}
