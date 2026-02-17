package ma.siblhish.exception;

/**
 * Exception métier pour les erreurs d'authentification (login, compte supprimé, etc.).
 * Permet de centraliser la gestion des erreurs d'auth dans GlobalExceptionHandler
 * et d'éviter les try/catch dans les controllers.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}


