package com.cybozu.datastore.crypto.internal

@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
@RequiresOptIn(
    message = "This API is internal to DataStore Crypto and should not be used from outside.",
    level = RequiresOptIn.Level.ERROR
)
public annotation class DataStoreCryptoInternalApi
