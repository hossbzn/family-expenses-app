package com.familyexpenses.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppDatabaseMigrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRecurrenceOccurrenceUniqueConstraintAndReviewAt() = runBlocking {
        db.accountDao().upsert(AccountEntity("acc-personal", "Personal", AccountType.PERSONAL, "EUR", 0L, false, 1000L))
        db.accountDao().upsert(AccountEntity("acc-family", "Familia", AccountType.FAMILY, "EUR", 0L, false, 1000L))
        db.categoryDao().upsert(CategoryEntity("cat-supermarket", "Supermercado", "ic_cart", CategoryType.EXPENSE, 1, true, 1, false, 1000L))

        val rule = RecurrenceRuleEntity(
            id = "rule-unique-1",
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-personal",
            responsibleAccountId = "acc-family",
            categoryId = "cat-supermarket",
            amountMinor = 5000L,
            note = "Compra semanal",
            frequency = RecurrenceFrequency.WEEKLY,
            intervalCount = 1,
            dayOfWeek = 1,
            mode = RecurrenceExecutionMode.PENDING,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = 1000000000L,
            endAt = null,
            nextOccurrenceAt = 1000000000L,
            createdAt = 1000000000L,
        )

        db.recurrenceRuleDao().upsert(rule)

        val occurrence1 = RecurrenceOccurrenceEntity(
            id = "occ-1",
            ruleId = "rule-unique-1",
            status = OccurrenceStatus.PENDING,
            scheduledAt = 1000000000L,
            reviewAt = 1000086400L,
            amountMinor = 5000L,
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-personal",
            responsibleAccountId = "acc-family",
            categoryId = "cat-supermarket",
            note = "Compra semanal",
            transactionId = null,
            createdAt = 1000000000L,
        )

        db.recurrenceOccurrenceDao().upsert(occurrence1)

        val fetched = db.recurrenceOccurrenceDao().getOccurrenceById("occ-1")
        assertNotNull(fetched)
        assertEquals(1000086400L, fetched?.reviewAt)
        assertEquals(1000000000L, fetched?.scheduledAt)

        val hasOccurrence = db.recurrenceOccurrenceDao().hasOccurrenceForDate("rule-unique-1", 1000000000L)
        assertEquals(true, hasOccurrence)
    }
}
