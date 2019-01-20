package rain.api.entity

import java.util.*

// x,y,vx,vy
private val moveLookup = ArrayList<Transform>()
private var moveData = FloatArray(1024)
private var nextIndex = 0

/*
    TODO: We use transform object as reference to connect Entity to the component
    Could we make this more efficient using a ID or index?
 */

fun addMoveComponent(transform: Transform, vx: Float, vy: Float) {
    moveLookup.add(transform)
    if (moveData.size <= nextIndex) {
        val tmp = FloatArray(moveData.size * 2)
        System.arraycopy(moveData, 0, tmp, 0, moveData.size)
        moveData = tmp
    }

    moveData[nextIndex] = transform.x
    moveData[nextIndex+1] = transform.y
    moveData[nextIndex+2] = vx
    moveData[nextIndex+3] = vy
    nextIndex += 4
}

fun changeMoveComponent(transform: Transform, vx: Float, vy: Float) {
    val index = moveLookup.indexOf(transform)
    if (index >= 0) {
        moveData[index*4] = transform.x
        moveData[index*4+1] = transform.y
        moveData[index*4+2] = vx
        moveData[index*4+3] = vy
    }
}

fun removeMoveComponent(transform: Transform) {
    val index = moveLookup.indexOf(transform)
    if (index >= 0) {
        System.arraycopy(moveData, index + 1, moveData, index, moveData.size - index);
    }
    moveLookup.remove(transform)
}

fun simulateMoveManager() {
    for (i in 0 until nextIndex/4) {
        moveData[i*4] += moveData[i*4+2]
        moveData[i*4+1] += moveData[i*4+3]
    }

    var index = 0
    for (i in moveLookup) {
        i.x = moveData[index]
        i.y = moveData[index+1]
        index += 4
    }
}