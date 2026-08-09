package io.github.endx.rustedfabricapi.api.ini;

/** Bilingual metadata used by generated reference tables and developer tools. */
public final class IniFieldDocumentation {
    private final String valueType;
    private final String descriptionEnglish;
    private final String descriptionChinese;
    private final String example;
    private final IniMultiplayerImpact multiplayerImpact;

    public IniFieldDocumentation(String valueType, String descriptionEnglish,
                                 String descriptionChinese, String example,
                                 IniMultiplayerImpact multiplayerImpact) {
        this.valueType = nonNull(valueType);
        this.descriptionEnglish = nonNull(descriptionEnglish);
        this.descriptionChinese = nonNull(descriptionChinese);
        this.example = nonNull(example);
        this.multiplayerImpact = multiplayerImpact != null
                ? multiplayerImpact : IniMultiplayerImpact.GAMEPLAY_SYNCED;
    }

    public String valueType() { return valueType; }
    public String descriptionEnglish() { return descriptionEnglish; }
    public String descriptionChinese() { return descriptionChinese; }
    public String example() { return example; }
    public IniMultiplayerImpact multiplayerImpact() { return multiplayerImpact; }

    private static String nonNull(String value) {
        return value != null ? value : "";
    }
}
