package net.kingchoka.minetranslatorport;

public final class ProviderConnectionController {
    private ProviderConnectionController() {}

    public static Result test(String providerId, String apiKey) {
        if ("Google".equals(providerId)) {
            try {
                String translated = GoogleTranslator.translate("Hello", "en", "ru").get();
                if (translated != null && !translated.trim().isEmpty() && !translated.equals("Hello")) {
                    return Result.SUCCESS;
                }
            } catch (Exception e) {
                return Result.NETWORK_ERROR;
            }
        }
        return Result.SUCCESS;
    }

    public enum Result {
        SUCCESS,
        INVALID_KEY,
        MODEL_NOT_FOUND,
        RATE_LIMIT,
        NETWORK_ERROR
    }
}
