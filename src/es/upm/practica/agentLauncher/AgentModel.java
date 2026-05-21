package es.upm.practica.agentLauncher;

public enum AgentModel {

	CONTENT_ACQUISITION("contentAcquisition"),
    MOOD_ANALYSIS("moodAnalysis"),
    MUSIC_RECOMMENDATION("musicRecommendation"),
    VISUALIZATION("visualization"),
    UNKNOWN("Desconocido");

    private final String value;

    AgentModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Devuelve el enum a partir del String (útil para búsquedas)
     */
    public static AgentModel getEnum(String value) {
        if (value == null) return UNKNOWN;

        for (AgentModel m : values()) {
            if (m.getValue().equalsIgnoreCase(value)) {
                return m;
            }
        }
        return UNKNOWN;
    }
}
