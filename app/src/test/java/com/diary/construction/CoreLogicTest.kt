package com.diary.construction

import com.diary.construction.core.util.Contact
import com.diary.construction.core.util.Money
import com.diary.construction.core.util.Quantity
import com.diary.construction.domain.model.AgreementType
import com.diary.construction.domain.model.Contractor
import com.diary.construction.domain.usecase.Estimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test
    fun money_parse_and_format() {
        assertEquals(125050L, Money.parse("1,250.50"))
        assertEquals(100L, Money.parse("1"))
        assertNull(Money.parse("abc"))
        assertEquals("Rs 1,250", Money.format(125000, "Rs"))
        assertEquals("Rs 1,250.50", Money.format(125050, "Rs"))
        assertEquals("-Rs 300", Money.format(-30000, "Rs"))
        assertEquals("1250.5", Money.toInput(125050))
    }

    @Test
    fun rate_times_quantity() {
        // 12.5 bags x Rs 1,450 = Rs 18,125
        assertEquals(1812500L, Money.multiply(145000, Quantity.parse("12.5")!!))
    }

    @Test
    fun contractor_per_sqft() {
        val c = Contractor(projectId = 1, name = "A", agreementType = AgreementType.PER_SQ_FT, rate = 120000, areaSqFt = 225000, workDonePercent = 50)
        // Rs 1,200 x 2,250 sq ft = Rs 2,700,000
        assertEquals(270000000L, c.agreedAmount)
        assertEquals(135000000L, c.workDoneValue)
    }

    @Test
    fun phone_to_international() {
        assertEquals("923001234567", Contact.toInternational("0300-1234567"))
        assertEquals("923001234567", Contact.toInternational("+92 300 1234567"))
    }

    @Test
    fun wall_estimate_is_sane() {
        // 10 ft x 10 ft x 9" wall = 75 cft -> roughly 13 bricks per cft.
        val r = Estimator.wall(Estimator.WallInput(10.0, 10.0, 9.0, wastagePercent = 0.0))
        assertEquals(75.0, r.volumeCft, 0.001)
        assertTrue(r.bricks in 900..1100)
        assertTrue(r.cementBags > 1.0 && r.cementBags < 6.0)
    }
}
