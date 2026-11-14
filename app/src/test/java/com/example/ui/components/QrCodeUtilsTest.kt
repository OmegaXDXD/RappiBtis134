package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeUtilsTest {

    @Test
    fun extractNumeroControl_fromPlainDgetiBarcode_returnsDigits() {
        assertEquals("23320051340123", QrCodeUtils.extractNumeroControl("23320051340123"))
    }

    @Test
    fun extractNumeroControl_fromTaggedQrPayload_extractsControlNumber() {
        val raw = "CBTIS134|NC:24320051340987|NOMBRE:JUAN PEREZ"
        assertEquals("24320051340987", QrCodeUtils.extractNumeroControl(raw))
    }

    @Test
    fun extractNumeroControl_fromUrlPayload_extractsDigits() {
        val raw = "https://cbtis134.edu.mx/credencial?id=22320051340055"
        assertEquals("22320051340055", QrCodeUtils.extractNumeroControl(raw))
    }
}
