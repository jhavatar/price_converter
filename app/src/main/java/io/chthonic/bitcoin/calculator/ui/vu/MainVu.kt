package io.chthonic.bitcoin.calculator.ui.vu

import android.app.Activity
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import io.chthonic.bitcoin.calculator.R
import io.chthonic.bitcoin.calculator.data.model.CryptoCurrency
import io.chthonic.bitcoin.calculator.ui.adapter.TickerListAdapter
import io.chthonic.bitcoin.calculator.ui.model.CalculationViewModel
import io.chthonic.bitcoin.calculator.ui.model.TickerViewModel
import io.chthonic.bitcoin.calculator.ui.view.CurrencyInputWatcher
import io.chthonic.bitcoin.calculator.utils.ExchangeUtils
import io.chthonic.bitcoin.calculator.utils.TextUtils
import io.chthonic.bitcoin.calculator.utils.UiUtils
import io.chthonic.mythos.mvp.FragmentWrapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.vu_main.view.*
import timber.log.Timber

/**
 * Created by jhavatar on 3/28/2018.
 */
class MainVu(inflater: LayoutInflater,
             activity: Activity,
             fragmentWrapper: FragmentWrapper? = null,
             parentView: ViewGroup? = null) : BaseVu(inflater,
        activity = activity,
        fragmentWrapper = fragmentWrapper,
        parentView = parentView) {

    private val tickerSelectPublisher: PublishSubject<String> by lazy {
        PublishSubject.create<String>()
    }
    val tickerSelectObservable: Observable<String>
        get() = tickerSelectPublisher.hide()

    private val bitcoinInputPublisher: PublishSubject<String> by lazy {
        PublishSubject.create<String>()
    }
    val bitcoinInputObserver: Flowable<String>
        get() = bitcoinInputPublisher
                .hide()
                .toFlowable(BackpressureStrategy.LATEST)

    private val bitcoinInputWatcher by lazy {
        CurrencyInputWatcher(bitcoinInput, bitcoinInputPublisher, true, maxBitcoinInputLength)
    }



    private val fiatInputPublisher: PublishSubject<String> by lazy {
        PublishSubject.create<String>()
    }
    val fiatInputObserver: Flowable<String>
        get() = fiatInputPublisher
                .hide()
                .toFlowable(BackpressureStrategy.LATEST)

    private val fiatInputWatcher by lazy {
        CurrencyInputWatcher(fiatInput, fiatInputPublisher, false, maxFiatInputLength)
    }

    private val listView: RecyclerView by lazy {
        rootView.list_tickers
    }

    private val bitcoinInput: EditText by lazy {
        rootView.input_bitcoin
    }

    private val maxBitcoinInputLength: Int by lazy {
        val lengthFilter: InputFilter.LengthFilter? = bitcoinInput.filters.find {
            it is InputFilter.LengthFilter
        } as? InputFilter.LengthFilter
        lengthFilter?.max ?: Int.MAX_VALUE
    }

    private val fiatInput: EditText by lazy {
        rootView.input_fiat
    }

    private val maxFiatInputLength: Int by lazy {
        val lengthFilter: InputFilter.LengthFilter? = fiatInput.filters.find {
            it is InputFilter.LengthFilter
        } as? InputFilter.LengthFilter
        lengthFilter?.max ?: Int.MAX_VALUE
    }

    private val bitcoinInfoLayout: View by lazy {
        rootView.layout_bitcoin_info
    }

    private val fiatInfoLayout: View by lazy {
        rootView.layout_fiat_info
    }

    private val fiatName: TextView by lazy {
        rootView.label_fiat
    }

    private val fiatImage: ImageView by lazy {
        rootView.image_fiat
    }


    private lateinit var tickerAdapter: TickerListAdapter

    override fun getRootViewLayoutId(): Int {
        return R.layout.vu_main
    }

    override fun onCreate() {
        super.onCreate()

        (activity as AppCompatActivity).setSupportActionBar(rootView.toolbar)

        tickerAdapter = TickerListAdapter(tickerSelectPublisher)
        listView.adapter = tickerAdapter
        listView.layoutManager = LinearLayoutManager(activity)
        val interItemPadding = listView.resources.getDimensionPixelSize(R.dimen.content_padding)
        listView.addItemDecoration(object:RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                outRect?.top = interItemPadding
            }
        })

        bitcoinInput.setCompoundDrawablesRelative(UiUtils.getCompoundDrawableForTextDrawable(UiUtils.getCurrencySign(CryptoCurrency.Bitcoin),
                bitcoinInput,
                bitcoinInput.currentTextColor), null,null, null)

        if (UiUtils.isHorizontal(rootView.resources)) {
            rootView.app_bar.setExpanded(false)
        }
        UiUtils.setRipple(rootView.clicker_bitcoin_info)

        RxView.clicks(rootView.clicker_bitcoin_info)
                .map {
                    TextUtils.deFormatCurrency(bitcoinInput.text.toString())
                }.subscribe(bitcoinInputPublisher)

        UiUtils.setRipple(rootView.clicker_fiat_info)
        RxView.clicks(rootView.clicker_fiat_info)
                .map {
                    TextUtils.deFormatCurrency(fiatInput.text.toString())
                }.subscribe(fiatInputPublisher)

        RxView.focusChanges(bitcoinInput)
                .skipInitialValue()
                .filter {
                    it && !bitcoinInfoLayout.isActivated
                }.map {
                    TextUtils.deFormatCurrency(bitcoinInput.text.toString())
                }.subscribe(bitcoinInputPublisher)

        RxView.focusChanges(fiatInput)
                .skipInitialValue()
                .filter {
                    it && !fiatInfoLayout.isActivated
                }.map {
                    TextUtils.deFormatCurrency(fiatInput.text.toString())
                }.subscribe(fiatInputPublisher)
    }


    private fun updateActivated(convertToFiat: Boolean): Boolean {
        val activatedChange = (bitcoinInfoLayout.isActivated != convertToFiat) || (fiatInfoLayout.isActivated != !convertToFiat)
        if (!activatedChange) {
            return false
        }

        bitcoinInfoLayout.isActivated = convertToFiat
        rootView.layout_bitcoin_input.isActivated = convertToFiat
        fiatInfoLayout.isActivated = !convertToFiat
        rootView.layout_fiat_input.isActivated = !convertToFiat

        bitcoinInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
                UiUtils.getCompoundDrawableForTextDrawable(
                        UiUtils.getCurrencySign(CryptoCurrency.Bitcoin),
                        bitcoinInput,
                        if (convertToFiat) bitcoinInput.resources.getColor(R.color.secondaryColor) else bitcoinInput.currentTextColor),
                null,null, null)

        return true
    }


    fun updateCalculation(calc: CalculationViewModel) {
        if (bitcoinInput.isFocused != calc.convertToFiat) {
            bitcoinInput.requestFocus()
        }
        if (fiatInput.isFocused != !calc.convertToFiat) {
            fiatInput.requestFocus()
        }

        val convertDirectChanged = updateActivated(calc.convertToFiat)

        if (calc.forceSet || calc.convertToFiat) {
            fiatInput.removeTextChangedListener(fiatInputWatcher)
            val text = calc.ticker?.price ?: TextUtils.PLACE_HOLDER_STRING
            val tooManyDigits = (text.length > maxFiatInputLength)

            if (tooManyDigits) {
                fiatInput.setText(TextUtils.TOO_MANY_DIGITS_MSG)

            } else {
                fiatInput.setText(text)
            }
            fiatInputWatcher.prevString = fiatInput.text.toString()

            if (text == TextUtils.PLACE_HOLDER_STRING) {
                fiatInput.isEnabled = false

            } else {

                // must be able to change text if selected
                if (!tooManyDigits || !calc.convertToFiat) {
                    fiatInput.addTextChangedListener(fiatInputWatcher)
                    fiatInput.isEnabled = true

                } else {
                    fiatInput.isEnabled = false
                }
            }
        }

        if (calc.forceSet || !calc.convertToFiat) {
            bitcoinInput.removeTextChangedListener(bitcoinInputWatcher)
            val text = calc.bitcoinPrice
//            Timber.d("setBitcoin: text = $text, length = ${text.length}, maxLength = ${maxBitcoinInputLength}")
            if (text.length > maxBitcoinInputLength) {
                bitcoinInput.setText(TextUtils.TOO_MANY_DIGITS_MSG)
                if (calc.convertToFiat) {
                    // must be able to change text if selected
                    bitcoinInput.addTextChangedListener(bitcoinInputWatcher)

                } else {
                    bitcoinInput.isEnabled = false
                }

            } else {
                bitcoinInput.isEnabled = true
                bitcoinInput.setText(text)
                bitcoinInput.addTextChangedListener(bitcoinInputWatcher)
            }
            bitcoinInputWatcher.prevString = bitcoinInput.text.toString()
        }

        val nuFiatName = calc.ticker?.name ?: TextUtils.PLACE_HOLDER_STRING
        val nameChanged = fiatName.text != nuFiatName

        // update fiat image and label
        if (nameChanged) {
            fiatName.text = nuFiatName
            if (calc.ticker != null) {
                fiatImage.setImageResource(UiUtils.getCurrencyVectorRes(calc.ticker.code))

            } else {
                fiatImage.setImageDrawable(null)
            }
        }

        // update fiat compound image
        if (nameChanged || convertDirectChanged)  {
            if (calc.ticker != null) {
                fiatInput.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        UiUtils.getCompoundDrawableForTextDrawable(
                                UiUtils.getCurrencySign(ExchangeUtils.getFiatCurrencyForTicker(calc.ticker.code)),
                                fiatInput,
                                if (!calc.convertToFiat) fiatInput.resources.getColor(R.color.secondaryColor) else fiatInput.currentTextColor),
                        null, null, null)

            } else {
                fiatInput.setCompoundDrawablesRelative(null, null, null, null)
            }
        }
    }


    fun updateTickers(tickers: List<TickerViewModel>) {
        tickerAdapter.items = tickers
        tickerAdapter.notifyDataSetChanged()
    }
}