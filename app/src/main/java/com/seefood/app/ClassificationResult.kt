package com.seefood.app

sealed class ClassificationResult {
    object Hotdog : ClassificationResult()
    object NotHotdog : ClassificationResult()
    data class Error(val message: String) : ClassificationResult()
}
