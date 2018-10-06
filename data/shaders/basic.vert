#version 420 core

layout(location = 0) in vec2 pos;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 Uv;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
} modelMatrix;

void main() {
    gl_Position = vec4(pos.x, pos.y, 0, 1.0) * modelMatrix.matrix;
    Uv = uv;
}
