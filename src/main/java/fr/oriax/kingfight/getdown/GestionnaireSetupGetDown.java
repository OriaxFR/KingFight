package fr.oriax.kingfight.getdown;

import fr.oriax.kingfight.KingFight;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GestionnaireSetupGetDown {

    private final KingFight plugin;
    private final Map<String, SetupMonde> setupsMondes;
    private ConfigurationMultiMaps configurationMultiMaps;

    public GestionnaireSetupGetDown(KingFight plugin) {
        this.plugin = plugin;
        this.setupsMondes = new HashMap<>();
        this.configurationMultiMaps = new ConfigurationMultiMaps();
        verifierEtCreerDossiers();
        chargerTousLesSetups();
        chargerConfigurationMultiMaps();
    }

    private void verifierEtCreerDossiers() {
        File dossierSetups = new File(plugin.getDataFolder(), "getdown/setups");
        if (!dossierSetups.exists()) {
            dossierSetups.mkdirs();
        }
        
        File dossierBackups = new File(plugin.getDataFolder(), "getdown/backups");
        if (!dossierBackups.exists()) {
            dossierBackups.mkdirs();
        }
    }

    public static class ZoneArrivee {
        private final Location coin1;
        private final Location coin2;

        public ZoneArrivee(Location coin1, Location coin2) {
            this.coin1 = coin1;
            this.coin2 = coin2;
        }

        public boolean contientJoueur(Player joueur) {
            Location posJoueur = joueur.getLocation();

            if (!posJoueur.getWorld().equals(coin1.getWorld())) {
                return false;
            }

            double minX = Math.min(coin1.getX(), coin2.getX());
            double maxX = Math.max(coin1.getX(), coin2.getX());
            double minZ = Math.min(coin1.getZ(), coin2.getZ());
            double maxZ = Math.max(coin1.getZ(), coin2.getZ());
            double minY = Math.min(coin1.getY(), coin2.getY());
            double maxY = Math.max(coin1.getY(), coin2.getY()) + 3;

            boolean dansZone = posJoueur.getX() >= minX && posJoueur.getX() <= maxX &&
                              posJoueur.getZ() >= minZ && posJoueur.getZ() <= maxZ &&
                              posJoueur.getY() >= minY && posJoueur.getY() <= maxY;

            return dansZone;
        }

        public Location getCoin1() { return coin1; }
        public Location getCoin2() { return coin2; }
    }

    public static class ConfigurationMultiMaps {
        private String mapParcours1;
        private String mapParcours2;
        private String mapParcours3;
        private String mapPvp;

        public ConfigurationMultiMaps() {
            this.mapParcours1 = null;
            this.mapParcours2 = null;
            this.mapParcours3 = null;
            this.mapPvp = null;
        }

        public String getMapParcours1() { return mapParcours1; }
        public String getMapParcours2() { return mapParcours2; }
        public String getMapParcours3() { return mapParcours3; }
        public String getMapPvp() { return mapPvp; }

        public void setMapParcours1(String map) { this.mapParcours1 = map; }
        public void setMapParcours2(String map) { this.mapParcours2 = map; }
        public void setMapParcours3(String map) { this.mapParcours3 = map; }
        public void setMapPvp(String map) { this.mapPvp = map; }

        public boolean estComplet() {
            return mapParcours1 != null && mapParcours2 != null && mapParcours3 != null;
        }

        public boolean aPhasePvp() {
            return mapPvp != null;
        }
    }

    public static class SetupMonde {
        private final List<Location> spawnsJoueurs;
        private final List<Location> spawnsDescente;
        private final List<Location> arriveesMaps;
        private final List<ZoneArrivee> zonesArrivees;
        private final Map<Integer, Integer> hauteursArrivee;
        private final Location spawnStuff;
        private final Map<Integer, Location> spawnsPvp;
        private final Location centreBordure;
        private final int rayonBordureInitial;

        public SetupMonde() {
            this.spawnsJoueurs = new ArrayList<>();
            this.spawnsDescente = new ArrayList<>();
            this.arriveesMaps = new ArrayList<>();
            this.zonesArrivees = new ArrayList<>();
            this.hauteursArrivee = new HashMap<>();
            this.spawnStuff = null;
            this.spawnsPvp = new HashMap<>();
            this.centreBordure = null;
            this.rayonBordureInitial = 100;
        }

        public SetupMonde(List<Location> spawnsJoueurs, List<Location> spawnsDescente,
                         List<Location> arriveesMaps, List<ZoneArrivee> zonesArrivees, Map<Integer, Integer> hauteursArrivee,
                         Location spawnStuff, Map<Integer, Location> spawnsPvp, Location centreBordure, int rayonBordureInitial) {
            this.spawnsJoueurs = spawnsJoueurs;
            this.spawnsDescente = spawnsDescente;
            this.arriveesMaps = arriveesMaps != null ? arriveesMaps : new ArrayList<>();
            this.zonesArrivees = zonesArrivees != null ? zonesArrivees : new ArrayList<>();
            this.hauteursArrivee = hauteursArrivee != null ? hauteursArrivee : new HashMap<>();
            this.spawnStuff = spawnStuff;
            this.spawnsPvp = spawnsPvp != null ? spawnsPvp : new HashMap<>();
            this.centreBordure = centreBordure;
            this.rayonBordureInitial = rayonBordureInitial;
        }

        public List<Location> getSpawnsJoueurs() { return spawnsJoueurs; }
        public List<Location> getSpawnsDescente() { return spawnsDescente; }
        public List<Location> getArriveesMaps() { return arriveesMaps; }
        public List<ZoneArrivee> getZonesArrivees() { return zonesArrivees; }
        public Map<Integer, Integer> getHauteursArrivee() { return hauteursArrivee; }
        public Integer getHauteurArrivee(int parcours) { return hauteursArrivee.get(parcours); }
        public void setHauteurArrivee(int parcours, int hauteur) { hauteursArrivee.put(parcours, hauteur); }
        public Location getSpawnStuff() { return spawnStuff; }
        public Map<Integer, Location> getSpawnsPvp() { return spawnsPvp; }
        public Location getSpawnPvp() { return spawnsPvp.isEmpty() ? null : spawnsPvp.values().iterator().next(); }
        public Location getCentreBordure() { return centreBordure; }
        public int getRayonBordureInitial() { return rayonBordureInitial; }
        
        public void definirSpawnPvp(int numeroSpawn, Location location) {
            spawnsPvp.put(numeroSpawn, location);
        }
    }

    public void definirSpawnMap(Player joueur, int numeroMap) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        String mapConfigureePourParcours = null;
        switch (numeroMap) {
            case 0: mapConfigureePourParcours = configurationMultiMaps.getMapParcours1(); break;
            case 1: mapConfigureePourParcours = configurationMultiMaps.getMapParcours2(); break;
            case 2: mapConfigureePourParcours = configurationMultiMaps.getMapParcours3(); break;
        }

        if (mapConfigureePourParcours == null) {
            joueur.sendMessage("§cErreur : Aucune map configurée pour le parcours " + (numeroMap + 1) + " !");
            joueur.sendMessage("§eUtilisez : /setup getdown map-parcours " + (numeroMap + 1));
            return;
        }

        if (!nomMonde.equals(mapConfigureePourParcours)) {
            joueur.sendMessage("§cErreur : Vous êtes sur la map '" + nomMonde + "' mais le parcours " + (numeroMap + 1) + " est configuré pour la map '" + mapConfigureePourParcours + "' !");
            joueur.sendMessage("§eAllez sur la map '" + mapConfigureePourParcours + "' ou reconfigurez avec : /setup getdown map-parcours " + (numeroMap + 1));
            return;
        }

        ajouterSpawnDescente(nomMonde, 0, location);
        joueur.sendMessage("§aSpawn parcours " + (numeroMap + 1) + " defini pour GetDown sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7Position: " + formatLocation(location));
    }

    public void definirArriveeMap(Player joueur, int numeroMap) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        String mapConfigureePourParcours = null;
        switch (numeroMap) {
            case 0: mapConfigureePourParcours = configurationMultiMaps.getMapParcours1(); break;
            case 1: mapConfigureePourParcours = configurationMultiMaps.getMapParcours2(); break;
            case 2: mapConfigureePourParcours = configurationMultiMaps.getMapParcours3(); break;
        }

        if (mapConfigureePourParcours == null) {
            joueur.sendMessage("§cErreur : Aucune map configurée pour le parcours " + (numeroMap + 1) + " !");
            joueur.sendMessage("§eUtilisez : /setup getdown map-parcours " + (numeroMap + 1));
            return;
        }

        if (!nomMonde.equals(mapConfigureePourParcours)) {
            joueur.sendMessage("§cErreur : Vous êtes sur la map '" + nomMonde + "' mais le parcours " + (numeroMap + 1) + " est configuré pour la map '" + mapConfigureePourParcours + "' !");
            joueur.sendMessage("§eAllez sur la map '" + mapConfigureePourParcours + "' ou reconfigurez avec : /setup getdown map-parcours " + (numeroMap + 1));
            return;
        }

        ajouterArriveeMap(nomMonde, 0, location);
        joueur.sendMessage("§aArrivee parcours " + (numeroMap + 1) + " definie pour GetDown sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7Position: " + formatLocation(location));
    }

    @Deprecated
    public void definirSpawnStuff(Player joueur) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        String mapPvpConfiguree = configurationMultiMaps.getMapPvp();
        boolean mapValide = false;
        
        if (mapPvpConfiguree != null && nomMonde.equals(mapPvpConfiguree)) {
            mapValide = true;
        } else if (mapPvpConfiguree == null) {
            String mapParcours3 = configurationMultiMaps.getMapParcours3();
            if (mapParcours3 != null && nomMonde.equals(mapParcours3)) {
                mapValide = true;
            }
        }

        if (!mapValide) {
            String mapAttendue = mapPvpConfiguree != null ? mapPvpConfiguree : configurationMultiMaps.getMapParcours3();
            joueur.sendMessage("§cErreur : Le spawn stuff doit être défini sur la map PvP !");
            joueur.sendMessage("§eMap attendue: '" + mapAttendue + "', map actuelle: '" + nomMonde + "'");
            return;
        }

        definirSpawnStuff(nomMonde, location);
        joueur.sendMessage("§aSpawn stuff defini pour GetDown sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7Position: " + formatLocation(location));
    }

    public void definirSpawnPvP(Player joueur) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        definirSpawnPvp(nomMonde, location);
        joueur.sendMessage("§aSpawn PvP defini pour GetDown !");
    }

    public void ajouterSpawnPvPMultiple(Player joueur, int numeroSpawn) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        String mapPvpConfiguree = configurationMultiMaps.getMapPvp();
        boolean mapValide = false;
        
        if (mapPvpConfiguree != null && nomMonde.equals(mapPvpConfiguree)) {
            mapValide = true;
        } else if (mapPvpConfiguree == null) {
            String mapParcours3 = configurationMultiMaps.getMapParcours3();
            if (mapParcours3 != null && nomMonde.equals(mapParcours3)) {
                mapValide = true;
            }
        }

        if (!mapValide) {
            String mapAttendue = mapPvpConfiguree != null ? mapPvpConfiguree : configurationMultiMaps.getMapParcours3();
            joueur.sendMessage("§cErreur : Les spawns PvP doivent être définis sur la map PvP !");
            joueur.sendMessage("§eMap attendue: '" + mapAttendue + "', map actuelle: '" + nomMonde + "'");
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        Map<Integer, Location> nouveauxSpawnsPvp = new HashMap<>(setup.getSpawnsPvp());
        nouveauxSpawnsPvp.put(numeroSpawn, location);

        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsJoueurs(),
            setup.getSpawnsDescente(),
            setup.getArriveesMaps(),
            setup.getZonesArrivees(),
            setup.getHauteursArrivee(),
            setup.getSpawnStuff(),
            nouveauxSpawnsPvp,
            setup.getCentreBordure(),
            setup.getRayonBordureInitial()
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);

        joueur.sendMessage("§aSpawn PvP " + (numeroSpawn + 1) + " defini pour GetDown sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7Position: " + formatLocation(location));
    }

    public void definirHauteurArrivee(Player joueur, int numeroParcours, int hauteur) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        String mapConfigureePourParcours = null;
        switch (numeroParcours) {
            case 0: mapConfigureePourParcours = configurationMultiMaps.getMapParcours1(); break;
            case 1: mapConfigureePourParcours = configurationMultiMaps.getMapParcours2(); break;
            case 2: mapConfigureePourParcours = configurationMultiMaps.getMapParcours3(); break;
        }

        if (mapConfigureePourParcours == null) {
            joueur.sendMessage("§cErreur : Aucune map configurée pour le parcours " + (numeroParcours + 1) + " !");
            joueur.sendMessage("§eUtilisez : /setup getdown map-parcours " + (numeroParcours + 1));
            return;
        }

        if (!nomMonde.equals(mapConfigureePourParcours)) {
            joueur.sendMessage("§cErreur : Vous êtes sur la map '" + nomMonde + "' mais le parcours " + (numeroParcours + 1) + " est configuré pour la map '" + mapConfigureePourParcours + "' !");
            joueur.sendMessage("§eAllez sur la map '" + mapConfigureePourParcours + "' ou reconfigurez avec : /setup getdown map-parcours " + (numeroParcours + 1));
            return;
        }

        if (hauteur < 0 || hauteur > 256) {
            joueur.sendMessage("§cErreur : La hauteur doit être comprise entre 0 et 256 !");
            return;
        }

        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.setHauteurArrivee(numeroParcours, hauteur);
        sauvegarderSetup(nomMonde, setup);

        String nomParcours = numeroParcours == 0 ? "premier" : numeroParcours == 1 ? "deuxieme" : "troisieme";
        joueur.sendMessage("§aHauteur d'arrivee definie pour le " + nomParcours + " parcours sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7» Hauteur Y: §e" + hauteur + " §7et en dessous");
        joueur.sendMessage("§7» Les joueurs qui atteignent cette hauteur ou moins passeront a la map suivante");
    }

    private String formatLocation(Location loc) {
        return (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ();
    }

    public void definirCentreBordure(Player joueur, int rayon) {
        Location location = joueur.getLocation();
        String nomMonde = location.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }
        String mapPvpConfiguree = configurationMultiMaps.getMapPvp();
        boolean mapValide = false;
        
        if (mapPvpConfiguree != null && nomMonde.equals(mapPvpConfiguree)) {
            mapValide = true;
        } else if (mapPvpConfiguree == null) {
            String mapParcours3 = configurationMultiMaps.getMapParcours3();
            if (mapParcours3 != null && nomMonde.equals(mapParcours3)) {
                mapValide = true;
            }
        }

        if (!mapValide) {
            String mapAttendue = mapPvpConfiguree != null ? mapPvpConfiguree : configurationMultiMaps.getMapParcours3();
            joueur.sendMessage("§cErreur : Le centre de bordure doit être défini sur la map PvP !");
            joueur.sendMessage("§eMap attendue: '" + mapAttendue + "', map actuelle: '" + nomMonde + "'");
            return;
        }

        location.getWorld().getWorldBorder().setCenter(location.getX(), location.getZ());
        location.getWorld().getWorldBorder().setSize(rayon * 2);

        definirCentreBordure(nomMonde, location, rayon);
        joueur.sendMessage("§aCentre de bordure defini pour GetDown sur la map '" + nomMonde + "' !");
        joueur.sendMessage("§7Position: " + formatLocation(location));
        joueur.sendMessage("§7Rayon initial: " + rayon + " blocs");
    }

    public void ajouterSpawnJoueur(String nomMonde, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        setup.getSpawnsJoueurs().add(location);
        sauvegarderSetup(nomMonde, setup);
    }

    public void ajouterSpawnDescente(String nomMonde, int numeroMap, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);

        while (setup.getSpawnsDescente().size() <= numeroMap) {
            setup.getSpawnsDescente().add(null);
        }

        setup.getSpawnsDescente().set(numeroMap, location);
        sauvegarderSetup(nomMonde, setup);
    }

    public void ajouterArriveeMap(String nomMonde, int numeroMap, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);

        while (setup.getArriveesMaps().size() <= numeroMap) {
            setup.getArriveesMaps().add(null);
        }

        setup.getArriveesMaps().set(numeroMap, location);
        sauvegarderSetup(nomMonde, setup);
    }

    public void ajouterZoneArrivee(String nomMonde, int numeroMap, ZoneArrivee zone) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);

        while (setup.getZonesArrivees().size() <= numeroMap) {
            setup.getZonesArrivees().add(null);
        }

        setup.getZonesArrivees().set(numeroMap, zone);
        sauvegarderSetup(nomMonde, setup);
    }
    @Deprecated
    public void definirSpawnStuff(String nomMonde, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsJoueurs(),
            setup.getSpawnsDescente(),
            setup.getArriveesMaps(),
            setup.getZonesArrivees(),
            setup.getHauteursArrivee(),
            location,
            setup.getSpawnsPvp(),
            setup.getCentreBordure(),
            setup.getRayonBordureInitial()
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    public void definirSpawnPvp(String nomMonde, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        Map<Integer, Location> nouveauxSpawnsPvp = new HashMap<>();
        nouveauxSpawnsPvp.put(0, location);

        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsJoueurs(),
            setup.getSpawnsDescente(),
            setup.getArriveesMaps(),
            setup.getZonesArrivees(),
            setup.getHauteursArrivee(),
            setup.getSpawnStuff(),
            nouveauxSpawnsPvp,
            setup.getCentreBordure(),
            setup.getRayonBordureInitial()
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    public void ajouterSpawnPvp(String nomMonde, Location location) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        Map<Integer, Location> nouveauxSpawnsPvp = new HashMap<>(setup.getSpawnsPvp());
        int prochainIndex = nouveauxSpawnsPvp.isEmpty() ? 0 : Collections.max(nouveauxSpawnsPvp.keySet()) + 1;
        nouveauxSpawnsPvp.put(prochainIndex, location);

        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsJoueurs(),
            setup.getSpawnsDescente(),
            setup.getArriveesMaps(),
            setup.getZonesArrivees(),
            setup.getHauteursArrivee(),
            setup.getSpawnStuff(),
            nouveauxSpawnsPvp,
            setup.getCentreBordure(),
            setup.getRayonBordureInitial()
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    public void definirCentreBordure(String nomMonde, Location location, int rayon) {
        SetupMonde setup = obtenirOuCreerSetup(nomMonde);
        SetupMonde nouveauSetup = new SetupMonde(
            setup.getSpawnsJoueurs(),
            setup.getSpawnsDescente(),
            setup.getArriveesMaps(),
            setup.getZonesArrivees(),
            setup.getHauteursArrivee(),
            setup.getSpawnStuff(),
            setup.getSpawnsPvp(),
            location,
            rayon
        );
        setupsMondes.put(nomMonde, nouveauSetup);
        sauvegarderSetup(nomMonde, nouveauSetup);
    }

    private SetupMonde obtenirOuCreerSetup(String nomMonde) {
        return setupsMondes.computeIfAbsent(nomMonde, k -> new SetupMonde());
    }

    public SetupMonde getSetupMonde(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) {
            return setupsMondes.get(nomMonde);
        }

        World monde = plugin.getServer().getWorld(nomMonde);
        if (monde == null) {
            return setupsMondes.get(nomMonde);
        }

        return chargerSetupDepuisFichier(monde);
    }

    private SetupMonde chargerSetupDepuisFichier(World monde) {
        File fichierSetup = new File(plugin.getDataFolder(), "getdown/setups/" + monde.getName() + ".yml");
        if (!fichierSetup.exists()) {
            return new SetupMonde();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        List<Location> spawnsJoueurs = new ArrayList<>();
        List<Location> spawnsDescente = new ArrayList<>();
        List<Location> arriveesMaps = new ArrayList<>();
        List<ZoneArrivee> zonesArrivees = new ArrayList<>();
        Map<Integer, Integer> hauteursArrivee = new HashMap<>();
        Location spawnStuff = null;
        Map<Integer, Location> spawnsPvp = new HashMap<>();
        Location centreBordure = null;
        int rayonBordure = 500;

        if (config.contains("spawns-joueurs")) {
            for (String spawnStr : config.getStringList("spawns-joueurs")) {
                Location spawn = stringVersLocation(spawnStr, monde);
                if (spawn != null) {
                    spawnsJoueurs.add(spawn);
                }
            }
        }

        if (config.contains("spawns-descente")) {
            for (String spawnStr : config.getStringList("spawns-descente")) {
                Location spawn = stringVersLocation(spawnStr, monde);
                if (spawn != null) {
                    spawnsDescente.add(spawn);
                }
            }
        }

        if (config.contains("arrivees-maps")) {
            for (String arriveeStr : config.getStringList("arrivees-maps")) {
                Location arrivee = stringVersLocation(arriveeStr, monde);
                if (arrivee != null) {
                    arriveesMaps.add(arrivee);
                }
            }
        }

        if (config.contains("zones-arrivees")) {
            for (String zoneStr : config.getStringList("zones-arrivees")) {
                if ("null".equals(zoneStr)) {
                    zonesArrivees.add(null);
                } else {
                    String[] parts = zoneStr.split("\\|");
                    if (parts.length == 2) {
                        Location coin1 = stringVersLocation(parts[0], monde);
                        Location coin2 = stringVersLocation(parts[1], monde);
                        if (coin1 != null && coin2 != null) {
                            zonesArrivees.add(new ZoneArrivee(coin1, coin2));
                        } else {
                            zonesArrivees.add(null);
                        }
                    } else {
                        zonesArrivees.add(null);
                    }
                }
            }
        }

        if (config.contains("hauteurs-arrivee")) {
            for (String key : config.getConfigurationSection("hauteurs-arrivee").getKeys(false)) {
                if (key.startsWith("parcours-")) {
                    try {
                        int parcours = Integer.parseInt(key.replace("parcours-", ""));
                        int hauteur = config.getInt("hauteurs-arrivee." + key);
                        hauteursArrivee.put(parcours, hauteur);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Erreur lors du chargement de la hauteur d'arrivée: " + key);
                    }
                }
            }
        }

        if (config.contains("spawn-stuff")) {
            spawnStuff = stringVersLocation(config.getString("spawn-stuff"), monde);
        }

        if (config.contains("spawns-pvp")) {
            List<String> spawnsList = config.getStringList("spawns-pvp");
            for (int i = 0; i < spawnsList.size(); i++) {
                Location spawn = stringVersLocation(spawnsList.get(i), monde);
                if (spawn != null) {
                    spawnsPvp.put(i, spawn);
                }
            }
        } else if (config.contains("spawn-pvp")) {
            Location spawnPvp = stringVersLocation(config.getString("spawn-pvp"), monde);
            if (spawnPvp != null) {
                spawnsPvp.put(0, spawnPvp);
            }
        }

        if (config.contains("bordure.centre")) {
            centreBordure = stringVersLocation(config.getString("bordure.centre"), monde);
        }

        if (config.contains("bordure.rayon")) {
            rayonBordure = config.getInt("bordure.rayon");
        }

        return new SetupMonde(spawnsJoueurs, spawnsDescente, arriveesMaps, zonesArrivees, hauteursArrivee, spawnStuff, spawnsPvp, centreBordure, rayonBordure);
    }

    public boolean mondeEstConfigure(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        boolean aZonesArrivee = !setup.getZonesArrivees().isEmpty() || !setup.getArriveesMaps().isEmpty();

        return !setup.getSpawnsDescente().isEmpty() &&
               aZonesArrivee &&
               setup.getSpawnStuff() != null &&
               !setup.getSpawnsPvp().isEmpty() &&
               setup.getCentreBordure() != null;
    }

    private void sauvegarderSetup(String nomMonde, SetupMonde setup) {
        File dossierSetups = new File(plugin.getDataFolder(), "getdown/setups");
        if (!dossierSetups.exists()) {
            dossierSetups.mkdirs();
        }

        File fichierSetup = new File(dossierSetups, nomMonde + ".yml");
        FileConfiguration config = new YamlConfiguration();

        List<String> spawnsJoueurs = new ArrayList<>();
        for (Location loc : setup.getSpawnsJoueurs()) {
            spawnsJoueurs.add(locationVersString(loc));
        }
        config.set("spawns-joueurs", spawnsJoueurs);

        List<String> spawnsDescente = new ArrayList<>();
        for (Location loc : setup.getSpawnsDescente()) {
            spawnsDescente.add(loc != null ? locationVersString(loc) : "null");
        }
        config.set("spawns-descente", spawnsDescente);

        List<String> arriveesMaps = new ArrayList<>();
        for (Location loc : setup.getArriveesMaps()) {
            arriveesMaps.add(loc != null ? locationVersString(loc) : "null");
        }
        config.set("arrivees-maps", arriveesMaps);

        List<String> zonesArrivees = new ArrayList<>();
        for (ZoneArrivee zone : setup.getZonesArrivees()) {
            if (zone != null) {
                String zoneStr = locationVersString(zone.getCoin1()) + "|" + locationVersString(zone.getCoin2());
                zonesArrivees.add(zoneStr);
            } else {
                zonesArrivees.add("null");
            }
        }
        config.set("zones-arrivees", zonesArrivees);

        Map<String, Integer> hauteursArriveeConfig = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : setup.getHauteursArrivee().entrySet()) {
            hauteursArriveeConfig.put("parcours-" + entry.getKey(), entry.getValue());
        }
        config.createSection("hauteurs-arrivee", hauteursArriveeConfig);

        if (setup.getSpawnStuff() != null) {
            config.set("spawn-stuff", locationVersString(setup.getSpawnStuff()));
        }

        List<String> spawnsPvp = new ArrayList<>();
        Map<Integer, Location> spawnsPvpMap = setup.getSpawnsPvp();

        int maxIndex = -1;
        for (Integer index : spawnsPvpMap.keySet()) {
            if (index > maxIndex) {
                maxIndex = index;
            }
        }

        for (int i = 0; i <= maxIndex; i++) {
            Location loc = spawnsPvpMap.get(i);
            if (loc != null) {
                spawnsPvp.add(locationVersString(loc));
            } else {
                spawnsPvp.add("null");
            }
        }
        config.set("spawns-pvp", spawnsPvp);

        if (setup.getCentreBordure() != null) {
            config.set("centre-bordure", locationVersString(setup.getCentreBordure()));
            config.set("rayon-bordure-initial", setup.getRayonBordureInitial());
        }

        try {
            config.save(fichierSetup);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde du setup GetDown pour " + nomMonde + ": " + e.getMessage());
        }
    }

    private void chargerTousLesSetups() {
        File dossierSetups = new File(plugin.getDataFolder(), "getdown/setups");
        if (!dossierSetups.exists()) return;

        File[] fichiers = dossierSetups.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fichiers == null) return;

        int setupsCharges = 0;
        for (File fichier : fichiers) {
            String nomMonde = fichier.getName().replace(".yml", "");
            chargerSetup(nomMonde);
            if (verifierIntegriteSetup(nomMonde)) {
                setupsCharges++;
            }
        }
        
        if (setupsCharges > 0) {
            plugin.getLogger().info("Setups GetDown charges: " + setupsCharges + " mondes configures");
        }
    }

    private void chargerSetup(String nomMonde) {
        File fichierSetup = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        if (!fichierSetup.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierSetup);

        List<Location> spawnsJoueurs = new ArrayList<>();
        for (String locStr : config.getStringList("spawns-joueurs")) {
            Location loc = stringVersLocation(locStr);
            if (loc != null) spawnsJoueurs.add(loc);
        }

        List<Location> spawnsDescente = new ArrayList<>();
        for (String locStr : config.getStringList("spawns-descente")) {
            if ("null".equals(locStr)) {
                spawnsDescente.add(null);
            } else {
                Location loc = stringVersLocation(locStr);
                spawnsDescente.add(loc);
            }
        }

        List<Location> arriveesMaps = new ArrayList<>();
        for (String locStr : config.getStringList("arrivees-maps")) {
            if ("null".equals(locStr)) {
                arriveesMaps.add(null);
            } else {
                Location loc = stringVersLocation(locStr);
                arriveesMaps.add(loc);
            }
        }

        List<ZoneArrivee> zonesArrivees = new ArrayList<>();
        for (String zoneStr : config.getStringList("zones-arrivees")) {
            if ("null".equals(zoneStr)) {
                zonesArrivees.add(null);
            } else {
                String[] parts = zoneStr.split("\\|");
                if (parts.length == 2) {
                    Location coin1 = stringVersLocation(parts[0]);
                    Location coin2 = stringVersLocation(parts[1]);
                    if (coin1 != null && coin2 != null) {
                        zonesArrivees.add(new ZoneArrivee(coin1, coin2));
                    } else {
                        zonesArrivees.add(null);
                    }
                } else {
                    zonesArrivees.add(null);
                }
            }
        }

        Location spawnStuff = null;
        if (config.contains("spawn-stuff")) {
            spawnStuff = stringVersLocation(config.getString("spawn-stuff"));
        }

        Map<Integer, Location> spawnsPvp = new HashMap<>();

        if (config.contains("spawn-pvp")) {
            Location spawnPvp = stringVersLocation(config.getString("spawn-pvp"));
            if (spawnPvp != null) {
                spawnsPvp.put(0, spawnPvp);
            }
        }

        if (config.contains("spawns-pvp")) {
            List<String> spawnsPvpList = config.getStringList("spawns-pvp");
            for (int i = 0; i < spawnsPvpList.size(); i++) {
                String locStr = spawnsPvpList.get(i);
                if (!"null".equals(locStr)) {
                    Location loc = stringVersLocation(locStr);
                    if (loc != null) {
                        spawnsPvp.put(i, loc);
                    }
                }
            }
        }

        Map<Integer, Integer> hauteursArrivee = new HashMap<>();
        if (config.contains("hauteurs-arrivee")) {
            for (String key : config.getConfigurationSection("hauteurs-arrivee").getKeys(false)) {
                if (key.startsWith("parcours-")) {
                    try {
                        int parcours = Integer.parseInt(key.replace("parcours-", ""));
                        int hauteur = config.getInt("hauteurs-arrivee." + key);
                        hauteursArrivee.put(parcours, hauteur);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Erreur lors du chargement de la hauteur d'arrivée: " + key);
                    }
                }
            }
        }

        Location centreBordure = null;
        int rayonBordure = 100;
        if (config.contains("centre-bordure")) {
            centreBordure = stringVersLocation(config.getString("centre-bordure"));
            rayonBordure = config.getInt("rayon-bordure-initial", 100);
        }

        SetupMonde setup = new SetupMonde(spawnsJoueurs, spawnsDescente, arriveesMaps, zonesArrivees, hauteursArrivee, spawnStuff, spawnsPvp, centreBordure, rayonBordure);
        setupsMondes.put(nomMonde, setup);
    }

    private String locationVersString(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private Location stringVersLocation(String str) {
        String[] parts = str.split(";");
        if (parts.length != 6) return null;

        World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Location stringVersLocation(String str, World mondeForce) {
        if (str == null) return null;

        String[] parts = str.split(";");
        if (parts.length != 6) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(mondeForce, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean verifierSpawnsConfigures(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            chargerSetup(nomMonde);
            setup = setupsMondes.get(nomMonde);
        }
        return setup != null && !setup.getSpawnsJoueurs().isEmpty() && setup.getSpawnsJoueurs().size() >= 16;
    }

    public boolean verifierArriveesConfigurees(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            chargerSetup(nomMonde);
            setup = setupsMondes.get(nomMonde);
        }
        return setup != null && !setup.getArriveesMaps().isEmpty() && setup.getArriveesMaps().size() >= 3;
    }

    public boolean verifierStuffConfigure(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            chargerSetup(nomMonde);
            setup = setupsMondes.get(nomMonde);
        }
        return setup != null && setup.getSpawnStuff() != null;
    }

    public boolean verifierPvpConfigure(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            chargerSetup(nomMonde);
            setup = setupsMondes.get(nomMonde);
        }
        return setup != null && !setup.getSpawnsPvp().isEmpty();
    }

    public void afficherInfosSetup(Player joueur, String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            chargerSetup(nomMonde);
            setup = setupsMondes.get(nomMonde);
        }

        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("§6§l           SETUP GETDOWN - " + nomMonde.toUpperCase());
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("");

        joueur.sendMessage("§e» Spawns descente:");
        for (int i = 0; i < setup.getSpawnsDescente().size(); i++) {
            Location spawn = setup.getSpawnsDescente().get(i);
            String statut = spawn != null ? "§a✓“" : "§c✗";
            String nomParcours = i == 0 ? "1er" : i == 1 ? "2e" : "3e";
            joueur.sendMessage("  §7" + nomParcours + " parcours: " + statut);
        }

        joueur.sendMessage("§e» Zones d'arrivee:");
        for (int i = 0; i < setup.getZonesArrivees().size(); i++) {
            GestionnaireSetupGetDown.ZoneArrivee zone = setup.getZonesArrivees().get(i);
            String statut = zone != null ? "§a✓“" : "§c✗";
            String nomParcours = i == 0 ? "1er" : i == 1 ? "2e" : "3e";
            joueur.sendMessage("  §7" + nomParcours + " parcours: " + statut);
        }

        if (!setup.getArriveesMaps().isEmpty()) {
            joueur.sendMessage("§e» Points d'arrivee (ancien systeme):");
            for (int i = 0; i < setup.getArriveesMaps().size(); i++) {
                Location arrivee = setup.getArriveesMaps().get(i);
                String statut = arrivee != null ? "§a✓“" : "§c✗";
                String nomParcours = i == 0 ? "1er" : i == 1 ? "2e" : "3e";
                joueur.sendMessage("  §7" + nomParcours + " parcours: " + statut);
            }
        }

        String statutStuff = setup.getSpawnStuff() != null ? "§a✓“" : "§c✗";
        joueur.sendMessage("§e» Spawn stuff: " + statutStuff);

        joueur.sendMessage("§e» Spawns PvP: §7" + setup.getSpawnsPvp().size() + " configures");

        String statutBordure = setup.getCentreBordure() != null ? "§a✓“" : "§c✗";
        joueur.sendMessage("§e» Centre bordure: " + statutBordure);
        if (setup.getCentreBordure() != null) {
            joueur.sendMessage("  §7Rayon initial: " + setup.getRayonBordureInitial());
        }

        joueur.sendMessage("");
        boolean complet = mondeEstConfigure(nomMonde);
        String statutGeneral = complet ? "§a✓“ COMPLET" : "§c✗ INCOMPLET";
        joueur.sendMessage("§e» Statut general: " + statutGeneral);

        if (!complet) {
            joueur.sendMessage("§7» Elements manquants:");
            if (setup.getSpawnsDescente().isEmpty()) joueur.sendMessage("  §c- Spawns descente");
            if (setup.getZonesArrivees().isEmpty() && setup.getArriveesMaps().isEmpty()) joueur.sendMessage("  §c- Zones d'arrivee");
            if (setup.getSpawnStuff() == null) joueur.sendMessage("  §c- Spawn stuff");
            if (setup.getSpawnsPvp().isEmpty()) joueur.sendMessage("  §c- Spawns PvP");
            if (setup.getCentreBordure() == null) joueur.sendMessage("  §c- Centre bordure");
        }

        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public void copierSetupVersMondeTemporaire(String nomMapOriginale, String nomMondeTemporaire) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "getdown/setups/" + nomMapOriginale + ".yml");
        if (!fichierSetupOriginal.exists()) {
            plugin.getLogger().warning("Impossible de copier le setup GetDown : setup original introuvable pour " + nomMapOriginale);
            return;
        }

        World mondeTemporaire = plugin.getServer().getWorld(nomMondeTemporaire);
        if (mondeTemporaire == null) {
            plugin.getLogger().warning("Impossible de copier le setup GetDown : monde temporaire introuvable " + nomMondeTemporaire);
            return;
        }

        FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "getdown/setups/" + nomMondeTemporaire + ".yml");
        fichierSetupTemporaire.getParentFile().mkdirs();

        FileConfiguration configTemporaire = new YamlConfiguration();

        if (configOriginal.contains("spawns-joueurs")) {
            List<String> spawnsJoueurs = configOriginal.getStringList("spawns-joueurs");
            configTemporaire.set("spawns-joueurs", spawnsJoueurs);
        }

        if (configOriginal.contains("spawns-descente")) {
            List<String> spawnsDescente = configOriginal.getStringList("spawns-descente");
            configTemporaire.set("spawns-descente", spawnsDescente);
        }

        if (configOriginal.contains("arrivees-maps")) {
            List<String> arriveesMaps = configOriginal.getStringList("arrivees-maps");
            configTemporaire.set("arrivees-maps", arriveesMaps);
        }

        if (configOriginal.contains("spawn-stuff")) {
            String spawnStuff = configOriginal.getString("spawn-stuff");
            configTemporaire.set("spawn-stuff", spawnStuff);
        }

        if (configOriginal.contains("spawn-pvp")) {
            String spawnPvp = configOriginal.getString("spawn-pvp");
            configTemporaire.set("spawn-pvp", spawnPvp);
        }

        if (configOriginal.contains("bordure")) {
            configTemporaire.set("bordure.centre", configOriginal.getString("bordure.centre"));
            configTemporaire.set("bordure.rayon", configOriginal.getInt("bordure.rayon"));
        }

        try {
            configTemporaire.save(fichierSetupTemporaire);
            plugin.getLogger().info("Setup GetDown copie de " + nomMapOriginale + " vers " + nomMondeTemporaire);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la copie du setup GetDown : " + e.getMessage());
        }
    }

    public void supprimerSetupTemporaire(String nomMondeTemporaire) {
        setupsMondes.remove(nomMondeTemporaire);

        File fichierSetupTemporaire = new File(plugin.getDataFolder(), "getdown/setups/" + nomMondeTemporaire + ".yml");
        if (fichierSetupTemporaire.exists()) {
            fichierSetupTemporaire.delete();
            plugin.getLogger().info("Setup GetDown temporaire supprime pour " + nomMondeTemporaire);
        }
    }


    private void chargerConfigurationMultiMaps() {
        File fichierConfig = new File(plugin.getDataFolder(), "getdown/multi-maps.yml");
        if (!fichierConfig.exists()) {
            creerConfigurationMultiMapsParDefaut();
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierConfig);
        configurationMultiMaps.setMapParcours1(config.getString("maps.parcours1"));
        configurationMultiMaps.setMapParcours2(config.getString("maps.parcours2"));
        configurationMultiMaps.setMapParcours3(config.getString("maps.parcours3"));
        configurationMultiMaps.setMapPvp(config.getString("maps.pvp"));
    }

    private void creerConfigurationMultiMapsParDefaut() {
        File fichierConfig = new File(plugin.getDataFolder(), "getdown/multi-maps.yml");
        fichierConfig.getParentFile().mkdirs();

        FileConfiguration config = new YamlConfiguration();
        config.set("maps.parcours1", null);
        config.set("maps.parcours2", null);
        config.set("maps.parcours3", null);
        config.set("maps.pvp", null);

        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la creation de la configuration multi-maps GetDown: " + e.getMessage());
        }
    }

    private void sauvegarderConfigurationMultiMaps() {
        File fichierConfig = new File(plugin.getDataFolder(), "getdown/multi-maps.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(fichierConfig);

        config.set("maps.parcours1", configurationMultiMaps.getMapParcours1());
        config.set("maps.parcours2", configurationMultiMaps.getMapParcours2());
        config.set("maps.parcours3", configurationMultiMaps.getMapParcours3());
        config.set("maps.pvp", configurationMultiMaps.getMapPvp());

        try {
            config.save(fichierConfig);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration multi-maps GetDown: " + e.getMessage());
        }
    }

    public void definirMapParcours(Player joueur, int numeroParcours) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        switch (numeroParcours) {
            case 1:
                configurationMultiMaps.setMapParcours1(nomMonde);
                joueur.sendMessage("§aMap du parcours 1 definie : " + nomMonde);
                break;
            case 2:
                configurationMultiMaps.setMapParcours2(nomMonde);
                joueur.sendMessage("§aMap du parcours 2 definie : " + nomMonde);
                break;
            case 3:
                configurationMultiMaps.setMapParcours3(nomMonde);
                joueur.sendMessage("§aMap du parcours 3 definie : " + nomMonde);
                break;
            default:
                joueur.sendMessage("§cNumero de parcours invalide ! Utilisez 1, 2 ou 3");
                return;
        }

        sauvegarderConfigurationMultiMaps();
    }

    public void definirMapPvp(Player joueur) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        configurationMultiMaps.setMapPvp(nomMonde);
        joueur.sendMessage("§aMap PvP definie : " + nomMonde);
        sauvegarderConfigurationMultiMaps();
    }

    public ConfigurationMultiMaps getConfigurationMultiMaps() {
        return configurationMultiMaps;
    }

    public void afficherInfosMultiMaps(Player joueur) {
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("§6§l           CONFIGURATION MULTI-MAPS GETDOWN");
        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        joueur.sendMessage("");

        String map1 = configurationMultiMaps.getMapParcours1();
        String map2 = configurationMultiMaps.getMapParcours2();
        String map3 = configurationMultiMaps.getMapParcours3();
        String mapPvp = configurationMultiMaps.getMapPvp();

        joueur.sendMessage("§e» Map Parcours 1: " + (map1 != null ? "§a" + map1 : "§c✗ Non definie"));
        joueur.sendMessage("§e» Map Parcours 2: " + (map2 != null ? "§a" + map2 : "§c✗ Non definie"));
        joueur.sendMessage("§e» Map Parcours 3: " + (map3 != null ? "§a" + map3 : "§c✗ Non definie"));
        joueur.sendMessage("§e» Map PvP: " + (mapPvp != null ? "§a" + mapPvp : "§7Aucune (optionnel)"));
        joueur.sendMessage("");

        boolean complet = configurationMultiMaps.estComplet();
        String statut = complet ? "§a✓ COMPLET" : "§c✗ INCOMPLET";
        joueur.sendMessage("§e» Statut: " + statut);

        if (!complet) {
            joueur.sendMessage("§7» Pour configurer:");
            joueur.sendMessage("  §7- Allez sur chaque map");
            joueur.sendMessage("  §7- Tapez: /setup getdown map-parcours <1|2|3>");
            joueur.sendMessage("  §7- Optionnel: /setup getdown map-pvp");
        }

        joueur.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    public void supprimerDernierSpawn(Player joueur) {
        String nomMonde = joueur.getWorld().getName();
        SetupMonde setup = setupsMondes.get(nomMonde);

        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        if (setup.getSpawnsDescente().isEmpty()) {
            joueur.sendMessage("§cAucun spawn a supprimer !");
            return;
        }

        setup.getSpawnsDescente().remove(setup.getSpawnsDescente().size() - 1);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aDernier spawn de descente supprime !");
    }

    public void supprimerDernierSpawnPvp(Player joueur) {
        String nomMonde = joueur.getWorld().getName();
        SetupMonde setup = setupsMondes.get(nomMonde);

        if (setup == null) {
            joueur.sendMessage("§cAucun setup trouve pour ce monde !");
            return;
        }

        Map<Integer, Location> spawnsPvp = setup.getSpawnsPvp();
        if (spawnsPvp.isEmpty()) {
            joueur.sendMessage("§cAucun spawn PvP a supprimer !");
            return;
        }

        int maxIndex = Collections.max(spawnsPvp.keySet());
        spawnsPvp.remove(maxIndex);
        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aDernier spawn PvP (index " + maxIndex + ") supprime !");
    }

    public void definirSpawnJoueur(Player joueur, int numeroParcours, int numeroJoueur) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            setup = new SetupMonde();
            setupsMondes.put(nomMonde, setup);
        }

        Location location = joueur.getLocation();

        while (setup.getSpawnsJoueurs().size() < numeroJoueur) {
            setup.getSpawnsJoueurs().add(null);
        }

        setup.getSpawnsJoueurs().set(numeroJoueur - 1, location);

        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aSpawn joueur " + numeroJoueur + " defini pour le parcours " + numeroParcours + " !");
        joueur.sendMessage("§7Position: " + (int)location.getX() + ", " + (int)location.getY() + ", " + (int)location.getZ());
        joueur.sendMessage("§7Map: " + nomMonde);
    }

    public void definirSpawnPvPNumero(Player joueur, int numeroSpawn) {
        String nomMonde = joueur.getWorld().getName();

        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps()
                .estMapDisponible(fr.oriax.kingfight.commun.TypeMiniJeu.GET_DOWN, nomMonde)) {
            joueur.sendMessage("§cErreur : Cette map n'est pas enregistree pour GetDown !");
            joueur.sendMessage("§eUtilisez d'abord : /setup maps getdown ajouter " + nomMonde);
            return;
        }

        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) {
            setup = new SetupMonde();
            setupsMondes.put(nomMonde, setup);
        }

        Location location = joueur.getLocation();

        setup.getSpawnsPvp().put(numeroSpawn - 1, location);

        sauvegarderSetup(nomMonde, setup);

        joueur.sendMessage("§aSpawn PvP " + numeroSpawn + " defini !");
        joueur.sendMessage("§7Position: " + (int)location.getX() + ", " + (int)location.getY() + ", " + (int)location.getZ());
        joueur.sendMessage("§7Map: " + nomMonde);
    }

    public SetupMonde obtenirSetup(String nomMonde) {
        return setupsMondes.get(nomMonde);
    }

    public boolean sauvegarderSetupVersBackup(String nomMonde) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File dossierBackups = new File(plugin.getDataFolder(), "getdown/backups");
        dossierBackups.mkdirs();

        String timestamp = String.valueOf(System.currentTimeMillis());
        File fichierBackup = new File(dossierBackups, nomMonde + "_" + timestamp + ".yml");

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierBackup);
            plugin.getLogger().info("Backup du setup GetDown cree pour " + nomMonde + ": " + fichierBackup.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la creation du backup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean restaurerSetupDepuisBackup(String nomMonde, String nomFichierBackup) {
        File fichierBackup = new File(plugin.getDataFolder(), "getdown/backups/" + nomFichierBackup);
        if (!fichierBackup.exists()) {
            return false;
        }

        File fichierSetupDestination = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configBackup = YamlConfiguration.loadConfiguration(fichierBackup);
            configBackup.save(fichierSetupDestination);
            
            chargerSetup(nomMonde);
            
            plugin.getLogger().info("Setup GetDown restaure pour " + nomMonde + " depuis " + nomFichierBackup);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la restauration du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean exporterSetup(String nomMonde, String cheminDestination) {
        File fichierSetupOriginal = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        if (!fichierSetupOriginal.exists()) {
            return false;
        }

        File fichierDestination = new File(cheminDestination);
        fichierDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configOriginal = YamlConfiguration.loadConfiguration(fichierSetupOriginal);
            configOriginal.save(fichierDestination);
            plugin.getLogger().info("Setup GetDown exporte pour " + nomMonde + " vers " + cheminDestination);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de l'exportation du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public boolean importerSetup(String nomMonde, String cheminSource) {
        File fichierSource = new File(cheminSource);
        if (!fichierSource.exists()) {
            return false;
        }

        File fichierSetupDestination = new File(plugin.getDataFolder(), "getdown/setups/" + nomMonde + ".yml");
        fichierSetupDestination.getParentFile().mkdirs();

        try {
            FileConfiguration configSource = YamlConfiguration.loadConfiguration(fichierSource);
            configSource.save(fichierSetupDestination);
            
            chargerSetup(nomMonde);
            
            plugin.getLogger().info("Setup GetDown importe pour " + nomMonde + " depuis " + cheminSource);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de l'importation du setup pour " + nomMonde + ": " + e.getMessage());
            return false;
        }
    }

    public void rechargerSetup(String nomMonde) {
        chargerSetup(nomMonde);
        plugin.getLogger().info("Setup GetDown recharge pour " + nomMonde);
    }

    public List<String> listerBackupsDisponibles(String nomMonde) {
        File dossierBackups = new File(plugin.getDataFolder(), "getdown/backups");
        if (!dossierBackups.exists()) {
            return new ArrayList<>();
        }

        File[] fichiers = dossierBackups.listFiles((dir, name) -> 
            name.startsWith(nomMonde + "_") && name.endsWith(".yml"));
        
        if (fichiers == null) {
            return new ArrayList<>();
        }

        List<String> backups = new ArrayList<>();
        for (File fichier : fichiers) {
            backups.add(fichier.getName());
        }
        
        backups.sort((a, b) -> b.compareTo(a));
        return backups;
    }

    public boolean verifierIntegriteSetup(String nomMonde) {
        SetupMonde setup = setupsMondes.get(nomMonde);
        if (setup == null) return false;

        boolean aZonesArrivee = !setup.getZonesArrivees().isEmpty() || !setup.getArriveesMaps().isEmpty();

        return !setup.getSpawnsDescente().isEmpty() &&
               aZonesArrivee &&
               setup.getSpawnStuff() != null &&
               !setup.getSpawnsPvp().isEmpty() &&
               setup.getCentreBordure() != null;
    }
}


