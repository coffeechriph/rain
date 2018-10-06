#version 420 core

layout(location = 0) out vec4 color;

layout(binding = 0) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
void main() {
    color = texture(texSampler, Uv);
}
