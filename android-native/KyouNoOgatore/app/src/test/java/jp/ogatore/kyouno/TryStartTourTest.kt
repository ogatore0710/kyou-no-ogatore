package jp.ogatore.kyouno

import jp.ogatore.kyouno.record.RecordStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Fable監査GO-12(alan5差し戻し2026-07-28・141条案件): tryStartTour(MainActivity.kt)は
// 「とじる」ボタン以外(外タップ・戻る・タブ移動)でツアーが起動しなかったD1近縁のバグを
// 直した今日新設の関数だが、ユニットテストが無かった。純粋なトップレベル関数として
// 十分テスト可能なので固定する。
class TryStartTourTest {
    @Test
    fun consumesPendingFlagSynchronouslyAndStartsTourAfterDelay() {
        val store = RecordStore.inMemory()
        store.set("tourpend", true)
        store.set("tourseen", false)
        var consumed = false
        var started = false

        tryStartTour(store, CoroutineScope(Dispatchers.Default), onTourpendConsumed = { consumed = true }) { started = true }

        // フラグの消費とonTourpendConsumedは同期的に起こる(350ms delayの前)。
        assertFalse(store.get("tourpend", true))
        assertTrue(store.get("tourseen", false))
        assertTrue(consumed)
        assertFalse(started)

        Thread.sleep(500)
        assertTrue(started)
    }

    @Test
    fun doesNothingWhenNotPending() {
        val store = RecordStore.inMemory()
        store.set("tourpend", false)
        store.set("tourseen", false)
        var started = false

        tryStartTour(store, CoroutineScope(Dispatchers.Default)) { started = true }
        Thread.sleep(500)

        assertFalse(started)
        assertFalse(store.get("tourseen", false))
    }

    @Test
    fun doesNothingWhenAlreadySeen() {
        val store = RecordStore.inMemory()
        store.set("tourpend", true)
        store.set("tourseen", true)
        var started = false

        tryStartTour(store, CoroutineScope(Dispatchers.Default)) { started = true }
        Thread.sleep(500)

        assertFalse(started)
        // 既に見た扱いのときはtourpendも消費しない(次にtourseenがリセットされたときに
        // 素通しで起動できるよう、フラグ自体は温存する)。
        assertTrue(store.get("tourpend", false))
    }
}
