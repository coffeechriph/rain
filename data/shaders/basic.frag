#version 420 core

layout(location = 0) out vec4 color;
layout(set = 0, binding = 0) uniform UniformColorBuffer {
    vec4 my_color;
} ubo;

layout(set = 1, binding = 0) uniform UniformColorBuffer2 {
    vec4 my_color;
} ubo2;

layout(location = 0) in vec3 Color;
void main() {
    color = vec4(ubo.my_color.r, ubo2.my_color.g, 0.0, 1.0);
}
