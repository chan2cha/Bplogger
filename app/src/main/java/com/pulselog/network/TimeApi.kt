package com.pulselog.network

import retrofit2.http.GET

// 서버는 이런 형태로 내려준다고 가정:
// { "nowIso": "2026-03-04T15:23:10+09:00" }
data class ServerTimeResponse(
    val nowIso: String
)

interface TimeApi {
    @GET("/api/time/now")
    suspend fun now(): ServerTimeResponse
}