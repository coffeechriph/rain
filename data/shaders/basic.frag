#version 420 core

layout(location = 0) out vec4 color;
layout(set = 0, binding = 0) uniform UniformColorBuffer {
    vec4 my_color;
} ubo;

layout(binding = 0) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
void main() {
    color = texture(texSampler, Uv);
    color *= ubo.my_color;
    //color.r = max(color.r, 0.5) * ubo.my_color.r;
    //color.a = 1.0;
}
