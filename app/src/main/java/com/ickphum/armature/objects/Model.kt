package com.ickphum.armature.objects

private const val TAG = "Model"

class Model {

    private val _cylinders = mutableListOf<Cylinder>( )
    private val _nodes = mutableListOf<Node>()

    fun clear() {
        _cylinders.clear()
        _nodes.clear()
    }

    fun clearSelections() {
        for (cyl in _cylinders) {
            cyl.selected = false;
        }
        for (node in _nodes) {
            node.selected = false;
        }
    }

    fun getCylinderById( id: Int) : Cylinder = _cylinders.first { c -> c.id == id }

    fun getNodeById( id: Int) : Node = _nodes.first { n -> n.id == id }

    fun items() : List<Item> = _cylinders + _nodes

    fun selectedItems() : List<Item> = selectedCylinders() + selectedNodes()

    fun cylinders() = _cylinders

    fun nodes() = _nodes

    fun addCylinder( cylinder: Cylinder ) = _cylinders.add( cylinder )

    fun addNode( node: Node ) = _nodes.add( node )

    fun findSelected() : Item {
        val cyl = _cylinders.firstOrNull { c -> c.selected }
        if (cyl != null)
            return cyl
        return _nodes.first { n -> n.selected }
    }

    fun selectedCylinders() = _cylinders.filter { c -> c.selected }
    fun selectedNodes() = _nodes.filter { n -> n.selected }

}