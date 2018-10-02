package rain.vulkan

import java.nio.LongBuffer

internal class DescriptorSet(val descriptorSet: LongBuffer, val layout: Long)
internal class DescriptorPool constructor(val pool: Long, val descriptorSets: List<DescriptorSet>)
