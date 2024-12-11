package com.easyeuro.eecardlib.utils

import android.content.Context
import com.checkout.oob.CheckoutOOBManager
import com.checkout.oob.Environment
import com.checkout.oob.error.CheckoutOOBError
import com.checkout.oob.error.ConfigurationErrorCode.E1000_INVALID_CARD_ID
import com.checkout.oob.error.ConfigurationErrorCode.E1001_INVALID_TOKEN
import com.checkout.oob.error.ConfigurationErrorCode.E1005_INVALID_TRANSACTION_ID
import com.checkout.oob.error.ConnectivityErrorCode.E1006_NO_INTERNET_CONNECTION
import com.checkout.oob.error.ConnectivityErrorCode.E1007_CONNECTION_FAILED
import com.checkout.oob.error.ConnectivityErrorCode.E1008_CONNECTION_TIMEOUT
import com.checkout.oob.model.Authentication
import com.checkout.oob.model.CardLocale
import com.checkout.oob.model.Decision
import com.checkout.oob.model.DeviceRegistration
import com.checkout.oob.model.Method
import com.checkout.oob.model.PhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EEOobManager(
    context: Context,
    cardEnvironment: CardEnvironment = CardEnvironment.PRODUCTION
) {
    private val mContext: Context
    private val environment: Environment

    init {
        this.mContext = context
        environment = if (cardEnvironment == CardEnvironment.PRODUCTION) {
            Environment.PRODUCTION
        } else {
            Environment.SANDBOX
        }

    }


    private val oobManager =
        CheckoutOOBManager(context = context, environment = environment)
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    /**
     *
     * @param countryCode: Phone country code. Example: +33
     * @param appCardId: Start with "crd_"
     * */
    fun registerDevice(
        accessToken: String,
        applicationId: String,
        appCardId: String,
        countryCode: String,
        phone: String,
        cardLocale: EECardLocal = EECardLocal.EN,
        completionHandler: ValuelessCompletion
    ) {
        val deviceRegistration = DeviceRegistration(
            token = accessToken,
            cardId = appCardId,
            applicationID = applicationId,
            phoneNumber = PhoneNumber(countryCode = countryCode, number = phone),
            cardLocale = if (cardLocale == EECardLocal.FR) {
                CardLocale.FR
            } else {
                CardLocale.EN
            },
        )
        scope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                oobManager.registerDevice(deviceRegistration)
            }.collect { result ->
                result.onSuccess {
                    completionHandler(Result.success(Unit))
                }.onFailure {
                    val message = provideErrorMessage(it)
                    completionHandler(Result.failure(Throwable(message = message)))
                }
            }

        }
    }

    fun authenticatePayment(
        accessToken: String,
        appCardId: String,
        transactionId: String,
        accept: Boolean,
        completionHandler: ValuelessCompletion
    ) {
        val authenticationRequest = Authentication(
            transactionId = transactionId,
            token = accessToken,
            cardId = appCardId,
            method = Method.OOB_BIOMETRICS,
            decision = if (accept) {
                Decision.ACCEPTED
            } else {
                Decision.DECLINED
            },
        )

        scope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                oobManager.authenticatePayment(authenticationRequest)
            }.collect { result ->
                result.onSuccess {
                    completionHandler(Result.success(Unit))
                }.onFailure {
                    val message = provideErrorMessage(it)
                    completionHandler(Result.failure(Throwable(message = message)))
                }
            }
        }

    }

    private fun provideErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is CheckoutOOBError.ConfigurationError -> {
                when (throwable.errorCode) {
                    E1000_INVALID_CARD_ID -> "Invalid card ID"
                    E1001_INVALID_TOKEN -> "Invalid token"
                    E1005_INVALID_TRANSACTION_ID -> "Invalid transaction ID"
                    else -> throwable.errorMessage
                }
            }

            is CheckoutOOBError.UnprocessableContentError ->
                "Server failure with error code: ${throwable.errorCode} and error message: ${throwable.errorMessage}"

            is CheckoutOOBError.HttpError -> "error code ${throwable.errorCode}, message: ${throwable.errorMessage}"
            is CheckoutOOBError.ConnectivityError -> {
                when (throwable.errorCode) {
                    E1006_NO_INTERNET_CONNECTION -> "No internet connection"
                    E1007_CONNECTION_FAILED -> "Network connection failed. Please check internet connection"
                    E1008_CONNECTION_TIMEOUT -> "Network connection timeout. Please try again later"
                    else -> "Unknown connectivity error"
                }
            }

            else -> throwable.message ?: "Unknown error from the SDKKit. Please contact support."
        }
    }


}