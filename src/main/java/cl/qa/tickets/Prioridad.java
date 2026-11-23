package cl.qa.tickets;

final class Prioridad {
    private Prioridad() {
    }

    static String calcular(String impacto) {
        return switch (impacto) {
            case "critico" -> "alta";
            case "moderado" -> "media";
            case "bajo" -> "baja";
            default -> throw new IllegalArgumentException("impacto inválido");
        };
    }
}
