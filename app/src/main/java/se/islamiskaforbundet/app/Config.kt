package se.islamiskaforbundet.app

/**
 * Organisation-specific settings. Fill these in before publishing to Google Play.
 * Empty values hide the matching feature instead of pointing users at a wrong recipient.
 */
object Config {
    const val WEBSITE = "https://www.islamiskaforbundet.se"

    /** Swish number for donations, e.g. "123 456 78 90". Empty = Swish button opens the website instead. */
    const val SWISH_NUMBER = ""

    /** Bankgiro/plusgiro shown on the donation screen. Empty = hidden. */
    const val BANKGIRO = ""

    /** Official YouTube channel URL. Empty = video categories open YouTube search. */
    const val YOUTUBE_CHANNEL = ""

    /** Google Play subscription product id for Premium (create it in Play Console). */
    const val PREMIUM_PRODUCT_ID = "premium_monthly"

    /** Share of Premium revenue that goes to zakat, as shown in the app. */
    const val PREMIUM_ZAKAT_PERCENT = 98
}
