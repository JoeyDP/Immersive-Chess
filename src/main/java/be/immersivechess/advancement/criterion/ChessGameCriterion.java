package be.immersivechess.advancement.criterion;

import be.immersivechess.ImmersiveChess;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ChessGameCriterion extends AbstractCriterion<ChessGameCriterion.Condition> {

    public enum Type {
        START,
        WIN,
        LOSE
    }

    public static final Identifier IDENTIFIER = new Identifier(ImmersiveChess.MOD_ID, "game_trigger");

    @Override
    protected ChessGameCriterion.Condition conditionsFromJson(JsonObject obj, EntityPredicate.Extended playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        JsonPrimitive typeJson = obj.getAsJsonPrimitive("type");
        String typeStr = typeJson.getAsString().toUpperCase();

        try {
            Type type = Type.valueOf(typeStr);
            return new ChessGameCriterion.Condition(playerPredicate, type);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid type found: " + typeStr);
        }
    }

    @Override
    public Identifier getId() {
        return IDENTIFIER;
    }

    public void trigger(ServerPlayerEntity player, Type type) {
        trigger(player, condition -> condition.test(type));
    }

    public static class Condition extends AbstractCriterionConditions {

        private final Type type;

        public Condition(EntityPredicate.Extended playerPredicate, Type type) {
            super(IDENTIFIER, playerPredicate);
            this.type = type;
        }

        public boolean test(Type type) {
            return this.type == type;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("type", new JsonPrimitive(type.toString()));
            return jsonObject;
        }
    }
}
