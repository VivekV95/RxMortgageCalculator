package com.example.rxmortgagecalculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.jakewharton.rxbinding3.InitialValueObservable
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        number_picker.minValue = 1
        number_picker.maxValue = 30
        number_picker.setOnValueChangedListener{ _, _, newVal ->
            loan_length.setText("${newVal.times(12)}")
        }

        val obsPurchase = purchase_price.textChanges().validate()
        val obsDown = down_payment.textChanges().validate()
        val obsInterest = interest_rate.textChanges().validate()
        val obsLength = loan_length.textChanges().validate()

        disposables.add(Observables.combineLatest(
            obsPurchase,
            obsDown,
            obsInterest,
            obsLength
        ) { purchase, down, interest, length ->
                calculatePayment(purchase, down, interest, length)
        }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ payment ->
                mortgage_payment.text = payment.toString()
            }, { throwable ->
                Log.e("error", throwable.message)
            })
        )

        disposables.add(
            getRandomNumbers().subscribe({ numbers ->
                loan_length.setText("${numbers[0]}")
            }, { throwable ->
                Log.e("test", throwable.message)
            })
        )
    }

    private fun getRandomNumbers(): Single<List<Int>> {
        return Retrofit.Builder()
            .baseUrl("http://qrng.anu.edu.au/API/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(RandomApi::class.java)
            .getRandomNumbers("2", "uint8")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { randomObject: RandomObject ->
                randomObject.data
            }
    }

    private fun InitialValueObservable<CharSequence>.validate(): Observable<Double> {
        return this
            .filter { it.length > 1 }
            .map { it.toString().toDouble() }
    }

    private fun calculatePayment(
        p: Double,
        d: Double,
        r: Double,
        l: Double
    ): Double {
        return ((p - d).times(r.div(12)).times((1 + r.div(12)).pow(l)))
            .div((1 + r.div(12)).pow(l) - 1)
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }
}

interface RandomApi {

    @GET("jsonI.php")
    fun getRandomNumbers(
        @Query("length") length: String,
        @Query("type") type: String
    ): Single<RandomObject>
}
