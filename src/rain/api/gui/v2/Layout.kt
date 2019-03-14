package rain.api.gui.v2

/*
    TODO: Improve managing of components that should not be affected by layout!
    We want to be able to add components to the panels that should not be affected by the layout
    instead of having to do these ugly hacks we're seeing here.

    The current way also imposes issues when creating your own layout. As there's more management
    involved
    Idea:
        Layout should supply with a manage method which takes in a List<Component>.
        This list will only include components that are supposed to be managed.
        Implementors of this method can then safely position them as desired
        The layout can then allow the other components to be sized & positioned as is
        The method will also take in a width & height component which tells the implementor the
        maximum with & height to position components at, this will not be the true panel size
        as none managed components may fill in the remaining area.
 */
open class Layout {
    var autoScrollWidth = 30.0f
    var autoScrollHeight = 30.0f
    protected var vScrollBar: VScrollBar? = null
    protected var hScrollBar: HScrollBar? = null
    internal var fillableWidth = 0.0f
    internal var fillableHeight = 0.0f

    protected fun resetScrollbars(panel: Panel) {
        fillableWidth = panel.w
        fillableHeight = panel.h

        // We must remove the auto scrollbars before managing layout
        if (hScrollBar != null) {
            panel.removeComponent(hScrollBar!!)
        }

        if (vScrollBar != null) {
            panel.removeComponent(vScrollBar!!)
        }
    }

    protected fun recreateScrollbars(panel: Panel, x: Float, y: Float) {
        if (panel.autoScroll) {
            if (x > panel.x + panel.w) {
                val value = x - (panel.x + panel.w)
                if (hScrollBar == null) {
                    hScrollBar = panel.createHScrollBar(value, "")
                }
                else {
                    hScrollBar!!.maxScrollAmount = value
                    panel.components.add(hScrollBar!!)
                    panel.texts.add(hScrollBar!!.text)
                }
            }

            if (y > panel.y + panel.h) {
                val value = y - (panel.y + panel.h)
                if (vScrollBar == null) {
                    vScrollBar = panel.createVScrollBar(value, "")
                }
                else {
                    vScrollBar!!.maxScrollAmount = value
                    panel.components.add(vScrollBar!!)
                    panel.texts.add(vScrollBar!!.text)
                }
            }

            if (hScrollBar != null) {
                hScrollBar!!.x = panel.x
                hScrollBar!!.y = panel.y + 5 + panel.h - autoScrollHeight
                hScrollBar!!.w = panel.w
                hScrollBar!!.h = autoScrollHeight
            }

            if (vScrollBar != null) {
                vScrollBar!!.x = panel.x + 5 + panel.w - autoScrollWidth
                vScrollBar!!.y = panel.y
                vScrollBar!!.w = autoScrollWidth
                vScrollBar!!.h = panel.w
            }
        }
    }

    open fun manage(panel: Panel) {
    }

    open fun updateFillableArea(panel: Panel) {

    }
}
