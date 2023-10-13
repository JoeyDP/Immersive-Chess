package be.immersivechess.advancement.criterion;

import be.immersivechess.ImmersiveChess;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ChessGameCriterion extends AbstractCriterion<ChessGameCriterion.Condition> {
    public enum Type {
        START,
        WIN,
        LOSE
    }

    @Override
    protected ChessGameCriterion.Condition conditionsFromJson(JsonObject obj, Optional<LootContextPredicate> predicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        JsonPrimitive typeJson = obj.getAsJsonPrimitive("type");
        String typeStr = typeJson.getAsString().toUpperCase();

        try {
            Type type = Type.valueOf(typeStr);
            return new ChessGameCriterion.Condition(predicate, type);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid type found: " + typeStr);
        }
    }

    public void trigger(ServerPlayerEntity player, Type type) {
        trigger(player, condition -> condition.test(type));
    }

    public static class Condition extends AbstractCriterionConditions {

        private final Type type;

        public Condition(Optional<LootContextPredicate> predicate, Type type) {
            super(predicate);
            this.type = type;
        }

        public boolean test(Type type) {
            return this.type == type;
        }

        @Override
        public JsonObject toJson() {
            JsonObject jsonObject = super.toJson();
            jsonObject.add("type", new JsonPrimitive(type.toString()));
            return jsonObject;
        }
    }
}
