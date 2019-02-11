package rain.api.manager

import rain.api.components.RenderComponent

typealias AddRenderComponentToRenderer = (renderComponent: RenderComponent) -> Unit
typealias RemoveRenderComponentFromRenderer = (renderComponent: RenderComponent) -> Unit

private val renderManagerRenderComponentsMap = HashMap<Long, ArrayList<RenderComponent>>()

internal lateinit var addNewRenderComponentToRenderer: AddRenderComponentToRenderer
internal lateinit var removeRenderComponentFromRenderer: RemoveRenderComponentFromRenderer

internal fun renderManagerInit(addNewRenderComponent: AddRenderComponentToRenderer, removeRenderComponent: RemoveRenderComponentFromRenderer) {
    addNewRenderComponentToRenderer = addNewRenderComponent
    removeRenderComponentFromRenderer = removeRenderComponent
}

internal fun renderManagerClear() {
    for (list in renderManagerRenderComponentsMap.values) {
        for (component in list) {
            removeRenderComponentFromRenderer(component)
        }
    }

    renderManagerRenderComponentsMap.clear()
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
