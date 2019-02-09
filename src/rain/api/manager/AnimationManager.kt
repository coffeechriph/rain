package rain.api.manager

import rain.api.components.Animator

private val animators = ArrayList<Animator>()
private val animatorsMap = HashMap<Long, ArrayList<Animator>>()

internal fun animatorManagerAddAnimatorComponent(entityId: Long, animator: Animator) {
    if (!animators.contains(animator)) {
        animators.add(animator)
    }

    if (animatorsMap.containsKey(entityId)) {
        animatorsMap[entityId]!!.add(animator)
    }
    else {
        val list = ArrayList<Animator>()
        list.add(animator)
        animatorsMap[entityId] = list
    }
}

internal fun animatorManagerRemoveAnimatorByEntity(entityId: Long) {
    val list = animatorsMap[entityId]
    if (list != null) {
        for (c in list) {
            animators.remove(c)
        }

        animatorsMap.remove(entityId)
    }
}

internal fun animatorManagerGetAnimatorByEntity(entityId: Long): ArrayList<Animator>? {
    return animatorsMap[entityId]
}

internal fun animatorManagerSimulate() {
    for (animator in animators) {
        if (animator.animationComplete) {
            if (animator.singlePlay) {
                continue
            }
            else {
                animator.animationComplete = false
            }
        }

        animator.textureTileOffset.y = animator.animation.yPos
        if (animator.animationTime >= 1.0f) {
            animator.animationTime = 0.0f
            animator.animationIndex += 1

            if (animator.animationIndex >= animator.animation.endFrame - animator.animation.startFrame) {
                animator.animationIndex = 0
                if (animator.singlePlay) {
                    animator.animationComplete = true
                }
            }

            animator.textureTileOffset.x = animator.animation.startFrame + animator.animationIndex
        }

        animator.animationTime += animator.animation.speed * (1.0f/60.0f)
    }
}