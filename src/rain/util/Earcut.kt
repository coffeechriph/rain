package rain.util

import java.util.*

internal class Node(// vertex index in coordinates array
        var i: Int, // vertex coordinates
        var x: Double, var y: Double) {

    // z-order curve value
    var z = java.lang.Double.MIN_VALUE

    // indicates whether this is a steiner point
    var steiner = false

    // previous and next vertex nodes in a polygon ring
    var prev: Node? = null
    var next: Node? = null

    // previous and next nodes in z-order
    var prevZ: Node? = null
    var nextZ: Node? = null
}

class Earcut {

    private var hasHoles: Boolean = false
    private var outerLen: Int = 0
    private var outerNode: Node? = null
    private var triangles: MutableList<Int>? = null

    fun triangulate(data: DoubleArray, holeIndices: IntArray?, dim: Int): IntArray {
        hasHoles = holeIndices != null && holeIndices.size > 0
        outerLen = if (hasHoles) holeIndices!!.size * dim else data.size
        outerNode = linkedList(data, 0, outerLen, dim, true)
        triangles = ArrayList()

        if (outerNode == null) return intArrayOf()

        var minX = 0.0
        var minY = 0.0
        var maxX: Double
        var maxY: Double
        var x: Double
        var y: Double
        var invSize = 0

        if (hasHoles) outerNode = eliminateHoles(data, holeIndices!!, outerNode, dim)

        // if the shape is not too simple, we'll use z-order curve hash later; calculate polygon bbox
        if (data.size > 80 * dim) {
            maxX = data[0]
            minX = maxX
            maxY = data[1]
            minY = maxY

            var i = dim
            while (i < outerLen) {
                x = data[i]
                y = data[i + 1]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                i += dim
            }

            // minX, minY and invSize are later used to transform coords into integers for z-order calculation
            invSize = Math.max(maxX - minX, maxY - minY).toInt()
            invSize = if (invSize != 0) 1 / invSize else 0
        }

        earcutLinked(outerNode, triangles!!, dim, minX, minY, invSize, 0)

        val indices = IntArray(triangles!!.size)
        for (i in indices.indices) {
            indices[i] = triangles!![i]
        }

        return indices
    }

    internal fun linkedList(data: DoubleArray, start: Int, end: Int, dim: Int, clockwise: Boolean): Node? {
        var i: Int
        var last: Node? = null

        if (clockwise == signedArea(data, start, end, dim) > 0) {
            i = start
            while (i < end) {
                last = insertNode(i, data[i], data[i + 1], last)
                i += dim
            }
        } else {
            i = end - dim
            while (i >= start) {
                last = insertNode(i, data[i], data[i + 1], last)
                i -= dim
            }
        }

        if (last != null && equals(last, last.next)) {
            removeNode(last)
            last = last.next
        }

        return last
    }

    internal fun removeNode(p: Node?) {
        p!!.next!!.prev = p.prev
        p.prev!!.next = p.next

        if (p.prevZ != null) p.prevZ!!.nextZ = p.nextZ
        if (p.nextZ != null) p.nextZ!!.prevZ = p.prevZ
    }

    // create a node and optionally link it with previous one (in a circular doubly linked list)
    internal fun insertNode(i: Int, x: Double, y: Double, last: Node?): Node {
        val p = Node(i, x, y)

        if (last == null) {
            p.prev = p
            p.next = p

        } else {
            p.next = last.next
            p.prev = last
            last.next!!.prev = p
            last.next = p
        }

        return p
    }

    // eliminate colinear or duplicate points
    internal fun filterPoints(start: Node?, end: Node?): Node? {
        var end = end
        if (start == null) return start
        if (end == null) end = start

        var p = start
        var again: Boolean
        do {
            again = false

            if (!p!!.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0.0)) {
                removeNode(p)
                end = p.prev
                p = end
                if (p === p!!.next) break
                again = true

            } else {
                p = p.next
            }
        } while (again || p !== end)

        return end
    }

    // main ear slicing loop which triangulates a polygon (given as a linked list)
    internal fun earcutLinked(ear: Node?, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, invSize: Int, pass: Int) {
        var ear: Node? = ear ?: return

        // interlink polygon nodes in z-order
        if (pass == 0 && invSize != 0) indexCurve(ear!!, minX, minY, invSize)

        var stop = ear
        var prev: Node?
        var next: Node?

        // iterate through ears, slicing them one by one
        while (ear!!.prev !== ear!!.next) {
            prev = ear!!.prev
            next = ear.next

            if (if (invSize != 0) isEarHashed(ear, minX, minY, invSize) else isEar(ear)) {
                // cut off the triangle
                triangles.add(prev!!.i / dim)
                triangles.add(ear.i / dim)
                triangles.add(next!!.i / dim)

                removeNode(ear)

                // skipping the next vertex leads to less sliver triangles
                ear = next.next
                stop = next.next

                continue
            }

            ear = next

            // if we looped through the whole remaining polygon and can't find any more ears
            if (ear === stop) {
                // try filtering points and slicing again
                if (pass == 0) {
                    earcutLinked(filterPoints(ear, null), triangles, dim, minX, minY, invSize, 1)

                    // if this didn't work, try curing all small self-intersections locally
                } else if (pass == 1) {
                    ear = cureLocalIntersections(ear!!, triangles, dim)
                    earcutLinked(ear, triangles, dim, minX, minY, invSize, 2)

                    // as a last resort, try splitting the remaining polygon into two
                } else if (pass == 2) {
                    splitEarcut(ear!!, triangles, dim, minX, minY, invSize)
                }

                break
            }
        }
    }

    // check whether a polygon node forms a valid ear with adjacent nodes
    internal fun isEar(ear: Node): Boolean {
        val a = ear.prev
        val c = ear.next

        if (area(a, ear, c) >= 0) return false // reflex, can't be an ear

        // now make sure we don't have other points inside the potential ear
        var p = ear.next!!.next

        while (p !== ear.prev) {
            if (pointInTriangle(a!!.x, a.y, ear.x, ear.y, c!!.x, c.y, p!!.x, p.y) && area(p.prev, p, p.next) >= 0)
                return false
            p = p.next
        }

        return true
    }

    internal fun isEarHashed(ear: Node, minX: Double, minY: Double, invSize: Int): Boolean {
        val a = ear.prev
        val c = ear.next

        if (area(a, ear, c) >= 0) return false // reflex, can't be an ear

        // triangle bbox; min & max are calculated like this for speed
        val minTX = if (a!!.x < ear.x) if (a.x < c!!.x) a.x else c.x else if (ear.x < c!!.x) ear.x else c.x
        val minTY = if (a.y < ear.y) if (a.y < c.y) a.y else c.y else if (ear.y < c.y) ear.y else c.y
        val maxTX = if (a.x > ear.x) if (a.x > c.x) a.x else c.x else if (ear.x > c.x) ear.x else c.x
        val maxTY = if (a.y > ear.y) if (a.y > c.y) a.y else c.y else if (ear.y > c.y) ear.y else c.y

        // z-order range for the current triangle bbox;
        val minZ = zOrder(minTX.toInt(), minTY.toInt(), minX, minY, invSize).toDouble()
        val maxZ = zOrder(maxTX.toInt(), maxTY.toInt(), minX, minY, invSize).toDouble()

        var p = ear.prevZ
        var n = ear.nextZ

        // look for points inside the triangle in both directions
        while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
            if (p !== ear.prev && p !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) &&
                area(p.prev, p, p.next) >= 0)
                return false
            p = p.prevZ

            if (n !== ear.prev && n !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, n.x, n.y) &&
                area(n.prev, n, n.next) >= 0)
                return false
            n = n.nextZ
        }

        // look for remaining points in decreasing z-order
        while (p != null && p.z >= minZ) {
            if (p !== ear.prev && p !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) &&
                area(p.prev, p, p.next) >= 0)
                return false
            p = p.prevZ
        }

        // look for remaining points in increasing z-order
        while (n != null && n.z <= maxZ) {
            if (n !== ear.prev && n !== ear.next &&
                pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, n.x, n.y) &&
                area(n.prev, n, n.next) >= 0)
                return false
            n = n.nextZ
        }

        return true
    }

    // go through all polygon nodes and cure small local self-intersections
    internal fun cureLocalIntersections(start: Node, triangles: MutableList<Int>, dim: Int): Node {
        var start = start
        var p: Node? = start
        do {
            val a = p!!.prev
            val b = p.next!!.next

            if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {

                triangles.add(a!!.i / dim)
                triangles.add(p.i / dim)
                triangles.add(b!!.i / dim)

                // remove two nodes involved
                removeNode(p)
                removeNode(p.next)

                start = b
                p = start
            }
            p = p.next
        } while (p !== start)

        return p
    }

    // try splitting polygon into two and triangulate them independently
    internal fun splitEarcut(start: Node, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, invSize: Int) {
        // look for a valid diagonal that divides the polygon into two
        var a: Node? = start
        do {
            var b = a!!.next!!.next
            while (b !== a!!.prev) {
                if (a!!.i != b!!.i && isValidDiagonal(a, b)) {
                    // split the polygon in two by the diagonal
                    var c: Node? = splitPolygon(a, b)

                    // filter colinear points around the cuts
                    a = filterPoints(a, a.next)
                    c = filterPoints(c, c!!.next)

                    // run earcut on each half
                    earcutLinked(a, triangles, dim, minX, minY, invSize, 0)
                    earcutLinked(c, triangles, dim, minX, minY, invSize, 0)
                    return
                }
                b = b.next
            }
            a = a!!.next
        } while (a !== start)
    }

    // link every hole into the outer loop, producing a single-ring polygon without holes
    internal fun eliminateHoles(data: DoubleArray, holeIndices: IntArray, outerNode: Node?, dim: Int): Node? {
        var outerNode = outerNode
        val queue = ArrayList<Node>()
        var i: Int
        val len: Int
        var start: Int
        var end: Int
        var list: Node?

        i = 0
        len = holeIndices.size
        while (i < len) {
            start = holeIndices[i] * dim
            end = if (i < len - 1) holeIndices[i + 1] * dim else data.size
            list = linkedList(data, start, end, dim, false)
            if (list === list!!.next) list!!.steiner = true
            queue.add(getLeftmost(list!!))
            i++
        }

        Collections.sort(queue) { a, b -> compareX(a, b) }

        // process holes from left to right
        i = 0
        while (i < queue.size) {
            eliminateHole(queue[i], outerNode)
            outerNode = filterPoints(outerNode, outerNode!!.next)
            i++
        }

        return outerNode
    }

    internal fun compareX(a: Node, b: Node): Int {
        return (a.x - b.x).toInt()
    }

    // find a bridge between vertices that connects hole with an outer ring and and link it
    internal fun eliminateHole(hole: Node, outerNode: Node?) {
        var outerNode = outerNode
        outerNode = findHoleBridge(hole, outerNode)
        if (outerNode != null) {
            val b = splitPolygon(outerNode, hole)
            filterPoints(b, b.next)
        }
    }

    // David Eberly's algorithm for finding a bridge between hole and outer polygon
    internal fun findHoleBridge(hole: Node, outerNode: Node?): Node? {
        var p = outerNode
        val hx = hole.x
        val hy = hole.y
        var qx = java.lang.Double.MIN_VALUE
        var m: Node? = null

        // find a segment intersected by a ray from the hole's leftmost point to the left;
        // segment's endpoint with lesser x will be potential connection point
        do {
            if (hy <= p!!.y && hy >= p.next!!.y && p.next!!.y != p.y) {
                val x = p.x + (hy - p.y) * (p.next!!.x - p.x) / (p.next!!.y - p.y)
                if (x <= hx && x > qx) {
                    qx = x
                    if (x == hx) {
                        if (hy == p.y) return p
                        if (hy == p.next!!.y) return p.next
                    }
                    m = if (p.x < p.next!!.x) p else p.next
                }
            }
            p = p.next
        } while (p !== outerNode)

        if (m == null) return null

        if (hx == qx) return m.prev // hole touches outer segment; pick lower endpoint

        // look for points inside the triangle of hole point, segment intersection and endpoint;
        // if there are no points found, we have a valid connection;
        // otherwise choose the point of the minimum angle with the ray as connection point

        val stop = m
        val mx = m.x
        val my = m.y
        var tanMin = java.lang.Double.MAX_VALUE
        var tan: Double

        p = m.next

        while (p !== stop) {
            if (hx >= p!!.x && p.x >= mx && hx != p.x &&
                pointInTriangle(if (hy < my) hx else qx, hy, mx, my, if (hy < my) qx else hx, hy, p.x, p.y)) {

                tan = Math.abs(hy - p.y) / (hx - p.x) // tangential

                if ((tan < tanMin || tan == tanMin && p.x > m!!.x) && locallyInside(p, hole)) {
                    m = p
                    tanMin = tan
                }
            }

            p = p.next
        }

        return m
    }

    // link two polygon vertices with a bridge; if the vertices belong to the same ring, it splits polygon into two;
    // if one belongs to the outer ring and another to a hole, it merges it into a single ring
    internal fun splitPolygon(a: Node, b: Node): Node {
        val a2 = Node(a.i, a.x, a.y)
        val b2 = Node(b.i, b.x, b.y)
        val an = a.next
        val bp = b.prev

        a.next = b
        b.prev = a

        a2.next = an
        an!!.prev = a2

        b2.next = a2
        a2.prev = b2

        bp!!.next = b2
        b2.prev = bp

        return b2
    }

    // check if the middle point of a polygon diagonal is inside the polygon
    internal fun middleInside(a: Node, b: Node): Boolean {
        var p: Node? = a
        var inside = false
        val px = (a.x + b.x) / 2
        val py = (a.y + b.y) / 2

        do {
            if (p!!.y > py != p.next!!.y > py && p.next!!.y != p.y &&
                px < (p.next!!.x - p.x) * (py - p.y) / (p.next!!.y - p.y) + p.x)
                inside = !inside
            p = p.next
        } while (p !== a)

        return inside
    }

    // check if a polygon diagonal is locally inside the polygon
    internal fun locallyInside(a: Node?, b: Node?): Boolean {
        return if (area(a!!.prev, a, a.next) < 0)
            area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0
        else
            area(a, b, a.prev) < 0 || area(a, a.next, b) < 0
    }

    // check if a polygon diagonal intersects any polygon segments
    internal fun intersectsPolygon(a: Node, b: Node): Boolean {
        var p: Node? = a
        do {
            if (p!!.i != a.i && p.next!!.i != a.i && p.i != b.i && p.next!!.i != b.i && intersects(p, p.next, a, b))
                return true
            p = p.next
        } while (p !== a)

        return false
    }

    // check if two segments intersect
    internal fun intersects(p1: Node?, q1: Node?, p2: Node?, q2: Node?): Boolean {
        return if (equals(p1, q1) && equals(p2, q2) || equals(p1, q2) && equals(p2, q1)) true else area(p1, q1, p2) > 0 != area(p1, q1, q2) > 0 && area(p2, q2, p1) > 0 != area(p2, q2, q1) > 0

    }

    // z-order of a point given coords and inverse of the longer side of data bbox
    internal fun zOrder(x: Int, y: Int, minX: Double, minY: Double, invSize: Int): Int {
        var x = x
        var y = y
        // coords are transformed into non-negative 15-bit integer range
        x = (32767.0 * (x - minX) * invSize.toDouble()).toInt()
        y = (32767.0 * (y - minY) * invSize.toDouble()).toInt()

        x = x or (x shl 8) and 0x00FF00FF
        x = x or (x shl 4) and 0x0F0F0F0F
        x = x or (x shl 2) and 0x33333333
        x = x or (x shl 1) and 0x55555555

        y = y or (y shl 8) and 0x00FF00FF
        y = y or (y shl 4) and 0x0F0F0F0F
        y = y or (y shl 2) and 0x33333333
        y = y or (y shl 1) and 0x55555555

        return x or (y shl 1)
    }

    // find the leftmost node of a polygon ring
    internal fun getLeftmost(start: Node): Node {
        var p: Node? = start
        var leftmost = start
        do {
            if (p!!.x < leftmost.x) leftmost = p
            p = p.next
        } while (p !== start)

        return leftmost
    }

    // check if a point lies within a convex triangle
    internal fun pointInTriangle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, px: Double, py: Double): Boolean {
        return (cx - px) * (ay - py) - (ax - px) * (cy - py) >= 0 &&
               (ax - px) * (by - py) - (bx - px) * (ay - py) >= 0 &&
               (bx - px) * (cy - py) - (cx - px) * (by - py) >= 0
    }

    // check if a diagonal between two polygon nodes is valid (lies in polygon interior)
    internal fun isValidDiagonal(a: Node, b: Node): Boolean {
        return a.next!!.i != b.i && a.prev!!.i != b.i && !intersectsPolygon(a, b) &&
               locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b)
    }

    // signed area of a triangle
    internal fun area(p: Node?, q: Node?, r: Node?): Double {
        return (q!!.y - p!!.y) * (r!!.x - q.x) - (q.x - p.x) * (r.y - q.y)
    }

    // check if two points are equal
    internal fun equals(p1: Node?, p2: Node?): Boolean {
        return p1!!.x == p2!!.x && p1.y == p2.y
    }

    // interlink polygon nodes in z-order
    internal fun indexCurve(start: Node, minX: Double, minY: Double, invSize: Int) {
        var p: Node? = start
        do {
            if (p!!.z == java.lang.Double.MIN_VALUE) p.z = zOrder(p.x.toInt(), p.y.toInt(), minX, minY, invSize).toDouble()
            p.prevZ = p.prev
            p.nextZ = p.next
            p = p.next
        } while (p !== start)

        p.prevZ!!.nextZ = null
        p.prevZ = null

        sortLinked(p)
    }

    // Simon Tatham's linked list merge sort algorithm
    // http://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html
    internal fun sortLinked(list: Node?): Node? {
        var list = list
        var i: Int
        var p: Node?
        var q: Node?
        var e: Node
        var tail: Node?
        var numMerges: Int
        var pSize: Int
        var qSize: Int
        var inSize = 1

        do {
            p = list
            list = null
            tail = null
            numMerges = 0

            while (p != null) {
                numMerges++
                q = p
                pSize = 0
                i = 0
                while (i < inSize) {
                    pSize++
                    q = q!!.nextZ
                    if (q == null) break
                    i++
                }
                qSize = inSize

                while (pSize > 0 || qSize > 0 && q != null) {

                    if (pSize != 0 && (qSize == 0 || q == null || p!!.z <= q.z)) {
                        e = p!!
                        p = p.nextZ
                        pSize--
                    } else {
                        e = q!!
                        q = q.nextZ
                        qSize--
                    }

                    if (tail != null)
                        tail.nextZ = e
                    else
                        list = e

                    e.prevZ = tail
                    tail = e
                }

                p = q
            }

            tail!!.nextZ = null
            inSize *= 2

        } while (numMerges > 1)

        return list
    }

    internal fun signedArea(data: DoubleArray, start: Int, end: Int, dim: Int): Double {
        var sum = 0.0
        var i = start
        var j = end - dim
        while (i < end) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1])
            j = i
            i += dim
        }
        return sum
    }
}