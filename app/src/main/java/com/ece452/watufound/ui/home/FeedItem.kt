package com.ece452.watufound.ui.home

import com.ece452.watufound.models.FoundItem
import com.ece452.watufound.models.LostItem

sealed class FeedItem {
    data class Lost(val item: LostItem) : FeedItem()
    data class Found(val item: FoundItem) : FeedItem()
}