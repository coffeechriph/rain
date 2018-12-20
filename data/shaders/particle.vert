#version 420 core

layout(location = 0) in vec3 pos;

layout(set = 1, binding = 0) uniform TextureData {
  vec2 uvScale;
} textureData;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec2 velocity;
    float time;
    float particleLifetime;
    vec4 startColor;
    vec4 endColor;
    float particleSize;
} inData;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout (location = 0) out float alpha;
layout (location = 1) out vec4 color;

void main() {
    float ftime = inData.time + pos.z;
    ftime = mod(ftime, inData.particleLifetime);
    alpha = (inData.particleLifetime - ftime) / inData.particleLifetime;

    float px = pos.x + inData.velocity.x * ftime;
    float py = pos.y + inData.velocity.y * ftime;
    float scale = inData.particleSize * (1.0 - alpha);
    px *= scale;
    py *= scale;
    color = mix(inData.startColor, inData.endColor, alpha);
    gl_Position = sceneData.projectionMatrix * inData.matrix * vec4(px, py, py, 1.0);
}
