package com.example.data.model

/**
 * A collection published from the admin panel («اذکار برنامه»). With a [builtInKey] its items are
 * added to the end of that built-in collection (e.g. "morning"); without one it is a new collection
 * on «اذکار و ادعیه».
 */
data class AdminCollection(
    val id: Int,
    val builtInKey: String?,
    val title: String,
    val description: String,
    val iconName: String,
    val items: List<DhikrItem>
)

internal data class MergedAdhkar(
    val categories: List<Category>,
    val adhkar: Map<String, List<DhikrItem>>
)

/**
 * Admin dhikr ids are offset so they never collide with built-in ids inside a collection; saved
 * progress and favorites are keyed by collection and dhikr id.
 */
const val ADMIN_DHIKR_ID_OFFSET = 1_000_000

/** Id of a new (not built-in) admin collection. Stable: progress and favorites are keyed by it. */
fun adminCategoryId(serverId: Int) = "app_$serverId"

internal fun mergeAdminCollections(
    builtInCategories: List<Category>,
    builtInAdhkar: Map<String, List<DhikrItem>>,
    collections: List<AdminCollection>
): MergedAdhkar {
    val adhkar = builtInAdhkar.toMutableMap()
    val added = mutableListOf<Category>()
    collections.filter { it.items.isNotEmpty() }.forEach { collection ->
        val key = collection.builtInKey
        when {
            key == null -> {
                val id = adminCategoryId(collection.id)
                adhkar[id] = collection.items
                added += Category(id, collection.title, collection.description, collection.iconName, collection.items.size)
            }
            // An unknown key (e.g. a collection this app version doesn't have) is ignored.
            key in builtInAdhkar -> adhkar[key] = adhkar.getValue(key) + collection.items
        }
    }
    val categories = builtInCategories.map { category ->
        val extra = adhkar[category.id].orEmpty().size - builtInAdhkar[category.id].orEmpty().size
        if (extra > 0) category.copy(count = category.count + extra) else category
    } + added
    return MergedAdhkar(categories, adhkar)
}
