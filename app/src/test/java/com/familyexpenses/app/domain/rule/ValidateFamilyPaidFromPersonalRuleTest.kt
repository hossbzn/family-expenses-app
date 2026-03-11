package com.familyexpenses.app.domain.rule

import com.familyexpenses.app.core.model.AccountType
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateFamilyPaidFromPersonalRuleTest {

    private val rule = ValidateFamilyPaidFromPersonalRule()

    @Test
    fun `allows family expense paid from personal`() {
        rule.validate(
            accountType = AccountType.FAMILY,
            paidFromPersonal = true,
        )
    }

    @Test
    fun `rejects personal account paid from personal`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.validate(
                accountType = AccountType.PERSONAL,
                paidFromPersonal = true,
            )
        }
    }
}
