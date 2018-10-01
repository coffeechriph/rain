#version 420 core

layout(location = 0) out vec4 color;
layout(binding = 0) uniform UniformColorBuffer {
    vec4 color;
} ubo;

layout(binding = 1) uniform UniformColorBuffer2 {
    vec4 color;
} ubo2;

layout(location = 0) in vec3 Color;
void main() {
    color = vec4(ubo.color.r, ubo2.color.g, 0.0, 1.0);
}