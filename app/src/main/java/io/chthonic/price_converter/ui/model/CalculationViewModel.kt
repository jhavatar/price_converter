package io.chthonic.price_converter.ui.model

/**
 * Created by jhavatar on 3/30/2018.
 */
data class CalculationViewModel(val bitcoinPrice: String,
                                val convertToFiat: Boolean,
                                val ticker: TickerViewModel?) {
}