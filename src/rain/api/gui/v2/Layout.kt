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

    open fun apply(components: List<Component>, panelX: Float, panelY: Float, panelWidth:
    Float, panelHeight: Float, outlineWidth: Float){}

    fun manage(panel: Panel) {
        val components = ArrayList<Component>()
        for (c in panel.components) {
            if (c.affectedByLayout) {
                components.add(c)
            }
        }

        apply(components, panel.x, panel.y, panel.w, panel.h,
                panel.skin.panelStyle.outlineWidth.toFloat())

        // If we want to have auto scrolls we must place them and reposition the other components
        if (panel.autoScroll) {
            var x = Float.MIN_VALUE
            var y = Float.MIN_VALUE

            for (c in components) {
                if (c.x + c.w > x) {
                    x = c.x + c.w
                }

                if (c.y + c.h > y) {
                    y = c.y + c.h
                }
            }

            var subHeight = 0.0f
            if (x > panel.x + panel.w + 1) {
                if (hScrollBar == null) {
                    hScrollBar = panel.createHScrollBar(0, "")
                    hScrollBar!!.affectedByLayout = false
                }
                subHeight = autoScrollHeight
            }

            var subWidth = 0.0f
            if (y > panel.y + panel.h + 1) {
                if (vScrollBar == null) {
                    vScrollBar = panel.createVScrollBar(0, "")
                    vScrollBar!!.affectedByLayout = false
                }
                subWidth = autoScrollWidth
            }

            apply(components, panel.x, panel.y, panel.w - subWidth, panel.h - subHeight,
                    panel.skin.panelStyle.outlineWidth.toFloat())

            for (component in components) {
                if (hScrollBar != null) {
                    component.x -= hScrollBar!!.value
                }
                if (vScrollBar != null) {
                    component.y -= vScrollBar!!.value
                }
            }

            if (hScrollBar != null) {
                val value = x - (panel.x + panel.w)
                hScrollBar!!.maxScrollAmount = value
                hScrollBar!!.x = panel.x
                hScrollBar!!.y = panel.y + panel.h - autoScrollHeight
                hScrollBar!!.w = panel.w
                hScrollBar!!.h = autoScrollHeight
                hScrollBar!!.affectedByLayout = false
            }

            if (vScrollBar != null) {
                val value = y - (panel.y + panel.h)
                vScrollBar!!.maxScrollAmount = value
                vScrollBar!!.x = panel.x + panel.w - autoScrollWidth
                vScrollBar!!.y = panel.y
                vScrollBar!!.w = autoScrollWidth
                vScrollBar!!.h = panel.w
                vScrollBar!!.affectedByLayout = false
            }
        }
    }
}
