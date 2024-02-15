package com.ickphum.armature.objects

enum class Axis {
    X {
        override fun otherAxes() = intArrayOf(1, 2)
        override fun axis() = 0
        override fun nextAxis() = Axis.Y
    },
    Y {
        override fun otherAxes() = intArrayOf(0, 2)
        override fun axis() = 1
        override fun nextAxis() = Axis.Z
    },
    Z {
        override fun otherAxes() = intArrayOf(0, 1)
        override fun axis() = 2
        override fun nextAxis() = Axis.X
    };

    abstract fun otherAxes(): IntArray
    abstract fun axis(): Int
    abstract fun nextAxis() : Axis
}