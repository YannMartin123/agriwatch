package cm.uy1.agriwatch.persistence;

import cm.uy1.agriwatch.core.MesureMeteo;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PersistanceService {

    private static final String SER_FILE_PATH = "data/historique_meteo.ser";
    private static final String CSV_FILE_PATH = "data/rapport_alertes.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Sauvegarde l'historique complet des mesures dans un fichier sérialisé.
     */
    public void sauvegarder(List<MesureMeteo> historique) {
        assurerDossierExiste("data");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SER_FILE_PATH))) {
            // Créer une copie pour éviter les ConcurrentModificationException
            List<MesureMeteo> copie;
            synchronized (historique) {
                copie = new ArrayList<>(historique);
            }
            oos.writeObject(copie);
            System.out.println("Sauvegarde réussie : " + copie.size() + " mesures enregistrées.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Charge l'historique des mesures depuis le fichier sérialisé.
     */
    @SuppressWarnings("unchecked")
    public List<MesureMeteo> charger() {
        File file = new File(SER_FILE_PATH);
        if (!file.exists()) {
            System.out.println("Aucun fichier de sauvegarde trouvé à " + SER_FILE_PATH + ". Initialisation à vide.");
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                List<MesureMeteo> charge = (List<MesureMeteo>) obj;
                System.out.println("Chargement réussi : " + charge.size() + " mesures restaurées.");
                return charge;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement : " + e.getMessage() + ". Retour d'une liste vide.");
        }
        return new ArrayList<>();
    }

    /**
     * Exporte une liste de mesures dans un fichier CSV (format : Zone,Température,Humidité,Timestamp,Statut).
     */
    public void exporterCSV(List<MesureMeteo> mesures) {
        System.out.println("Export CSV désactivé.");
    }

    private void assurerDossierExiste(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                System.out.println("Dossier créé : " + path);
            }
        }
    }
}
