package cm.uy1.agriwatch.persistence;

import cm.uy1.agriwatch.core.MesureMeteo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// TODO Équipe 2 — Implémenter la sérialisation et l'export CSV
public class PersistanceService {
    private static final String FILE_PATH = "data/historique_meteo.ser";
    private static final String CSV_PATH = "data/rapport_alertes.csv";

    public void sauvegarder(List<MesureMeteo> historique) {
        // TODO : ObjectOutputStream vers data/historique_meteo.ser
        try {
            // 1. Créer le dossier 'data' s'il n'existe pas
            Files.createDirectories(Paths.get("data"));

            // 2. Sécuriser la liste contre le multithreading pendant la lecture
            // On se synchronise sur la liste pour bloquer temporairement les capteurs
            synchronized (historique) {
                try (FileOutputStream fos = new FileOutputStream(FILE_PATH);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                    // On écrit toute la liste d'un coup
                    oos.writeObject(historique);
                    System.out.println("Persistance : " + historique.size() + " mesures sauvegardées.");

                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<MesureMeteo> charger() {
        // TODO : ObjectInputStream depuis data/historique_meteo.ser
        File fichier = new File(FILE_PATH);

        // Si le fichier n'existe pas encore (premier démarrage), on renvoie une liste vide
        if (!fichier.exists()) {
            System.out.println("Persistance : Aucun historique trouvé. Création d'une nouvelle session.");
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(fichier);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            // On lit l'objet et on le convertit (cast) en Liste de MesureMeteo
            List<MesureMeteo> historiqueCharge = (List<MesureMeteo>) ois.readObject();
            System.out.println("Persistance : " + historiqueCharge.size() + " mesures chargées avec succès.");
            return historiqueCharge;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement : " + e.getMessage());
            // En cas d'erreur de lecture (fichier corrompu), on renvoie une liste vide pour éviter le crash
            return new ArrayList<>();
        }
    }

    /*
    public void exporterCSV(List<MesureMeteo> mesures) {
        // TODO : écriture dans data/rapport_alertes.csv
        try {
            Files.createDirectories(Paths.get("data"));

            try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_PATH))) {
                // Écriture de l'en-tête (Header)
                writer.println("Zone,Temperature,Humidite,Timestamp");

                // Écriture des données
                synchronized (mesures) {
                    for (MesureMeteo m : mesures) {
                        writer.printf("%s,%.2f,%.2f,%s%n",
                                m.getZone().name(),
                                m.getTemperature(),
                                m.getHumidite(),
                                m.getTimestamp().toString()
                        );
                    }
                }
                System.out.println("Persistance : Export CSV réussi -> " + CSV_PATH);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'export CSV : " + e.getMessage());
        }
    }
     */

    public void exporterCSV(List<MesureMeteo> mesures) {
        try {
            Files.createDirectories(Paths.get("data"));

            // 1. Formateur pour extraire uniquement la date (ex: 2026-07-02)
            DateTimeFormatter formatFichier = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 2. Sécuriser la lecture de la liste face aux threads capteurs
            synchronized (mesures) {
                if (mesures.isEmpty()) return;

                // Pour chaque mesure, on va déterminer son fichier cible selon sa date
                for (MesureMeteo m : mesures) {
                    // ex: "data/rapport_2026-07-02.csv"
                    String dateDuJour = m.getTimestamp().format(formatFichier);
                    String nomFichierLog = "data/rapport_" + dateDuJour + ".csv";

                    File fichier = new File(nomFichierLog);
                    boolean fichierExisteDeja = fichier.exists();

                    // On ouvre le fichier en mode "APPEND" (true) pour ajouter à la fin sans écraser
                    try (PrintWriter writer = new PrintWriter(new FileWriter(fichier, true))) {
                        // Si le fichier vient d'être créé aujourd'hui, on écrit l'en-tête d'abord
                        if (!fichierExisteDeja) {
                            writer.println("Heure,Zone,Temperature,Humidite");
                        }

                        // Écriture de la ligne (on extrait l'heure précise HH:mm:ss pour la lisibilité)
                        DateTimeFormatter formatHeure = DateTimeFormatter.ofPattern("HH:mm:ss");
                        writer.printf("%s,%s,%.2f,%.2f%n",
                                m.getTimestamp().format(formatHeure),
                                m.getZone().name(),
                                m.getTemperature(),
                                m.getHumidite()
                        );
                    }
                }
            }
            System.out.println("Persistance : Exportation par date terminée dans le dossier 'data/'");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'export CSV scindé : " + e.getMessage());
        }
    }
}
