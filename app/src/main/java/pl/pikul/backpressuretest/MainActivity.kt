package pl.pikul.backpressuretest

import android.hardware.Sensor
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.github.pwittchen.reactivesensors.library.ReactiveSensorEvent
import com.github.pwittchen.reactivesensors.library.ReactiveSensors
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var reactiveSensors: ReactiveSensors

    val textView by lazy { findViewById<TextView>(R.id.textView) }

    var disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        reactiveSensors = ReactiveSensors(this)

    }

    override fun onResume() {
        super.onResume()
        println("onResume")

//       / manyEvents()

//        tooMuchEvents()
        zipExample()
    }

    private fun zipExample() {
        disposables.add(
                Observable.zip(
                        observeRotationVector().toObservable(), tick(), BiFunction { event: ReactiveSensorEvent, tick: Long ->
                    return@BiFunction event
                }).subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleEvent(), handleError())
        )
    }

    private fun tooMuchEvents() {
        disposables.add(
                observeRotationVector()
                        .flatMap { event ->
                            Flowable.range(1, 1000).map { _ -> event }
                        }
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleEvent(), handleError())
        )
    }

    private fun manyEvents() {
        disposables.add(
                observeRotationVector()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleEvent(), handleError())
        )
    }

    private fun handleError(): ((error: Throwable) -> Unit) {
        return { error ->
            textView.post {
                textView.text = error.localizedMessage
            }
        }
    }

    private fun handleEvent(): ((event: ReactiveSensorEvent) -> Unit) {
        return { event ->
            if (event.sensorEvent != null) {
                textView.text = Arrays.toString(event.sensorEvent.values)
                        .replace(",".toRegex(), ",\n")
            }
        }
    }

    fun observeRotationVector(): Flowable<ReactiveSensorEvent> {
        return reactiveSensors.observeSensor(Sensor.TYPE_ROTATION_VECTOR, 100, null,
                BackpressureStrategy.DROP)
    }

    fun tick(): Observable<Long> {
        return Observable.interval(1, TimeUnit.SECONDS)
    }

    override fun onPause() {
        super.onPause()
        println("onPause")
        disposables.clear()
    }

}
