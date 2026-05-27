package com.tpi.pokemon.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cards", uniqueConstraints = @UniqueConstraint(name = "uk_cards_card_id", columnNames = "card_id"))
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false, unique = true, length = 80)
    private String cardId;

    @Column(nullable = false)
    private String name;

    private String supertype;

    @Column(columnDefinition = "text")
    private String subtypes;

    private String setId;

    private String setName;

    private String number;
    private String rarity;
    private String hp;

    @Column(columnDefinition = "text")
    private String types;

    private String evolvesFrom;

    @Column(columnDefinition = "text")
    private String rules;

    @Column(columnDefinition = "text")
    private String attacks;

    @Column(columnDefinition = "text")
    private String abilities;

    @Column(columnDefinition = "text")
    private String weaknesses;

    @Column(columnDefinition = "text")
    private String resistances;

    @Column(columnDefinition = "text")
    private String retreatCost;

    private Integer convertedRetreatCost;
    private String imageSmall;
    private String imageLarge;

    @Column(columnDefinition = "text")
    private String rawJson;

    public Long getId() { return id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupertype() { return supertype; }
    public void setSupertype(String supertype) { this.supertype = supertype; }
    public String getSubtypes() { return subtypes; }
    public void setSubtypes(String subtypes) { this.subtypes = subtypes; }
    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public String getHp() { return hp; }
    public void setHp(String hp) { this.hp = hp; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public String getEvolvesFrom() { return evolvesFrom; }
    public void setEvolvesFrom(String evolvesFrom) { this.evolvesFrom = evolvesFrom; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
    public String getAttacks() { return attacks; }
    public void setAttacks(String attacks) { this.attacks = attacks; }
    public String getAbilities() { return abilities; }
    public void setAbilities(String abilities) { this.abilities = abilities; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public String getResistances() { return resistances; }
    public void setResistances(String resistances) { this.resistances = resistances; }
    public String getRetreatCost() { return retreatCost; }
    public void setRetreatCost(String retreatCost) { this.retreatCost = retreatCost; }
    public Integer getConvertedRetreatCost() { return convertedRetreatCost; }
    public void setConvertedRetreatCost(Integer convertedRetreatCost) { this.convertedRetreatCost = convertedRetreatCost; }
    public String getImageSmall() { return imageSmall; }
    public void setImageSmall(String imageSmall) { this.imageSmall = imageSmall; }
    public String getImageLarge() { return imageLarge; }
    public void setImageLarge(String imageLarge) { this.imageLarge = imageLarge; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
