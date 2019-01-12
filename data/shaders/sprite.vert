#version 420 core

layout(location = 0) in vec2 pos;
layout(location = 1) in vec2 uv;
layout(location = 2) in float index;

layout(location = 0) out vec2 Uv;
layout(location = 1) out vec4 Color;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec4 color;
    vec2 textureOffset;
} modelMatrix;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout(set = 0, binding = 1) uniform TextureData {
    vec2 uvScale;
} textureData;

layout(set = 0, binding = 2) uniform samplerBuffer instanceData;

void main() {
    int Index = int(index);
    mat4 iModelMatrix = mat4(texelFetch(instanceData, Index*4),
                            texelFetch(instanceData, Index*4+4),
                            texelFetch(instanceData, Index*4+8),
                            texelFetch(instanceData, Index*4+12));

    gl_Position = sceneData.projectionMatrix * iModelMatrix * vec4(pos.x, pos.y, 1.0, 1.0);
    Uv = vec2(uv.x * textureData.uvScale.x + textureData.uvScale.x * modelMatrix.textureOffset.x,
              uv.y * textureData.uvScale.y + textureData.uvScale.y * modelMatrix.textureOffset.y);
    Color = modelMatrix.color;
}
