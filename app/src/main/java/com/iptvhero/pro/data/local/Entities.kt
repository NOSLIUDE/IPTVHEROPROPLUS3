package com.iptvhero.pro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val heroAvatar: String = "BATMAN",
    val sourceType: String = "M3U",
    val urlOrServer: String = "",
    val username: String = "",
    val password: String = "",
    val activeConnections: Int = 0,
    val maxConnections: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val remoteId: String? = null,
    val title: String,
    val groupName: String = "Geral",
    val streamIcon: String? = null,
    val url: String,
    val contentType: String = "LIVE"
)
