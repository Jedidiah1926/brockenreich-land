package com.brockenreich.landplugin.economy

/** The server's currency: 'ȼ' (cent sign), formatted with thousands separators - e.g. "1,234ȼ". */
object Currency {
    const val SYMBOL = "ȼ"

    fun format(amount: Long): String = "%,d$SYMBOL".format(amount)
}
