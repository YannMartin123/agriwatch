package cm.uy1.agriwatch;

import cm.uy1.agriwatch.core.CentraleMeteo;

public class Main {
    public static void main(String[] args) {
        System.out.println("AgriWatch â€” dÃ©marrage...");
        CentraleMeteo centrale = new CentraleMeteo();
        // TODO J3+ : instancier FenetrePrincipale, l'abonner Ã  centrale,
        //            puis lancer CapteurManager.demarrer(centrale)
    }
}
