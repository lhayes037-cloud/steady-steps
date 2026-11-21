package com.lance.steadysteps

object Dinners {
    const val BASE = "https://lhayes037-cloud.github.io/steady-steps/"

    // index 0 = Monday
    val titles = listOf(
        "Lemon Garlic Chicken with Roasted Broccoli and Rice",
        "Turkey Taco Lettuce Bowls",
        "Baked Salmon with Green Beans and Sweet Potato",
        "Chicken and Veggie Stir Fry over Rice",
        "Shrimp and Veggie Skillet with Quinoa",
        "Turkey and Bean Chili",
        "Sheet Pan Chicken Sausage and Veggies"
    )
    val keys = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    fun todayIndex(cal: java.util.Calendar = java.util.Calendar.getInstance()): Int =
        (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    fun message(i: Int): String =
        "Good morning! Dinner tonight is " + titles[i] + ".\nRecipe: " + BASE + "?d=" + keys[i] +
            "\nAll dinners this week: " + BASE + "week.html" +
            "\nGrocery list: " + BASE + "grocery.html"
}
