package fr.oriax.kingfight.commun;

public enum TypeMiniJeu {
    HUNGER_GAMES("Hunger Games", "hungergames"),
    JUMP_LEAGUE("Jump League", "jumpleague"),
    GET_DOWN("GetDown", "getdown");

    private final String nomAffichage;
    private final String nomConfig;

    TypeMiniJeu(String nomAffichage, String nomConfig) {
        this.nomAffichage = nomAffichage;
        this.nomConfig = nomConfig;
    }

    public String getNomAffichage() {
        return nomAffichage;
    }

    public String getNomConfig() {
        return nomConfig;
    }

    public static TypeMiniJeu parNom(String nom) {
        for (TypeMiniJeu type : values()) {
            if (type.nomConfig.equalsIgnoreCase(nom) || type.nomAffichage.equalsIgnoreCase(nom)) {
                return type;
            }
        }
        return null;
    }
}
