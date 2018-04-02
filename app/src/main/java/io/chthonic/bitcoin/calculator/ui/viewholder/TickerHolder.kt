package io.chthonic.bitcoin.calculator.ui.viewholder

import android.os.Build
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.chthonic.price_converter.R
import io.chthonic.bitcoin.calculator.ui.model.TickerViewModel
import io.chthonic.bitcoin.calculator.utils.UiUtils
import kotlinx.android.synthetic.main.holder_ticker.view.*

/**
 * Created by jhavatar on 3/28/2018.
 */
class TickerHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    companion object {
        fun createView(parent: ViewGroup): View {
            return LayoutInflater.from(parent.context).inflate(R.layout.holder_ticker, parent, false)
        }
    }

    fun init() {
        UiUtils.setRipple(itemView)
    }

    fun update(ticker: TickerViewModel) {
        itemView.ticker_name.text = ticker.name

        val price = "<b>${ticker.sign}</b> ${ticker.price}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            itemView.ticker_price.text = Html.fromHtml(price,  Html.FROM_HTML_MODE_COMPACT)
        } else {
            itemView.ticker_price.text = Html.fromHtml(price)
        }

        itemView.ticker_date.text = itemView.resources.getString(R.string.updated) + " ${ticker.dateTime}"

        // note, circleImageView does not work with vectors
        itemView.ticker_image.setImageResource(UiUtils.getFiatImageSmallRes(ticker.code))

        val selectedVis = if (ticker.selected) View.VISIBLE else View.GONE
        if (itemView.ticker_selected.visibility != selectedVis) {
            itemView.ticker_selected.visibility = selectedVis
            itemView.ticker_image.borderColor = itemView.resources.getColor(if (ticker.selected) R.color.secondaryColor else R.color.primaryDarkestColor)
            itemView.ticker_image.borderWidth = if (ticker.selected) UiUtils.dipToPixels(4, itemView.context) else UiUtils.dipToPixels(1, itemView.context)
        }
    }

}