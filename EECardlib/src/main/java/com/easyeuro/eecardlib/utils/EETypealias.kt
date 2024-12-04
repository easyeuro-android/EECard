package com.easyeuro.eecardlib.utils

import androidx.compose.ui.platform.AbstractComposeView

typealias ValuelessCompletion = (result: Result<Unit>) -> Unit

typealias SecureInfoCompletion = (result: Result<AbstractComposeView>) -> Unit

typealias SecurePropertiesCompletion = (Result<Pair<AbstractComposeView, AbstractComposeView>>) -> Unit