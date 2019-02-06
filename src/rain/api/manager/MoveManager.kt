package rain.api.manager

import rain.api.components.MoveComponent
import rain.api.components.Transform
import java.util.*

// x,y,vx,vy
private val moveLookup = HashMap<Long, Transform>()
private var moveData = FloatArray(256)
private var nextIndex = 0

/*
    TODO: We use transform object as reference to connect Entity to the component
    Could we make this more efficient using a ID or index?
 */

internal fun moveManagerAddMoveComponent(id: Long, transform: Transform, vx: Float, vy: Float) {
    moveLookup[id] = transform
    if (moveData.size <= nextIndex) {
        val tmp = FloatArray(moveData.size * 2)
        System.arraycopy(moveData, 0, tmp, 0, moveData.size)
        moveData = tmp
    }

    moveData[nextIndex] = transform.x
    moveData[nextIndex + 1] = transform.y
    moveData[nextIndex + 2] = vx
    moveData[nextIndex + 3] = vy
    nextIndex += 4
}

internal fun moveManagerGetMoveComponent(id: Long): MoveComponent? {
    val transform = moveLookup[id]
    if (transform != null) {
        val index = moveLookup.values.indexOf(transform)
        return MoveComponent(moveData[index*4],moveData[index*4 + 1],moveData[index*4 + 2],moveData[index*4 + 3], id)
    }

    return null
}

internal fun moveManagerChangeMoveComponent(id: Long, vx: Float, vy: Float) {
    val transform = moveLookup[id]
    if (transform != null) {
        val index = moveLookup.values.indexOf(transform)
        moveData[index * 4] = transform.x
        moveData[index * 4 + 1] = transform.y
        moveData[index * 4 + 2] = vx
        moveData[index * 4 + 3] = vy
    }
}

internal fun moveManagerRemoveMoveComponent(id: Long) {
    val transform = moveLookup[id]
    if (transform != null) {
        val index = moveLookup.values.indexOf(transform)
        System.arraycopy(moveData, index + 1, moveData, index, moveData.size - index);
    }
    moveLookup.remove(id)
}

internal fun moveManagerSimulate() {
    for (i in 0 until nextIndex /4) {
        moveData[i * 4] += moveData[i * 4 + 2]
        moveData[i * 4 + 1] += moveData[i * 4 + 3]
    }

    var index = 0
    for (i in moveLookup.values) {
        i.x = moveData[index]
        i.y = moveData[index + 1]
        index += 4
    }
}