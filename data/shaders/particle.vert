#version 420 core

layout(location = 0) in vec3 pos;

layout(set = 1, binding = 0) uniform TextureData {
  vec2 uvScale;
} textureData;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec4 startColor;
    vec4 endColor;
} inData;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout (location = 0) out vec4 color;

void main() {
    color = mix(inData.startColor, inData.endColor, pos.z);
    gl_Position = sceneData.projectionMatrix * inData.matrix * vec4(pos.x, pos.y, 1.0 - pos.z, 1.0);
}