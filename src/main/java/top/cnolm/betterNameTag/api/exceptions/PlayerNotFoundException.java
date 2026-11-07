package top.cnolm.betterNameTag.api.exceptions;

/**
 * 玩家未找到异常
 */
public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String message) {
        super(message);
    }

    public PlayerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}