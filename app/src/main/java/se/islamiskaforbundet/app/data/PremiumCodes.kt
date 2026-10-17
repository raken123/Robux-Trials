package se.islamiskaforbundet.app.data

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import se.islamiskaforbundet.app.Config

/** Free-Premium QR codes: IFPREMIUM1|<campaign>|<days>|<first 20 hex chars of HMAC-SHA256>. */
object PremiumCodes {
    sealed interface Result {
        data class Granted(val days: Int, val untilMillis: Long) : Result
        data object AlreadyUsed : Result
        data object Invalid : Result
    }

    fun sign(campaign: String, days: Int): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(Config.PREMIUM_CODE_KEY.toByteArray(), "HmacSHA256"))
        val digest = mac.doFinal("IFPREMIUM1|$campaign|$days".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }

    fun redeem(raw: String, now: Long = System.currentTimeMillis()): Result {
        val parts = raw.trim().split("|")
        if (parts.size != 4 || parts[0] != "IFPREMIUM1") return Result.Invalid
        val campaign = parts[1]
        val days = parts[2].toIntOrNull()?.takeIf { it in 1..365 } ?: return Result.Invalid
        if (!sign(campaign, days).equals(parts[3], ignoreCase = true)) return Result.Invalid
        if (Prefs.campaignUsed(campaign)) return Result.AlreadyUsed
        val until = maxOf(now, Prefs.trialUntil) + days * 24L * 60 * 60 * 1000
        Prefs.markCampaignUsed(campaign)
        Billing.grantTrial(until)
        return Result.Granted(days, until)
    }
}
