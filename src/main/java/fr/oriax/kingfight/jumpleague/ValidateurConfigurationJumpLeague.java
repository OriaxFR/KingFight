package fr.oriax.kingfight.jumpleague;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidateurConfigurationJumpLeague {

    private final KingFight plugin;
    private final ConfigurationJumpLeague configuration;
    private final GestionnaireSetupJumpLeague gestionnaireSetup;

    public ValidateurConfigurationJumpLeague(KingFight plugin, ConfigurationJumpLeague configuration, GestionnaireSetupJumpLeague gestionnaireSetup) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.gestionnaireSetup = gestionnaireSetup;
    }

    public ResultatValidation validerConfigurationComplete() {
        List<String> erreurs = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();

        List<String> mapsDisponibles = plugin.getGestionnairePrincipal()
                .getGestionnaireMaps()
                .obtenirMapsDisponibles(TypeMiniJeu.JUMP_LEAGUE);

        if (mapsDisponibles.isEmpty()) {
            erreurs.add("Aucune map configuree pour JumpLeague");
            return new ResultatValidation(false, erreurs, avertissements);
        }

        int tailleFileAttente = configuration.getTailleFileAttente();

        for (String nomMap : mapsDisponibles) {
            ResultatValidationMap validationMap = validerMap(nomMap, tailleFileAttente);

            if (!validationMap.estValide()) {
                erreurs.addAll(validationMap.getErreurs());
            }

            avertissements.addAll(validationMap.getAvertissements());
        }

        return new ResultatValidation(erreurs.isEmpty(), erreurs, avertissements);
    }

    public ResultatValidationMap validerMap(String nomMap, int tailleFileAttente) {
        List<String> erreurs = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();

        GestionnaireSetupJumpLeague.SetupMonde setup = gestionnaireSetup.obtenirSetup(nomMap);

        if (setup == null) {
            erreurs.add("Map " + nomMap + " : Aucune configuration trouvee");
            return new ResultatValidationMap(nomMap, false, erreurs, avertissements);
        }

        validerSpawnsJoueurs(setup, nomMap, tailleFileAttente, erreurs, avertissements);
        validerCheckpoints(setup, nomMap, tailleFileAttente, erreurs, avertissements);
        validerCoffres(setup, nomMap, tailleFileAttente, erreurs, avertissements);
        validerSpawnsPvp(setup, nomMap, tailleFileAttente, erreurs, avertissements);
        validerBordure(setup, nomMap, erreurs, avertissements);

        return new ResultatValidationMap(nomMap, erreurs.isEmpty(), erreurs, avertissements);
    }

    private void validerSpawnsJoueurs(GestionnaireSetupJumpLeague.SetupMonde setup, String nomMap, int tailleFileAttente, List<String> erreurs, List<String> avertissements) {
        Map<Integer, Location> spawnsParParcours = setup.getSpawnsParParcours();

        for (int i = 1; i <= tailleFileAttente; i++) {
            Location spawn = spawnsParParcours.get(i);
            if (spawn == null) {
                erreurs.add("Map " + nomMap + " : Spawn manquant pour le joueur " + i);
            }
        }

        if (spawnsParParcours.size() < tailleFileAttente) {
            erreurs.add("Map " + nomMap + " : Seulement " + spawnsParParcours.size() + " spawns de joueurs configures sur " + tailleFileAttente + " requis");
        }
    }

    private void validerCheckpoints(GestionnaireSetupJumpLeague.SetupMonde setup, String nomMap, int tailleFileAttente, List<String> erreurs, List<String> avertissements) {
        Map<Integer, List<Location>> checkpointsParParcours = setup.getCheckpointsParParcours();

        for (int i = 1; i <= tailleFileAttente; i++) {
            List<Location> checkpoints = checkpointsParParcours.get(i);

            if (checkpoints == null || checkpoints.isEmpty()) {
                erreurs.add("Map " + nomMap + " : Aucun checkpoint configure pour le joueur " + i);
            } else if (checkpoints.size() < 2) {
                erreurs.add("Map " + nomMap + " : Joueur " + i + " doit avoir au minimum 2 checkpoints (actuellement " + checkpoints.size() + ")");
            }
        }
    }

    private void validerCoffres(GestionnaireSetupJumpLeague.SetupMonde setup, String nomMap, int tailleFileAttente, List<String> erreurs, List<String> avertissements) {
        Map<Integer, List<Location>> coffresParParcours = setup.getCoffresParParcours();

        for (int i = 1; i <= tailleFileAttente; i++) {
            List<Location> coffres = coffresParParcours.get(i);

            if (coffres == null || coffres.isEmpty()) {
                erreurs.add("Map " + nomMap + " : Aucun coffre configure pour le joueur " + i);
            }
        }
    }

    private void validerSpawnsPvp(GestionnaireSetupJumpLeague.SetupMonde setup, String nomMap, int tailleFileAttente, List<String> erreurs, List<String> avertissements) {
        Map<Integer, Location> spawnsPvp = setup.getSpawnsPvp();

        if (spawnsPvp.size() < tailleFileAttente) {
            erreurs.add("Map " + nomMap + " : Seulement " + spawnsPvp.size() + " spawns PvP configures sur " + tailleFileAttente + " requis");
        }

        for (int i = 1; i <= tailleFileAttente; i++) {
            Location spawnPvp = spawnsPvp.get(i);
            if (spawnPvp == null) {
                erreurs.add("Map " + nomMap + " : Spawn PvP manquant pour la position " + i);
            }
        }
    }

    private void validerBordure(GestionnaireSetupJumpLeague.SetupMonde setup, String nomMap, List<String> erreurs, List<String> avertissements) {
        Location centreBordure = setup.getCentreBordure();

        if (centreBordure == null) {
            avertissements.add("Map " + nomMap + " : Centre de bordure non defini, utilisation des coordonnees par defaut");
        }

        int rayonBordure = setup.getRayonBordureInitial();
        if (rayonBordure <= 0) {
            avertissements.add("Map " + nomMap + " : Rayon de bordure invalide (" + rayonBordure + "), utilisation de la valeur par defaut");
        }
    }

    public static class ResultatValidation {
        private final boolean valide;
        private final List<String> erreurs;
        private final List<String> avertissements;

        public ResultatValidation(boolean valide, List<String> erreurs, List<String> avertissements) {
            this.valide = valide;
            this.erreurs = erreurs;
            this.avertissements = avertissements;
        }

        public boolean estValide() { return valide; }
        public List<String> getErreurs() { return erreurs; }
        public List<String> getAvertissements() { return avertissements; }
    }

    public static class ResultatValidationMap {
        private final String nomMap;
        private final boolean valide;
        private final List<String> erreurs;
        private final List<String> avertissements;

        public ResultatValidationMap(String nomMap, boolean valide, List<String> erreurs, List<String> avertissements) {
            this.nomMap = nomMap;
            this.valide = valide;
            this.erreurs = erreurs;
            this.avertissements = avertissements;
        }

        public String getNomMap() { return nomMap; }
        public boolean estValide() { return valide; }
        public List<String> getErreurs() { return erreurs; }
        public List<String> getAvertissements() { return avertissements; }
    }
}
