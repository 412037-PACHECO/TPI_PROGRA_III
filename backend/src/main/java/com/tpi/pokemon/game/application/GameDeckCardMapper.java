package com.tpi.pokemon.game.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpi.pokemon.cards.domain.CardEntity;
import com.tpi.pokemon.game.domain.enums.CardSubtype;
import com.tpi.pokemon.game.domain.enums.CardSupertype;
import com.tpi.pokemon.game.domain.enums.EnergyType;
import com.tpi.pokemon.game.domain.enums.PokemonType;
import com.tpi.pokemon.game.domain.model.AttackDefinition;
import com.tpi.pokemon.game.domain.model.CardDefinitionRef;
import com.tpi.pokemon.game.domain.model.CardInstance;
import com.tpi.pokemon.game.domain.model.EnergyProfile;
import com.tpi.pokemon.game.domain.model.Resistance;
import com.tpi.pokemon.game.domain.model.Weakness;
import com.tpi.pokemon.game.domain.value.CardInstanceId;
import com.tpi.pokemon.game.domain.value.PlayerId;
import com.tpi.pokemon.game.engine.effect.mapping.Xy1EffectCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GameDeckCardMapper {
    private final ObjectMapper objectMapper;
    private final Xy1EffectCatalog effectCatalog = new Xy1EffectCatalog();

    public GameDeckCardMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CardInstance toInstance(CardEntity card, PlayerId owner, String instanceId) {
        return new CardInstance(new CardInstanceId(instanceId), toDefinition(card), owner);
    }

    public CardDefinitionRef toDefinition(CardEntity card) {
        CardSupertype supertype = supertype(card.getSupertype());
        Set<CardSubtype> subtypes = subtypes(card.getSubtypes());
        return new CardDefinitionRef(
                card.getCardId(),
                card.getName(),
                supertype,
                subtypes,
                card.getEvolvesFrom(),
                card.getConvertedRetreatCost(),
                parseHp(card.getHp()),
                pokemonTypes(card.getTypes()),
                attacks(card),
                weaknesses(card.getWeaknesses()),
                resistances(card.getResistances()),
                energyProfile(card, supertype, subtypes)
        );
    }

    private CardSupertype supertype(String value) {
        String normalized = normalize(value);
        if (normalized.contains("pokemon")) return CardSupertype.POKEMON;
        if (normalized.contains("energy") || normalized.contains("energia")) return CardSupertype.ENERGY;
        return CardSupertype.TRAINER;
    }

    private Set<CardSubtype> subtypes(String json) {
        Set<CardSubtype> values = new LinkedHashSet<>();
        for (String raw : stringArray(json)) {
            String normalized = normalize(raw);
            if (normalized.contains("basic")) values.add(CardSubtype.BASIC);
            if (normalized.contains("stage 1")) values.add(CardSubtype.STAGE_1);
            if (normalized.contains("stage 2")) values.add(CardSubtype.STAGE_2);
            if (normalized.contains("ex")) values.add(CardSubtype.EX);
            if (normalized.contains("item")) values.add(CardSubtype.ITEM);
            if (normalized.contains("supporter")) values.add(CardSubtype.SUPPORTER);
            if (normalized.contains("stadium")) values.add(CardSubtype.STADIUM);
            if (normalized.contains("tool")) values.add(CardSubtype.TOOL);
            if (normalized.contains("energy") || normalized.contains("energia")) values.add(CardSubtype.BASIC_ENERGY);
            if (normalized.contains("special")) values.add(CardSubtype.SPECIAL_ENERGY);
        }
        return values;
    }

    private Integer parseHp(String hp) {
        if (hp == null || hp.isBlank()) return null;
        try {
            return Integer.parseInt(hp.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<PokemonType> pokemonTypes(String json) {
        return stringArray(json).stream().map(this::pokemonType).filter(java.util.Objects::nonNull).toList();
    }

    private PokemonType pokemonType(String value) {
        String normalized = normalize(value);
        if (normalized.contains("grass")) return PokemonType.GRASS;
        if (normalized.contains("fire")) return PokemonType.FIRE;
        if (normalized.contains("water")) return PokemonType.WATER;
        if (normalized.contains("lightning")) return PokemonType.LIGHTNING;
        if (normalized.contains("psychic")) return PokemonType.PSYCHIC;
        if (normalized.contains("fighting")) return PokemonType.FIGHTING;
        if (normalized.contains("darkness")) return PokemonType.DARKNESS;
        if (normalized.contains("metal")) return PokemonType.METAL;
        if (normalized.contains("fairy")) return PokemonType.FAIRY;
        if (normalized.contains("dragon")) return PokemonType.DRAGON;
        if (normalized.contains("colorless")) return PokemonType.COLORLESS;
        return null;
    }

    private EnergyType energyType(String value) {
        String normalized = normalize(value);
        if (normalized.contains("grass")) return EnergyType.GRASS;
        if (normalized.contains("fire")) return EnergyType.FIRE;
        if (normalized.contains("water")) return EnergyType.WATER;
        if (normalized.contains("lightning")) return EnergyType.LIGHTNING;
        if (normalized.contains("psychic")) return EnergyType.PSYCHIC;
        if (normalized.contains("fighting")) return EnergyType.FIGHTING;
        if (normalized.contains("darkness")) return EnergyType.DARKNESS;
        if (normalized.contains("metal")) return EnergyType.METAL;
        if (normalized.contains("fairy")) return EnergyType.FAIRY;
        if (normalized.contains("dragon")) return EnergyType.DRAGON;
        return EnergyType.COLORLESS;
    }

    private List<AttackDefinition> attacks(CardEntity card) {
        if (card.getAttacks() == null || card.getAttacks().isBlank()) return List.of();
        List<AttackDefinition> attacks = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(card.getAttacks());
            if (array != null && array.isArray()) {
                for (JsonNode attack : array) {
                    String name = text(attack, "name");
                    if (name == null || name.isBlank()) continue;
                    List<EnergyType> cost = new ArrayList<>();
                    JsonNode costNode = attack.get("cost");
                    if (costNode != null && costNode.isArray()) {
                        costNode.forEach(node -> cost.add(energyType(node.asText())));
                    }
                    attacks.add(new AttackDefinition(name, name, cost, parseDamage(text(attack, "damage")), effectCatalog.effectsForAttack(card.getCardId(), name)));
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return attacks;
    }

    private int parseDamage(String value) {
        if (value == null) return 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(value.trim());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private List<Weakness> weaknesses(String json) {
        List<Weakness> values = new ArrayList<>();
        for (TypedValue typed : typedValues(json)) {
            PokemonType type = pokemonType(typed.type());
            if (type != null) values.add(new Weakness(type, typed.value() != null && typed.value().contains("x2") ? 2 : 2));
        }
        return values;
    }

    private List<Resistance> resistances(String json) {
        List<Resistance> values = new ArrayList<>();
        for (TypedValue typed : typedValues(json)) {
            PokemonType type = pokemonType(typed.type());
            if (type != null) values.add(new Resistance(type, 20));
        }
        return values;
    }

    private EnergyProfile energyProfile(CardEntity card, CardSupertype supertype, Set<CardSubtype> subtypes) {
        return effectCatalog.energyMappingForCard(card.getCardId()).map(mapping -> mapping.energyProfile()).orElseGet(() -> {
            if (supertype != CardSupertype.ENERGY) return EnergyProfile.none();
            EnergyType type = energyType(card.getName());
            return subtypes.contains(CardSubtype.SPECIAL_ENERGY) ? EnergyProfile.of(type) : EnergyProfile.basic(type);
        });
    }

    private List<String> stringArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return List.of();
            List<String> values = new ArrayList<>();
            node.forEach(item -> values.add(item.asText()));
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<TypedValue> typedValues(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) return List.of();
            List<TypedValue> values = new ArrayList<>();
            node.forEach(item -> values.add(new TypedValue(text(item, "type"), text(item, "value"))));
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("é", "e").replace("í", "i").trim();
    }

    private record TypedValue(String type, String value) {}
}
