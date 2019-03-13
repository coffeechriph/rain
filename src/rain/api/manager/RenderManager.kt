package rain.api.manager

import rain.api.components.GuiRenderComponent
import rain.api.components.RenderComponent

typealias AddRenderComponentToRenderer = (renderComponent: RenderComponent) -> Unit
typealias RemoveRenderComponentFromRenderer = (renderComponent: RenderComponent) -> Unit
typealias AddGuiRenderComponentToRenderer = (renderComponent: GuiRenderComponent) -> Unit
typealias RemoveGuiRenderComponentFromRenderer = (renderComponent: GuiRenderComponent) -> Unit

private val renderManagerRenderComponentsMap = HashMap<Long, ArrayList<RenderComponent>>()
private val renderManagerGuiRenderComponents = ArrayList<GuiRenderComponent>()

internal lateinit var addNewRenderComponentToRenderer: AddRenderComponentToRenderer
internal lateinit var removeRenderComponentFromRenderer: RemoveRenderComponentFromRenderer
internal lateinit var addNewGuiRenderComponentToRenderer: AddGuiRenderComponentToRenderer
internal lateinit var removeGuiRenderComponentFromRenderer: RemoveGuiRenderComponentFromRenderer

internal fun renderManagerInit(addNewRenderComponent: AddRenderComponentToRenderer,
                               removeRenderComponent: RemoveRenderComponentFromRenderer,
                               addNewGuiRenderComponent: AddGuiRenderComponentToRenderer,
                               removeGuiRenderComponent: RemoveGuiRenderComponentFromRenderer) {
    addNewRenderComponentToRenderer = addNewRenderComponent
    removeRenderComponentFromRenderer = removeRenderComponent
    addNewGuiRenderComponentToRenderer = addNewGuiRenderComponent
    removeGuiRenderComponentFromRenderer = removeGuiRenderComponent
}

internal fun renderManagerClear() {
    for (list in renderManagerRenderComponentsMap.values) {
        for (component in list) {
            removeRenderComponentFromRenderer(component)
        }
    }

    renderManagerRenderComponentsMap.clear()

    for (component in renderManagerGuiRenderComponents) {
        removeGuiRenderComponentFromRenderer(component)
    }

    renderManagerGuiRenderComponents.clear()
}

internal fun renderManagerAddRenderComponent(entityId: Long, renderComponent: RenderComponent) {
    addNewRenderComponentToRenderer(renderComponent)
    if (renderManagerRenderComponentsMap.containsKey(entityId)) {
        renderManagerRenderComponentsMap[entityId]!!.add(renderComponent)
    }
    else {
        val list = ArrayList<RenderComponent>()
        list.add(renderComponent)
        renderManagerRenderComponentsMap.put(entityId, list)
    }
}

internal fun renderManagerAddGuiRenderComponent(renderComponent: GuiRenderComponent) {
    addNewGuiRenderComponentToRenderer(renderComponent)
    renderManagerGuiRenderComponents.add(renderComponent)
}

internal fun renderManagerGetRenderComponentByEntity(entityId: Long): List<RenderComponent>? {
    return renderManagerRenderComponentsMap[entityId]
}

internal fun renderManagerRemoveRenderComponentByEntity(entityId: Long) {
    val component = renderManagerRenderComponentsMap[entityId]
    if (component != null) {
        for (c in component) {
            removeRenderComponentFromRenderer(c)
         }
        component.clear()
    }
}
