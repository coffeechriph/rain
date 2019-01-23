package rain.api.gfx

import rain.api.entity.RenderComponent

// Populated as soon as a new render component should be added to the renderer
internal val renderManagerNewRenderComponents = ArrayList<RenderComponent>()

// Populated as soon as a render component should be removed from the renderer
internal val renderManagerRemoveRenderComponents = ArrayList<RenderComponent>()