package cm.uy1.agriwatch.persistence;

import cm.uy1.agriwatch.core.MesureMeteo;
import java.util.List;

// TODO Ã‰quipe 2 â€” ImplÃ©menter la sÃ©rialisation et l'export CSV
public class PersistanceService {

    public void sauvegarder(List<MesureMeteo> historique) {
        // TODO : ObjectOutputStream vers data/historique_meteo.ser
    }

    public List<MesureMeteo> charger() {
        // TODO : ObjectInputStream depuis data/historique_meteo.ser
        return new java.util.ArrayList<>();
    }

    public void exporterCSV(List<MesureMeteo> alertes) {
        // TODO : Ã©criture dans data/rapport_alertes.csv
        // Format : Zone,Temperature,Humidite,Timestamp,Statut
    }
}
