package com.familyexpenses.app.domain.rule

import com.familyexpenses.app.core.model.AccountType
import javax.inject.Inject

class ValidateFamilyPaidFromPersonalRule @Inject constructor() {

    fun validate(
        accountType: AccountType,
        paidFromPersonal: Boolean,
    ) {
        if (paidFromPersonal && accountType != AccountType.FAMILY) {
            throw IllegalArgumentException(
                "Solo un gasto de cuenta familiar puede marcarse como pagado con personal.",
            )
        }
    }
}
