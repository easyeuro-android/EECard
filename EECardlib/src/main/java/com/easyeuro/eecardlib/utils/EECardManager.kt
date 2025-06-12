package com.easyeuro.eecardlib.utils

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.checkout.cardmanagement.CheckoutCardManager
import com.checkout.cardmanagement.model.Card
import com.checkout.cardmanagement.model.CardManagementDesignSystem
import com.checkout.cardmanagement.model.Environment
import com.checkout.cardmanagement.model.ProvisioningConfiguration
import com.checkout.cardmanagement.model.SecurePropertiesResult
import com.checkout.cardmanagement.model.getPANAndSecurityCode
import com.checkout.cardmanagement.model.getPan
import com.checkout.cardmanagement.model.getPin
import com.checkout.cardmanagement.model.getSecurityCode
import com.checkout.cardmanagement.model.handleCardResult
import com.checkout.cardmanagement.model.provision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EECardManager(
    context: Context,
    textStyle: TextStyle = TextStyle(
        fontSize = TextUnit(16f, TextUnitType.Sp),
        color = Color(22, 22, 22)
    ),
    panTextSeparator: String = "-",
    cardEnvironment: CardEnvironment = CardEnvironment.PRODUCTION

) {

    private val mContext: Context
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val manager: CheckoutCardManager
    private val environment: Environment
    private var card: Card? = null

    init {
        mContext = context
        environment = if (cardEnvironment == CardEnvironment.PRODUCTION) {
            Environment.PRODUCTION
        } else {
            Environment.SANDBOX
        }

        manager =
            newCardManagement(mContext, newCardManagementDesignSystem(textStyle, panTextSeparator))
    }

    private fun newCardManagementDesignSystem(
        textStyle: TextStyle,
        panTextSeparator: String = "-",
    ): CardManagementDesignSystem {
        return CardManagementDesignSystem(
            textStyle = textStyle,
            panTextSeparator = panTextSeparator,
        )

    }

    private fun newCardManagement(
        mContext: Context,
        cardManagementDesignSystem: CardManagementDesignSystem
    ): CheckoutCardManager {
        return CheckoutCardManager(
            mContext,
            cardManagementDesignSystem,
            environment
        )
    }

    /**
     *
     * @param appCardId: Start with "crd_"
     * */
    fun initCard(
        accessToken: String,
        appCardId: String,
        completionHandler: ValuelessCompletion,
    ) {
        scope.launch(Dispatchers.Default) {
            val loginResult = manager.logInSession(accessToken)
            if (!loginResult) {
                withContext(Dispatchers.Main) {
                    completionHandler(Result.failure(Throwable(message = "Token error!")))
                }
                return@launch
            }

            manager.getCards { result: Result<List<Card>> ->
                result.onSuccess {
                    val targetCard = getTargetCard(it, appCardId)
                    if (targetCard == null) {
                        completionHandler(Result.failure(Throwable(message = "Card does not exist!")))
                        return@getCards
                    }
                    card = targetCard
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.success(Unit))
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }

            }
        }


    }

    private fun getTargetCard(cards: List<Card>, targetCardId: String): Card? {
        for (c in cards) {
            if (targetCardId.equals(c.id, ignoreCase = true)) {
                return c
            }
        }
        return null
    }

    fun getCardExpDate(): String {
        val year = card?.expiryDate?.year ?: ""
        val month = card?.expiryDate?.month ?: ""
        return "$year-$month"
    }

    fun getCardPin(
        accessToken: String,
        completionHandler: SecureInfoCompletion
    ) {
        scope.launch(Dispatchers.Default) {
            card?.getPin(accessToken) { result: Result<AbstractComposeView> ->
                result
                    .onSuccess {
                        scope.launch(Dispatchers.Main) {
                            completionHandler(Result.success(it))
                        }
                    }.onFailure {
                        scope.launch(Dispatchers.Main) {
                            completionHandler(Result.failure(it))
                        }
                    }
            }
                ?: scope.launch(Dispatchers.Main) {
                    completionHandler(
                        Result.failure(
                            Throwable(
                                message = "Please initialize first!"
                            )
                        )
                    )
                }

        }

    }

    /**
     *
     * @param completionHandler CardPan:it.first, CardPanCvv:it.second
     *
     * */
    fun getCardPanAndCvv(
        accessToken: String,
        completionHandler: SecurePropertiesCompletion
    ) {
        scope.launch(Dispatchers.Default) {
            card?.getPANAndSecurityCode(accessToken) { result: SecurePropertiesResult ->
                result.onSuccess {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.success(it))
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }
            }
                ?: scope.launch(Dispatchers.Main) {
                    completionHandler(
                        Result.failure(
                            Throwable(
                                message = "Please initialize first!"
                            )
                        )
                    )
                }

        }

    }

    fun getCardPan(
        accessToken: String,
        completionHandler: SecureInfoCompletion
    ) {

        scope.launch(Dispatchers.Default) {
            card?.getPan(accessToken) { result: Result<AbstractComposeView> ->
                result.onSuccess {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.success(it))
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }
            }
                ?: scope.launch(Dispatchers.Main) {
                    completionHandler(
                        Result.failure(
                            Throwable(
                                message = "Please initialize first!"
                            )
                        )
                    )
                }

        }

    }

    fun getCardCvv(
        accessToken: String,
        completionHandler: SecureInfoCompletion
    ) {
        scope.launch(Dispatchers.Default) {
            card?.getSecurityCode(accessToken) { result: Result<AbstractComposeView> ->
                result.onSuccess {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.success(it))
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }
            }
                ?: scope.launch(Dispatchers.Main) {
                    completionHandler(
                        Result.failure(
                            Throwable(
                                message = "Please initialize first!"
                            )
                        )
                    )
                }

        }

    }

    /**
     *
     * Add card to google wallet
     * PRODUCTION environment only
     *
     * */
    fun provision(
        activity: android.app.Activity,
        appCardholderId: String,
        provisionToken: String,
        serviceRSAModulus: String,
        completionHandler: ValuelessCompletion
    ) {
        configCardManager(
            activity = activity,
            appCardholderId = appCardholderId,
            serviceRSAModulus = serviceRSAModulus,
            completionHandler = fun(result: Result<Unit>) {
                Log.i("Provision config result", result.toString())
                result.onSuccess {
                    doProvision(activity, provisionToken, completionHandler)
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }
            })
    }

    private val serviceRSAExponent = "0x10001".toByteArray()
    private val serviceURLPrd = "https://client-api.d1.thalescloud.io/"
    private val digitalCardURLPrd = "https://hapi.dbp.thalescloud.io/mg/tpc/"
    private val issuerID = "is_cko_oui"

    private fun configCardManager(
        activity: android.app.Activity,
        appCardholderId: String,
        serviceRSAModulus: String,
        completionHandler: (Result<Unit>) -> Unit
    ) {
        val configuration = ProvisioningConfiguration(
            serviceRSAExponent = serviceRSAExponent,
            serviceRSAModulus = serviceRSAModulus.toByteArray(),
            serviceURL = serviceURLPrd,
            digitalCardURL = digitalCardURLPrd,
            issuerID = issuerID
        )
        manager.configurePushProvisioning(
            activity = activity,
            cardholderId = appCardholderId,
            configuration = configuration,
            completionHandler = completionHandler
        )
    }


    private fun doProvision(
        activity: android.app.Activity,
        //   appCardholderId: String,
        provisionToken: String,
        //    serviceRSAModulus:String,
        completionHandler: ValuelessCompletion
    ) {


        card?.provision(
            activity = activity,
//            cardholderID = appCardholderId,
//            configuration = configuration,
            token = provisionToken,
            completionHandler = fun(result: Result<Unit>) {
                Log.i("Provision result", result.toString())
                result.onSuccess {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.success(Unit))
                    }
                }.onFailure {
                    scope.launch(Dispatchers.Main) {
                        completionHandler(Result.failure(it))
                    }
                }
            })
            ?: scope.launch(Dispatchers.Main) { completionHandler(Result.failure(Throwable(message = "Please initialize first!"))) }


    }

    /**
     *
     * Handle the provision result.
     * Call in the 'onActivityResult' method of your Activity or Fragment class
     *
     * */
    fun handleCardResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        card?.handleCardResult(requestCode, resultCode, data)
    }

    fun destroy() {
        manager.logoutSession()
        scope.cancel()
    }
}
