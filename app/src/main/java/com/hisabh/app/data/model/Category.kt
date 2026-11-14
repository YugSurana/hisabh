package com.hisabh.app.data.model

enum class Category(
    val title: String,
    val iconName: String,
    val defaultKeywords: List<String>
) {
    FOOD_AND_DINING(
        title = "Food & Dining",
        iconName = "restaurant",
        defaultKeywords = listOf(
            "zomato", "swiggy", "chai", "tea", "cafe", "restaurant",
            "mcdonald", "burger", "starbucks", "eat", "pizza", "kfc",
            "subway", "domino", "haldiram", "biryani", "dhaba"
        )
    ),
    TRAVEL_AND_FUEL(
        title = "Travel & Fuel",
        iconName = "directions_car",
        defaultKeywords = listOf(
            "uber", "ola", "rapido", "petrol", "fuel", "hpcl", "bpcl",
            "ioc", "indianoil", "irctc", "metro", "fastag", "toll",
            "railway", "indigo", "airindia"
        )
    ),
    GROCERIES(
        title = "Groceries",
        iconName = "shopping_cart",
        defaultKeywords = listOf(
            "blinkit", "zepto", "instamart", "dmart", "supermarket",
            "grocery", "bigbasket", "nature basket", "spencer", "kirana",
            "milk", "vegetables", "fruits"
        )
    ),
    BILLS_AND_RECHARGE(
        title = "Bills & Recharge",
        iconName = "receipt_long",
        defaultKeywords = listOf(
            "airtel", "jio", "vi", "bescom", "electricity", "broadband",
            "bill", "recharge", "tatasky", "dth", "water", "gas", "cesc",
            "torrent", "adanielectricity"
        )
    ),
    SHOPPING(
        title = "Shopping",
        iconName = "shopping_bag",
        defaultKeywords = listOf(
            "amazon", "flipkart", "myntra", "ajio", "zara", "retail",
            "h&m", "nykaa", "meesho", "croma", "reliancedigital",
            "apple", "tata cliq", "westside"
        )
    ),
    ENTERTAINMENT(
        title = "Entertainment",
        iconName = "movie",
        defaultKeywords = listOf(
            "bookmyshow", "pvr", "inox", "netflix", "spotify", "prime",
            "hotstar", "youtube", "sony liv", "cinema", "theatre"
        )
    ),
    TRANSFER_AND_FRIENDS(
        title = "Transfer & Friends",
        iconName = "swap_horiz",
        defaultKeywords = listOf(
            "upi", "transfer", "okaxis", "okhdfcbank", "okicici",
            "oksbi", "paytm", "ybl", "ibl", "axl"
        )
    ),
    POCKET_MONEY_RECEIVED(
        title = "Pocket Money / Received",
        iconName = "account_balance_wallet",
        defaultKeywords = listOf(
            "salary", "refund", "cashback", "dividend", "interest",
            "received", "credited", "bonus"
        )
    ),
    OTHER(
        title = "Other",
        iconName = "category",
        defaultKeywords = emptyList()
    );

    companion object {
        fun fromTitle(title: String): Category {
            return entries.firstOrNull { it.title.equals(title, ignoreCase = true) } ?: OTHER
        }

        fun inferCategory(merchant: String, txType: TransactionType): Category {
            if (txType == TransactionType.CREDIT) {
                return POCKET_MONEY_RECEIVED
            }
            val lower = merchant.lowercase()
            for (category in entries) {
                if (category == OTHER || category == POCKET_MONEY_RECEIVED) continue
                if (category.defaultKeywords.any { lower.contains(it) }) {
                    return category
                }
            }
            return OTHER
        }
    }
}

