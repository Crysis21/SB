package ro.holdone.swissborg.extensions

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

fun Disposable.dispose(by: CompositeDisposable) {
    by.add(this)
}