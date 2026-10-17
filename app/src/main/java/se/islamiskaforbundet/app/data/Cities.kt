package se.islamiskaforbundet.app.data

data class City(val name: String, val lat: Double, val lng: Double)

val SWEDISH_CITIES = listOf(
    City("Stockholm", 59.3293, 18.0686),
    City("Göteborg", 57.7089, 11.9746),
    City("Malmö", 55.6050, 13.0038),
    City("Uppsala", 59.8586, 17.6389),
    City("Västerås", 59.6099, 16.5448),
    City("Örebro", 59.2753, 15.2134),
    City("Linköping", 58.4108, 15.6214),
    City("Helsingborg", 56.0465, 12.6945),
    City("Jönköping", 57.7826, 14.1618),
    City("Norrköping", 58.5877, 16.1924),
    City("Lund", 55.7047, 13.1910),
    City("Umeå", 63.8258, 20.2630),
    City("Gävle", 60.6749, 17.1413),
    City("Borås", 57.7210, 12.9401),
    City("Södertälje", 59.1955, 17.6253),
    City("Eskilstuna", 59.3666, 16.5077),
    City("Halmstad", 56.6745, 12.8578),
    City("Växjö", 56.8777, 14.8091),
    City("Karlstad", 59.4022, 13.5115),
    City("Sundsvall", 62.3908, 17.3069),
    City("Luleå", 65.5848, 22.1547),
    City("Kiruna", 67.8558, 20.2253),
)
