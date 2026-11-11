package id.plato.user;

/** Kebutuhan model User terhadap backend, tanpa membuat model bergantung pada REST. */
public interface UserGateway {
    boolean verifyCredentials(String email, String password);

    void endSession();

    void saveProfile(String name, String email);
}
