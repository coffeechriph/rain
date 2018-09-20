#version 420 core

layout(location = 0) out vec4 color;

layout(location = 0) in vec3 Color;
void main() {
    color = vec4(Color,1.0);
}