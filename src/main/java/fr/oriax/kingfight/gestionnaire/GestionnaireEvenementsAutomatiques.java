package fr.oriax.kingfight.gestionnaire;

import fr.oriax.kingfight.KingFight;
import fr.oriax.kingfight.commun.TypeMiniJeu;
import fr.oriax.kingfight.jumpleague.ValidateurConfigurationJumpLeague;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Calendar;
import java.util.TimeZone;

public class GestionnaireEvenementsAutomatiques {

    private final KingFight plugin;
    private BukkitRunnable tacheVerification;
    private String dernierEvenementDemarre;

    public GestionnaireEvenementsAutomatiques(KingFight plugin) {
        this.plugin = plugin;
        this.dernierEvenementDemarre = "";
        demarrerVerificationAutomatique();
    }

    private void demarrerVerificationAutomatique() {
        this.tacheVerification = new BukkitRunnable() {
            @Override
            public void run() {
                verifierEtGererEvenements();
            }
        };

        tacheVerification.runTaskTimer(plugin, 0L, 100L);
    }

    private void verifierEtGererEvenements() {
        if (!plugin.getGestionnairePrincipal().getConfigPrincipale().getBoolean("evenements.automatiques", true)) {
            return;
        }

        Calendar maintenant = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        int jour = maintenant.get(Calendar.DAY_OF_WEEK);
        int heure = maintenant.get(Calendar.HOUR_OF_DAY);
        int minute = maintenant.get(Calendar.MINUTE);

        String jourActuel = obtenirNomJour(jour);

        TypeMiniJeu evenementActuel = plugin.getGestionnairePrincipal().getEvenementActuel();
        TypeMiniJeu nouvelEvenement = null;
        boolean doitDemarrerEvenement = false;

        if (plugin.getGestionnairePrincipal().getConfigPrincipale().contains("evenements.horaires." + jourActuel)) {
            String debutStr = plugin.getGestionnairePrincipal().getConfigPrincipale().getString("evenements.horaires." + jourActuel + ".debut", "");
            String finStr = plugin.getGestionnairePrincipal().getConfigPrincipale().getString("evenements.horaires." + jourActuel + ".fin", "");
            String typeEvenement = plugin.getGestionnairePrincipal().getConfigPrincipale().getString("evenements.horaires." + jourActuel + ".type", "");

            int[] heureMinuteDebut = parseHeure(debutStr);
            int[] heureMinuteFin = parseHeure(finStr);

            if (heureMinuteDebut != null && heureMinuteFin != null) {
                int heureDebut = heureMinuteDebut[0];
                int minuteDebut = heureMinuteDebut[1];
                int heureFin = heureMinuteFin[0];
                int minuteFin = heureMinuteFin[1];

                if (heure == heureDebut && minute == minuteDebut) {
                    plugin.getLogger().info("Heure de debut d'evenement detectee (" + debutStr + ")");

                    String cleEvenement = jourActuel + "_" + typeEvenement + "_" + maintenant.get(Calendar.DAY_OF_YEAR);

                    if (!cleEvenement.equals(dernierEvenementDemarre)) {
                        switch (typeEvenement.toLowerCase()) {
                            case "hungergames":
                                nouvelEvenement = TypeMiniJeu.HUNGER_GAMES;
                                break;
                            case "jumpleague":
                                nouvelEvenement = TypeMiniJeu.JUMP_LEAGUE;
                                break;
                            case "getdown":
                                nouvelEvenement = TypeMiniJeu.GET_DOWN;
                                break;
                        }

                        if (nouvelEvenement != null) {
                            if (plugin.getGestionnairePrincipal().peutDemarrerEvenement(nouvelEvenement)) {
                                dernierEvenementDemarre = cleEvenement;
                                doitDemarrerEvenement = true;
                                plugin.getLogger().info("Nouvel evenement programme: " + nouvelEvenement + " pour " + jourActuel);
                            } else {
                                afficherErreursEvenementAutomatique(nouvelEvenement);
                                nouvelEvenement = null;
                            }
                        }
                    }
                } else if (heure == heureFin && minute == minuteFin) {
                    plugin.getLogger().info("Heure de fin d'evenement detectee (" + finStr + ")");
                    if (evenementActuel != null) {
                        plugin.getGestionnairePrincipal().arreterEvenement(evenementActuel);
                    }
                }
            }
        }

        if (doitDemarrerEvenement && nouvelEvenement != null && !plugin.getGestionnairePrincipal().estEvenementForce()) {
            plugin.getGestionnairePrincipal().changerEvenementActuel(nouvelEvenement);
        }

        verifierFinEvenementParDuree(plugin.getGestionnairePrincipal().getEvenementActuel());
    }

    private int[] parseHeure(String heureStr) {
        if (heureStr == null || heureStr.isEmpty()) {
            return null;
        }

        try {
            if (heureStr.contains(":")) {
                String[] parts = heureStr.split(":");
                if (parts.length == 2) {
                    int heure = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());
                    if (heure >= 0 && heure < 24 && minute >= 0 && minute < 60) {
                        return new int[]{heure, minute};
                    }
                }
            } else {
                int heure = Integer.parseInt(heureStr.trim());
                if (heure >= 0 && heure < 24) {
                    return new int[]{heure, 0};
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Format d'heure invalide: " + heureStr);
        }

        return null;
    }

    private String obtenirNomJour(int jourCalendar) {
        switch (jourCalendar) {
            case Calendar.SUNDAY:
                return "dimanche";
            case Calendar.MONDAY:
                return "lundi";
            case Calendar.TUESDAY:
                return "mardi";
            case Calendar.WEDNESDAY:
                return "mercredi";
            case Calendar.THURSDAY:
                return "jeudi";
            case Calendar.FRIDAY:
                return "vendredi";
            case Calendar.SATURDAY:
                return "samedi";
            default:
                return "";
        }
    }

    private void verifierFinEvenementParDuree(TypeMiniJeu evenementActuel) {
        if (evenementActuel == null) return;

        if (plugin.getGestionnairePrincipal().estEvenementForce()) {
            return;
        }

        long tempsRestant = plugin.getGestionnairePrincipal().getTempsRestantEvenement();

        if (tempsRestant <= 0 && !plugin.getGestionnairePrincipal().estEvenementEnFinition(evenementActuel)) {
            plugin.getGestionnairePrincipal().arreterEvenement(evenementActuel);
        }
    }

    public void arreter() {
        if (tacheVerification != null) {
            tacheVerification.cancel();
        }
    }

    private void afficherErreursEvenementAutomatique(TypeMiniJeu type) {
        if (!plugin.getGestionnairePrincipal().getGestionnaireMaps().aMapsConfigurees(type)) {
            plugin.getLogger().warning("Impossible de demarrer l'evenement automatique " + type + " - aucune map configuree");
        }

        if (!plugin.getGestionnairePrincipal().getGestionnaireLobby().estSpawnLobbyDefini()) {
            plugin.getLogger().warning("Impossible de demarrer l'evenement automatique " + type + " - spawn du lobby non defini");
        }

        if (type == TypeMiniJeu.JUMP_LEAGUE) {
            ValidateurConfigurationJumpLeague.ResultatValidation validation =
                plugin.getGestionnairePrincipal().getGestionnaireJumpLeague().validerConfiguration();

            if (!validation.estValide()) {
                plugin.getLogger().warning("Impossible de demarrer l'evenement automatique JumpLeague - erreurs de configuration :");
                for (String erreur : validation.getErreurs()) {
                    plugin.getLogger().warning("- " + erreur);
                }
            }
        }
    }
}
