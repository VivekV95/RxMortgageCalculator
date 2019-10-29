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

class MainActivity : AppCompatActivity() {

    val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val obsPurchase = purchase_price.textChanges().validate()
        val obsDown = down_payment.textChanges().validate()
        val obsInterest = interest_rate.textChanges().validate()
        val obsLength = loan_length.textChanges().validate()

        val obsCombined = Observables.combineLatest(
            obsPurchase,
            obsDown,
            obsInterest,
            obsLength) {purchase, down, interest, length -> {
        }
        }

        disposables.add(
            getRandomNumbers().subscribe({ numbers ->
                val i = 0
            }, {throwable -> Log.e("test", throwable.message)}) )
    }

    fun getRandomNumbers(): Single<List<Int>> {
        return Retrofit.Builder()
            .baseUrl("https://qrng.anu.edu.au/API/")
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

    fun InitialValueObservable<CharSequence>.validate(): Observable<Double> {
        return this
            .filter { it.length > 1 }
            .map { it.toString().toDouble() }
    }

    fun calculatePayment(purchasePrice: Double,
                         downPayment: Double,
                         interestRate: Double,
                         loanLength: Double): Double {
    }
}

interface RandomApi {

    @GET("jsonI.php")
    fun getRandomNumbers(@Query("length")length: String,
                         @Query("type")type: String): Single<RandomObject>
}
