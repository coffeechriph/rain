package rain.api.gui.v2

class WindowLayout(private val componentLayout: Layout): Layout() {
    override fun apply(components: List<Component>, panelX: Float, panelY: Float, panelWidth: Float, panelHeight: Float, outlineWidth: Float) {
        val closeButton = components[0]
        val yOffset = closeButton.text.parentPanel.font.fontHeight / 4.0f + outlineWidth
        closeButton.w = 20.0f
        closeButton.h = 20.0f
        closeButton.x = panelX + yOffset
        closeButton.y = panelY + yOffset

        val titleLabel = components[1]
        titleLabel.w = titleLabel.text.w
        titleLabel.h = 20.0f
        titleLabel.x = panelX + panelWidth/2 - titleLabel.w/2
        titleLabel.y = panelY + yOffset
        titleLabel.text.textAlign = TextAlign.LEFT
        val subComponents = components.subList(2, components.size)
        componentLayout.apply(subComponents, panelX, panelY + yOffset*2 + closeButton.h, panelWidth, panelHeight, outlineWidth)
    }
}
