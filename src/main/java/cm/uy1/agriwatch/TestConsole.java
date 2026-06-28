package cm.uy1.agriwatch;

import cm.uy1.agriwatch.core.*;
import cm.uy1.agriwatch.threading.CapteurManager;

public class TestConsole implements MeteoListener {

    private int compteur = 0;

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        compteur++;
        String alerte = (humidite < 30.0) ? " ** ALERTE IRRIGATION **" : "";
        System.out.printf("[%2d] %s | Temp: %5.1f°C | Hum: %5.1f%%%s%n",
                compteur, zone, temperature, humidite, alerte);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== AgriWatch — Test Console ===");

        CentraleMeteo centrale = new CentraleMeteo();
        TestConsole test = new TestConsole();
        CapteurManager manager = new CapteurManager();

        // Abonner le test à la centrale (qui relaie les capteurs)
        centrale.abonner(test);
        manager.demarrer(centrale);

        System.out.println("Capteurs actifs pendant 10 secondes...\n");

        Thread.sleep(10_000);
        manager.arreter();
        Thread.sleep(500);

        System.out.println("\n=== Arrêt ===");
        System.out.println("Mesures reçues : " + test.compteur);
        System.out.println("Historique     : " + centrale.getHistorique().size() + " entrées");

        // Vérifier qu'il y a bien eu des mesures
        if (test.compteur > 0) {
            System.out.println("✅ Pattern Observer fonctionnel !");
        } else {
            System.out.println("❌ Aucune mesure reçue.");
        }
    }
}
