package rain.api.gfx

import rain.api.entity.RenderComponent

// Populated as soon as a new render component should be added to the renderer
internal val renderManagerNewRenderComponents = ArrayList<RenderComponent>()

// Populated as soon as a render component should be removed from the renderer
internal val renderManagerRemoveRenderComponents = ArrayList<RenderComponent>()

private val renderManagerRenderComponentsMap = HashMap<Long, ArrayList<RenderComponent>>()

internal fun renderManagerAddRenderComponent(entityId: Long, renderComponent: RenderComponent) {
    renderManagerNewRenderComponents.add(renderComponent)

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
            renderManagerRemoveRenderComponents.add(c)
         }
        component.clear()
    }
}

// TODO: Move RenderManager logic which we put in VulkanRenderer into this method instead!
internal fun renderManagerManage() {

}