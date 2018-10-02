#version 420 core

layout(location = 0) out vec4 color;
layout(set = 0, binding = 0) uniform UniformColorBuffer {
    vec4 my_color;
} ubo;

layout(set = 1, binding = 0) uniform UniformColorBuffer2 {
    vec4 my_color;
} ubo2;

layout(binding = 1) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
void main() {
    color = texture(texSampler, Uv);
}
