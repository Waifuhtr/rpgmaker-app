package com.waifuhtr.pixelstore.ui

import kotlin.math.round

/** Türkçe biçimlendirme yardımcıları: ondalık ayırıcı virgül, kısaltmalar "B" ve "Mn". */
object Format {

    fun count(value: Number): String {
        val n = value.toDouble()
        return when {
            n >= 1_000_000 -> "${decimal(n / 1_000_000)} Mn"
            n >= 1_000 -> "${decimal(n / 1_000)} B"
            else -> n.toLong().toString()
        }
    }

    fun size(mb: Double): String =
        if (mb >= 1024) "${decimal(mb / 1024)} GB" else "${decimal(mb)} MB"

    fun rating(value: Double): String = decimal(value)

    private fun decimal(value: Double): String {
        val rounded = round(value * 10) / 10
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString().replace('.', ',')
        }
    }
}
