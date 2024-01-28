package com.ickphum.armature.objects

enum class Axis {
    X {
        override fun otherAxes() = intArrayOf(1, 2)
        override fun axis() = 0
    },
    Y {
        override fun otherAxes() = intArrayOf(0, 2)
        override fun axis() = 1
    },
    Z {
        override fun otherAxes() = intArrayOf(0, 1)
        override fun axis() = 2
    };

    abstract fun otherAxes(): IntArray
    abstract fun axis(): Int
}